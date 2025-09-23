package com.ido.idoprojectapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class HelperUserDB extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "UsersDB.db";
    public static final String TABLE_NAME = "users";

    public static final String Id = "Id";
    public static final String Username = "Username";
    public static final String Email = "Email";
    public static final String Pass = "Pass";

    public HelperUserDB(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String string = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                + Id + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + Username + " TEXT, "
                + Email + " TEXT UNIQUE, "
                + Pass + " TEXT);";
        sqLiteDatabase.execSQL(string);
        Log.d("data", "creating Table user");
    }

    public boolean insertUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        if (checkEmail(user.getEmail())) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put(Username, user.getUsername());
        values.put(Email, user.getEmail());
        values.put(Pass, user.getPassword());
        long result = db.insert(TABLE_NAME, null, values);
        return result != -1;
    }

    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_NAME + " WHERE " + Username + " = ? AND " + Pass + " = ?",
                new String[]{username, password}
        );
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public boolean checkEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME +
                " WHERE " + Email + " = ?", new String[]{email});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public boolean updatePassword(String email, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Pass, newPassword);
        int rowsAffected = db.update(TABLE_NAME, values, Email + " = ?", new String[]{email});
        return rowsAffected > 0;
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}