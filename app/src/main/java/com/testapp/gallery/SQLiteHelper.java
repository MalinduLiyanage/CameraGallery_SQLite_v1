package com.testapp.gallery;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLiteHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "galleryDB";
    private static final int DB_VERSION = 1;

    public SQLiteHelper(Context context){
        super(context, DB_NAME, null, DB_VERSION);
        getWritableDatabase();
    }

    public void onCreate(SQLiteDatabase db) {
        String captureQuery = "CREATE TABLE captures ("
                + "img_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "path TEXT,"
                + "timestamp TEXT,"
                + "lat TEXT,"
                + "lon TEXT)";

        db.execSQL(captureQuery);

    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS captures");
        onCreate(db);
    }

    public void addCapture(String path, String timestamp, String lat, String lon) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("path", path);
        values.put("timestamp", timestamp);
        values.put("lat", lat);
        values.put("lon", lon);
        db.insert("captures", null, values);
        db.close();
    }
}



