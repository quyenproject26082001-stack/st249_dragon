package com.dragon.oc.avatar.creator.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dragon.oc.avatar.creator.data.local.dao.UserDao
import com.dragon.oc.avatar.creator.data.local.entity.User

@Database(entities = [User::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}