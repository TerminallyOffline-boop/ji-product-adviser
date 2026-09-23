package com.jitelecom.productadviser

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class AppSafetyConfigurationTest {
    @Test fun nestedRoutesHighlightTheirPrimaryNavigationDestination() {
        assertThat(destinationSelected("product/{id}", "products")).isTrue()
        assertThat(destinationSelected("compare", "more")).isTrue()
        assertThat(destinationSelected("admin", "more")).isTrue()
        assertThat(destinationSelected("compatibility", "compatibility")).isTrue()
        assertThat(destinationSelected("recommend", "products")).isFalse()
    }

    @Test fun androidBackupRulesExcludeTheDataStorePreferencesFile() {
        val backup = locate("src/main/res/xml/backup_rules.xml").readText()
        val extraction = locate("src/main/res/xml/data_extraction_rules.xml").readText()
        assertThat(backup).contains("datastore/app_preferences.preferences_pb")
        assertThat(extraction).contains("datastore/app_preferences.preferences_pb")
        assertThat(backup).doesNotContain("admin_preferences.xml")
    }

    private fun locate(relative: String): File = listOf(File(relative), File("app/$relative"))
        .firstOrNull(File::isFile) ?: error("Could not locate $relative")
}
