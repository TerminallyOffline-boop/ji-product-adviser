package com.jitelecom.productadviser.data.seed

import com.jitelecom.productadviser.data.local.ProductEntity
import com.jitelecom.productadviser.domain.model.VerificationStatus

/** Add missing published specs without replacing store-maintained prices or hardware. */
internal fun ProductEntity.withMissingBundledSpecifications(bundled: ProductEntity): ProductEntity {
    if (sku != bundled.sku || brand != bundled.brand || model != bundled.model || category != bundled.category) return this
    if (archived || verificationStatus == VerificationStatus.VERIFIED || bundled.sourceUrl == null) return this
    if (sourceUrl != null && sourceUrl != bundled.sourceUrl) return this
    if (processorId != null && processorId != bundled.processorId) return this
    if (gpuId != null && gpuId != bundled.gpuId) return this

    val mergedNotes = when {
        notes.isNullOrBlank() -> bundled.notes
        bundled.notes.isNullOrBlank() || notes.contains(bundled.notes) -> notes
        else -> "$notes\n\n${bundled.notes}"
    }
    return copy(
        processorId = processorId ?: bundled.processorId,
        gpuId = gpuId ?: bundled.gpuId,
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
        operatingSystem = operatingSystem ?: bundled.operatingSystem,
        architecture = architecture ?: bundled.architecture,
        batteryCapacityWh = batteryCapacityWh ?: bundled.batteryCapacityWh,
        weightKg = weightKg ?: bundled.weightKg,
        notes = mergedNotes,
        sourceUrl = sourceUrl ?: bundled.sourceUrl,
        verifiedDate = verifiedDate ?: bundled.verifiedDate,
        verifiedBy = verifiedBy ?: bundled.verifiedBy
    )
}
