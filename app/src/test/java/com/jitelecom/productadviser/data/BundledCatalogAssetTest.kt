package com.jitelecom.productadviser.data

import com.google.common.truth.Truth.assertThat
import com.jitelecom.productadviser.data.seed.BundledCatalog
import com.jitelecom.productadviser.data.seed.BundledSpecifications
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Test
import java.io.File

class BundledCatalogAssetTest {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private fun catalog(): BundledCatalog {
        val asset = listOf(
            File("src/main/assets/ji_catalog_july_2026.json"),
            File("app/src/main/assets/ji_catalog_july_2026.json")
        ).firstOrNull(File::isFile) ?: error("Bundled catalog asset was not found.")
        return requireNotNull(moshi.adapter(BundledCatalog::class.java).fromJson(asset.readText()))
    }

    private fun specifications(): BundledSpecifications {
        val asset = listOf(
            File("src/main/assets/ji_official_specs_2026.json"),
            File("app/src/main/assets/ji_official_specs_2026.json")
        ).firstOrNull(File::isFile) ?: error("Bundled specification asset was not found.")
        return requireNotNull(moshi.adapter(BundledSpecifications::class.java).fromJson(asset.readText()))
    }

    @Test
    fun containsExpectedNonPrinterCatalog() {
        val catalog = catalog()
        assertThat(catalog.catalogVersion).isEqualTo("2026.11.16.4")
        assertThat(catalog.products).hasSize(263)
        assertThat(catalog.products.count { it.category == "LAPTOP" }).isEqualTo(42)
        assertThat(catalog.products.count { it.category == "SMARTPHONE" }).isEqualTo(189)
        assertThat(catalog.products.count { it.category == "TABLET" }).isEqualTo(30)
        assertThat(catalog.products.count { it.category == "ACCESSORIES" }).isEqualTo(2)
        assertThat(catalog.products.any { "printer" in "${it.brand} ${it.model}".lowercase() }).isFalse()
    }

    @Test
    fun importKeysAndPricesAreValid() {
        val products = catalog().products
        assertThat(products.map { it.sku }.distinct()).hasSize(products.size)
        assertThat(products.all { it.price > 0 }).isTrue()
        assertThat(products.all { it.verificationStatus in setOf("VERIFIED", "NEEDS_REVIEW", "UNVERIFIED", "OUTDATED") }).isTrue()
        assertThat(products.single { it.sku == "JUL26-PHN-042" }.storageGB).isEqualTo(1024)
        assertThat(products.single { it.sku == "JUL26-PHN-049" }.storageGB).isEqualTo(1024)
        assertThat(products.single { it.sku == "JUL26-PHN-140" }.ramGB).isEqualTo(4)
        assertThat(products.single { it.sku == "JUL26-PHN-140" }.storageGB).isEqualTo(128)
    }

    @Test
    fun officialSpecificationsReferenceValidUniqueCatalogRows() {
        val catalogSkus = catalog().products.map { it.sku }.toSet()
        val specifications = specifications()
        val assignedSkus = specifications.assignments.flatMap { it.skus }
        val processorIds = specifications.processors.map { it.id }.toSet()
        val gpuIds = specifications.gpus.map { it.id }.toSet()

        assertThat(specifications.version).isEqualTo("2026.11.16.4")
        assertThat(assignedSkus).hasSize(262)
        assertThat(assignedSkus.distinct()).hasSize(assignedSkus.size)
        assertThat(catalogSkus).containsAtLeastElementsIn(assignedSkus)
        assertThat(specifications.assignments.mapNotNull { it.processorId }.all { it in processorIds }).isTrue()
        assertThat(specifications.assignments.mapNotNull { it.gpuId }.all { it in gpuIds }).isTrue()
        assertThat(specifications.assignments.all { it.sourceUrl.startsWith("https://") }).isTrue()
        val verified = specifications.assignments.filter { it.verificationStatus == "VERIFIED" }
        assertThat(verified.all { !it.sourceName.isNullOrBlank() && !it.verifiedBy.isNullOrBlank() }).isTrue()
        val inventorySkus = catalog().products.filter { it.sourceName == "Inventory_One_Variant.xlsx" }.map { it.sku }.toSet()
        assertThat(assignedSkus.filter { it in inventorySkus }).hasSize(171)
        assertThat(specifications.assignments.filter { it.verificationStatus == "NEEDS_REVIEW" }.flatMap { it.skus })
            .containsAtLeastElementsIn(listOf("JUL26-LAP-007", "JUL26-LAP-014"))
    }

    @Test
    fun everyLaptopHasTheFieldsNeededForCompatibilityChecks() {
        val products = catalog().products.filter { it.category == "LAPTOP" }
        val assignments = specifications().assignments
            .flatMap { assignment -> assignment.skus.map { it to assignment } }
            .toMap()

        products.forEach { product ->
            val assignment = requireNotNull(assignments[product.sku]) { "Missing assignment for ${product.sku}" }
            assertThat(assignment.processorId).isNotNull()
            assertThat(assignment.gpuId).isNotNull()
            val hasRam = product.ramGB ?: assignment.ramGB
            val hasStorage = product.storageGB ?: assignment.storageGB
            if (hasRam == null || hasStorage == null) {
                assertThat(assignment.verificationStatus).isEqualTo("NEEDS_REVIEW")
            }
            assertThat(assignment.operatingSystem).isNotNull()
            assertThat(assignment.architecture).isNotNull()
        }
    }

    @Test
    fun everyComputingDeviceHasPlatformDataAndFlagsUnknownHardware() {
        val products = catalog().products.filter {
            it.category in setOf("LAPTOP", "SMARTPHONE", "TABLET")
        }
        val assignments = specifications().assignments
            .flatMap { assignment -> assignment.skus.map { it to assignment } }
            .toMap()

        products.forEach { product ->
            val assignment = requireNotNull(assignments[product.sku]) { "Missing assignment for ${product.sku}" }
            assertThat(assignment.operatingSystem).isNotNull()
            val proprietaryPhone = assignment.operatingSystem?.contains("Proprietary", ignoreCase = true) == true
            if (proprietaryPhone) return@forEach
            if (assignment.processorId == null || assignment.gpuId == null) {
                assertThat(assignment.verificationStatus).isEqualTo("NEEDS_REVIEW")
            } else {
                assertThat(assignment.architecture).isNotNull()
            }
        }
    }

    @Test
    fun gt888ModelsHaveVerifiedCompatibilityDataAndSearchableCodes() {
        val products = catalog().products
        val assignments = specifications().assignments
            .flatMap { assignment -> assignment.skus.map { it to assignment } }
            .toMap()

        val imported = products.filter { it.sku.startsWith("OCT26-") }
        assertThat(imported).hasSize(13)
        assertThat(imported.all { it.notes.contains("GT888") }).isTrue()
        assertThat(imported.all { it.price > 0 && it.promotionalPrice == null }).isTrue()
        assertThat(products.single { it.sku == "JUL26-PHN-047" }.notes).contains("195950639070")

        val computable = imported.filter { it.category in setOf("LAPTOP", "SMARTPHONE") }
        computable.forEach { product ->
            val assignment = requireNotNull(assignments[product.sku]) { "Missing assignment for ${product.sku}" }
            assertThat(assignment.verificationStatus).isEqualTo("VERIFIED")
            assertThat(assignment.processorId).isNotNull()
            assertThat(assignment.gpuId).isNotNull()
            assertThat(assignment.operatingSystem).isNotNull()
            assertThat(assignment.architecture).isNotNull()
        }
        assertThat(products.single { it.sku == "OCT26-PHN-005" }.price).isEqualTo(22500.0)
        assertThat(assignments.getValue("OCT26-PHN-005").gpuId).isNotNull()
        assertThat(assignments.getValue("OCT26-PHN-008").gpuId).isNotNull()
    }
}

