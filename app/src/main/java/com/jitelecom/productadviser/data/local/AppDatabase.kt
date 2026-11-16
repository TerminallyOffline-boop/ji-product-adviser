package com.jitelecom.productadviser.data.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProductEntity::class, ProcessorEntity::class, GpuEntity::class, SoftwareEntity::class,
        RequirementEntity::class, WorkloadProfileEntity::class, AnalyticsEntity::class, DatabaseMetadataEntity::class
    ],
    version = 4,
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

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS index_requirements_softwareId_requirementType")
                db.execSQL("ALTER TABLE requirements ADD COLUMN platform TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_requirements_softwareId_requirementType_platform " +
                        "ON requirements(softwareId, requirementType, platform)"
                )
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN availabilityUpdatedAt TEXT")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN inventoryQuantity INTEGER")
            }
        }
    }
}
