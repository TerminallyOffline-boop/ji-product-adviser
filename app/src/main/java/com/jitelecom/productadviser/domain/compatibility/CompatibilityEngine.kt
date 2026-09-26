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
        val usesSharedMemory = gpu.type == GpuType.INTEGRATED && gpu.vramGB == null
        val vramStatus = if (usesSharedMemory) ComponentStatus.NOT_APPLICABLE
        else valueStatus(gpu.vramGB, min.minimumVramGB, rec?.minimumVramGB)
        val status = if (usesSharedMemory) tier else worst(tier, vramStatus)
        return ComponentCompatibilityResult(
            component = "GPU", actual = "${gpu.displayName}${gpu.vramGB?.let { " • ${it} GB VRAM" } ?: ""}",
            minimum = describeTierAndValue(minTier, min.minimumVramGB),
            recommended = describeTierAndValue(recTier, rec?.minimumVramGB), status = status,
            explanation = if (usesSharedMemory) {
                "Integrated graphics uses shared system memory; compatibility is evaluated using the reviewed graphics performance tier."
            } else explanationFor(status, "graphics")
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
        val minimumMatch = bestOperatingSystemMatch(actual, min.supportedOperatingSystems)
        if (minimumMatch == OperatingSystemCompatibility.UNKNOWN) {
            return ComponentCompatibilityResult(
                "Operating system", actual, min.supportedOperatingSystems.joinToString(" • "),
                rec?.supportedOperatingSystems?.takeIf { it.isNotEmpty() }?.joinToString(" • "),
                ComponentStatus.UNKNOWN,
                "The operating-system family matches, but the stored device record does not include the version needed for this check."
            )
        }
        val compatible = minimumMatch == OperatingSystemCompatibility.MATCH
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
                inferred -> "The app supports the device's inferred ${platformLabel(actual)} platform for this stored requirement."
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
            CompatibilityStatus.MEETS_RECOMMENDED -> "Meets the stored published or reviewed recommended requirements. This is a specification comparison, not a guarantee of real-world performance or app availability."
            CompatibilityStatus.MEETS_MINIMUM -> "Meets the stored published or reviewed minimum requirements${limited.take(3).takeIf { it.isNotEmpty() }?.joinToString(prefix = "; limitations may involve ") ?: ""}. This is a specification comparison and demanding workloads may still be limited."
            CompatibilityStatus.BELOW_MINIMUM -> "One or more components fall below the stored published or reviewed minimum requirements. This configuration is not recommended for this workload."
            CompatibilityStatus.NOT_AVAILABLE -> "This application is not offered for the device's operating-system platform. This is a platform availability limitation, not a hardware-performance failure."
            CompatibilityStatus.NOT_VERIFIED -> {
                val os = components.firstOrNull { it.component == "Operating system" }
                if (os?.status == ComponentStatus.MEETS_MINIMUM || os?.status == ComponentStatus.MEETS_RECOMMENDED) {
                    "The app is available for this operating-system family, but a compatibility-critical product or app specification is missing. Review the component details."
                } else {
                    "A compatibility-critical product or app specification is missing, so this check cannot produce a reliable result."
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
                dataStatus = VerificationStatus.VERIFIED
            )
        }
        val applicableRequirements = requirementsForPlatform(requirements, operatingSystem)
        val minimum = applicableRequirements.firstOrNull { it.type == RequirementType.MINIMUM }
        if (minimum == null) {
            val platform = platformLabel(operatingSystem)
            val reason = if (requirements.any { it.platform.isNotBlank() }) {
                "Minimum requirements for $platform are not stored."
            } else {
                "Minimum requirements are not stored."
            }
            return unverified(product.id, software.id, reason, VerificationStatus.UNVERIFIED)
        }
        val recommended = applicableRequirements.firstOrNull { it.type == RequirementType.RECOMMENDED }
        val components = evaluator.evaluate(product, minimum, recommended)
        val hasUnresolvedData = platformCompatibility == PlatformCompatibility.UNKNOWN ||
            components.any { it.status == ComponentStatus.UNKNOWN }
        val dataStatus = if (hasUnresolvedData) VerificationStatus.NEEDS_REVIEW else VerificationStatus.VERIFIED
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

private fun requirementsForPlatform(requirements: List<RequirementSet>, operatingSystem: String?): List<RequirementSet> {
    val scoped = requirements.filter { requirement ->
        requirement.platform.isNotBlank() &&
            platformCompatibility(requirement.platform, operatingSystem) == PlatformCompatibility.SUPPORTED
    }
    val general = requirements.filter { it.platform.isBlank() }
    if (scoped.isEmpty()) return general
    return RequirementType.entries.mapNotNull { type ->
        scoped.firstOrNull { it.type == type } ?: general.firstOrNull { it.type == type }
    }
}

private fun bestOperatingSystemMatch(actual: String, requiredValues: Set<String>): OperatingSystemCompatibility {
    val matches = requiredValues.map { operatingSystemCompatibility(actual, it) }
    return when {
        OperatingSystemCompatibility.MATCH in matches -> OperatingSystemCompatibility.MATCH
        OperatingSystemCompatibility.UNKNOWN in matches -> OperatingSystemCompatibility.UNKNOWN
        else -> OperatingSystemCompatibility.NOT_SUPPORTED
    }
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
