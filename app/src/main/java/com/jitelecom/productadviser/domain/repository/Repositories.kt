package com.jitelecom.productadviser.domain.repository

import com.jitelecom.productadviser.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun observeProducts(query: String = ""): Flow<List<ProductSpec>>
    suspend fun getProduct(id: Long): ProductSpec?
    suspend fun getProducts(): List<ProductSpec>
    suspend fun saveProduct(product: ProductSpec): Long
    suspend fun archiveProduct(id: Long)
}

interface CatalogRepository {
    fun observeSoftware(): Flow<List<SoftwareSpec>>
    fun observeRequirements(softwareId: Long): Flow<List<RequirementSet>>
    suspend fun getSoftware(): List<SoftwareSpec>
    suspend fun getRequirements(softwareId: Long): List<RequirementSet>
}
