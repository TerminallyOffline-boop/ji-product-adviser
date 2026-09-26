package com.jitelecom.productadviser.data.importexport

import android.content.Context
import androidx.room.withTransaction
import com.jitelecom.productadviser.BuildConfig
import com.jitelecom.productadviser.data.local.*
import com.jitelecom.productadviser.domain.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import okio.buffer
import okio.source
import java.io.ByteArrayInputStream
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImportExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val packageAdapter = moshi.adapter(DatabasePackage::class.java).indent("  ")

    suspend fun exportJson(): ByteArray {
        val metadata = db.metadataDao().getAll().associate { it.key to it.value }
        val pkg = DatabasePackage(
            metadata = PackageMetadata(
                databaseVersion = metadata["databaseVersion"] ?: BuildConfig.DATABASE_VERSION,
                exportedAt = Instant.now().toString(),
                releaseNotes = metadata["releaseNotes"].orEmpty()
            ),
            processors = db.hardwareDao().getProcessors(), gpus = db.hardwareDao().getGpus(),
            products = db.productDao().getAllOnce().map { it.product }, software = db.softwareDao().getAllOnce(),
            requirements = db.requirementDao().getAll(), analytics = db.analyticsDao().getAll()
        )
        return requireNotNull(packageAdapter.toJson(pkg)).toByteArray()
    }

    suspend fun createSafetyBackup(): File {
        val backupDir = File(context.filesDir, "database_backups").apply { mkdirs() }
        val file = File(backupDir, "before_import_${System.currentTimeMillis()}.json")
        file.writeBytes(exportJson())
        backupDir.listFiles { candidate -> candidate.isFile && candidate.name.startsWith("before_import_") }
            ?.sortedByDescending(File::lastModified)?.drop(5)?.forEach(File::delete)
        return file
    }

    fun preview(bytes: ByteArray, fileName: String): Pair<DatabasePackage?, ImportPreview> = try {
        val pkg = when {
            fileName.endsWith(".zip", true) -> parseZip(bytes)
            else -> packageAdapter.fromJson(bytes.toString(Charsets.UTF_8))
        } ?: return null to ImportPreview(errors = listOf(ImportIssue(fileName, "The package is empty or unreadable.")))
        pkg to validate(pkg)
    } catch (error: Exception) {
        null to ImportPreview(errors = listOf(ImportIssue(fileName, error.message ?: "Invalid package.")))
    }

    suspend fun importPackage(bytes: ByteArray, fileName: String, expectedSha256: String? = null): ImportResult {
        if (!expectedSha256.isNullOrBlank() && !sha256(bytes).equals(expectedSha256, true)) {
            return ImportResult(false, "SHA-256 checksum mismatch. Existing data was not changed.", ImportPreview(errors=listOf(ImportIssue(fileName, "Checksum mismatch"))))
        }
        val (pkg, preview) = preview(bytes, fileName)
        if (pkg == null || !preview.canImport) return ImportResult(false, "Validation failed. Existing data was not changed.", preview)
        createSafetyBackup()
        return try {
            db.withTransaction {
                db.productDao().deleteAll()
                db.requirementDao().deleteAll()
                db.softwareDao().deleteAll()
                db.hardwareDao().deleteProcessors()
                db.hardwareDao().deleteGpus()
                db.analyticsDao().deleteAll()
                db.metadataDao().deleteAll()
                db.hardwareDao().insertProcessors(pkg.processors)
                db.hardwareDao().insertGpus(pkg.gpus)
                db.softwareDao().insertAll(pkg.software)
                db.requirementDao().insertAll(pkg.requirements)
                db.productDao().insertAll(pkg.products)
                db.analyticsDao().insertAll(pkg.analytics)
                db.metadataDao().putAll(listOf(
                    DatabaseMetadataEntity("databaseVersion", pkg.metadata.databaseVersion),
                    DatabaseMetadataEntity("lastUpdated", pkg.metadata.exportedAt),
                    DatabaseMetadataEntity("releaseNotes", pkg.metadata.releaseNotes)
                ))
            }
            ImportResult(true, "Imported database ${pkg.metadata.databaseVersion}.", preview)
        } catch (error: Exception) {
            ImportResult(false, "Import failed and was rolled back: ${error.message}", preview)
        }
    }

    fun previewProductCsv(text: String, processorIds: Set<Long>, gpuIds: Set<Long>): Pair<List<ProductEntity>, ImportPreview> {
        val rows = parseCsv(text)
        if (rows.isEmpty()) return emptyList<ProductEntity>() to ImportPreview(errors=listOf(ImportIssue("CSV", "No records found.")))
        val header = rows.first().map { it.trim() }
        val required = setOf("sku", "brand", "model", "category", "price", "processorId", "gpuId", "ramGB", "storageGB")
        val missing = required - header.toSet()
        if (missing.isNotEmpty()) return emptyList<ProductEntity>() to ImportPreview(errors=listOf(ImportIssue("Header", "Missing columns: ${missing.joinToString()}.")))
        val errors = mutableListOf<ImportIssue>()
        val products: List<ProductEntity> = rows.drop(1).mapIndexedNotNull { index, values ->
            val row = header.zip(values + List((header.size - values.size).coerceAtLeast(0)) { "" }).toMap()
            val record = "Row ${index + 2}"
            val sku = row["sku"].orEmpty().trim(); val brand = row["brand"].orEmpty().trim(); val model = row["model"].orEmpty().trim()
            val price = row["price"]?.toDoubleOrNull(); val ram = row["ramGB"]?.toIntOrNull(); val storage = row["storageGB"]?.toIntOrNull()
            val cpu = row["processorId"]?.toLongOrNull(); val gpu = row["gpuId"]?.toLongOrNull()
            val category = runCatching { com.jitelecom.productadviser.domain.model.ProductCategory.valueOf(row["category"].orEmpty().uppercase()) }.getOrNull()
            when {
                sku.isBlank() || brand.isBlank() || model.isBlank() -> { errors += ImportIssue(record, "SKU, brand and model are required."); null }
                price == null || price < 0 -> { errors += ImportIssue(record, "Price is invalid."); null }
                ram == null || ram <= 0 -> { errors += ImportIssue(record, "RAM is missing or invalid."); null }
                storage == null || storage <= 0 -> { errors += ImportIssue(record, "Storage is missing or invalid."); null }
                cpu == null || cpu !in processorIds -> { errors += ImportIssue(record, "Processor ID does not exist."); null }
                gpu == null || gpu !in gpuIds -> { errors += ImportIssue(record, "GPU ID does not exist."); null }
                category == null -> { errors += ImportIssue(record, "Category is invalid."); null }
                else -> ProductEntity(sku=sku, brand=brand, model=model, category=category, price=price, processorId=cpu, gpuId=gpu, ramGB=ram, storageGB=storage)
            }
        }
        val duplicates = products.groupBy { it.sku.lowercase() }.filterValues { it.size > 1 }.keys
        duplicates.forEach { errors += ImportIssue(it, "Duplicate SKU in file.") }
        return products to ImportPreview(productCount=products.size, errors=errors)
    }

    suspend fun importProductCsv(bytes: ByteArray): ImportResult {
        val processors = db.hardwareDao().getProcessors().map { it.id }.toSet()
        val gpus = db.hardwareDao().getGpus().map { it.id }.toSet()
        val (products, basicPreview) = previewProductCsv(bytes.toString(Charsets.UTF_8), processors, gpus)
        val existing = db.productDao().getAllOnce().map { it.product.sku.lowercase() }.toSet()
        val duplicateErrors = products.filter { it.sku.lowercase() in existing }.map { ImportIssue(it.sku, "SKU already exists in the local database.") }
        val preview = basicPreview.copy(errors = basicPreview.errors + duplicateErrors)
        if (!preview.canImport) return ImportResult(false, "CSV validation failed. Existing data was not changed.", preview)
        createSafetyBackup()
        return try {
            db.withTransaction { db.productDao().insertAll(products) }
            ImportResult(true, "Imported ${products.size} product records from CSV.", preview)
        } catch (error: Exception) {
            ImportResult(false, "CSV import failed and was rolled back: ${error.message}", preview)
        }
    }

    private fun validate(pkg: DatabasePackage): ImportPreview {
        val errors = mutableListOf<ImportIssue>(); val warnings = mutableListOf<ImportIssue>()
        if (pkg.metadata.formatVersion != 1) errors += ImportIssue("metadata", "Unsupported formatVersion ${pkg.metadata.formatVersion}.")
        if (compareVersions(pkg.metadata.minimumAppVersion, BuildConfig.VERSION_NAME) > 0) errors += ImportIssue("metadata", "Requires app ${pkg.metadata.minimumAppVersion} or later.")
        duplicateValues(pkg.products.map { it.sku.lowercase() }).forEach { errors += ImportIssue("products", "Duplicate SKU: $it") }
        duplicateValues(pkg.software.map { "${it.name}|${it.version}|${it.platform}".lowercase() }).forEach { errors += ImportIssue("software", "Duplicate software version: $it") }
        duplicateValues(pkg.requirements.map { "${it.softwareId}|${it.type}|${it.platform.trim()}".lowercase() }).forEach { errors += ImportIssue("requirements", "Duplicate software/type/platform requirement: $it") }
        val processorIds = pkg.processors.map { it.id }.toSet(); val gpuIds = pkg.gpus.map { it.id }.toSet(); val softwareIds = pkg.software.map { it.id }.toSet()
        pkg.products.forEach {
            if (it.sku.isBlank() || it.brand.isBlank() || it.model.isBlank()) errors += ImportIssue(it.sku, "SKU, brand and model are required.")
            if (it.price < 0) errors += ImportIssue(it.sku, "Price cannot be negative.")
            if (it.ramGB == null) errors += ImportIssue(it.sku, "RAM is required.")
            if (it.processorId != null && it.processorId !in processorIds) errors += ImportIssue(it.sku, "Invalid processor reference.")
            if (it.gpuId != null && it.gpuId !in gpuIds) errors += ImportIssue(it.sku, "Invalid GPU reference.")
            if (it.sourceUrl.isNullOrBlank()) warnings += ImportIssue(it.sku, "Official source is missing.")
            if (it.verificationStatus == VerificationStatus.VERIFIED && it.sourceUrl?.startsWith("https://", true) != true) errors += ImportIssue(it.sku, "Verified products require an HTTPS source URL.")
        }
        pkg.requirements.forEach {
            if (it.softwareId !in softwareIds) errors += ImportIssue("requirement ${it.id}", "Invalid software reference.")
            if (listOfNotNull(it.minimumRamGB, it.minimumStorageGB, it.minimumCpuTier, it.minimumGpuTier).any { value -> value <= 0 }) errors += ImportIssue("requirement ${it.id}", "Stored requirement values must be greater than zero.")
            if (it.minimumVramGB != null && it.minimumVramGB <= 0) errors += ImportIssue("requirement ${it.id}", "VRAM must be greater than zero.")
            if (it.minimumCpuTier != null && it.minimumCpuTier !in 1..7) errors += ImportIssue("requirement ${it.id}", "CPU tier must be from 1 to 7.")
            if (it.minimumGpuTier != null && it.minimumGpuTier !in 1..7) errors += ImportIssue("requirement ${it.id}", "GPU tier must be from 1 to 7.")
            if (it.acceptedProcessorIds.any { id -> id !in processorIds }) errors += ImportIssue("requirement ${it.id}", "Approved processor list contains an invalid reference.")
            if (it.acceptedGpuIds.any { id -> id !in gpuIds }) errors += ImportIssue("requirement ${it.id}", "Approved GPU list contains an invalid reference.")
            val app = pkg.software.firstOrNull { app -> app.id == it.softwareId }
            if (it.verificationStatus == VerificationStatus.VERIFIED && app?.requirementsSourceUrl?.startsWith("https://", true) != true) errors += ImportIssue("requirement ${it.id}", "Verified requirements need an HTTPS source URL on the software record.")
            if (it.platform.isNotBlank() && platformFamilies(it.platform).intersect(platformFamilies(app?.platform)).isEmpty()) errors += ImportIssue("requirement ${it.id}", "Requirement platform is not supported by its software record.")
            val hasMeaningfulValue = it.minimumRamGB != null || it.minimumStorageGB != null || it.minimumCpuTier != null || it.minimumGpuTier != null ||
                it.minimumVramGB != null || !it.requiredArchitecture.isNullOrBlank() || it.supportedOperatingSystems.isNotEmpty() ||
                it.requiredFeatures.isNotEmpty() || it.acceptedProcessorIds.isNotEmpty() || it.acceptedGpuIds.isNotEmpty()
            if (!hasMeaningfulValue) errors += ImportIssue("requirement ${it.id}", "At least one meaningful requirement value is required.")
        }
        pkg.requirements.groupBy { it.softwareId to it.platform.trim().lowercase() }.values.forEach { group ->
            val minimum = group.firstOrNull { it.type == RequirementType.MINIMUM } ?: return@forEach
            val recommended = group.firstOrNull { it.type == RequirementType.RECOMMENDED } ?: return@forEach
            val comparisons = listOf(
                "RAM" to (minimum.minimumRamGB?.toDouble() to recommended.minimumRamGB?.toDouble()),
                "storage" to (minimum.minimumStorageGB?.toDouble() to recommended.minimumStorageGB?.toDouble()),
                "CPU tier" to (minimum.minimumCpuTier?.toDouble() to recommended.minimumCpuTier?.toDouble()),
                "GPU tier" to (minimum.minimumGpuTier?.toDouble() to recommended.minimumGpuTier?.toDouble()),
                "VRAM" to (minimum.minimumVramGB to recommended.minimumVramGB)
            )
            comparisons.forEach { (label, values) ->
                val (minimumValue, recommendedValue) = values
                if (minimumValue != null && recommendedValue != null && recommendedValue < minimumValue) errors += ImportIssue("requirement ${recommended.id}", "Recommended $label cannot be below minimum $label.")
            }
        }
        return ImportPreview(pkg.products.size, pkg.processors.size, pkg.gpus.size, pkg.software.size, pkg.requirements.size, pkg.metadata.databaseVersion, errors, warnings)
    }

    private fun parseZip(bytes: ByteArray): DatabasePackage {
        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) { if (!entry.isDirectory) entries[entry.name.substringAfterLast('/')] = zip.bufferedReader().readText(); entry = zip.nextEntry }
        }
        entries["database.json"]?.let { return requireNotNull(packageAdapter.fromJson(it)) }
        fun <T> list(name: String, clazz: Class<T>): List<T> {
            val type = Types.newParameterizedType(List::class.java, clazz)
            return moshi.adapter<List<T>>(type).fromJson(entries[name] ?: "[]").orEmpty()
        }
        val metadata = moshi.adapter(PackageMetadata::class.java).fromJson(entries["metadata.json"] ?: error("metadata.json is missing")) ?: error("Invalid metadata")
        return DatabasePackage(metadata, list("processors.json", ProcessorEntity::class.java), list("gpus.json", GpuEntity::class.java),
            list("products.json", ProductEntity::class.java), list("software.json", SoftwareEntity::class.java), list("requirements.json", RequirementEntity::class.java))
    }

    private fun parseCsv(text: String): List<List<String>> = text.lineSequence().filter { it.isNotBlank() }.map { line ->
        val values = mutableListOf<String>(); val current = StringBuilder(); var quoted = false; var i = 0
        while (i < line.length) { val c=line[i]; when { c=='"' && quoted && i+1<line.length && line[i+1]=='"' -> { current.append('"'); i++ }; c=='"' -> quoted=!quoted; c==',' && !quoted -> { values+=current.toString(); current.clear() }; else -> current.append(c) }; i++ }
        values += current.toString(); values
    }.toList()

    companion object {
        fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        private fun <T> duplicateValues(values: List<T>) = values.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        private fun compareVersions(a: String, b: String): Int { val av=a.split('.').mapNotNull(String::toIntOrNull); val bv=b.split('.').mapNotNull(String::toIntOrNull); for(i in 0 until maxOf(av.size,bv.size)){ val diff=(av.getOrElse(i){0})-(bv.getOrElse(i){0}); if(diff!=0)return diff }; return 0 }
    }
}
