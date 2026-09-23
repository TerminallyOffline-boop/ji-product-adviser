package com.jitelecom.productadviser.domain.compatibility

import com.jitelecom.productadviser.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CpuEvaluator @Inject constructor() {
    fun evaluate(product: ProductSpec, min: RequirementSet, rec: RequirementSet?): ComponentCompatibilityResult {
        val cpu = product.processor
        val minTier = min.minimumCpuTier
        val recTier = rec?.minimumCpuTier
        if (minTier == null && min.acceptedProcessorIds.isEmpty()) return notApplicable("CPU", cpu?.displayName)
        if (cpu == null || cpu.performanceTier == null) return unknown("CPU", cpu?.displayName, minTier, recTier)
        val accepted = min.acceptedProcessorIds.isEmpty() || cpu.id in min.acceptedProcessorIds
        return tierResult("CPU", cpu.displayName, cpu.performanceTier, minTier, recTier, accepted)
    }
}

@Singleton
class GpuEvaluator @Inject constructor() {
    fun evaluate(product: ProductSpec, min: RequirementSet, rec: RequirementSet?): ComponentCompatibilityResult {
        val gpu = product.gpu
        val minTier = min.minimumGpuTier
        val recTier = rec?.minimumGpuTier
        if (minTier == null && min.minimumVramGB == null && min.acceptedGpuIds.isEmpty()) return notApplicable("GPU", gpu?.displayName)
        if (gpu == null || (minTier != null && gpu.performanceTier == null)) return unknown("GPU", gpu?.displayName, minTier, recTier)
        val accepted = min.acceptedGpuIds.isEmpty() || gpu.id in min.acceptedGpuIds
        val tier = if (minTier == null) ComponentStatus.MEETS_MINIMUM else tierStatus(gpu.performanceTier!!, minTier, recTier, accepted)
        val vramStatus = valueStatus(gpu.vramGB, min.minimumVramGB, rec?.minimumVramGB)
        val status = worst(tier, vramStatus)
        return ComponentCompatibilityResult(
            component = "GPU", actual = "${gpu.displayName}${gpu.vramGB?.let { " • ${it} GB VRAM" } ?: ""}",
            minimum = describeTierAndValue(minTier, min.minimumVramGB),
            recommended = describeTierAndValue(recTier, rec?.minimumVramGB), status = status,
            explanation = explanationFor(status, "graphics")
        )
    }
}

@Singleton
class RamEvaluator @Inject constructor() {
    fun evaluate(product: ProductSpec, min: RequirementSet, rec: RequirementSet?) = numericResult(
        "RAM", product.ramGB?.toDouble(), min.minimumRamGB?.toDouble(), rec?.minimumRamGB?.toDouble(), "GB"
    )
}

@Singleton
class StorageEvaluator @Inject constructor() {
    fun evaluate(product: ProductSpec, min: RequirementSet, rec: RequirementSet?) = numericResult(
        "Storage", product.storageGB?.toDouble(), min.minimumStorageGB?.toDouble(), rec?.minimumStorageGB?.toDouble(), "GB"
    )
}

@Singleton
class OperatingSystemEvaluator @Inject constructor() {
    fun evaluate(product: ProductSpec, min: RequirementSet, rec: RequirementSet?): ComponentCompatibilityResult {
        val actual = product.operatingSystemForCompatibility()
        if (min.supportedOperatingSystems.isEmpty()) return notApplicable("Operating system", actual)
        if (actual == null) return unknown("Operating system", null, null, null)
        val compatible = min.supportedOperatingSystems.any { required -> operatingSystemMatches(actual, required) }
        val recommendedMatch = rec?.supportedOperatingSystems?.takeIf { it.isNotEmpty() }?.any { required ->
            operatingSystemMatches(actual, required)
        } == true
        val supported = min.supportedOperatingSystems.joinToString(" • ")
        val inferred = product.hasInferredOperatingSystem()
        return ComponentCompatibilityResult(
            "Operating system", actual, supported, rec?.supportedOperatingSystems?.takeIf { it.isNotEmpty() }?.joinToString(" • "),
            if (!compatible) ComponentStatus.BELOW_MINIMUM else if (recommendedMatch) ComponentStatus.MEETS_RECOMMENDED else ComponentStatus.MEETS_MINIMUM,
            when {
                !compatible -> "This app is not available for ${platformLabel(actual)} in the stored catalog. Supported platforms: $supported."
                inferred -> "The app supports the inferred ${platformLabel(actual)} platform. Confirm the device's exact OS version before purchase."
                else -> "The app supports ${platformLabel(actual)}. Exact OS-version requirements may still apply."
            }
        )
    }
}

@Singleton
class RequirementEvaluator @Inject constructor(
    private val cpu: CpuEvaluator,
    private val gpu: GpuEvaluator,
    private val ram: RamEvaluator,
    private val storage: StorageEvaluator,
    private val os: OperatingSystemEvaluator
) {
    fun evaluate(product: ProductSpec, minimum: RequirementSet, recommended: RequirementSet?): List<ComponentCompatibilityResult> {
        val results = mutableListOf(
            cpu.evaluate(product, minimum, recommended), gpu.evaluate(product, minimum, recommended),
            ram.evaluate(product, minimum, recommended), storage.evaluate(product, minimum, recommended),
            os.evaluate(product, minimum, recommended)
        )
        if (minimum.requiredArchitecture != null) {
            val actual = product.architecture
            results += if (actual == null) unknown("Architecture", null, null, null) else ComponentCompatibilityResult(
                "Architecture", actual, minimum.requiredArchitecture, recommended?.requiredArchitecture,
                if (!actual.contains(minimum.requiredArchitecture, true)) ComponentStatus.BELOW_MINIMUM
                else if (recommended?.requiredArchitecture?.let { actual.contains(it, true) } == true) ComponentStatus.MEETS_RECOMMENDED
                else ComponentStatus.MEETS_MINIMUM,
                if (actual.contains(minimum.requiredArchitecture, true)) "Required architecture is present." else "Required architecture is not present."
            )
        }
        if (minimum.requiredFeatures.isNotEmpty()) {
            val missing = minimum.requiredFeatures.filterNot { required -> product.supportedFeatures.any { it.equals(required, true) } }
            results += ComponentCompatibilityResult(
                "Required features", product.supportedFeatures.ifEmpty { setOf("Unknown") }.joinToString(), minimum.requiredFeatures.joinToString(),
                recommended?.requiredFeatures?.joinToString(), if (missing.isEmpty()) ComponentStatus.MEETS_MINIMUM else ComponentStatus.BELOW_MINIMUM,
                if (missing.isEmpty()) "All stored required features are present." else "Missing: ${missing.joinToString()}."
            )
        }
        return results
    }
}

@Singleton
class CompatibilityExplanationBuilder @Inject constructor() {
    fun build(status: CompatibilityStatus, components: List<ComponentCompatibilityResult>): String {
        val limited = components.filter { it.status == ComponentStatus.BELOW_MINIMUM || it.status == ComponentStatus.MEETS_MINIMUM }
            .map { it.component.lowercase() }
        return when (status) {
            CompatibilityStatus.MEETS_RECOMMENDED -> "Meets the stored recommended requirements. Expected to be suitable for the documented workload, subject to actual configuration and workload."
            CompatibilityStatus.MEETS_MINIMUM -> "Meets the stored minimum requirements${limited.take(3).takeIf { it.isNotEmpty() }?.joinToString(prefix = "; limitations may involve ") ?: ""}. It may experience limited performance on demanding workloads."
            CompatibilityStatus.BELOW_MINIMUM -> "One or more required components fall below the stored minimum requirements. This configuration is not recommended for this workload."
            CompatibilityStatus.NOT_VERIFIED -> {
                val os = components.firstOrNull { it.component == "Operating system" }
                if (os?.status == ComponentStatus.MEETS_MINIMUM || os?.status == ComponentStatus.MEETS_RECOMMENDED) {
                    "The app is available for this operating-system family, but some hardware or requirement data still needs verification. Review the component details before recommending it."
                } else {
                    "There is not enough verified product or requirement data to make a confident compatibility determination."
                }
            }
        }
    }
}

@Singleton
class CompatibilityEngine @Inject constructor(
    private val evaluator: RequirementEvaluator,
    private val explanationBuilder: CompatibilityExplanationBuilder
) {
    fun evaluate(product: ProductSpec, software: SoftwareSpec, requirements: List<RequirementSet>): CompatibilityResult {
        val minimum = requirements.firstOrNull { it.type == RequirementType.MINIMUM }
        if (minimum == null) return unverified(product.id, software.id, "Minimum requirements are not stored.")
        val recommended = requirements.firstOrNull { it.type == RequirementType.RECOMMENDED }
        val components = evaluator.evaluate(product, minimum, recommended)
        val dataVerified = product.verificationStatus == VerificationStatus.VERIFIED &&
            software.verificationStatus == VerificationStatus.VERIFIED &&
            minimum.verificationStatus == VerificationStatus.VERIFIED &&
            (recommended == null || recommended.verificationStatus == VerificationStatus.VERIFIED)
        val status = when {
            components.any { it.status == ComponentStatus.BELOW_MINIMUM } -> CompatibilityStatus.BELOW_MINIMUM
            components.any { it.status == ComponentStatus.UNKNOWN } || !dataVerified -> CompatibilityStatus.NOT_VERIFIED
            recommended == null -> CompatibilityStatus.MEETS_MINIMUM
            components.any { it.status == ComponentStatus.MEETS_MINIMUM } -> CompatibilityStatus.MEETS_MINIMUM
            else -> CompatibilityStatus.MEETS_RECOMMENDED
        }
        return CompatibilityResult(product.id, software.id, status, components, explanationBuilder.build(status, components))
    }

    private fun unverified(productId: Long, softwareId: Long, reason: String) = CompatibilityResult(
        productId, softwareId, CompatibilityStatus.NOT_VERIFIED, emptyList(), reason
    )
}

private fun numericResult(name: String, actual: Double?, min: Double?, rec: Double?, unit: String): ComponentCompatibilityResult {
    if (min == null) return notApplicable(name, actual?.let { format(it, unit) })
    if (actual == null) return unknown(name, null, min, rec, unit)
    val status = valueStatus(actual, min, rec)
    return ComponentCompatibilityResult(name, format(actual, unit), format(min, unit), rec?.let { format(it, unit) }, status, explanationFor(status, name.lowercase()))
}

private fun tierResult(name: String, actualName: String, actual: Int, min: Int?, rec: Int?, accepted: Boolean): ComponentCompatibilityResult {
    val status = if (min == null) ComponentStatus.MEETS_MINIMUM else tierStatus(actual, min, rec, accepted)
    return ComponentCompatibilityResult(name, "$actualName • internal tier $actual", min?.let { "Internal tier $it" }, rec?.let { "Internal tier $it" }, status, explanationFor(status, name.lowercase()))
}

private fun tierStatus(actual: Int, min: Int, rec: Int?, accepted: Boolean) = when {
    !accepted || actual < min -> ComponentStatus.BELOW_MINIMUM
    rec != null && actual >= rec -> ComponentStatus.MEETS_RECOMMENDED
    else -> ComponentStatus.MEETS_MINIMUM
}

private fun valueStatus(actual: Double?, min: Double?, rec: Double?): ComponentStatus = when {
    min == null -> ComponentStatus.NOT_APPLICABLE
    actual == null -> ComponentStatus.UNKNOWN
    actual < min -> ComponentStatus.BELOW_MINIMUM
    rec != null && actual >= rec -> ComponentStatus.MEETS_RECOMMENDED
    else -> ComponentStatus.MEETS_MINIMUM
}

private fun worst(a: ComponentStatus, b: ComponentStatus): ComponentStatus {
    val order = listOf(ComponentStatus.BELOW_MINIMUM, ComponentStatus.UNKNOWN, ComponentStatus.MEETS_MINIMUM, ComponentStatus.MEETS_RECOMMENDED, ComponentStatus.NOT_APPLICABLE)
    return if (order.indexOf(a) < order.indexOf(b)) a else b
}

private fun unknown(name: String, actual: String?, min: Int?, rec: Int?) = ComponentCompatibilityResult(
    name, actual ?: "Unknown", min?.let { "Internal tier $it" }, rec?.let { "Internal tier $it" }, ComponentStatus.UNKNOWN, "Stored data is insufficient to evaluate this component."
)

private fun unknown(name: String, actual: String?, min: Double?, rec: Double?, unit: String) = ComponentCompatibilityResult(
    name, actual ?: "Unknown", min?.let { format(it, unit) }, rec?.let { format(it, unit) }, ComponentStatus.UNKNOWN, "Stored data is insufficient to evaluate this component."
)

private fun notApplicable(name: String, actual: String?) = ComponentCompatibilityResult(
    name, actual ?: "Not stored", null, null, ComponentStatus.NOT_APPLICABLE, "No stored requirement for this component."
)

private fun explanationFor(status: ComponentStatus, noun: String) = when (status) {
    ComponentStatus.MEETS_RECOMMENDED -> "The stored $noun specification meets the recommended requirement."
    ComponentStatus.MEETS_MINIMUM -> "The stored $noun specification meets the minimum requirement but is below, or cannot establish, the recommended level."
    ComponentStatus.BELOW_MINIMUM -> "The stored $noun specification is below the minimum requirement."
    ComponentStatus.UNKNOWN -> "Stored data is insufficient to evaluate $noun."
    ComponentStatus.NOT_APPLICABLE -> "No stored requirement for $noun."
}

private fun describeTierAndValue(tier: Int?, value: Double?): String? = listOfNotNull(tier?.let { "Tier $it" }, value?.let { "${format(it, "GB")} VRAM" }).takeIf { it.isNotEmpty() }?.joinToString(" • ")
private fun format(value: Double, unit: String) = "${if (value % 1.0 == 0.0) value.toInt() else value} $unit"
