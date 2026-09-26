package com.jitelecom.productadviser.data.seed

import androidx.room.withTransaction
import com.jitelecom.productadviser.BuildConfig
import com.jitelecom.productadviser.data.local.*
import com.jitelecom.productadviser.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoDataSeeder @Inject constructor(
    private val db: AppDatabase,
    private val catalogLoader: BundledCatalogLoader
) {
    suspend fun seedIfEmpty() {
        if (db.productDao().count() > 0) {
            refreshBundledCatalogIfEligible()
            refreshBundledSoftwareIfEligible()
            return
        }
        val catalogProducts = catalogLoader.productEntities()
        val verifiedProcessors = catalogLoader.processorEntities()
        val verifiedGpus = catalogLoader.gpuEntities()
        db.withTransaction {
            val processors = listOf(
                ProcessorEntity(manufacturer="Intel", family="N Series", model="Sample N100-class", coreCount=4, threadCount=4, performanceTier=2, workloadTier=2, notes=SAMPLE),
                ProcessorEntity(manufacturer="Intel", family="Core i3", model="Sample Core i3-class", coreCount=6, threadCount=8, performanceTier=3, workloadTier=3, notes=SAMPLE),
                ProcessorEntity(manufacturer="Intel", family="Core i5", model="Sample Core i5-class", coreCount=10, threadCount=12, performanceTier=5, workloadTier=5, notes=SAMPLE),
                ProcessorEntity(manufacturer="Intel", family="Core i7", model="Sample Core i7-class", coreCount=14, threadCount=20, performanceTier=6, workloadTier=6, notes=SAMPLE),
                ProcessorEntity(manufacturer="AMD", family="Ryzen 3", model="Sample Ryzen 3-class", coreCount=4, threadCount=8, performanceTier=3, workloadTier=3, notes=SAMPLE),
                ProcessorEntity(manufacturer="AMD", family="Ryzen 5", model="Sample Ryzen 5-class", coreCount=6, threadCount=12, performanceTier=5, workloadTier=5, notes=SAMPLE),
                ProcessorEntity(manufacturer="AMD", family="Ryzen 7", model="Sample Ryzen 7-class", coreCount=8, threadCount=16, performanceTier=6, workloadTier=6, notes=SAMPLE),
                ProcessorEntity(manufacturer="Qualcomm", family="Snapdragon", model="Sample Mobile Entry", coreCount=8, performanceTier=2, workloadTier=2, notes=SAMPLE),
                ProcessorEntity(manufacturer="Qualcomm", family="Snapdragon", model="Sample Mobile Mainstream", coreCount=8, performanceTier=4, workloadTier=4, notes=SAMPLE),
                ProcessorEntity(manufacturer="MediaTek", family="Dimensity", model="Sample Mobile Performance", coreCount=8, performanceTier=5, workloadTier=5, notes=SAMPLE)
            )
            db.hardwareDao().insertProcessors(processors)
            db.hardwareDao().upsertProcessors(verifiedProcessors)
            val gpus = listOf(
                GpuEntity(manufacturer="Intel", model="Sample UHD-class", type=GpuType.INTEGRATED, performanceTier=2, graphicsTier=2, notes=SAMPLE),
                GpuEntity(manufacturer="Intel", model="Sample Iris Xe-class", type=GpuType.INTEGRATED, performanceTier=3, graphicsTier=3, notes=SAMPLE),
                GpuEntity(manufacturer="AMD", model="Sample Radeon Integrated", type=GpuType.INTEGRATED, performanceTier=3, graphicsTier=3, notes=SAMPLE),
                GpuEntity(manufacturer="NVIDIA", model="Sample Entry Dedicated", type=GpuType.DEDICATED, vramGB=4.0, performanceTier=4, graphicsTier=4, notes=SAMPLE),
                GpuEntity(manufacturer="NVIDIA", model="Sample Mainstream Dedicated", type=GpuType.DEDICATED, vramGB=6.0, performanceTier=5, graphicsTier=5, notes=SAMPLE),
                GpuEntity(manufacturer="NVIDIA", model="Sample Performance Dedicated", type=GpuType.DEDICATED, vramGB=8.0, performanceTier=6, graphicsTier=6, notes=SAMPLE),
                GpuEntity(manufacturer="AMD", model="Sample Entry Dedicated", type=GpuType.DEDICATED, vramGB=4.0, performanceTier=4, graphicsTier=4, notes=SAMPLE),
                GpuEntity(manufacturer="AMD", model="Sample Performance Dedicated", type=GpuType.DEDICATED, vramGB=8.0, performanceTier=6, graphicsTier=6, notes=SAMPLE),
                GpuEntity(manufacturer="Qualcomm", model="Sample Adreno-class", type=GpuType.INTEGRATED, performanceTier=3, graphicsTier=3, notes=SAMPLE),
                GpuEntity(manufacturer="MediaTek", model="Sample Mali-class", type=GpuType.INTEGRATED, performanceTier=3, graphicsTier=3, notes=SAMPLE)
            )
            db.hardwareDao().insertGpus(gpus)
            db.hardwareDao().upsertGpus(verifiedGpus)
            db.productDao().insertAll(catalogProducts)
            val softwareIds = db.softwareDao().insertAll(BundledSoftwareCatalog.entries.map { it.entity() })
            db.requirementDao().insertAll(BundledSoftwareCatalog.entries.zip(softwareIds).flatMap { (software, id) -> software.requirements(id) })
            db.metadataDao().putAll(listOf(
                DatabaseMetadataEntity("databaseVersion", BuildConfig.DATABASE_VERSION),
                DatabaseMetadataEntity("bundledSpecificationsVersion", catalogLoader.specifications().version),
                DatabaseMetadataEntity("bundledSoftwareVersion", BundledSoftwareCatalog.VERSION),
                DatabaseMetadataEntity("lastUpdated", "2026-09-26"),
                DatabaseMetadataEntity("dataNotice", DATA_NOTICE)
            ))
        }
    }

    private suspend fun refreshBundledCatalogIfEligible() {
        val specificationsVersion = catalogLoader.specifications().version
        if (db.metadataDao().getValue("bundledSpecificationsVersion") == specificationsVersion) return
        val catalogProducts = catalogLoader.productEntities()
        db.withTransaction {
            val existing = db.productDao().getAllOnce().map { it.product }
            if (existing.isEmpty()) return@withTransaction
            val bundledBySku = catalogProducts.associateBy { it.sku }
            val isUntouchedSample = existing.all { it.sku.startsWith("SAMPLE-") }
            if (!isUntouchedSample && existing.none { it.sku in bundledBySku }) return@withTransaction

            // Resolve real database IDs by name; fixed asset IDs may overlap with admin-created hardware.
            val currentProcessors = db.hardwareDao().getProcessors().associateBy { it.manufacturer to it.model }
            val processorIds = catalogLoader.processorEntities().associate { processor ->
                processor.id to (currentProcessors[processor.manufacturer to processor.model]?.id
                    ?: db.hardwareDao().insertProcessor(processor.copy(id = 0)))
            }
            val currentGpus = db.hardwareDao().getGpus().associateBy { it.manufacturer to it.model }
            val gpuIds = catalogLoader.gpuEntities().associate { gpu ->
                gpu.id to (currentGpus[gpu.manufacturer to gpu.model]?.id
                    ?: db.hardwareDao().insertGpu(gpu.copy(id = 0)))
            }
            val mappedCatalog = catalogProducts.map { product -> product.copy(
                processorId = product.processorId?.let { processorIds.getValue(it) },
                gpuId = product.gpuId?.let { gpuIds.getValue(it) }
            ) }
            if (isUntouchedSample) {
                db.productDao().deleteAll()
                db.productDao().insertAll(mappedCatalog)
            } else {
                val mappedBySku = mappedCatalog.associateBy { it.sku }
                existing.forEach { current ->
                    val bundled = mappedBySku[current.sku] ?: return@forEach
                    val enriched = current.withMissingBundledSpecifications(bundled)
                    if (enriched != current) db.productDao().update(enriched)
                }
            }
            val previousVersion = db.metadataDao().getValue("databaseVersion")
            if (isUntouchedSample || previousVersion in setOf("2026.07.1", "2026.07.2")) {
                db.metadataDao().put(DatabaseMetadataEntity("databaseVersion", BuildConfig.DATABASE_VERSION))
            }
            db.metadataDao().putAll(listOf(
                DatabaseMetadataEntity("bundledSpecificationsVersion", specificationsVersion),
                DatabaseMetadataEntity("specificationsUpdated", "2026-09-25"),
                DatabaseMetadataEntity("dataNotice", DATA_NOTICE)
            ))
        }
    }
    private suspend fun refreshBundledSoftwareIfEligible() {
        if (db.metadataDao().getValue("bundledSoftwareVersion") == BundledSoftwareCatalog.VERSION) return
        db.withTransaction {
            val existing = db.softwareDao().getAllOnce().toMutableList()
            BundledSoftwareCatalog.entries.forEach { bundled ->
                val exact = existing.firstOrNull {
                    it.name.equals(bundled.name, true) && it.version == bundled.version && it.platform == bundled.platform
                }
                val legacySample = existing.firstOrNull { it.name.equals(bundled.name, true) && it.description == SAMPLE }
                val current = exact ?: legacySample
                val managed = current == null || current.description == SAMPLE || current.requirementsSourceUrl == bundled.requirementsSourceUrl
                if (!managed) return@forEach

                val softwareId = if (current == null) {
                    db.softwareDao().insert(bundled.entity())
                } else {
                    db.softwareDao().update(bundled.entity(current.id))
                    current.id
                }
                val existingRequirements = db.requirementDao().getForSoftware(softwareId)
                val bundledRequirements = bundled.requirements(softwareId, existingRequirements)
                db.requirementDao().insertAll(bundledRequirements)
                if (bundled.recommended == null) {
                    existingRequirements.filter { requirement ->
                        requirement.type == RequirementType.RECOMMENDED &&
                            requirement.platform.isBlank() &&
                            listOf("JI target", "Practical JI", "Comfortable JI").any {
                                marker -> requirement.notes.orEmpty().contains(marker, ignoreCase = true)
                            }
                    }.forEach { db.requirementDao().delete(it) }
                }
            }
            db.metadataDao().putAll(listOf(
                DatabaseMetadataEntity("bundledSoftwareVersion", BundledSoftwareCatalog.VERSION),
                DatabaseMetadataEntity("databaseVersion", BuildConfig.DATABASE_VERSION),
                DatabaseMetadataEntity("softwareUpdated", "2026-09-26")
            ))
        }
    }

    companion object {
        const val SAMPLE = "SAMPLE DATA — NOT VERIFIED"
        private const val DATA_NOTICE = "JI Telecom July 2026 lineup with a platform-aware software catalog for Windows, macOS, Android, iOS and iPadOS. Selected technical specifications link to publisher or manufacturer sources; records marked for review remain estimates."
    }
}
