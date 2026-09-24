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
        if (cpu == null) return unknown("CPU", null, minTier, recTier)
        val accepted = min.acceptedProcessorIds.isEmpty() || cpu.id in min.acceptedProcessorIds
        if (!accepted) return ComponentCompatibilityResult(
            "CPU", cpu.displayName, "Approved processor list", recTier?.let { "Internal tier $it" },
            ComponentStatus.BELOW_MINIMUM, "This processor is not in the stored list of accepted processors."
        )
        if (minTier == null) return ComponentCompatibilityResult(
            "CPU", cpu.displayName, "Approved processor list", recTier?.let { "Internal tier $it" },
            ComponentStatus.MEETS_MINIMUM, "This processor is in the stored list of accepted processors."
        )
        if (cpu.performanceTier == null) return unknown("CPU", cpu.displayName, minTier, recTier)
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
        if (gpu == null) return unknown("GPU", null, minTier, recTier)
        val accepted = min.acceptedGpuIds.isEmpty() || gpu.id in min.acceptedGpuIds
        if (!accepted) return ComponentCompatibilityResult(
            "GPU", gpu.displayName, "Approved graphics list", recTier?.let { "Internal tier $it" },
            ComponentStatus.BELOW_MINIMUM, "This graphics processor is not in the stored list of accepted GPUs."
        )
        if (minTier != null && gpu.performanceTier == null) return unknown("GPU", gpu.displayName, minTier, recTier)
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
                if (!architectureMatches(actual, minimum.requiredArchitecture)) ComponentStatus.BELOW_MINIMUM
                else if (recommended?.requiredArchitecture?.let { architectureMatches(actual, it) } == true) ComponentStatus.MEETS_RECOMMENDED
                else ComponentStatus.MEETS_MINIMUM,
                if (architectureMatches(actual, minimum.requiredArchitecture)) "Required architecture is present." else "Required architecture is not present."
            )
        }
        if (minimum.requiredFeatures.isNotEmpty()) {
            if (product.supportedFeatures.isEmpty()) {
                results += ComponentCompatibilityResult(
                    "Required features", "Unknown", minimum.requiredFeatures.joinToString(), recommended?.requiredFeatures?.joinToString(),
                    ComponentStatus.UNKNOWN, "Stored product data does not say whether these required features are present."
                )
            } else {
                val missing = minimum.requiredFeatures.filterNot { required -> product.supportedFeatures.any { it.equals(required, true) } }
                results += ComponentCompatibilityResult(
                    "Required features", product.supportedFeatures.joinToString(), minimum.requiredFeatures.joinToString(),
                    recommended?.requiredFeatures?.joinToString(), if (missing.isEmpty()) ComponentStatus.MEETS_MINIMUM else ComponentStatus.BELOW_MINIMUM,
                    if (missing.isEmpty()) "All stored required features are present." else "Missing: ${missing.joinToString()}."
                )
            }
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
            CompatibilityStatus.NOT_AVAILABLE -> "This application is not offered for the device's operating-system platform. This is a platform availability limitation, not a hardware-performance failure."
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
        val operatingSystem = product.operatingSystemForCompatibility()
        val platformCompatibility = platformCompatibility(software.platform, operatingSystem)
        val storedDataStatus = aggregateVerificationStatus(
            listOf(product.verificationStatus, software.verificationStatus) + requirements.map { it.verificationStatus }
        )
        if (platformCompatibility == PlatformCompatibility.NOT_SUPPORTED) {
            val actualPlatform = platformLabel(operatingSystem)
            return CompatibilityResult(
                productId = product.id,
                softwareId = software.id,
                status = CompatibilityStatus.NOT_AVAILABLE,
                components = listOf(
                    ComponentCompatibilityResult(
                        component = "Operating system",
                        actual = operatingSystem ?: "Unknown",
                        minimum = software.platform,
                        status = ComponentStatus.NOT_AVAILABLE,
                        explanation = "${software.name} is not available for $actualPlatform. Stored app platforms: ${software.platform}."
                    )
                ),
                explanation = "${software.name} is not available for $actualPlatform. Choose an app built for this platform or a different device.",
                dataStatus = storedDataStatus
            )
        }
        val minimum = requirements.firstOrNull { it.type == RequirementType.MINIMUM }
        if (minimum == null) return unverified(product.id, software.id, "Minimum requirements are not stored.", VerificationStatus.UNVERIFIED)
        val recommended = requirements.firstOrNull { it.type == RequirementType.RECOMMENDED }
        val components = evaluator.evaluate(product, minimum, recommended)
        val hasUnresolvedData = platformCompatibility == PlatformCompatibility.UNKNOWN ||
            components.any { it.status == ComponentStatus.UNKNOWN }
        val dataStatus = if (hasUnresolvedData && storedDataStatus == VerificationStatus.VERIFIED) {
            VerificationStatus.NEEDS_REVIEW
        } else {
            storedDataStatus
        }
        val status = when {
            components.any { it.status == ComponentStatus.BELOW_MINIMUM } -> CompatibilityStatus.BELOW_MINIMUM
            hasUnresolvedData -> CompatibilityStatus.NOT_VERIFIED
            recommended == null -> CompatibilityStatus.MEETS_MINIMUM
            components.any { it.status == ComponentStatus.MEETS_MINIMUM } -> CompatibilityStatus.MEETS_MINIMUM
            else -> CompatibilityStatus.MEETS_RECOMMENDED
        }
        return CompatibilityResult(
            productId = product.id,
            softwareId = software.id,
            status = status,
            components = components,
            explanation = explanationBuilder.build(status, components),
            dataStatus = dataStatus
        )
    }

    private fun unverified(productId: Long, softwareId: Long, reason: String, dataStatus: VerificationStatus) = CompatibilityResult(
        productId = productId,
        softwareId = softwareId,
        status = CompatibilityStatus.NOT_VERIFIED,
        components = emptyList(),
        explanation = reason,
        dataStatus = dataStatus
    )
}

private fun aggregateVerificationStatus(statuses: List<VerificationStatus>): VerificationStatus = when {
    statuses.any { it == VerificationStatus.OUTDATED } -> VerificationStatus.OUTDATED
    statuses.any { it == VerificationStatus.UNVERIFIED } -> VerificationStatus.UNVERIFIED
    statuses.any { it == VerificationStatus.NEEDS_REVIEW } -> VerificationStatus.NEEDS_REVIEW
    else -> VerificationStatus.VERIFIED
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
    val order = listOf(ComponentStatus.NOT_AVAILABLE, ComponentStatus.BELOW_MINIMUM, ComponentStatus.UNKNOWN, ComponentStatus.MEETS_MINIMUM, ComponentStatus.MEETS_RECOMMENDED, ComponentStatus.NOT_APPLICABLE)
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
    ComponentStatus.NOT_AVAILABLE -> "This application is not available for the stored platform."
    ComponentStatus.UNKNOWN -> "Stored data is insufficient to evaluate $noun."
    ComponentStatus.NOT_APPLICABLE -> "No stored requirement for $noun."
}

private fun describeTierAndValue(tier: Int?, value: Double?): String? = listOfNotNull(tier?.let { "Tier $it" }, value?.let { "${format(it, "GB")} VRAM" }).takeIf { it.isNotEmpty() }?.joinToString(" • ")
private fun format(value: Double, unit: String) = "${if (value % 1.0 == 0.0) value.toInt() else value} $unit"

internal fun architectureMatches(actual: String, required: String): Boolean {
    val actualValues = architectureValues(actual)
    val requiredValues = architectureValues(required)
    if (actualValues.isNotEmpty() && requiredValues.isNotEmpty()) return actualValues.any { it in requiredValues }
    return actual.contains(required, true) || required.contains(actual, true)
}

private fun architectureValues(value: String): Set<String> {
    val normalized = value.lowercase()
    return buildSet {
        if (Regex("\\b(x64|x86[-_ ]?64|amd64)\\b").containsMatchIn(normalized)) add("x64")
        if (Regex("\\b(arm64|aarch64|armv8(?:-a)?)\\b").containsMatchIn(normalized)) add("arm64")
        if (Regex("\\b(x86|i[3-6]86|32[- ]?bit)\\b").containsMatchIn(normalized) && "x86_64" !in normalized && "x86-64" !in normalized) add("x86")
        if (Regex("\\b(armv7|armeabi|arm32)\\b").containsMatchIn(normalized)) add("arm32")
    }
}
