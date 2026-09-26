package com.jitelecom.productadviser.data

import com.google.common.truth.Truth.assertThat
import com.jitelecom.productadviser.data.local.ProductEntity
import com.jitelecom.productadviser.data.seed.withMissingBundledSpecifications
import com.jitelecom.productadviser.domain.model.AvailabilityStatus
import com.jitelecom.productadviser.domain.model.ProductCategory
import com.jitelecom.productadviser.domain.model.VerificationStatus
import org.junit.Test

class BundledSpecificationMergeTest {
    private val current = ProductEntity(
        id = 42, sku = "JUL26-LAP-017", brand = "HP", model = "OmniBook 16-af1062TU",
        category = ProductCategory.LAPTOP, price = 43000.0, promotionalPrice = 41000.0,
        ramGB = 16, storageGB = 512, notes = "Store note", availabilityStatus = AvailabilityStatus.UNAVAILABLE
    )
    private val bundled = current.copy(
        id = 0, price = 45000.0, promotionalPrice = null, ramGB = 32, storageGB = 1024,
        processorId = 9001, gpuId = 9002, displaySize = 16.0, batteryCapacityWh = 59.0,
        ramType = "LPDDR5", notes = "Published specs", sourceUrl = "https://www.hp.com/",
        availabilityStatus = AvailabilityStatus.AVAILABLE
    )

    @Test fun fillsMissingSpecsAndPreservesStoreDataAndProductIdentity() {
        val result = current.withMissingBundledSpecifications(bundled)
        assertThat(result.processorId).isEqualTo(9001)
        assertThat(result.gpuId).isEqualTo(9002)
        assertThat(result.displaySize).isEqualTo(16.0)
        assertThat(result.batteryCapacityWh).isEqualTo(59.0)
        assertThat(result.id).isEqualTo(current.id)
        assertThat(result.price).isEqualTo(current.price)
        assertThat(result.promotionalPrice).isEqualTo(current.promotionalPrice)
        assertThat(result.ramGB).isEqualTo(current.ramGB)
        assertThat(result.storageGB).isEqualTo(current.storageGB)
        assertThat(result.availabilityStatus).isEqualTo(current.availabilityStatus)
        assertThat(result.notes).contains("Store note")
        assertThat(result.withMissingBundledSpecifications(bundled)).isEqualTo(result)
    }

    @Test fun doesNotOverwriteCustomHardwareVerifiedRecordsOrDifferentVariants() {
        listOf(
            current.copy(processorId = 777),
            current.copy(gpuId = 778),
            current.copy(model = "Another model"),
            current.copy(sourceUrl = "https://www.hp.com/custom-source"),
            current.copy(archived = true)
        ).forEach { custom -> assertThat(custom.withMissingBundledSpecifications(bundled)).isEqualTo(custom) }
    }

    @Test fun fillsMissingRamAndStorageWithoutReplacingExistingValues() {
        val missing = current.copy(ramGB=null,storageGB=null)
        val result = missing.withMissingBundledSpecifications(bundled)
        assertThat(result.ramGB).isEqualTo(32)
        assertThat(result.storageGB).isEqualTo(1024)
        assertThat(current.withMissingBundledSpecifications(bundled).ramGB).isEqualTo(16)
        assertThat(current.withMissingBundledSpecifications(bundled).storageGB).isEqualTo(512)
    }

    @Test fun promotesUntouchedCatalogRecordWhenBundledSourceIsVerified() {
        val verified = bundled.copy(
            sourceName = "HP official specifications",
            verifiedDate = "2026-09-25",
            verifiedBy = "JI Telecom official-source audit",
            verificationStatus = VerificationStatus.VERIFIED
        )
        val result = current.copy(verificationStatus = VerificationStatus.NEEDS_REVIEW)
            .withMissingBundledSpecifications(verified)

        assertThat(result.verificationStatus).isEqualTo(VerificationStatus.VERIFIED)
        assertThat(result.sourceName).isEqualTo("HP official specifications")
        assertThat(result.verifiedDate).isEqualTo("2026-09-25")
        assertThat(result.verifiedBy).isEqualTo("JI Telecom official-source audit")
    }

    @Test fun upgradesPreviouslyBundledVerifiedRecordWhenTheExactSourceImproves() {
        val previous = current.copy(
            ramGB = null,
            storageGB = null,
            sourceName = "GIGABYTE official specifications",
            sourceUrl = "https://www.gigabyte.com/Laptop/G5--2023",
            verifiedBy = "JI Telecom official-source audit",
            verificationStatus = VerificationStatus.VERIFIED
        )
        val exact = bundled.copy(
            ramGB = 8,
            storageGB = 512,
            sourceName = "NVIDIA Philippines marketplace exact configuration",
            sourceUrl = "https://marketplace.nvidia.com/en-ph/example",
            verifiedBy = "JI Telecom exact-model audit",
            verificationStatus = VerificationStatus.VERIFIED
        )
        val result = previous.withMissingBundledSpecifications(exact)

        assertThat(result.ramGB).isEqualTo(8)
        assertThat(result.storageGB).isEqualTo(512)
        assertThat(result.sourceUrl).isEqualTo(exact.sourceUrl)
        assertThat(result.sourceName).isEqualTo(exact.sourceName)
    }
}
