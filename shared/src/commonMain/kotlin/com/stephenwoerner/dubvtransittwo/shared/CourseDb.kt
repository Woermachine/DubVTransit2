package com.stephenwoerner.dubvtransittwo.shared

import com.squareup.sqldelight.db.SqlDriver
import com.stephenwoerner.dubvtransittwo.AppDatabase
import com.stephenwoerner.dubvtransittwo.CoursesQueries

expect class CourseDb() {
    var driver: SqlDriver?
    var database: AppDatabase?
    var coursesQueries: CoursesQueries?
    companion object {
        fun get(): CourseDb
    }
}