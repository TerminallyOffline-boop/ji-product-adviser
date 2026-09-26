package com.jitelecom.productadviser.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Transaction
    @Query("SELECT * FROM products WHERE archived = 0 ORDER BY brand, model")
    fun observeAll(): Flow<List<ProductWithHardware>>

    @Transaction
    @Query("""
        SELECT * FROM products p WHERE p.archived = 0 AND (
            :query = '' OR lower(p.brand || ' ' || p.model || ' ' || p.sku || ' ' || p.category || ' ' || p.price || ' ' || ifnull(p.ramGB,'') || 'GB ' || ifnull(p.storageGB,'') || 'GB') LIKE '%' || lower(:query) || '%'
            OR p.processorId IN (SELECT id FROM processors WHERE lower(manufacturer || ' ' || family || ' ' || model) LIKE '%' || lower(:query) || '%')
            OR p.gpuId IN (SELECT id FROM gpus WHERE lower(manufacturer || ' ' || model) LIKE '%' || lower(:query) || '%')
        ) ORDER BY brand, model
    """)
    fun search(query: String): Flow<List<ProductWithHardware>>

    @Transaction @Query("SELECT * FROM products WHERE id = :id") suspend fun get(id: Long): ProductWithHardware?
    @Transaction @Query("SELECT * FROM products WHERE archived = 0") suspend fun getAllOnce(): List<ProductWithHardware>
    @Query("SELECT COUNT(*) FROM products") suspend fun count(): Int
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(value: ProductEntity): Long
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertAll(values: List<ProductEntity>): List<Long>
    @Update suspend fun update(value: ProductEntity)
    @Query("UPDATE products SET archived = 1 WHERE id = :id") suspend fun archive(id: Long)
    @Query("UPDATE products SET archived = :archived WHERE id = :id") suspend fun setArchived(id: Long, archived: Boolean)
    @Query("DELETE FROM products") suspend fun deleteAll()
}

@Dao
interface HardwareDao {
    @Query("SELECT * FROM processors ORDER BY manufacturer, model") fun observeProcessors(): Flow<List<ProcessorEntity>>
    @Query("SELECT * FROM processors ORDER BY id") suspend fun getProcessors(): List<ProcessorEntity>
    @Query("SELECT * FROM gpus ORDER BY manufacturer, model") fun observeGpus(): Flow<List<GpuEntity>>
    @Query("SELECT * FROM gpus ORDER BY id") suspend fun getGpus(): List<GpuEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertProcessors(values: List<ProcessorEntity>): List<Long>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertGpus(values: List<GpuEntity>): List<Long>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertProcessors(values: List<ProcessorEntity>): List<Long>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertGpus(values: List<GpuEntity>): List<Long>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertProcessor(value: ProcessorEntity): Long
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertGpu(value: GpuEntity): Long
    @Update suspend fun updateProcessor(value: ProcessorEntity)
    @Update suspend fun updateGpu(value: GpuEntity)
    @Delete suspend fun deleteProcessor(value: ProcessorEntity)
    @Delete suspend fun deleteGpu(value: GpuEntity)
    @Query("DELETE FROM processors") suspend fun deleteProcessors()
    @Query("DELETE FROM gpus") suspend fun deleteGpus()
}

@Dao
interface SoftwareDao {
    @Query("SELECT * FROM software ORDER BY name, version DESC") fun observeAll(): Flow<List<SoftwareEntity>>
    @Query("SELECT * FROM software ORDER BY id") suspend fun getAllOnce(): List<SoftwareEntity>
    @Query("SELECT * FROM software WHERE id = :id") suspend fun get(id: Long): SoftwareEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(value: SoftwareEntity): Long
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertAll(values: List<SoftwareEntity>): List<Long>
    @Update suspend fun update(value: SoftwareEntity)
    @Delete suspend fun delete(value: SoftwareEntity)
    @Query("DELETE FROM software") suspend fun deleteAll()
}

@Dao
interface RequirementDao {
    @Query("SELECT * FROM requirements ORDER BY softwareId, platform, requirementType") fun observeAllRequirements(): Flow<List<RequirementEntity>>
    @Query("SELECT * FROM requirements WHERE softwareId = :softwareId ORDER BY requirementType") fun observeForSoftware(softwareId: Long): Flow<List<RequirementEntity>>
    @Query("SELECT * FROM requirements WHERE softwareId = :softwareId ORDER BY requirementType") suspend fun getForSoftware(softwareId: Long): List<RequirementEntity>
    @Query("SELECT * FROM requirements ORDER BY softwareId, requirementType") suspend fun getAll(): List<RequirementEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(value: RequirementEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(values: List<RequirementEntity>): List<Long>
    @Update suspend fun update(value: RequirementEntity)
    @Delete suspend fun delete(value: RequirementEntity)
    @Query("DELETE FROM requirements") suspend fun deleteAll()
}

@Dao
interface MetadataDao {
    @Query("SELECT value FROM database_metadata WHERE `key` = :key") fun observe(key: String): Flow<String?>
    @Query("SELECT * FROM database_metadata") suspend fun getAll(): List<DatabaseMetadataEntity>
    @Query("SELECT value FROM database_metadata WHERE `key` = :key") suspend fun getValue(key: String): String?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(value: DatabaseMetadataEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putAll(value: List<DatabaseMetadataEntity>)
    @Query("DELETE FROM database_metadata") suspend fun deleteAll()
}

@Dao
interface AnalyticsDao {
    @Query("SELECT * FROM local_analytics ORDER BY count DESC") suspend fun getAll(): List<AnalyticsEntity>
    @Query("SELECT * FROM local_analytics WHERE event = :event ORDER BY count DESC, updatedAt DESC LIMIT :limit") fun observePopular(event: String, limit: Int): Flow<List<AnalyticsEntity>>
    @Query("""INSERT INTO local_analytics(event, `key`, count, updatedAt) VALUES(:event, :key, 1, :now)
        ON CONFLICT(event, `key`) DO UPDATE SET count = count + 1, updatedAt = :now""")
    suspend fun increment(event: String, key: String, now: Long = System.currentTimeMillis())
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(values: List<AnalyticsEntity>)
    @Query("DELETE FROM local_analytics") suspend fun deleteAll()
}
