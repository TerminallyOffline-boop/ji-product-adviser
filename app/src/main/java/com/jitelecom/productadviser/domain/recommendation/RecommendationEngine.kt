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
            if (results.any { it.status == CompatibilityStatus.MEETS_MINIMUM }) add("Selected software can run at minimum level; performance may be lower than recommended")
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
        fun containsPhrase(phrase: String) = Regex("(?<![a-z0-9])${Regex.escape(phrase)}(?![a-z0-9])").containsMatchIn(lower)
        val budgetMatch = Regex("(?:₱|php\\s*)?((?:\\d{1,3}(?:,\\d{3})+)|(?:\\d+(?:\\.\\d+)?\\s*k)|(?:\\d{4,6}))", RegexOption.IGNORE_CASE)
            .find(lower)
        val rawBudget = budgetMatch?.groupValues?.get(1)?.trim()?.replace(",", "")
        val parsedNumber = rawBudget?.removeSuffix("k")?.trim()?.toDoubleOrNull()
        val usesThousands = rawBudget?.endsWith("k") == true
        val explicitBudgetContext = "₱" in lower || "php" in lower || usesThousands || "," in (budgetMatch?.value ?: "") ||
            listOf("budget", "max", "under", "below", "around", "up to", "less than").any { it in lower }
        val deviceContext = listOf("laptop", "notebook", "macbook", "chromebook", "phone", "smartphone", "iphone", "tablet", "ipad", "desktop", "computer", "pc").any(::containsPhrase)
        val hasBudgetContext = explicitBudgetContext || (rawBudget?.count(Char::isDigit) ?: 0) >= 4 && deviceContext
        val budget = parsedNumber?.let { if (usesThousands) it * 1000 else it }
            ?.takeIf { hasBudgetContext && it in 1_000.0..10_000_000.0 && !(it in 1900.0..2100.0 && !explicitBudgetContext) }

        val category = when {
            listOf("smartphone", "smart phone", "mobile phone", "iphone", "android phone", "phone").any(::containsPhrase) -> ProductCategory.SMARTPHONE
            listOf("tablet", "ipad", "galaxy tab", "redmi pad").any(::containsPhrase) -> ProductCategory.TABLET
            listOf("laptop", "notebook", "macbook", "chromebook", "windows laptop").any(::containsPhrase) -> ProductCategory.LAPTOP
            listOf("desktop", "pc", "computer", "imac", "mac mini").any(::containsPhrase) -> ProductCategory.DESKTOP
            else -> ProductCategory.entries.firstOrNull { lower.contains(it.name.lowercase().replace('_', ' ')) }
        }
        val profileAliases = listOf(
            "Architecture Student" to listOf("architecture", "architect"),
            "Engineering Student" to listOf("engineering", "engineer"),
            "Programmer" to listOf("programmer", "programming", "developer", "coding", "code"),
            "Graphic Designer" to listOf("graphic designer", "graphic design", "designer", "photoshop"),
            "Video Editor" to listOf("video editor", "video editing", "edit videos"),
            "Gamer" to listOf("gamer", "gaming", "games"),
            "Content Creator" to listOf("content creator", "streamer", "streaming", "creator"),
            "Business Owner" to listOf("business owner", "business", "entrepreneur"),
            "Office Worker" to listOf("office worker", "office work"),
            "Teacher" to listOf("teacher", "teaching"),
            "Student" to listOf("student", "school", "college")
        )
        val profile = profileAliases.firstOrNull { (_, aliases) -> aliases.any { it in lower } }?.first
        val queryWords = Regex("[a-z0-9]+(?:[+#][a-z0-9]+)?").findAll(lower).map { it.value }.toSet()
        val ignoredAppWords = setOf("adobe", "microsoft", "mobile", "desktop", "current", "workplace", "studio", "pro", "app", "software")
        val aliases = mapOf(
            "photoshop" to listOf("photoshop", "adobe photoshop"),
            "premiere" to listOf("premiere", "premiere pro"),
            "autocad" to listOf("autocad", "auto cad"),
            "365" to listOf("microsoft 365", "office 365"),
            "vs code" to listOf("vs code", "vscode", "visual studio code"),
            "davinci" to listOf("davinci", "resolve"),
            "final cut" to listOf("final cut", "final cut pro"),
            "obs" to listOf("obs", "obs studio"),
            "chrome" to listOf("chrome", "google chrome"),
            "capcut" to listOf("capcut", "cap cut"),
            "zoom" to listOf("zoom", "zoom workplace"),
            "roblox" to listOf("roblox"),
            "valorant" to listOf("valorant")
        )
        val softwareIds = knownSoftware.filter { app ->
            val appName = app.name.lowercase()
            val nameWords = Regex("[a-z0-9]+(?:[+#][a-z0-9]+)?").findAll(appName)
                .map { it.value }.filter { it !in ignoredAppWords && it.length >= 3 }.toList()
            val aliasMatches = aliases.values.any { terms ->
                terms.any { term -> term in lower && (appName.contains(term) || (term == "vs code" && "visual studio code" in appName) || (term == "auto cad" && "autocad" in appName)) }
            }
            aliasMatches || nameWords.any { it in queryWords }
        }.map { it.id }.toSet()
        return ParsedQuery(budget, category, profile, softwareIds)
    }
}

data class ParsedQuery(val budget: Double?, val category: ProductCategory?, val profile: String?, val softwareIds: Set<Long>)

interface AiQueryParser { suspend fun parse(query: String): CustomerRequest }
interface InventoryProvider { suspend fun availability(productId: Long): AvailabilityStatus }
interface AnalyticsSink { suspend fun record(event: String, metadata: Map<String, String> = emptyMap()) }
