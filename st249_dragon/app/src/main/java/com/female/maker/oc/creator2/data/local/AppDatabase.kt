package com.female.maker.oc.creator2.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.female.maker.oc.creator2.data.local.dao.UserDao
import com.female.maker.oc.creator2.data.local.entity.User

@Database(entities = [User::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}