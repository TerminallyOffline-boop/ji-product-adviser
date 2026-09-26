package com.jitelecom.productadviser.data.seed

import android.content.Context
import com.jitelecom.productadviser.data.local.GpuEntity
import com.jitelecom.productadviser.data.local.ProductEntity
import com.jitelecom.productadviser.data.local.ProcessorEntity
import com.jitelecom.productadviser.domain.model.AvailabilityStatus
import com.jitelecom.productadviser.domain.model.GpuType
import com.jitelecom.productadviser.domain.model.ProductCategory
import com.jitelecom.productadviser.domain.model.VerificationStatus
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class BundledCatalog(
    val catalogVersion: String,
    val sourceName: String,
    val sourceSheet: String,
    val products: List<BundledProduct>
)

data class BundledProduct(
    val sku: String,
    val brand: String,
    val model: String,
    val modelFamily: String? = null,
    val variant: String? = null,
    val category: String,
    val price: Double,
    val promotionalPrice: Double? = null,
    val availabilityStatus: String,
    val inventorySnapshot: Int? = null,
    val ramGB: Int? = null,
    val storageGB: Int? = null,
    val processorLabel: String? = null,
    val notes: String,
    val sourceName: String,
    val lastUpdated: String,
    val verificationStatus: String
)

data class BundledSpecifications(
    val version: String,
    val verifiedDate: String,
    val processors: List<BundledProcessor> = emptyList(),
    val gpus: List<BundledGpu> = emptyList(),
    val assignments: List<BundledSpecificationAssignment> = emptyList()
)

data class BundledProcessor(
    val id: Long,
    val manufacturer: String,
    val family: String,
    val model: String,
    val generation: String? = null,
    val architecture: String? = null,
    val coreCount: Int? = null,
    val threadCount: Int? = null,
    val baseClockGhz: Double? = null,
    val boostClockGhz: Double? = null,
    val integratedGpu: String? = null,
    val performanceTier: Int? = null,
    val workloadTier: Int? = null,
    val notes: String? = null,
    val sourceUrl: String? = null
)

data class BundledGpu(
    val id: Long,
    val manufacturer: String,
    val model: String,
    val type: String,
    val vramGB: Double? = null,
    val architecture: String? = null,
    val performanceTier: Int? = null,
    val graphicsTier: Int? = null,
    val notes: String? = null,
    val sourceUrl: String? = null
)

data class BundledSpecificationAssignment(
    val skus: List<String>,
    val processorId: Long? = null,
    val gpuId: Long? = null,
    val ramType: String? = null,
    val ramUpgradeable: Boolean? = null,
    val maximumRamGB: Int? = null,
    val storageType: String? = null,
    val additionalStorageSupport: String? = null,
    val displaySize: Double? = null,
    val displayResolution: String? = null,
    val displayRefreshRate: Int? = null,
    val operatingSystem: String? = null,
    val architecture: String? = null,
    val batteryCapacityWh: Double? = null,
    val weightKg: Double? = null,
    val supportedFeatures: Set<String> = emptySet(),
    val sourceUrl: String,
    val sourceName: String? = null,
    val verifiedBy: String? = null,
    val verificationStatus: String = "NEEDS_REVIEW",
    val notes: String? = null
)

@Singleton
class BundledCatalogLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    moshi: Moshi
) {
    private val catalogAdapter = moshi.adapter(BundledCatalog::class.java)
    private val specificationsAdapter = moshi.adapter(BundledSpecifications::class.java)

    fun load(): BundledCatalog = context.assets.open(ASSET_NAME).bufferedReader().use { reader ->
        requireNotNull(catalogAdapter.fromJson(reader.readText())) { "Bundled catalog is unreadable." }
    }

    fun specifications(): BundledSpecifications = context.assets.open(SPECIFICATIONS_ASSET_NAME).bufferedReader().use { reader ->
        requireNotNull(specificationsAdapter.fromJson(reader.readText())) { "Bundled specifications are unreadable." }
    }

    fun processorEntities(): List<ProcessorEntity> = specifications().processors.map { processor ->
        ProcessorEntity(
            id = processor.id,
            manufacturer = processor.manufacturer,
            family = processor.family,
            model = processor.model,
            generation = processor.generation,
            architecture = processor.architecture,
            coreCount = processor.coreCount,
            threadCount = processor.threadCount,
            baseClockGhz = processor.baseClockGhz,
            boostClockGhz = processor.boostClockGhz,
            integratedGpu = processor.integratedGpu,
            performanceTier = processor.performanceTier,
            workloadTier = processor.workloadTier ?: processor.performanceTier,
            notes = processor.notes ?: INTERNAL_TIER_NOTICE,
            sourceUrl = processor.sourceUrl
        )
    }

    fun gpuEntities(): List<GpuEntity> = specifications().gpus.map { gpu ->
        GpuEntity(
            id = gpu.id,
            manufacturer = gpu.manufacturer,
            model = gpu.model,
            type = GpuType.valueOf(gpu.type),
            vramGB = gpu.vramGB,
            architecture = gpu.architecture,
            performanceTier = gpu.performanceTier,
            graphicsTier = gpu.graphicsTier ?: gpu.performanceTier,
            notes = gpu.notes ?: INTERNAL_TIER_NOTICE,
            sourceUrl = gpu.sourceUrl
        )
    }

    fun productEntities(): List<ProductEntity> {
        val specifications = specifications()
        val assignments = specifications.assignments
            .flatMap { assignment -> assignment.skus.map { sku -> sku to assignment } }
            .toMap()
        return load().products.map { product ->
            val specification = assignments[product.sku]
            val specificationStatus = specification?.verificationStatus
                ?.let(VerificationStatus::valueOf)
                ?: VerificationStatus.valueOf(product.verificationStatus)
            val isOfficiallyVerified = specificationStatus == VerificationStatus.VERIFIED
        ProductEntity(
            sku = product.sku,
            brand = product.brand,
            model = product.model,
            modelFamily = product.modelFamily,
            variant = product.variant,
            category = ProductCategory.valueOf(product.category),
            price = product.price,
            promotionalPrice = product.promotionalPrice,
            availabilityStatus = AvailabilityStatus.valueOf(product.availabilityStatus),
            processorId = specification?.processorId,
            gpuId = specification?.gpuId,
            ramGB = product.ramGB,
            ramType = specification?.ramType,
            ramUpgradeable = specification?.ramUpgradeable,
            maximumRamGB = specification?.maximumRamGB,
            storageGB = product.storageGB,
            storageType = specification?.storageType,
            additionalStorageSupport = specification?.additionalStorageSupport,
            displaySize = specification?.displaySize,
            displayResolution = specification?.displayResolution,
            displayRefreshRate = specification?.displayRefreshRate,
            operatingSystem = specification?.operatingSystem,
            architecture = specification?.architecture,
            batteryCapacityWh = specification?.batteryCapacityWh,
            weightKg = specification?.weightKg,
            supportedFeatures = specification?.supportedFeatures.orEmpty(),
            notes = listOfNotNull(
                product.notes,
                product.processorLabel?.let { "Spreadsheet processor label: $it." },
                specification?.notes,
                specification?.let {
                    if (isOfficiallyVerified) {
                        "The compatibility-critical details were checked against the linked official specification."
                    } else {
                        "A linked reference supports some details, but the exact model or variant still needs verification."
                    }
                }
            ).joinToString(" "),
            sourceName = specification?.sourceName ?: product.sourceName,
            sourceUrl = specification?.sourceUrl,
            verifiedDate = specification?.let { specifications.verifiedDate },
            lastUpdated = product.lastUpdated,
            verifiedBy = specification?.verifiedBy?.takeIf { isOfficiallyVerified },
            verificationStatus = specificationStatus
        )
    }

    }

    companion object {
        const val ASSET_NAME = "ji_catalog_july_2026.json"
        const val SPECIFICATIONS_ASSET_NAME = "ji_official_specs_2026.json"
        private const val INTERNAL_TIER_NOTICE = "Internal comparison tier; not a manufacturer rating."
    }
}
