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

fun platformFamilies(value: String?): Set<PlatformFamily> {
    val normalized = value.orEmpty().lowercase()
    if (normalized.isBlank()) return emptySet()
    return buildSet {
        if ("windows" in normalized || Regex("\\bwin(10|11)?\\b").containsMatchIn(normalized)) add(PlatformFamily.WINDOWS)
        if ("macos" in normalized || "mac os" in normalized || "macbook" in normalized) add(PlatformFamily.MACOS)
        if ("android" in normalized) add(PlatformFamily.ANDROID)
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

fun operatingSystemMatches(actual: String, required: String): Boolean {
    val actualFamilies = platformFamilies(actual)
    val requiredFamilies = platformFamilies(required)
    if (actualFamilies.isNotEmpty() && requiredFamilies.isNotEmpty()) {
        if (actualFamilies.none { it in requiredFamilies }) return false
        if (PlatformFamily.WINDOWS in actualFamilies && PlatformFamily.WINDOWS in requiredFamilies) {
            val requiredVersion = Regex("windows\\s*(10|11)", RegexOption.IGNORE_CASE).find(required)?.groupValues?.get(1)?.toIntOrNull()
            val actualVersion = Regex("windows\\s*(10|11)", RegexOption.IGNORE_CASE).find(actual)?.groupValues?.get(1)?.toIntOrNull()
            if (requiredVersion != null && actualVersion != null) return actualVersion >= requiredVersion
        }
        return true
    }
    return actual.contains(required, ignoreCase = true) || required.contains(actual, ignoreCase = true)
}

fun platformLabel(value: String?): String = platformFamilies(value).joinToString(" / ") { it.displayName }
    .ifBlank { value?.substringBefore(" (")?.takeIf { it.isNotBlank() } ?: "Unknown OS" }
