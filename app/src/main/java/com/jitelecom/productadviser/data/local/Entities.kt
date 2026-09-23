package com.jitelecom.productadviser.data.local

import androidx.room.*
import com.jitelecom.productadviser.domain.model.*

@Entity(tableName = "processors", indices = [Index(value = ["manufacturer", "model"], unique = true)])
data class ProcessorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val manufacturer: String, val family: String, val model: String, val generation: String? = null,
    val architecture: String? = null, val coreCount: Int? = null, val threadCount: Int? = null,
    val baseClockGhz: Double? = null, val boostClockGhz: Double? = null, val integratedGpu: String? = null,
    val performanceTier: Int? = null, val workloadTier: Int? = null, val notes: String? = null, val sourceUrl: String? = null
)

@Entity(tableName = "gpus", indices = [Index(value = ["manufacturer", "model"], unique = true)])
data class GpuEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val manufacturer: String, val model: String, val type: GpuType, val vramGB: Double? = null,
    val architecture: String? = null, val performanceTier: Int? = null, val graphicsTier: Int? = null,
    val notes: String? = null, val sourceUrl: String? = null
)

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(entity = ProcessorEntity::class, parentColumns = ["id"], childColumns = ["processorId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = GpuEntity::class, parentColumns = ["id"], childColumns = ["gpuId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index(value = ["sku"], unique = true), Index("processorId"), Index("gpuId"), Index("brand"), Index("model"), Index("category")]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sku: String, val brand: String, val model: String, val modelFamily: String? = null, val variant: String? = null,
    val category: ProductCategory, val subcategory: String? = null, val releaseYear: Int? = null, val productImage: String? = null,
    val price: Double, val promotionalPrice: Double? = null, val availabilityStatus: AvailabilityStatus = AvailabilityStatus.UNKNOWN,
    val processorId: Long? = null, val gpuId: Long? = null, val ramGB: Int? = null, val ramType: String? = null,
    val ramUpgradeable: Boolean? = null, val maximumRamGB: Int? = null, val storageGB: Int? = null, val storageType: String? = null,
    val additionalStorageSupport: String? = null, val displaySize: Double? = null, val displayResolution: String? = null,
    val displayRefreshRate: Int? = null, val operatingSystem: String? = null, val architecture: String? = null,
    val batteryCapacityWh: Double? = null, val weightKg: Double? = null, val supportedFeatures: Set<String> = emptySet(),
    val notes: String? = null, val sourceName: String? = null, val sourceUrl: String? = null, val verifiedDate: String? = null,
    val lastUpdated: String? = null, val verifiedBy: String? = null,
    val verificationStatus: VerificationStatus = VerificationStatus.UNVERIFIED, val archived: Boolean = false
)

data class ProductWithHardware(
    @Embedded val product: ProductEntity,
    @Relation(parentColumn = "processorId", entityColumn = "id") val processor: ProcessorEntity?,
    @Relation(parentColumn = "gpuId", entityColumn = "id") val gpu: GpuEntity?
)

@Entity(tableName = "software", indices = [Index(value = ["name", "version", "platform"], unique = true)])
data class SoftwareEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, val developer: String? = null, val version: String, val category: String, val platform: String,
    val description: String? = null, val officialWebsite: String? = null, val requirementsSourceUrl: String? = null,
    val lastVerified: String? = null, val iconPath: String? = null,
    val verificationStatus: VerificationStatus = VerificationStatus.UNVERIFIED
)

@Entity(
    tableName = "requirements",
    foreignKeys = [ForeignKey(entity = SoftwareEntity::class, parentColumns = ["id"], childColumns = ["softwareId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("softwareId"), Index(value = ["softwareId", "requirementType"], unique = true)]
)
data class RequirementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, val softwareId: Long, @ColumnInfo(name = "requirementType") val type: RequirementType,
    val minimumRamGB: Int? = null, val minimumStorageGB: Int? = null, val minimumCpuTier: Int? = null,
    val minimumGpuTier: Int? = null, val minimumVramGB: Double? = null, val requiredArchitecture: String? = null,
    val supportedOperatingSystems: Set<String> = emptySet(), val requiredFeatures: Set<String> = emptySet(),
    val acceptedProcessorIds: Set<Long> = emptySet(), val acceptedGpuIds: Set<Long> = emptySet(), val notes: String? = null,
    val verificationStatus: VerificationStatus = VerificationStatus.UNVERIFIED
)

@Entity(
    tableName = "workload_profiles",
    foreignKeys = [ForeignKey(entity = SoftwareEntity::class, parentColumns = ["id"], childColumns = ["softwareId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("softwareId")]
)
data class WorkloadProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, val softwareId: Long, val name: String, val description: String,
    val minimumCpuTier: Int? = null, val minimumGpuTier: Int? = null, val minimumRamGB: Int? = null
)

@Entity(tableName = "local_analytics", indices = [Index(value = ["event", "key"], unique = true)])
data class AnalyticsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, val event: String, val key: String,
    val count: Long = 1, val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "database_metadata")
data class DatabaseMetadataEntity(
    @PrimaryKey val key: String, val value: String
)

class DatabaseConverters {
    @TypeConverter fun fromVerification(value: VerificationStatus) = value.name
    @TypeConverter fun toVerification(value: String) = VerificationStatus.valueOf(value)
    @TypeConverter fun fromAvailability(value: AvailabilityStatus) = value.name
    @TypeConverter fun toAvailability(value: String) = AvailabilityStatus.valueOf(value)
    @TypeConverter fun fromCategory(value: ProductCategory) = value.name
    @TypeConverter fun toCategory(value: String) = ProductCategory.valueOf(value)
    @TypeConverter fun fromGpuType(value: GpuType) = value.name
    @TypeConverter fun toGpuType(value: String) = GpuType.valueOf(value)
    @TypeConverter fun fromRequirementType(value: RequirementType) = value.name
    @TypeConverter fun toRequirementType(value: String) = RequirementType.valueOf(value)
    @TypeConverter fun fromStringSet(value: Set<String>) = value.joinToString("\u001F")
    @TypeConverter fun toStringSet(value: String) = value.takeIf { it.isNotEmpty() }?.split("\u001F")?.toSet().orEmpty()
    @TypeConverter fun fromLongSet(value: Set<Long>) = value.joinToString(",")
    @TypeConverter fun toLongSet(value: String) = value.takeIf { it.isNotEmpty() }?.split(',')?.mapNotNull(String::toLongOrNull)?.toSet().orEmpty()
}
