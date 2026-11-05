package com.jitelecom.productadviser.data.seed

import com.jitelecom.productadviser.data.local.ProductEntity
import com.jitelecom.productadviser.domain.model.VerificationStatus

/** Add missing published specs without replacing store-maintained prices or hardware. */
internal fun ProductEntity.withMissingBundledSpecifications(bundled: ProductEntity): ProductEntity {
    if (sku != bundled.sku || brand != bundled.brand || model != bundled.model || category != bundled.category) return this
    if (archived || bundled.sourceUrl == null) return this
    val managedByBundledCatalog = sourceUrl == null ||
        verifiedBy?.startsWith("JI Telecom", ignoreCase = true) == true ||
        sourceName?.contains("Product-Line-up", ignoreCase = true) == true ||
        sourceUrl == bundled.sourceUrl
    if (!managedByBundledCatalog) return this
    val bundledSpecsVerified = bundled.verificationStatus == VerificationStatus.VERIFIED
    if (!bundledSpecsVerified && processorId != null && processorId != bundled.processorId) return this
    if (!bundledSpecsVerified && gpuId != null && gpuId != bundled.gpuId) return this
    val promoteExactPlatform = bundled.verificationStatus == VerificationStatus.VERIFIED &&
        verificationStatus != VerificationStatus.VERIFIED

    val mergedNotes = when {
        notes.isNullOrBlank() -> bundled.notes
        bundled.notes.isNullOrBlank() || notes.contains(bundled.notes) -> notes
        else -> "$notes\n\n${bundled.notes}"
    }
    return copy(
        processorId = if (bundledSpecsVerified) bundled.processorId ?: processorId else processorId ?: bundled.processorId,
        gpuId = if (bundledSpecsVerified) bundled.gpuId ?: gpuId else gpuId ?: bundled.gpuId,
        ramGB = ramGB ?: bundled.ramGB,
        ramType = ramType ?: bundled.ramType,
        ramUpgradeable = ramUpgradeable ?: bundled.ramUpgradeable,
        maximumRamGB = maximumRamGB ?: bundled.maximumRamGB,
        storageGB = storageGB ?: bundled.storageGB,
        storageType = storageType ?: bundled.storageType,
        additionalStorageSupport = additionalStorageSupport ?: bundled.additionalStorageSupport,
        displaySize = displaySize ?: bundled.displaySize,
        displayResolution = displayResolution ?: bundled.displayResolution,
        displayRefreshRate = displayRefreshRate ?: bundled.displayRefreshRate,
        operatingSystem = if (promoteExactPlatform) bundled.operatingSystem ?: operatingSystem else operatingSystem ?: bundled.operatingSystem,
        architecture = if (promoteExactPlatform) bundled.architecture ?: architecture else architecture ?: bundled.architecture,
        batteryCapacityWh = batteryCapacityWh ?: bundled.batteryCapacityWh,
        weightKg = weightKg ?: bundled.weightKg,
        supportedFeatures = supportedFeatures.ifEmpty { bundled.supportedFeatures },
        notes = mergedNotes,
        sourceName = bundled.sourceName ?: sourceName,
        sourceUrl = bundled.sourceUrl,
        verifiedDate = bundled.verifiedDate ?: verifiedDate,
        verifiedBy = bundled.verifiedBy ?: verifiedBy,
        verificationStatus = bundled.verificationStatus,
        availabilityUpdatedAt = availabilityUpdatedAt ?: bundled.availabilityUpdatedAt
    )
}

