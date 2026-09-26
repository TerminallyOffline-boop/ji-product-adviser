package com.jitelecom.productadviser.ui

import com.google.common.truth.Truth.assertThat
import com.jitelecom.productadviser.data.local.RequirementEntity
import com.jitelecom.productadviser.data.local.SoftwareEntity
import com.jitelecom.productadviser.domain.model.ProductCategory
import com.jitelecom.productadviser.domain.model.ProductSpec
import com.jitelecom.productadviser.domain.model.RequirementType
import com.jitelecom.productadviser.domain.model.VerificationStatus
import org.junit.Test

class DataQualityTest {
    @Test fun incompleteProductIsReportedInsteadOfSilentlyCompared() {
        val product=ProductSpec(1,"SKU","Brand","Model",category=ProductCategory.LAPTOP,price=20_000.0)
        val issues=buildDataQualityIssues(listOf(product),emptyList(),emptyList(),emptyList(),emptyList())
        assertThat(issues.any{it.severity==DataQualitySeverity.ERROR&&it.detail.contains("RAM")}).isTrue()
        assertThat(issues.any{it.detail.contains("Operating system")}).isTrue()
    }

    @Test fun recommendedValuesBelowMinimumAreBlockingIssues() {
        val app=SoftwareEntity(1,"App",version="1",category="Design",platform="Windows",requirementsSourceUrl="https://example.com/requirements",verificationStatus=VerificationStatus.VERIFIED)
        val minimum=RequirementEntity(1,1,RequirementType.MINIMUM,minimumRamGB=8,supportedOperatingSystems=setOf("Windows 11"),verificationStatus=VerificationStatus.VERIFIED,platform="Windows")
        val recommended=RequirementEntity(2,1,RequirementType.RECOMMENDED,minimumRamGB=4,supportedOperatingSystems=setOf("Windows 11"),verificationStatus=VerificationStatus.VERIFIED,platform="Windows")
        val issues=buildDataQualityIssues(emptyList(),emptyList(),emptyList(),listOf(app),listOf(minimum,recommended))
        assertThat(issues.any{it.severity==DataQualitySeverity.ERROR&&it.detail.contains("RAM is below the minimum")}).isTrue()
    }
}
