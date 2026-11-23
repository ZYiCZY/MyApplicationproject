package com.example.myapplicationproject.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.myapplicationproject.models.Event
import com.example.myapplicationproject.models.Trip
import com.example.myapplicationproject.models.TripLog
import com.example.myapplicationproject.models.User

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "ontime.db"
        private const val DATABASE_VERSION = 4

        // Table Names
        const val TABLE_USER = "User"
        const val TABLE_EVENT = "Event"
        const val TABLE_TRIP = "Trip"
        const val TABLE_TRIP_LOG = "TripLog"

        // Common Column
        const val COLUMN_ID = "id"

        // User Columns
        const val COLUMN_EMAIL = "email"
        const val COLUMN_NICKNAME = "nickname"
        const val COLUMN_DEFAULT_PREP_TIME = "defaultPrepTime"
        const val COLUMN_DEFAULT_BUFFER_TIME = "defaultBufferTime"
        const val COLUMN_DEFAULT_TRANSPORT = "defaultTransportMode"

        // Event Columns
        const val COLUMN_EVENT_GROUP_ID = "groupId"
        const val COLUMN_EVENT_NAME = "name"
        const val COLUMN_EVENT_DESC = "description"
        const val COLUMN_LOCATION_NAME = "locationName"
        const val COLUMN_LATITUDE = "latitude"
        const val COLUMN_LONGITUDE = "longitude"
        const val COLUMN_MEETING_TIME = "meetingTime"
        const val COLUMN_GRACE_PERIOD = "gracePeriodMins"
        const val COLUMN_IS_CANCELLED = "isCancelled"

        // Trip Columns
        const val COLUMN_TRIP_TITLE = "title"
        const val COLUMN_DESTINATION_NAME = "destinationName"
        const val COLUMN_DESTINATION_ADDRESS = "destinationAddress"
        const val COLUMN_ARRIVAL_DEADLINE = "arrivalDeadline"
        const val COLUMN_TRANSPORT_MODE = "transportMode"
        const val COLUMN_PREP_MINS = "prepMins"
        const val COLUMN_EST_TRAVEL_MINS = "estTravelMins"
        const val COLUMN_LAST_MILE_MINS = "lastMileMins"
        const val COLUMN_BUFFER_MINS = "bufferMins"
        const val COLUMN_RAIN_BUFFER_MINS = "rainBufferMins"
        const val COLUMN_IS_RAINY_DAY = "isRainyDay"
        const val COLUMN_MUST_BRING_ITEMS = "mustBringItems"
        const val COLUMN_REMIND_EVENING = "remindEveningBefore"
        const val COLUMN_REMIND_START_PREP = "remindStartPrep"
        const val COLUMN_REMIND_DEPARTURE = "remindDeparture"
        const val COLUMN_IS_MULTI_STOP = "isMultiStop"
        const val COLUMN_STOPS_SUMMARY = "stopsSummary"
        const val COLUMN_CREATED_AT = "createdAt"

        // TripLog Columns
        const val COLUMN_LOG_TRIP_ID = "tripId"
        const val COLUMN_LOG_TRIP_TITLE = "tripTitle"
        const val COLUMN_LOG_DATE_TEXT = "dateText"
        const val COLUMN_START_PREP_TIME = "startPrepTime"
        const val COLUMN_LEFT_HOME_TIME = "leftHomeTime"
        const val COLUMN_PARKED_TIME = "parkedTime"
        const val COLUMN_ARRIVED_TIME = "arrivedTime"
        const val COLUMN_ACTUAL_PREP_MINS = "actualPrepMins"
        const val COLUMN_ACTUAL_TRAVEL_MINS = "actualTravelMins"
        const val COLUMN_ACTUAL_LAST_MILE_MINS = "actualLastMileMins"
        const val COLUMN_ACTUAL_TOTAL_MINS = "actualTotalMins"
        const val COLUMN_IS_COMPLETE = "isComplete"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createUserTable = """
            CREATE TABLE $TABLE_USER (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_EMAIL TEXT,
                $COLUMN_NICKNAME TEXT,
                $COLUMN_DEFAULT_PREP_TIME INTEGER,
                $COLUMN_DEFAULT_BUFFER_TIME INTEGER,
                $COLUMN_DEFAULT_TRANSPORT TEXT
            )
        """.trimIndent()

        val createEventTable = """
            CREATE TABLE $TABLE_EVENT (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_EVENT_GROUP_ID TEXT,
                $COLUMN_EVENT_NAME TEXT,
                $COLUMN_EVENT_DESC TEXT,
                $COLUMN_LOCATION_NAME TEXT,
                $COLUMN_LATITUDE REAL,
                $COLUMN_LONGITUDE REAL,
                $COLUMN_MEETING_TIME INTEGER,
                $COLUMN_GRACE_PERIOD INTEGER,
                $COLUMN_IS_CANCELLED INTEGER
            )
        """.trimIndent()

        val createTripTable = """
            CREATE TABLE $TABLE_TRIP (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_TRIP_TITLE TEXT,
                $COLUMN_DESTINATION_NAME TEXT,
                $COLUMN_DESTINATION_ADDRESS TEXT,
                $COLUMN_LATITUDE REAL,
                $COLUMN_LONGITUDE REAL,
                $COLUMN_ARRIVAL_DEADLINE INTEGER,
                $COLUMN_TRANSPORT_MODE TEXT,
                $COLUMN_PREP_MINS INTEGER,
                $COLUMN_EST_TRAVEL_MINS INTEGER,
                $COLUMN_LAST_MILE_MINS INTEGER,
                $COLUMN_BUFFER_MINS INTEGER,
                $COLUMN_RAIN_BUFFER_MINS INTEGER,
                $COLUMN_IS_RAINY_DAY INTEGER,
                $COLUMN_MUST_BRING_ITEMS TEXT,
                $COLUMN_REMIND_EVENING INTEGER,
                $COLUMN_REMIND_START_PREP INTEGER,
                $COLUMN_REMIND_DEPARTURE INTEGER,
                $COLUMN_IS_MULTI_STOP INTEGER,
                $COLUMN_STOPS_SUMMARY TEXT,
                $COLUMN_CREATED_AT INTEGER
            )
        """.trimIndent()

        val createTripLogTable = """
            CREATE TABLE $TABLE_TRIP_LOG (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_LOG_TRIP_ID TEXT,
                $COLUMN_LOG_TRIP_TITLE TEXT,
                $COLUMN_LOG_DATE_TEXT TEXT,
                $COLUMN_START_PREP_TIME INTEGER,
                $COLUMN_LEFT_HOME_TIME INTEGER,
                $COLUMN_PARKED_TIME INTEGER,
                $COLUMN_ARRIVED_TIME INTEGER,
                $COLUMN_ACTUAL_PREP_MINS INTEGER,
                $COLUMN_ACTUAL_TRAVEL_MINS INTEGER,
                $COLUMN_ACTUAL_LAST_MILE_MINS INTEGER,
                $COLUMN_ACTUAL_TOTAL_MINS INTEGER,
                $COLUMN_IS_COMPLETE INTEGER
            )
        """.trimIndent()

        db.execSQL(createUserTable)
        db.execSQL(createEventTable)
        db.execSQL(createTripTable)
        db.execSQL(createTripLogTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS User")
        db.execSQL("DROP TABLE IF EXISTS GroupTable")
        db.execSQL("DROP TABLE IF EXISTS Event")
        db.execSQL("DROP TABLE IF EXISTS EventParticipant")
        db.execSQL("DROP TABLE IF EXISTS Penalty")
        db.execSQL("DROP TABLE IF EXISTS Appeal")
        db.execSQL("DROP TABLE IF EXISTS Trip")
        db.execSQL("DROP TABLE IF EXISTS TripLog")
        onCreate(db)
    }

    // ==========================================
    // CRUD Operations for Trips & Logs & Users
    // ==========================================

    fun insertUser(user: User): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, user.id)
            put(COLUMN_EMAIL, user.email)
            put(COLUMN_NICKNAME, user.nickname)
            put(COLUMN_DEFAULT_PREP_TIME, user.defaultPrepTime)
            put(COLUMN_DEFAULT_BUFFER_TIME, user.defaultBufferTime)
            put(COLUMN_DEFAULT_TRANSPORT, user.defaultTransportMode)
        }
        return db.insertWithOnConflict(TABLE_USER, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getUser(id: String): User? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_USER, null, "$COLUMN_ID = ?", arrayOf(id),
            null, null, null
        )
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)),
                nickname = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NICKNAME)),
                defaultPrepTime = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DEFAULT_PREP_TIME)),
                defaultBufferTime = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DEFAULT_BUFFER_TIME)),
                defaultTransportMode = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DEFAULT_TRANSPORT)) ?: "機車"
            )
        }
        cursor.close()
        return user
    }

    fun insertEvent(event: Event): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, event.id)
            put(COLUMN_EVENT_GROUP_ID, event.groupId)
            put(COLUMN_EVENT_NAME, event.name)
            put(COLUMN_EVENT_DESC, event.description)
            put(COLUMN_LOCATION_NAME, event.locationName)
            put(COLUMN_LATITUDE, event.latitude)
            put(COLUMN_LONGITUDE, event.longitude)
            put(COLUMN_MEETING_TIME, event.meetingTime)
            put(COLUMN_GRACE_PERIOD, event.gracePeriodMins)
            put(COLUMN_IS_CANCELLED, if (event.isCancelled) 1 else 0)
        }
        return db.insertWithOnConflict(TABLE_EVENT, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun insertTrip(trip: Trip): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, trip.id)
            put(COLUMN_TRIP_TITLE, trip.title)
            put(COLUMN_DESTINATION_NAME, trip.destinationName)
            put(COLUMN_DESTINATION_ADDRESS, trip.destinationAddress)
            put(COLUMN_LATITUDE, trip.latitude)
            put(COLUMN_LONGITUDE, trip.longitude)
            put(COLUMN_ARRIVAL_DEADLINE, trip.arrivalDeadline)
            put(COLUMN_TRANSPORT_MODE, trip.transportMode)
            put(COLUMN_PREP_MINS, trip.prepMins)
            put(COLUMN_EST_TRAVEL_MINS, trip.travelMins)
            put(COLUMN_LAST_MILE_MINS, trip.lastMileMins)
            put(COLUMN_BUFFER_MINS, trip.bufferMins)
            put(COLUMN_RAIN_BUFFER_MINS, trip.rainBufferMins)
            put(COLUMN_IS_RAINY_DAY, if (trip.isRainyDay) 1 else 0)
            put(COLUMN_MUST_BRING_ITEMS, trip.mustBringItems)
            put(COLUMN_REMIND_EVENING, if (trip.remindEveningBefore) 1 else 0)
            put(COLUMN_REMIND_START_PREP, if (trip.remindStartPrep) 1 else 0)
            put(COLUMN_REMIND_DEPARTURE, if (trip.remindDeparture) 1 else 0)
            put(COLUMN_IS_MULTI_STOP, if (trip.isMultiStop) 1 else 0)
            put(COLUMN_STOPS_SUMMARY, trip.stopsSummary)
            put(COLUMN_CREATED_AT, trip.createdAt)
        }
        return db.insertWithOnConflict(TABLE_TRIP, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getTrips(): List<Trip> {
        val list = mutableListOf<Trip>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_TRIP ORDER BY $COLUMN_ARRIVAL_DEADLINE ASC", null)
        if (cursor.moveToFirst()) {
            do {
                val trip = Trip(
                    id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRIP_TITLE)),
                    destinationName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESTINATION_NAME)),
                    destinationAddress = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESTINATION_ADDRESS)),
                    latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE)),
                    longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE)),
                    arrivalDeadline = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ARRIVAL_DEADLINE)),
                    transportMode = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRANSPORT_MODE)),
                    prepMins = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PREP_MINS)),
                    travelMins = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_EST_TRAVEL_MINS)),
                    lastMileMins = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LAST_MILE_MINS)),
                    bufferMins = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_BUFFER_MINS)),
                    rainBufferMins = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RAIN_BUFFER_MINS)),
                    isRainyDay = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_RAINY_DAY)) == 1,
                    mustBringItems = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MUST_BRING_ITEMS)) ?: "安全帽,鑰匙,學生證",
                    remindEveningBefore = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMIND_EVENING)) == 1,
                    remindStartPrep = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMIND_START_PREP)) == 1,
                    remindDeparture = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMIND_DEPARTURE)) == 1,
                    isMultiStop = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_MULTI_STOP)) == 1,
                    stopsSummary = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STOPS_SUMMARY)) ?: "",
                    createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT))
                )
                list.add(trip)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun updateTripRainyMode(id: String, isRainy: Boolean): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_IS_RAINY_DAY, if (isRainy) 1 else 0)
        }
        return db.update(TABLE_TRIP, values, "$COLUMN_ID = ?", arrayOf(id))
    }

    fun updateTripTimeSettings(id: String, prepMins: Int, travelMins: Int, lastMileMins: Int): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PREP_MINS, prepMins)
            put(COLUMN_EST_TRAVEL_MINS, travelMins)
            put(COLUMN_LAST_MILE_MINS, lastMileMins)
        }
        return db.update(TABLE_TRIP, values, "$COLUMN_ID = ?", arrayOf(id))
    }

    fun insertTripLog(log: TripLog): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, log.id)
            put(COLUMN_LOG_TRIP_ID, log.tripId)
            put(COLUMN_LOG_TRIP_TITLE, log.tripTitle)
            put(COLUMN_LOG_DATE_TEXT, log.dateText)
            put(COLUMN_START_PREP_TIME, log.startPrepTime)
            put(COLUMN_LEFT_HOME_TIME, log.leftHomeTime)
            put(COLUMN_PARKED_TIME, log.parkedTime)
            put(COLUMN_ARRIVED_TIME, log.arrivedTime)
            put(COLUMN_ACTUAL_PREP_MINS, log.actualPrepMins)
            put(COLUMN_ACTUAL_TRAVEL_MINS, log.actualTravelMins)
            put(COLUMN_ACTUAL_LAST_MILE_MINS, log.actualLastMileMins)
            put(COLUMN_ACTUAL_TOTAL_MINS, log.actualTotalMins)
            put(COLUMN_IS_COMPLETE, if (log.isComplete) 1 else 0)
        }
        return db.insertWithOnConflict(TABLE_TRIP_LOG, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getTripLogs(): List<TripLog> {
        val list = mutableListOf<TripLog>()
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_TRIP_LOG ORDER BY CASE WHEN $COLUMN_ARRIVED_TIME IS NULL THEN 1 ELSE 0 END, $COLUMN_ARRIVED_TIME DESC",
            null
        )
        if (cursor.moveToFirst()) {
            do {
                val log = TripLog(
                    id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    tripId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_TRIP_ID)),
                    tripTitle = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_TRIP_TITLE)),
                    dateText = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_DATE_TEXT)),
                    startPrepTime = if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_START_PREP_TIME))) null else cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_START_PREP_TIME)),
                    leftHomeTime = if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_LEFT_HOME_TIME))) null else cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LEFT_HOME_TIME)),
                    parkedTime = if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_PARKED_TIME))) null else cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_PARKED_TIME)),
                    arrivedTime = if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_ARRIVED_TIME))) null else cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ARRIVED_TIME)),
                    actualPrepMins = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACTUAL_PREP_MINS)),
                    actualTravelMins = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACTUAL_TRAVEL_MINS)),
                    actualLastMileMins = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACTUAL_LAST_MILE_MINS)),
                    actualTotalMins = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACTUAL_TOTAL_MINS)),
                    isComplete = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_COMPLETE)) == 1
                )
                list.add(log)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getTripLogsByTripId(tripId: String): List<TripLog> {
        val list = mutableListOf<TripLog>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_TRIP_LOG,
            null,
            "$COLUMN_LOG_TRIP_ID = ?",
            arrayOf(tripId),
            null,
            null,
            "CASE WHEN $COLUMN_ARRIVED_TIME IS NULL THEN 1 ELSE 0 END, $COLUMN_ARRIVED_TIME DESC"
        )
        if (cursor.moveToFirst()) {
            do {
                val log = TripLog(
                    id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    tripId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_TRIP_ID)),
                    tripTitle = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_TRIP_TITLE)),
                    dateText = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_DATE_TEXT)),
                    startPrepTime = if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_START_PREP_TIME))) null else cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_START_PREP_TIME)),
                    leftHomeTime = if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_LEFT_HOME_TIME))) null else cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LEFT_HOME_TIME)),
                    parkedTime = if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_PARKED_TIME))) null else cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_PARKED_TIME)),
                    arrivedTime = if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_ARRIVED_TIME))) null else cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ARRIVED_TIME)),
                    actualPrepMins = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACTUAL_PREP_MINS)),
                    actualTravelMins = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACTUAL_TRAVEL_MINS)),
                    actualLastMileMins = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACTUAL_LAST_MILE_MINS)),
                    actualTotalMins = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACTUAL_TOTAL_MINS)),
                    isComplete = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_COMPLETE)) == 1
                )
                list.add(log)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }
}
