package com.stephenwoerner.dubvtransittwo

import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver

object CourseDb {

    lateinit var driver : SqlDriver
    lateinit var database : Database
    lateinit var coursesQueries: CoursesQueries

    fun get(context: Context) : CourseDb {
        if(!this::database.isInitialized) {
            driver = AndroidSqliteDriver(Database.Schema, context, "test.db")
            database = Database(driver)
            coursesQueries = database.coursesQueries
        }
        return this
    }

}