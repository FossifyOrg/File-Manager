package org.fossify.filemanager.database

import androidx.room.Database
import androidx.room.RoomDatabase
import org.fossify.filemanager.dao.DocumentProviderDao
import org.fossify.filemanager.dao.NetworkConnectionDao
import org.fossify.filemanager.entity.DocumentProviderEntity
import org.fossify.filemanager.entity.NetworkConnectionEntity

@Database(entities = [NetworkConnectionEntity::class, DocumentProviderEntity::class], version = 1)
abstract class Database : RoomDatabase() {
    abstract fun networkConnectionDao(): NetworkConnectionDao
    abstract fun docDao(): DocumentProviderDao
}
