package com.jitelecom.productadviser.domain.model

enum class PlatformFamily(val displayName: String) {
    WINDOWS("Windows"),
    MACOS("macOS"),
    ANDROID("Android"),
    IOS("iPhone"),
    IPADOS("iPad"),
    LINUX("Linux"),
    CHROMEOS("ChromeOS")
}

enum class PlatformCompatibility { SUPPORTED, NOT_SUPPORTED, UNKNOWN }
enum class OperatingSystemCompatibility { MATCH, NOT_SUPPORTED, UNKNOWN }

fun platformFamilies(value: String?): Set<PlatformFamily> {
    val normalized = value.orEmpty().lowercase()
    if (normalized.isBlank()) return emptySet()
    return buildSet {
        if ("windows" in normalized || Regex("\\bwin(10|11)?\\b").containsMatchIn(normalized)) add(PlatformFamily.WINDOWS)
        if ("macos" in normalized || "mac os" in normalized || "macbook" in normalized) add(PlatformFamily.MACOS)
        if (
            "android" in normalized ||
            listOf("coloros", "magic os", "magicos", "hios", "hyperos", "originos", "realme ui", "one ui").any { it in normalized }
        ) add(PlatformFamily.ANDROID)
        if (Regex("(^|[^a-z])ios([^a-z]|$)").containsMatchIn(normalized) || "iphone" in normalized) add(PlatformFamily.IOS)
        if ("ipados" in normalized || "ipad" in normalized) add(PlatformFamily.IPADOS)
        if ("linux" in normalized || "ubuntu" in normalized || "debian" in normalized || "fedora" in normalized) add(PlatformFamily.LINUX)
        if ("chromeos" in normalized || "chrome os" in normalized || "chromebook" in normalized) add(PlatformFamily.CHROMEOS)
    }
}

fun ProductSpec.operatingSystemForCompatibility(): String? = operatingSystem?.takeIf { it.isNotBlank() } ?: when (category) {
    ProductCategory.SMARTPHONE -> if (brand.equals("Apple", true)) "iOS (inferred)" else "Android (inferred)"
    ProductCategory.TABLET -> if (brand.equals("Apple", true)) "iPadOS (inferred)" else "Android (inferred)"
    ProductCategory.LAPTOP, ProductCategory.DESKTOP -> when {
        brand.equals("Apple", true) -> "macOS (inferred)"
        model.contains("Chromebook", true) -> "ChromeOS (inferred)"
        else -> null
    }
    else -> null
}

fun ProductSpec.hasInferredOperatingSystem(): Boolean = operatingSystem.isNullOrBlank() && operatingSystemForCompatibility() != null

fun platformCompatibility(appPlatform: String?, operatingSystem: String?): PlatformCompatibility {
    val deviceFamilies = platformFamilies(operatingSystem)
    val appFamilies = platformFamilies(appPlatform)
    if (deviceFamilies.isEmpty() || appFamilies.isEmpty()) return PlatformCompatibility.UNKNOWN
    return if (deviceFamilies.any { it in appFamilies }) PlatformCompatibility.SUPPORTED else PlatformCompatibility.NOT_SUPPORTED
}

fun SoftwareSpec.supportsOperatingSystem(operatingSystem: String?): Boolean =
    platformCompatibility(platform, operatingSystem) == PlatformCompatibility.SUPPORTED

fun operatingSystemCompatibility(actual: String, required: String): OperatingSystemCompatibility {
    val actualFamilies = platformFamilies(actual)
    val requiredFamilies = platformFamilies(required)
    if (actualFamilies.isNotEmpty() && requiredFamilies.isNotEmpty()) {
        val sharedFamilies = actualFamilies.intersect(requiredFamilies)
        if (sharedFamilies.isEmpty()) return OperatingSystemCompatibility.NOT_SUPPORTED
        var needsVersion = false
        sharedFamilies.forEach { family ->
            val requiredVersion = operatingSystemVersion(required, family)
            if (requiredVersion == null) return OperatingSystemCompatibility.MATCH
            needsVersion = true
            val actualVersion = operatingSystemVersion(actual, family) ?: return@forEach
            if (compareVersionParts(actualVersion, requiredVersion) >= 0) return OperatingSystemCompatibility.MATCH
        }
        return if (needsVersion && sharedFamilies.none { operatingSystemVersion(actual, it) != null }) {
            OperatingSystemCompatibility.UNKNOWN
        } else {
            OperatingSystemCompatibility.NOT_SUPPORTED
        }
    }
    return if (actual.contains(required, ignoreCase = true) || required.contains(actual, ignoreCase = true)) {
        OperatingSystemCompatibility.MATCH
    } else {
        OperatingSystemCompatibility.NOT_SUPPORTED
    }
}

fun operatingSystemMatches(actual: String, required: String): Boolean =
    operatingSystemCompatibility(actual, required) == OperatingSystemCompatibility.MATCH

private fun operatingSystemVersion(value: String, family: PlatformFamily): List<Int>? {
    val prefix = when (family) {
        PlatformFamily.WINDOWS -> "windows|win"
        PlatformFamily.MACOS -> "macos|mac\\s+os(?:\\s+x)?"
        PlatformFamily.ANDROID -> "android"
        PlatformFamily.IOS -> "ios|iphone\\s+os"
        PlatformFamily.IPADOS -> "ipados"
        PlatformFamily.CHROMEOS -> "chromeos|chrome\\s+os"
        PlatformFamily.LINUX -> return null
    }
    val match = Regex("(?:$prefix)\\s*(?:version\\s*)?(\\d+(?:\\.\\d+)*)", RegexOption.IGNORE_CASE).find(value)
        ?: return null
    return match.groupValues[1].split('.').mapNotNull(String::toIntOrNull).takeIf { it.isNotEmpty() }
}

private fun compareVersionParts(actual: List<Int>, required: List<Int>): Int {
    val length = maxOf(actual.size, required.size)
    repeat(length) { index ->
        val comparison = (actual.getOrElse(index) { 0 }).compareTo(required.getOrElse(index) { 0 })
        if (comparison != 0) return comparison
    }
    return 0
}

fun platformLabel(value: String?): String = platformFamilies(value).joinToString(" / ") { it.displayName }
    .ifBlank { value?.substringBefore(" (")?.takeIf { it.isNotBlank() } ?: "Unknown OS" }
