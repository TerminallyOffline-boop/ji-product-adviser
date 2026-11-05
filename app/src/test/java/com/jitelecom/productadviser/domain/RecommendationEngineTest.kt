package com.jitelecom.productadviser.domain

import com.google.common.truth.Truth.assertThat
import com.jitelecom.productadviser.domain.compatibility.*
import com.jitelecom.productadviser.domain.model.*
import com.jitelecom.productadviser.domain.recommendation.*
import org.junit.Test
import java.time.LocalDate

class RecommendationEngineTest {
    private val compatibility = CompatibilityEngine(RequirementEvaluator(CpuEvaluator(),GpuEvaluator(),RamEvaluator(),StorageEvaluator(),OperatingSystemEvaluator()),CompatibilityExplanationBuilder())
    private val engine = RecommendationEngine(compatibility,BudgetEvaluator(),ProductScorer(BudgetEvaluator(),SoftwareCompatibilityScorer(),PerformanceScorer(),PreferenceScorer()),RecommendationExplanationBuilder())
    private val cpu=ProcessorSpec(1,"T","F","CPU",performanceTier=5);private val gpu=GpuSpec(1,"T","GPU",GpuType.DEDICATED,4.0,performanceTier=5)
    private fun product(id:Long,price:Double,brand:String="A",ram:Int?=16,cpuValue:ProcessorSpec?=cpu)=ProductSpec(id,"S$id",brand,"M$id",category=ProductCategory.LAPTOP,price=price,availabilityStatus=AvailabilityStatus.AVAILABLE,processor=cpuValue,gpu=gpu,ramGB=ram,storageGB=512,operatingSystem="Windows 11",architecture="x64",verificationStatus=VerificationStatus.VERIFIED)
    private val apps=mapOf(1L to SoftwareSpec(1,"App","1","D","CAD","Windows",verificationStatus=VerificationStatus.VERIFIED),2L to SoftwareSpec(2,"App2","1","D","CAD","Windows",verificationStatus=VerificationStatus.VERIFIED))
    private fun req(id:Long)=listOf(RequirementSet(id*2,id,RequirementType.MINIMUM,8,20,3,3,verificationStatus=VerificationStatus.VERIFIED),RequirementSet(id*2+1,id,RequirementType.RECOMMENDED,16,100,5,5,verificationStatus=VerificationStatus.VERIFIED))
    private fun run(request:CustomerRequest,products:List<ProductSpec>)=engine.recommend(request,products,apps,mapOf(1L to req(1),2L to req(2)))

    @Test fun budgetFiltering(){assertThat(run(CustomerRequest(budget=30000.0),listOf(product(1,29000.0),product(2,31000.0))).map{it.product.id}).containsExactly(1L)}
    @Test fun softwareCompatibilityAffectsScore(){val good=product(1,30000.0);val weak=product(2,30000.0,ram=4);val result=run(CustomerRequest(budget=40000.0,softwareIds=setOf(1)),listOf(weak,good));assertThat(result.first().product.id).isEqualTo(1)}
    @Test fun multipleSoftwareRequirements(){val result=run(CustomerRequest(budget=40000.0,softwareIds=setOf(1,2)),listOf(product(1,30000.0)));assertThat(result.single().compatibility).hasSize(2)}
    @Test fun brandFiltering(){val result=run(CustomerRequest(budget=40000.0,preferredBrand="B"),listOf(product(1,30000.0,"A"),product(2,30000.0,"B")));assertThat(result.single().product.brand).isEqualTo("B")}
    @Test fun aboveBudgetRequiresOptIn(){val products=listOf(product(1,32000.0));assertThat(run(CustomerRequest(budget=30000.0),products)).isEmpty();assertThat(run(CustomerRequest(budget=30000.0,showSlightlyAboveBudget=true),products)).isNotEmpty()}
    @Test fun customAboveBudgetPercentIsApplied(){val products=listOf(product(1,35000.0));assertThat(run(CustomerRequest(budget=30000.0,showSlightlyAboveBudget=true,aboveBudgetPercent=10),products)).isEmpty();assertThat(run(CustomerRequest(budget=30000.0,showSlightlyAboveBudget=true,aboveBudgetPercent=20),products)).isNotEmpty()}
    @Test fun missingSpecificationsArePenalized(){val complete=product(1,30000.0);val missing=product(2,30000.0,ram=null,cpuValue=null);val result=run(CustomerRequest(budget=40000.0,softwareIds=setOf(1)),listOf(missing,complete));assertThat(result.first().product.id).isEqualTo(1)}
    @Test fun requiredSoftwareBelowMinimumIsNotRecommended(){val weak=product(1,30000.0,ram=4);assertThat(run(CustomerRequest(budget=40000.0,softwareIds=setOf(1)),listOf(weak))).isEmpty()}
    @Test fun requestedMinimumRamAndStorageAreHardFilters(){val lowRam=product(1,25000.0,ram=8);val tooLittleStorage=product(2,26000.0).copy(storageGB=256);val match=product(3,30000.0,ram=16);val request=CustomerRequest(budget=40000.0,minimumRamGB=16,minimumStorageGB=512);assertThat(run(request,listOf(lowRam,tooLittleStorage,match)).map{it.product.id}).containsExactly(3L)}
    @Test fun localQueryParsesRamStorageAndFlagsUnappliedDisplayRequest(){val parsed=engine.parseLocalQuery("₱30,000 laptop with 16 GB RAM, 1 TB SSD and OLED",apps.values.toList());assertThat(parsed.budget).isEqualTo(30000.0);assertThat(parsed.minimumRamGB).isEqualTo(16);assertThat(parsed.minimumStorageGB).isEqualTo(1024);assertThat(parsed.unappliedConstraints).contains("oled")}
    @Test fun stockAvailabilityExpiresUnlessRecentlyChecked(){val p=product(1,30000.0).copy(availabilityUpdatedAt="2026-10-31");assertThat(p.currentAvailability(LocalDate.parse("2026-11-05"))).isEqualTo(AvailabilityStatus.AVAILABLE);assertThat(p.currentAvailability(LocalDate.parse("2026-11-16"))).isEqualTo(AvailabilityStatus.UNKNOWN);assertThat(product(2,30000.0).copy(availabilityUpdatedAt="2026-07").currentAvailability(LocalDate.parse("2026-11-05"))).isEqualTo(AvailabilityStatus.UNKNOWN)}
    @Test fun selectedRamStoragePriorityChangesPreferenceScore(){val scorer=PreferenceScorer();val request=CustomerRequest(profile="Office Worker",budget=50000.0,priorities=setOf("RAM / storage"));assertThat(scorer.score(product(1,30000.0,ram=16),request)).isGreaterThan(scorer.score(product(2,30000.0,ram=8).copy(storageGB=256),request))}
    @Test fun demandingStudentProfileIsNotAlsoScoredAsEveryday(){val score=PreferenceScorer().score(product(1,30000.0),CustomerRequest(profile="Architecture Student",budget=50000.0));assertThat(score).isWithin(0.0001).of(0.53)}
}
