package com.stephenwoerner.dubvtransittwo.shared

import com.squareup.sqldelight.db.SqlDriver
import com.stephenwoerner.dubvtransittwo.AppDatabase
import com.squareup.sqldelight.native.NativeSqliteDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(AppDatabase.Schema, "test.db")
    }
}