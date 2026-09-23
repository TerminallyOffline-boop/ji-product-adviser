package com.jitelecom.productadviser.data.importexport

import com.jitelecom.productadviser.data.local.*

data class PackageMetadata(
    val databaseVersion: String,
    val minimumAppVersion: String = "1.0.0",
    val exportedAt: String,
    val releaseNotes: String = "",
    val formatVersion: Int = 1
)

data class DatabasePackage(
    val metadata: PackageMetadata,
    val processors: List<ProcessorEntity>,
    val gpus: List<GpuEntity>,
    val products: List<ProductEntity>,
    val software: List<SoftwareEntity>,
    val requirements: List<RequirementEntity>,
    val analytics: List<AnalyticsEntity> = emptyList()
)

data class ImportIssue(val record: String, val message: String)
data class ImportPreview(
    val productCount: Int = 0,
    val processorCount: Int = 0,
    val gpuCount: Int = 0,
    val softwareCount: Int = 0,
    val requirementCount: Int = 0,
    val version: String? = null,
    val errors: List<ImportIssue> = emptyList(),
    val warnings: List<ImportIssue> = emptyList()
) { val canImport: Boolean get() = errors.isEmpty() }

data class ImportResult(val success: Boolean, val message: String, val preview: ImportPreview)
