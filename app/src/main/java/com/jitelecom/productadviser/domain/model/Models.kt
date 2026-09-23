package com.jitelecom.productadviser.domain.model

enum class VerificationStatus { VERIFIED, NEEDS_REVIEW, UNVERIFIED, OUTDATED }
enum class AvailabilityStatus { AVAILABLE, LIMITED, UNAVAILABLE, DISPLAY_UNIT, UNKNOWN }
enum class ProductCategory { LAPTOP, SMARTPHONE, TABLET, DESKTOP, MONITOR, PRINTER, SMART_TV, GAMING_CONSOLE, ACCESSORIES }
enum class GpuType { INTEGRATED, DEDICATED }
enum class RequirementType { MINIMUM, RECOMMENDED }
enum class CompatibilityStatus { MEETS_RECOMMENDED, MEETS_MINIMUM, BELOW_MINIMUM, NOT_AVAILABLE, NOT_VERIFIED }
enum class ComponentStatus { MEETS_RECOMMENDED, MEETS_MINIMUM, BELOW_MINIMUM, NOT_AVAILABLE, UNKNOWN, NOT_APPLICABLE }

data class ProcessorSpec(
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
) { val displayName: String get() = "$manufacturer $model" }

data class GpuSpec(
    val id: Long,
    val manufacturer: String,
    val model: String,
    val type: GpuType,
    val vramGB: Double? = null,
    val architecture: String? = null,
    val performanceTier: Int? = null,
    val graphicsTier: Int? = null,
    val notes: String? = null,
    val sourceUrl: String? = null
) { val displayName: String get() = "$manufacturer $model" }

data class ProductSpec(
    val id: Long,
    val sku: String,
    val brand: String,
    val model: String,
    val modelFamily: String? = null,
    val variant: String? = null,
    val category: ProductCategory,
    val subcategory: String? = null,
    val releaseYear: Int? = null,
    val productImage: String? = null,
    val price: Double,
    val promotionalPrice: Double? = null,
    val availabilityStatus: AvailabilityStatus = AvailabilityStatus.UNKNOWN,
    val processor: ProcessorSpec? = null,
    val gpu: GpuSpec? = null,
    val ramGB: Int? = null,
    val ramType: String? = null,
    val ramUpgradeable: Boolean? = null,
    val maximumRamGB: Int? = null,
    val storageGB: Int? = null,
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
    val notes: String? = null,
    val sourceName: String? = null,
    val sourceUrl: String? = null,
    val verifiedDate: String? = null,
    val lastUpdated: String? = null,
    val verifiedBy: String? = null,
    val verificationStatus: VerificationStatus = VerificationStatus.UNVERIFIED,
    val archived: Boolean = false
) {
    val displayName: String get() = "$brand $model"
    val effectivePrice: Double get() = promotionalPrice ?: price
}

data class SoftwareSpec(
    val id: Long,
    val name: String,
    val developer: String? = null,
    val version: String,
    val category: String,
    val platform: String,
    val description: String? = null,
    val officialWebsite: String? = null,
    val requirementsSourceUrl: String? = null,
    val lastVerified: String? = null,
    val iconPath: String? = null,
    val verificationStatus: VerificationStatus = VerificationStatus.UNVERIFIED
) { val displayName: String get() = "$name $version".trim() }

data class RequirementSet(
    val id: Long,
    val softwareId: Long,
    val type: RequirementType,
    val minimumRamGB: Int? = null,
    val minimumStorageGB: Int? = null,
    val minimumCpuTier: Int? = null,
    val minimumGpuTier: Int? = null,
    val minimumVramGB: Double? = null,
    val requiredArchitecture: String? = null,
    val supportedOperatingSystems: Set<String> = emptySet(),
    val requiredFeatures: Set<String> = emptySet(),
    val acceptedProcessorIds: Set<Long> = emptySet(),
    val acceptedGpuIds: Set<Long> = emptySet(),
    val notes: String? = null,
    val verificationStatus: VerificationStatus = VerificationStatus.UNVERIFIED
)

data class WorkloadProfile(
    val id: Long,
    val softwareId: Long,
    val name: String,
    val description: String,
    val minimumCpuTier: Int? = null,
    val minimumGpuTier: Int? = null,
    val minimumRamGB: Int? = null
)

data class ComponentCompatibilityResult(
    val component: String,
    val actual: String,
    val minimum: String? = null,
    val recommended: String? = null,
    val status: ComponentStatus,
    val explanation: String
)

data class CompatibilityResult(
    val productId: Long,
    val softwareId: Long,
    val status: CompatibilityStatus,
    val components: List<ComponentCompatibilityResult>,
    val explanation: String,
    val disclaimer: String = COMPATIBILITY_DISCLAIMER
)

const val COMPATIBILITY_DISCLAIMER = "Compatibility results are based on stored hardware specifications and published software requirements. Actual performance may vary depending on software version, drivers, operating system, workload, thermal conditions and configuration."

data class CustomerRequest(
    val profile: String? = null,
    val category: ProductCategory = ProductCategory.LAPTOP,
    val budget: Double,
    val softwareIds: Set<Long> = emptySet(),
    val priorities: Set<String> = emptySet(),
    val preferredBrand: String? = null,
    val showSlightlyAboveBudget: Boolean = false,
    val aboveBudgetPercent: Int = 10
)

data class RecommendationWeights(
    val compatibility: Double = 0.40,
    val performance: Double = 0.20,
    val budget: Double = 0.20,
    val memoryStorage: Double = 0.10,
    val preferences: Double = 0.10
)

data class RecommendationResult(
    val product: ProductSpec,
    val internalScore: Int,
    val compatibility: Map<Long, CompatibilityResult>,
    val strengths: List<String>,
    val limitations: List<String>,
    val explanation: String
)
