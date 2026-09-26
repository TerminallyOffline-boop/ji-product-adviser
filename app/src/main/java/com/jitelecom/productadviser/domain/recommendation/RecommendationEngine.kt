package com.jitelecom.productadviser.domain.recommendation

import com.jitelecom.productadviser.domain.compatibility.CompatibilityEngine
import com.jitelecom.productadviser.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class BudgetEvaluator @Inject constructor() {
    fun maxAllowed(request: CustomerRequest) = request.budget * if (request.showSlightlyAboveBudget) 1.0 + request.aboveBudgetPercent / 100.0 else 1.0
    fun score(price: Double, budget: Double): Double = when {
        budget <= 0 -> 0.0
        price <= budget -> (0.65 + 0.35 * (price / budget)).coerceIn(0.0, 1.0)
        else -> (1.0 - ((price - budget) / budget) * 3).coerceIn(0.0, 0.6)
    }
}

@Singleton
class PerformanceScorer @Inject constructor() {
    fun score(product: ProductSpec): Double {
        val cpu = product.processor?.performanceTier ?: return 0.0
        val gpu = product.gpu?.performanceTier ?: 1
        return ((cpu * 0.65 + gpu * 0.35) / 7.0).coerceIn(0.0, 1.0)
    }
}

@Singleton
class SoftwareCompatibilityScorer @Inject constructor() {
    fun score(results: Collection<CompatibilityResult>): Double {
        if (results.isEmpty()) return 0.5
        return results.map {
            val compatibilityScore = when (it.status) {
                CompatibilityStatus.MEETS_RECOMMENDED -> 1.0
                CompatibilityStatus.MEETS_MINIMUM -> 0.65
                CompatibilityStatus.NOT_VERIFIED -> 0.20
                CompatibilityStatus.NOT_AVAILABLE -> 0.0
                CompatibilityStatus.BELOW_MINIMUM -> 0.0
            }
            val confidenceMultiplier = when (it.dataStatus) {
                VerificationStatus.VERIFIED -> 1.0
                VerificationStatus.NEEDS_REVIEW -> 0.85
                VerificationStatus.UNVERIFIED -> 0.65
                VerificationStatus.OUTDATED -> 0.55
            }
            compatibilityScore * confidenceMultiplier
        }.average()
    }
}

@Singleton
class PreferenceScorer @Inject constructor() {
    fun score(product: ProductSpec, request: CustomerRequest): Double {
        var points = 0.35
        if (request.preferredBrand != null && product.brand.equals(request.preferredBrand, true)) points += 0.20
        if (request.priorities.any { it.equals("Portability", true) } && (product.weightKg ?: 99.0) <= 1.7) points += 0.18
        if (request.priorities.any { it.equals("Battery", true) } && (product.batteryCapacityWh ?: 0.0) >= 50) points += 0.18
        if (request.priorities.any { it.equals("RAM / storage", true) } && (product.ramGB ?: 0) >= 16 && (product.storageGB ?: 0) >= 512) points += 0.18
        if (request.priorities.any { it.equals("Performance", true) } && (product.processor?.performanceTier ?: 0) >= 5 && (product.gpu?.performanceTier ?: 0) >= 4) points += 0.18
        val demandingProfile = request.profile?.lowercase()?.let { profile ->
            listOf("architecture", "engineering", "programmer", "graphic", "video", "gamer", "content").any(profile::contains)
        } == true
        if (demandingProfile && (product.processor?.performanceTier ?: 0) >= 5 && (product.ramGB ?: 0) >= 16) points += 0.18
        val everydayProfile = !demandingProfile && request.profile?.lowercase()?.let { profile ->
            listOf("student", "teacher", "office", "business").any(profile::contains)
        } == true
        if (everydayProfile && (product.ramGB ?: 0) >= 8 && (product.storageGB ?: 0) >= 256) points += 0.12
        return points.coerceIn(0.0, 1.0)
    }
}

@Singleton
class ProductScorer @Inject constructor(
    private val budget: BudgetEvaluator,
    private val compatibility: SoftwareCompatibilityScorer,
    private val performance: PerformanceScorer,
    private val preference: PreferenceScorer
) {
    fun score(product: ProductSpec, request: CustomerRequest, results: Collection<CompatibilityResult>, weights: RecommendationWeights): Int {
        val memoryStorage = (((product.ramGB ?: 0) / 32.0) * 0.55 + ((product.storageGB ?: 0) / 1024.0) * 0.45).coerceIn(0.0, 1.0)
        val total = compatibility.score(results) * weights.compatibility +
            performance.score(product) * weights.performance + budget.score(product.effectivePrice, request.budget) * weights.budget +
            memoryStorage * weights.memoryStorage + preference.score(product, request) * weights.preferences
        return (total * 100).roundToInt().coerceIn(0, 100)
    }
}

@Singleton
class RecommendationExplanationBuilder @Inject constructor() {
    fun build(product: ProductSpec, score: Int, results: Collection<CompatibilityResult>): Triple<List<String>, List<String>, String> {
        val strengths = buildList {
            if (results.isNotEmpty() && results.all { it.status == CompatibilityStatus.MEETS_RECOMMENDED }) add("Meets stored recommended requirements for all selected software")
            if ((product.ramGB ?: 0) >= 16) add("16 GB or more memory")
            if ((product.storageGB ?: 0) >= 512) add("512 GB or more storage")
            if ((product.processor?.performanceTier ?: 0) >= 5) add("Higher internal CPU performance tier")
            if (product.weightKg != null && product.weightKg <= 1.7) add("Portable weight")
        }.ifEmpty { listOf("Fits the selected filters") }
        val limitations = buildList {
            if (results.any { it.status == CompatibilityStatus.MEETS_MINIMUM }) add("Some selected software only meets minimum requirements")
            if (results.any { it.status == CompatibilityStatus.BELOW_MINIMUM }) add("Below minimum for at least one selected application")
            if (results.any { it.status == CompatibilityStatus.NOT_AVAILABLE }) add("At least one selected application is unavailable on this platform")
            if (results.any { it.status == CompatibilityStatus.NOT_VERIFIED }) add("A required product or app specification is missing")
        }
        val explanation = "Internal recommendation score: $score/100. ${strengths.first()}. This is a relative match against locally stored products, not a performance guarantee."
        return Triple(strengths, limitations, explanation)
    }
}

@Singleton
class RecommendationEngine @Inject constructor(
    private val compatibilityEngine: CompatibilityEngine,
    private val budget: BudgetEvaluator,
    private val scorer: ProductScorer,
    private val explanationBuilder: RecommendationExplanationBuilder
) {
    fun recommend(
        request: CustomerRequest,
        products: List<ProductSpec>,
        software: Map<Long, SoftwareSpec>,
        requirements: Map<Long, List<RequirementSet>>,
        weights: RecommendationWeights = RecommendationWeights(),
        limit: Int = 5
    ): List<RecommendationResult> = products.asSequence()
        .filter { !it.archived && it.category == request.category && it.availabilityStatus != AvailabilityStatus.UNAVAILABLE }
        .filter { it.effectivePrice <= budget.maxAllowed(request) }
        .filter { request.preferredBrand.isNullOrBlank() || it.brand.equals(request.preferredBrand, true) }
        .mapNotNull { product ->
            val compatibility = request.softwareIds.mapNotNull { id ->
                software[id]?.let { id to compatibilityEngine.evaluate(product, it, requirements[id].orEmpty()) }
            }.toMap()
            if (compatibility.values.any { it.status == CompatibilityStatus.NOT_AVAILABLE || it.status == CompatibilityStatus.BELOW_MINIMUM }) return@mapNotNull null
            val score = scorer.score(product, request, compatibility.values, weights)
            val (strengths, limitations, explanation) = explanationBuilder.build(product, score, compatibility.values)
            RecommendationResult(product, score, compatibility, strengths, limitations, explanation)
        }
        .sortedWith(compareByDescending<RecommendationResult> { it.internalScore }.thenBy { it.product.effectivePrice })
        .take(limit.coerceIn(3, 5)).toList()

    fun parseLocalQuery(text: String, knownSoftware: List<SoftwareSpec>): ParsedQuery {
        val lower = text.lowercase()
        val budget = Regex("(?:₱|php\\s*)?(\\d{1,3})(?:k|,000)").find(lower)?.groupValues?.get(1)?.toDoubleOrNull()?.times(1000)
        val category = ProductCategory.entries.firstOrNull { lower.contains(it.name.lowercase().replace('_', ' ')) }
        val profile = listOf("Architecture Student", "Engineering Student", "Programmer", "Graphic Designer", "Video Editor", "Gamer", "Student")
            .firstOrNull { lower.contains(it.lowercase().removeSuffix(" student")) }
        val softwareIds = knownSoftware.filter { lower.contains(it.name.lowercase()) }.map { it.id }.toSet()
        return ParsedQuery(budget, category, profile, softwareIds)
    }
}

data class ParsedQuery(val budget: Double?, val category: ProductCategory?, val profile: String?, val softwareIds: Set<Long>)

interface AiQueryParser { suspend fun parse(query: String): CustomerRequest }
interface InventoryProvider { suspend fun availability(productId: Long): AvailabilityStatus }
interface AnalyticsSink { suspend fun record(event: String, metadata: Map<String, String> = emptyMap()) }
