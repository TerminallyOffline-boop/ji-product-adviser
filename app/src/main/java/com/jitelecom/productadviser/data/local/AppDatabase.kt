package com.jitelecom.productadviser.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ProductEntity::class, ProcessorEntity::class, GpuEntity::class, SoftwareEntity::class,
        RequirementEntity::class, WorkloadProfileEntity::class, AnalyticsEntity::class, DatabaseMetadataEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun hardwareDao(): HardwareDao
    abstract fun softwareDao(): SoftwareDao
    abstract fun requirementDao(): RequirementDao
    abstract fun metadataDao(): MetadataDao
    abstract fun analyticsDao(): AnalyticsDao
}
