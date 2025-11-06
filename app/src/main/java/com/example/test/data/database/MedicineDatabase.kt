package com.example.test.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.test.data.dao.MedicineDao
import com.example.test.data.dao.MedicineIntakeDao
import com.example.test.data.dao.MedicineScheduleDao
import com.example.test.data.model.Medicine
import com.example.test.data.model.MedicineIntake
import com.example.test.data.model.MedicineSchedule

@Database(
    entities = [Medicine::class, MedicineSchedule::class, MedicineIntake::class],
    version = 3,
    exportSchema = false
)
abstract class MedicineDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao
    abstract fun medicineScheduleDao(): MedicineScheduleDao
    abstract fun medicineIntakeDao(): MedicineIntakeDao

    companion object {
        @Volatile
        private var INSTANCE: MedicineDatabase? = null

        fun getDatabase(context: Context): MedicineDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MedicineDatabase::class.java,
                    "medicine_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
