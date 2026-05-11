package com.studytimer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "study_records.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE = "sessions";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "date TEXT NOT NULL," +      // yyyy-MM-dd
                "seconds INTEGER NOT NULL" + // us din ki total padhai
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    // Ek session add karo (ya existing date pe add karo)
    public void addSession(String date, long seconds) {
        SQLiteDatabase db = getWritableDatabase();
        // Check karo agar iss date ka record pehle se hai
        Cursor cursor = db.rawQuery(
                "SELECT id, seconds FROM " + TABLE + " WHERE date = ?",
                new String[]{date}
        );
        if (cursor.moveToFirst()) {
            int id = cursor.getInt(0);
            long existing = cursor.getLong(1);
            ContentValues cv = new ContentValues();
            cv.put("seconds", existing + seconds);
            db.update(TABLE, cv, "id = ?", new String[]{String.valueOf(id)});
        } else {
            ContentValues cv = new ContentValues();
            cv.put("date", date);
            cv.put("seconds", seconds);
            db.insert(TABLE, null, cv);
        }
        cursor.close();
        db.close();
    }

    // Kisi date ki total seconds wapas karo
    public long getTotalForDate(String date) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT seconds FROM " + TABLE + " WHERE date = ?",
                new String[]{date}
        );
        long total = 0;
        if (cursor.moveToFirst()) total = cursor.getLong(0);
        cursor.close();
        db.close();
        return total;
    }

    // Sabhi records dikhao (latest pehle) - DELETE KA OPTION NAHI
    public List<String> getAllRecords() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT date, seconds FROM " + TABLE + " ORDER BY date DESC",
                null
        );
        List<String> records = new ArrayList<>();
        long grandTotal = 0;
        while (cursor.moveToNext()) {
            String dateStr = cursor.getString(0);
            long secs = cursor.getLong(1);
            grandTotal += secs;
            long h = secs / 3600;
            long m = (secs % 3600) / 60;

            // Date ko readable format mein
            try {
                Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr);
                String readable = new SimpleDateFormat("dd MMM yyyy (EEE)", new Locale("hi")).format(d);
                String line;
                if (h > 0) {
                    line = readable + "\n   " + h + " ghante " + m + " minute";
                } else {
                    line = readable + "\n   " + m + " minute";
                }
                records.add(line);
            } catch (Exception e) {
                records.add(dateStr + " - " + h + "h " + m + "m");
            }
        }
        cursor.close();
        db.close();

        // Sabse upar total dikhao
        if (!records.isEmpty()) {
            long th = grandTotal / 3600;
            long tm = (grandTotal % 3600) / 60;
            records.add(0, "══ TOTAL: " + th + " ghante " + tm + " minute ══");
            records.add(1, ""); // empty line
        }
        return records;
    }
}
