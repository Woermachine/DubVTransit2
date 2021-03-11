package com.stephenwoerner.dubvtransittwo.shared

import com.squareup.sqldelight.db.SqlDriver
import com.stephenwoerner.dubvtransittwo.AppDatabase
import com.stephenwoerner.dubvtransittwo.CoursesQueries

actual class CourseDb {

    actual var driver: SqlDriver? = null
    actual var database: AppDatabase? = null
    actual var coursesQueries: CoursesQueries? = null

    actual companion object {
        var instance: CourseDb? = null
        actual fun get(): CourseDb {
            if (instance == null) {
                instance = CourseDb().also {
                    it.driver = NativeSqliteDriver(AppDatabase.Schema, "test.db")
                    it.database = AppDatabase(it.driver as NativeSqliteDriver)
                    it.coursesQueries = (it.driver as AppDatabase).coursesQueries
                }
            }
            return instance!!
        }
    }

}