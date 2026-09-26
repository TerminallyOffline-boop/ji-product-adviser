package com.jitelecom.productadviser.data.repository

import com.jitelecom.productadviser.data.local.*
import com.jitelecom.productadviser.domain.model.*
import com.jitelecom.productadviser.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepositoryImpl @Inject constructor(private val dao: ProductDao) : ProductRepository {
    override fun observeProducts(query: String): Flow<List<ProductSpec>> =
        (if (query.isBlank()) dao.observeAll() else dao.search(query.trim())).map { rows -> rows.map(ProductWithHardware::toDomain) }
    override suspend fun getProduct(id: Long) = dao.get(id)?.toDomain()
    override suspend fun getProducts() = dao.getAllOnce().map(ProductWithHardware::toDomain)
    override suspend fun saveProduct(product: ProductSpec): Long {
        val entity = product.toEntity()
        return if (product.id == 0L) dao.insert(entity) else { dao.update(entity); product.id }
    }
    override suspend fun archiveProduct(id: Long) = dao.archive(id)
}

@Singleton
class CatalogRepositoryImpl @Inject constructor(
    private val softwareDao: SoftwareDao,
    private val requirementDao: RequirementDao
) : CatalogRepository {
    override fun observeSoftware() = softwareDao.observeAll().map { list -> list.map(SoftwareEntity::toDomain) }
    override fun observeRequirements(softwareId: Long) = requirementDao.observeForSoftware(softwareId).map { list -> list.map(RequirementEntity::toDomain) }
    override suspend fun getSoftware() = softwareDao.getAllOnce().map(SoftwareEntity::toDomain)
    override suspend fun getRequirements(softwareId: Long) = requirementDao.getForSoftware(softwareId).map(RequirementEntity::toDomain)
}

fun ProductWithHardware.toDomain() = ProductSpec(
    id = product.id, sku = product.sku, brand = product.brand, model = product.model, modelFamily = product.modelFamily,
    variant = product.variant, category = product.category, subcategory = product.subcategory, releaseYear = product.releaseYear,
    productImage = product.productImage, price = product.price, promotionalPrice = product.promotionalPrice,
    availabilityStatus = product.availabilityStatus, processor = processor?.toDomain(), gpu = gpu?.toDomain(), ramGB = product.ramGB,
    ramType = product.ramType, ramUpgradeable = product.ramUpgradeable, maximumRamGB = product.maximumRamGB,
    storageGB = product.storageGB, storageType = product.storageType, additionalStorageSupport = product.additionalStorageSupport,
    displaySize = product.displaySize, displayResolution = product.displayResolution, displayRefreshRate = product.displayRefreshRate,
    operatingSystem = product.operatingSystem, architecture = product.architecture, batteryCapacityWh = product.batteryCapacityWh,
    weightKg = product.weightKg, supportedFeatures = product.supportedFeatures, notes = product.notes, sourceName = product.sourceName,
    sourceUrl = product.sourceUrl, verifiedDate = product.verifiedDate, lastUpdated = product.lastUpdated,
    verifiedBy = product.verifiedBy, verificationStatus = product.verificationStatus, archived = product.archived
)

fun ProcessorEntity.toDomain() = ProcessorSpec(id, manufacturer, family, model, generation, architecture, coreCount, threadCount, baseClockGhz, boostClockGhz, integratedGpu, performanceTier, workloadTier, notes, sourceUrl)
fun GpuEntity.toDomain() = GpuSpec(id, manufacturer, model, type, vramGB, architecture, performanceTier, graphicsTier, notes, sourceUrl)
fun SoftwareEntity.toDomain() = SoftwareSpec(id, name, developer, version, category, platform, description, officialWebsite, requirementsSourceUrl, lastVerified, iconPath, verificationStatus)
fun RequirementEntity.toDomain() = RequirementSet(id, softwareId, type, minimumRamGB, minimumStorageGB, minimumCpuTier, minimumGpuTier, minimumVramGB, requiredArchitecture, supportedOperatingSystems, requiredFeatures, acceptedProcessorIds, acceptedGpuIds, notes, verificationStatus, platform)

fun ProductSpec.toEntity() = ProductEntity(
    id, sku, brand, model, modelFamily, variant, category, subcategory, releaseYear, productImage, price, promotionalPrice,
    availabilityStatus, processor?.id, gpu?.id, ramGB, ramType, ramUpgradeable, maximumRamGB, storageGB, storageType,
    additionalStorageSupport, displaySize, displayResolution, displayRefreshRate, operatingSystem, architecture,
    batteryCapacityWh, weightKg, supportedFeatures, notes, sourceName, sourceUrl, verifiedDate, lastUpdated, verifiedBy,
    verificationStatus, archived
)
