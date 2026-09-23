package com.jitelecom.productadviser.data

import com.google.common.truth.Truth.assertThat
import com.jitelecom.productadviser.data.seed.BundledSoftwareCatalog
import com.jitelecom.productadviser.domain.model.PlatformFamily
import com.jitelecom.productadviser.domain.model.platformFamilies
import org.junit.Test

class BundledSoftwareCatalogTest {
    private val entries = BundledSoftwareCatalog.entries

    @Test fun catalogHasUniquePlatformAwareApps() {
        assertThat(entries).hasSize(22)
        assertThat(entries.map { Triple(it.name, it.version, it.platform) }.distinct()).hasSize(entries.size)
        assertThat(entries.count { PlatformFamily.MACOS in platformFamilies(it.platform) }).isAtLeast(10)
        assertThat(entries.count { PlatformFamily.WINDOWS in platformFamilies(it.platform) }).isAtLeast(10)
        assertThat(entries.count { PlatformFamily.ANDROID in platformFamilies(it.platform) }).isAtLeast(5)
        assertThat(entries.count { PlatformFamily.IOS in platformFamilies(it.platform) }).isAtLeast(5)
    }

    @Test fun everyBundledAppHasSourceAndOperatingSystemData() {
        entries.forEach { app ->
            assertThat(app.requirementsSourceUrl).startsWith("https://")
            assertThat(app.minimum.operatingSystems).isNotEmpty()
        }
    }

    @Test fun windowsOnlyAndMacOnlyAppsStaySeparated() {
        val valorant = entries.single { it.name == "Valorant" }
        val finalCut = entries.single { it.name == "Final Cut Pro" }
        assertThat(platformFamilies(valorant.platform)).containsExactly(PlatformFamily.WINDOWS)
        assertThat(platformFamilies(finalCut.platform)).containsExactly(PlatformFamily.MACOS)
    }
}
