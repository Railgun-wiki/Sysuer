package com.sysu.edu.home;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AppCollection extends SQLiteOpenHelper {
    public AppCollection(Context context) {
        super(context, "service_collection.db", null, 3);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS service_collection (id INTEGER PRIMARY KEY AUTOINCREMENT, serviceId INTEGER, serviceJson TEXT, collectTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP, position INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion <= 1) {
            db.execSQL("ALTER TABLE service_collection ADD COLUMN serviceId INTEGER");
        }
        if (oldVersion <= 2) {
            db.execSQL("ALTER TABLE service_collection ADD COLUMN position INTEGER");
        }
    }

    public void addService(Integer id, String serviceJson, Integer position) {
        if (!isCollected(id)) {
            ContentValues values = new ContentValues();
            values.put("serviceId", id);
            values.put("serviceJson", serviceJson);
            values.put("position", position);
            getWritableDatabase().insertOrThrow("service_collection", null, values);
        }
    }

    public void updateServicePosition(Integer id, Integer position) {
        if (isCollected(id)) {
            ContentValues values = new ContentValues();
            if (position != null) values.put("position", position);
            getWritableDatabase().update("service_collection", values, "serviceId = ?", new String[]{id.toString()});
        }
    }

    public boolean isCollected(Integer id) {
        Cursor cursor = getReadableDatabase().query("service_collection", null, "serviceId = ?", new String[]{id.toString()}, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }

    public void deleteService(Integer id) {
        getWritableDatabase().delete("service_collection", "serviceId = ?", new String[]{id.toString()});
    }
}
