package com.ido.idoprojectapp;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class HelperUserDB extends SQLiteOpenHelper {
    public static final String UserTable = "UsersDB.db";
    public static final String Id = "Id";
    public static final String Pass = "Pass";
    public static final String Email = "Email";
    public static final String Username = "Username";

    public HelperUserDB(Context context) {
        super(context, UserTable, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String string = "CREATE TABLE IF NOT EXISTS " + UserTable +" ( " + Id + " TEXT, " + Username + " TEXT, " + Email + " TEXT, " + Pass + " TEXT);";
        sqLiteDatabase.execSQL(string);
        Log.d("data", "creating Table user");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
    }
}
