package com.stephenwoerner.dubvtransittwo

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import timber.log.Timber

/**
 * @deprecated
 * Use CourseDb instead
 *
 * Adapter for a database to
 *
 * Created by srwoerner on 8/26/17.
 */
@SuppressWarnings("unused")
class CourseDbAdapter {
        private lateinit var mDbHelper: DatabaseHelper
        private lateinit var mDb: SQLiteDatabase

        private class DatabaseHelper(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL(DATABASE_CREATE)
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                Timber.w( "Upgrading database from version %s to %s, which will destroy all old data",oldVersion,newVersion)
                db.execSQL("DROP TABLE IF EXISTS courses")
                onCreate(db)
            }
        }

        /**
         * Open the courses database. If it cannot be opened, try to create a new
         * instance of the database. If it cannot be created, throw an exception to
         * signal the failure
         *
         * @return this (self reference, allowing this to be chained in an
         * initialization call)
         * @throws SQLException
         * if the database could be neither opened or created
         */
        @Throws(SQLException::class)
        @SuppressWarnings("unused")
        fun open(context: Context): CourseDbAdapter {
            mDbHelper = DatabaseHelper(context)
            mDb = mDbHelper.writableDatabase
            return this
        }

        fun close() {
            mDbHelper.close()
        }

        /**
         * Add a new course using the course and body provided. If the course is
         * successfully created return the new rowId for that course, otherwise return
         * a -1 to indicate failure.
         *
         * @param course
         * the course of the course
         * @return rowId or -1 if failed
         */
        @SuppressWarnings("unused")
        fun createCourse(course: String?, location: String?, note: String?): Long {
            val initialValues = ContentValues()
            initialValues.put(KEY_COURSE, course)
            initialValues.put(KEY_LOCATION, location)
            initialValues.put(KEY_NOTE, note)
            return mDb.insert(DATABASE_TABLE, null, initialValues)
        }

        /**
         * Delete the course with the given rowId
         *
         * @param rowId
         * id of pharse to delete
         * @return true if deleted, false otherwise
         */
        fun deleteCourse(rowId: Long): Boolean {
            return mDb.delete(DATABASE_TABLE, "$KEY_ROWID=$rowId", null) > 0
        }

        /**
         * Delete the course with the given rowId
         *
         * @param title
         * id of pharse to delete
         * @return true if deleted, false otherwise
         */
        fun deleteCourse(title: String): Boolean {
            return mDb.delete(DATABASE_TABLE, "$KEY_COURSE= \'$title\'", null) > 0
        }

        /**
         * Return a Cursor over the list of all courses in the database
         *
         * @return Cursor over all courses
         */
        fun fetchAllCourses(): Cursor {
            return mDb.query(DATABASE_TABLE, arrayOf(KEY_ROWID, KEY_COURSE, KEY_LOCATION), null, null,
                    null, null, null)
        }

        /**
         * Return a Cursor positioned at the course that matches the given rowId
         *
         * @param rowId
         * id of course to retrieve
         * @return Cursor positioned to matching course, if found
         * @throws SQLException
         * if course could not be found/retrieved
         */
        @Throws(SQLException::class)
        fun fetchCourse(rowId: Long): Cursor {
            val cursor = mDb.query(true, DATABASE_TABLE, arrayOf(KEY_ROWID, KEY_COURSE, KEY_LOCATION), KEY_ROWID
                    + "=" + rowId, null, null, null, null, null)
            cursor?.moveToFirst()
            return cursor
        }

        /**
         * Return a Cursor positioned at the course that matches the given rowId
         *
         * @param title
         * id of course to retrieve
         * @return Cursor positioned to matching course, if found
         * @throws SQLException
         * if course could not be found/retrieved
         */
        @Throws(SQLException::class)
        fun fetchCourse(title: String): Cursor {
            val cursor = mDb.query(true, DATABASE_TABLE, arrayOf(KEY_ROWID, KEY_COURSE, KEY_LOCATION, KEY_NOTE), KEY_COURSE
                    + "= \'" + title + "\'", null, null, null, null, null)
            cursor?.moveToFirst()
            return cursor
        }

        /**
         * Returns true if the course is in the database
         *
         * @param title  title of the course
         * @return  true is the course is in the
         */
        operator fun contains(title: String): Boolean {
            val cursor = mDb.query(false, DATABASE_TABLE, arrayOf(KEY_ROWID, KEY_COURSE), KEY_COURSE
                    + "=\'" + title + "\'", null, null, null, null, null)
            val count = cursor.count
            cursor.close()
            return count > 0
        }

        /**
         * Update the course using the details provided. The course to be updated is
         * specified using the rowId, and it is altered to use the new course passed
         * in
         *
         * @param rowId  id of course to update
         * @param course  value to set course body to
         * @return true if the course was successfully updated, false otherwise
         */
        fun updateCourse(rowId: Long, course: String?, location: String?, note: String?): Boolean {
            val args = ContentValues()
            args.put(KEY_COURSE, course)
            args.put(KEY_LOCATION, location)
            args.put(KEY_NOTE, note)
            return mDb.update(DATABASE_TABLE, args, "$KEY_ROWID=$rowId", null) > 0
        }

        companion object {
            // date is month-day-year compiled for Planner's simpleDatabaseAdapter
            const val KEY_ROWID = "_id"
            const val KEY_COURSE = "course"
            const val KEY_LOCATION = "location"
            const val KEY_NOTE = "note"

            // A notes a user may want to make about the Class
            private const val TAG = "CourseDbAdapter"

            /**
             * Database creation sql statement
             */
            private const val DATABASE_CREATE = ("create table courses (" + KEY_ROWID + " integer primary key autoincrement, "
                    + KEY_COURSE + " text not null, " + KEY_LOCATION + " text not null, " + KEY_NOTE + " text not null);")

            // CREATE TABLE COURSES (_id INTEGER PRIMARY KEY AUTOINCREMENT course TEXT NOT NULL, location TEXT NOT NULL, note TEXT NOT NULL);
            private const val DATABASE_NAME = "data"
            private const val DATABASE_TABLE = "courses"
            private const val DATABASE_VERSION = 1
        }

    }