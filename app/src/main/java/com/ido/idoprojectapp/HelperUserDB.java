package com.ido.idoprojectapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;


public class HelperUserDB extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "UsersDB.db";
    public static final String TABLE_NAME = "users";
    public static final String Id = "Id";
    public static final String Username = "Username";
    public static final String Email = "Email";
    public static final String Pass = "Pass";
    public static final String ProfilePicture = "ProfilePicture";

    // ====== Creation & Upgrade ======

    public HelperUserDB(Context context) {
        super(context, DATABASE_NAME, null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String string = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                + Id + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + Username + " TEXT, "
                + Email + " TEXT UNIQUE, "
                + Pass + " TEXT,"
                + ProfilePicture + " BLOB);";
        sqLiteDatabase.execSQL(string);
        Log.d("data", "creating Table user");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

    // ====== User Management ======

    public boolean insertUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        if (checkEmail(user.getEmail())) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put(Username, user.getUsername());
        values.put(Email, user.getEmail());
        values.put(Pass, user.getPassword());
        values.put(ProfilePicture, user.getProfilePicture());
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

    // ====== Profile Picture Handling ======

    public static byte[] convertDrawableToByteArray(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);

        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            return stream.toByteArray();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
        drawable.draw(new android.graphics.Canvas(bitmap));
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    public byte[] getProfilePicture(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + ProfilePicture + " FROM " + TABLE_NAME + " WHERE " + Username + " = ?",
                new String[]{username}
        );

        if (cursor.moveToFirst()) {
            byte[] profilePic = cursor.getBlob(0);
            cursor.close();
            return profilePic;
        }

        cursor.close();
        return null;
    }

    public boolean updateProfilePicture(String username, byte[] profilePicture) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ProfilePicture, profilePicture);
        int rowsAffected = db.update(TABLE_NAME, values, Username + " = ?", new String[]{username});
        return rowsAffected > 0;
    }
}