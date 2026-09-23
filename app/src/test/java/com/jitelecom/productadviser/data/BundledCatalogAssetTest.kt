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
        assertThat(catalog.catalogVersion).isEqualTo("2026.07.1")
        assertThat(catalog.products).hasSize(161)
        assertThat(catalog.products.count { it.category == "LAPTOP" }).isEqualTo(18)
        assertThat(catalog.products.count { it.category == "SMARTPHONE" }).isEqualTo(121)
        assertThat(catalog.products.count { it.category == "TABLET" }).isEqualTo(22)
        assertThat(catalog.products.any { "printer" in "${it.brand} ${it.model}".lowercase() }).isFalse()
    }

    @Test
    fun importKeysAndPricesAreValid() {
        val products = catalog().products
        assertThat(products.map { it.sku }.distinct()).hasSize(products.size)
        assertThat(products.all { it.price > 0 }).isTrue()
        assertThat(products.all { it.verificationStatus == "NEEDS_REVIEW" }).isTrue()
    }

    @Test
    fun officialSpecificationsReferenceValidUniqueCatalogRows() {
        val catalogSkus = catalog().products.map { it.sku }.toSet()
        val specifications = specifications()
        val assignedSkus = specifications.assignments.flatMap { it.skus }
        val processorIds = specifications.processors.map { it.id }.toSet()
        val gpuIds = specifications.gpus.map { it.id }.toSet()

        assertThat(specifications.version).isEqualTo("2026.09.22.3")
        assertThat(assignedSkus).hasSize(84)
        assertThat(assignedSkus.distinct()).hasSize(assignedSkus.size)
        assertThat(catalogSkus).containsAtLeastElementsIn(assignedSkus)
        assertThat(specifications.assignments.mapNotNull { it.processorId }.all { it in processorIds }).isTrue()
        assertThat(specifications.assignments.mapNotNull { it.gpuId }.all { it in gpuIds }).isTrue()
        assertThat(specifications.assignments.all { it.sourceUrl.startsWith("https://") }).isTrue()
    }
}
