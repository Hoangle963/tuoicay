package com.app.tuoicay;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "scheduleDB";
    private static final int DATABASE_VERSION = 1;

    // Tạo bảng Schedule
    private static final String TABLE_SCHEDULE = "schedule";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_PUMP_ACTIVE = "pump_active";
    private static final String COLUMN_PUMP_TIME = "pump_time";
    private static final String COLUMN_LIGHT_ACTIVE = "light_active";
    private static final String COLUMN_LIGHT_HOUR = "light_hour";
    private static final String COLUMN_LIGHT_MINUTE = "light_minute";
    private static final String COLUMN_LIGHT_OFF_TIME = "light_off_time";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_SCHEDULE_TABLE = "CREATE TABLE " + TABLE_SCHEDULE + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_PUMP_ACTIVE + " INTEGER,"
                + COLUMN_PUMP_TIME + " INTEGER,"
                + COLUMN_LIGHT_ACTIVE + " INTEGER,"
                + COLUMN_LIGHT_HOUR + " INTEGER,"
                + COLUMN_LIGHT_MINUTE + " INTEGER,"
                + COLUMN_LIGHT_OFF_TIME + " INTEGER"
                + ")";
        db.execSQL(CREATE_SCHEDULE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCHEDULE);
        onCreate(db);
    }
}
