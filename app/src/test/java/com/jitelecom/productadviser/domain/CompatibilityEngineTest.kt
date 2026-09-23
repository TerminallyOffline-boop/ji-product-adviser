package com.jitelecom.productadviser.domain

import com.google.common.truth.Truth.assertThat
import com.jitelecom.productadviser.domain.compatibility.*
import com.jitelecom.productadviser.domain.model.*
import org.junit.Test

class CompatibilityEngineTest {
    private val engine = CompatibilityEngine(RequirementEvaluator(CpuEvaluator(), GpuEvaluator(), RamEvaluator(), StorageEvaluator(), OperatingSystemEvaluator()), CompatibilityExplanationBuilder())
    private val cpu = ProcessorSpec(1,"Test","Family","CPU",performanceTier=5)
    private val gpu = GpuSpec(1,"Test","GPU",GpuType.DEDICATED,vramGB=6.0,performanceTier=5)
    private val software = SoftwareSpec(1,"Design App","1","Vendor","CAD","Windows",verificationStatus=VerificationStatus.VERIFIED)
    private fun product(cpuValue:ProcessorSpec?=cpu,gpuValue:GpuSpec?=gpu,ram:Int?=16,storage:Int?=512,os:String?="Windows 11",verified:VerificationStatus=VerificationStatus.VERIFIED)=ProductSpec(1,"SKU","Brand","Model",category=ProductCategory.LAPTOP,price=30000.0,processor=cpuValue,gpu=gpuValue,ramGB=ram,storageGB=storage,operatingSystem=os,architecture="x64",verificationStatus=verified)
    private fun min(cpuTier:Int=3,gpuTier:Int=3,ram:Int=8)=RequirementSet(1,1,RequirementType.MINIMUM,ram,20,cpuTier,gpuTier,2.0,"x64",setOf("Windows 11"),verificationStatus=VerificationStatus.VERIFIED)
    private fun rec()=RequirementSet(2,1,RequirementType.RECOMMENDED,16,100,5,5,4.0,"x64",setOf("Windows 11"),verificationStatus=VerificationStatus.VERIFIED)

    @Test fun meetsRecommended(){assertThat(engine.evaluate(product(),software,listOf(min(),rec())).status).isEqualTo(CompatibilityStatus.MEETS_RECOMMENDED)}
    @Test fun meetsMinimum(){assertThat(engine.evaluate(product(ram=8),software,listOf(min(),rec())).status).isEqualTo(CompatibilityStatus.MEETS_MINIMUM)}
    @Test fun ramBelowMinimum(){assertThat(engine.evaluate(product(ram=4),software,listOf(min(),rec())).status).isEqualTo(CompatibilityStatus.BELOW_MINIMUM)}
    @Test fun cpuBelowMinimum(){assertThat(engine.evaluate(product(cpuValue=cpu.copy(performanceTier=2)),software,listOf(min(),rec())).status).isEqualTo(CompatibilityStatus.BELOW_MINIMUM)}
    @Test fun gpuBelowMinimum(){assertThat(engine.evaluate(product(gpuValue=gpu.copy(performanceTier=2)),software,listOf(min(),rec())).status).isEqualTo(CompatibilityStatus.BELOW_MINIMUM)}
    @Test fun unsupportedOperatingSystem(){assertThat(engine.evaluate(product(os="Linux"),software,listOf(min(),rec())).status).isEqualTo(CompatibilityStatus.NOT_AVAILABLE)}
    @Test fun unknownCpu(){assertThat(engine.evaluate(product(cpuValue=null),software,listOf(min(),rec())).status).isEqualTo(CompatibilityStatus.NOT_VERIFIED)}
    @Test fun unknownGpu(){assertThat(engine.evaluate(product(gpuValue=null),software,listOf(min(),rec())).status).isEqualTo(CompatibilityStatus.NOT_VERIFIED)}
    @Test fun missingRequirementData(){assertThat(engine.evaluate(product(),software,emptyList()).status).isEqualTo(CompatibilityStatus.NOT_VERIFIED)}
    @Test fun missingProductData(){assertThat(engine.evaluate(product(ram=null),software,listOf(min(),rec())).status).isEqualTo(CompatibilityStatus.NOT_VERIFIED)}
    @Test fun notVerified(){assertThat(engine.evaluate(product(verified=VerificationStatus.UNVERIFIED),software,listOf(min(),rec())).status).isEqualTo(CompatibilityStatus.NOT_VERIFIED)}

    @Test fun macOsMatchesCrossPlatformDesktopApp(){
        val macMinimum=min().copy(requiredArchitecture=null,supportedOperatingSystems=setOf("Windows 11","macOS"))
        val macRecommended=rec().copy(requiredArchitecture=null,supportedOperatingSystems=setOf("Windows 11","macOS"))
        val result=engine.evaluate(product(os="macOS"),software.copy(platform="Windows • macOS"),listOf(macMinimum,macRecommended))
        assertThat(result.status).isEqualTo(CompatibilityStatus.MEETS_RECOMMENDED)
        assertThat(result.components.first{it.component=="Operating system"}.status).isEqualTo(ComponentStatus.MEETS_RECOMMENDED)
    }

    @Test fun windowsOnlyAppIsUnavailableOnMac(){
        val result=engine.evaluate(product(os="macOS"),software.copy(platform="Windows"),listOf(min(),rec()))
        assertThat(result.status).isEqualTo(CompatibilityStatus.NOT_AVAILABLE)
        assertThat(result.components.first{it.component=="Operating system"}.explanation).contains("not available for macOS")
        assertThat(result.components.first{it.component=="Operating system"}.status).isEqualTo(ComponentStatus.NOT_AVAILABLE)
    }

    @Test fun androidIsInferredForNonApplePhone(){
        val phone=product(os=null).copy(brand="Samsung",category=ProductCategory.SMARTPHONE,architecture="arm64")
        val mobileMin=min().copy(requiredArchitecture=null,supportedOperatingSystems=setOf("Android","iOS","iPadOS"))
        val mobileRec=rec().copy(requiredArchitecture=null,supportedOperatingSystems=setOf("Android","iOS","iPadOS"))
        val result=engine.evaluate(phone,software.copy(platform="Android • iOS • iPadOS"),listOf(mobileMin,mobileRec))
        assertThat(result.components.first{it.component=="Operating system"}.status).isEqualTo(ComponentStatus.MEETS_RECOMMENDED)
        assertThat(result.components.first{it.component=="Operating system"}.actual).contains("inferred")
    }

    @Test fun platformFilteringSeparatesDesktopAndMobileApps(){
        val desktop=software.copy(platform="Windows • macOS")
        val mobile=software.copy(id=2,platform="Android • iOS • iPadOS")
        assertThat(desktop.supportsOperatingSystem("macOS")).isTrue()
        assertThat(desktop.supportsOperatingSystem("Android 16")).isFalse()
        assertThat(mobile.supportsOperatingSystem("iPadOS")).isTrue()
        assertThat(mobile.supportsOperatingSystem("Windows 11")).isFalse()
    }
}
