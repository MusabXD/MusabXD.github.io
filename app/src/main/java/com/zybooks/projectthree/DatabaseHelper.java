package com.zybooks.projectthree;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "InventoryApp.db";

    // Users table
    public static final String TABLE_USERS = "users";
    public static final String COL_USERNAME = "username";
    public static final String COL_PASSWORD = "password";

    // Inventory table
    public static final String TABLE_INVENTORY = "inventory";
    public static final String COL_ID = "id";
    public static final String COL_ITEM_NAME = "item_name";
    public static final String COL_QUANTITY = "quantity";
    public static final String COL_ITEM_CATEGORY = "category_name";

    // Categories table
    public static final String TABLE_CATEGORIES = "categories";
    public static final String COL_CAT_ID = "id";
    public static final String COL_CAT_NAME = "category_name";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USERS + " (" +
                COL_USERNAME + " TEXT PRIMARY KEY, " +
                COL_PASSWORD + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_INVENTORY + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_ITEM_NAME + " TEXT, " +
                COL_QUANTITY + " INTEGER, " +
                COL_ITEM_CATEGORY + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_CATEGORIES + " (" +
                COL_CAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_CAT_NAME + " TEXT UNIQUE)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INVENTORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        onCreate(db);
    }

    // ----------------------
    // USER METHODS
    // ----------------------
    public boolean checkUser(String username, String password) {
        try (Cursor cursor = getReadableDatabase().query(
                TABLE_USERS, null,
                COL_USERNAME + "=? AND " + COL_PASSWORD + "=?",
                new String[]{username, password}, null, null, null)) {
            return cursor.getCount() > 0;
        }
    }

    public boolean createUser(String username, String password) {
        if (checkUser(username, password)) return false;

        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, username);
        values.put(COL_PASSWORD, password);
        return getWritableDatabase().insert(TABLE_USERS, null, values) != -1;
    }

    // ----------------------
    // INVENTORY METHODS
    // ----------------------

    public boolean addInventoryItem(String name, int quantity) {
        ContentValues values = new ContentValues();
        values.put(COL_ITEM_NAME, name);
        values.put(COL_QUANTITY, quantity);
        values.putNull(COL_ITEM_CATEGORY); // default: no category
        return getWritableDatabase().insert(TABLE_INVENTORY, null, values) != -1;
    }

    public boolean updateInventoryItem(int id, String name, int quantity) {
        ContentValues values = new ContentValues();
        values.put(COL_ITEM_NAME, name);
        values.put(COL_QUANTITY, quantity);
        return getWritableDatabase().update(TABLE_INVENTORY, values,
                COL_ID + "=?", new String[]{String.valueOf(id)}) > 0;
    }

    public boolean assignItemToCategory(int itemId, String category) {
        ContentValues values = new ContentValues();
        values.put(COL_ITEM_CATEGORY, category);
        return getWritableDatabase().update(TABLE_INVENTORY, values,
                COL_ID + "=?", new String[]{String.valueOf(itemId)}) > 0;
    }

    public ArrayList<InventoryItem> getItemsByCategory(String category) {
        ArrayList<InventoryItem> list = new ArrayList<>();

        String selection = null;
        String[] selectionArgs = null;
        if (!"All".equals(category)) {
            selection = COL_ITEM_CATEGORY + "=?";
            selectionArgs = new String[]{category};
        }

        try (Cursor cursor = getReadableDatabase().query(
                TABLE_INVENTORY, null,
                selection, selectionArgs, null, null, null)) {
            while (cursor.moveToNext()) {
                list.add(cursorToInventoryItem(cursor));
            }
        }
        return list;
    }

    public boolean deleteInventoryItem(int id) {
        return getWritableDatabase().delete(TABLE_INVENTORY,
                COL_ID + "=?", new String[]{String.valueOf(id)}) > 0;
    }

    // ----------------------
    // CATEGORY METHODS
    // ----------------------
    public boolean addCategory(String name) {
        ContentValues values = new ContentValues();
        values.put(COL_CAT_NAME, name);
        return getWritableDatabase().insert(TABLE_CATEGORIES, null, values) != -1;
    }

    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                TABLE_CATEGORIES, new String[]{COL_CAT_NAME},
                null, null, null, null, null)) {
            while (cursor.moveToNext()) {
                categories.add(cursor.getString(0));
            }
        }
        return categories;
    }

    // ----------------------
    // HELPER
    // ----------------------
    private InventoryItem cursorToInventoryItem(Cursor cursor) {
        return new InventoryItem(
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_ITEM_NAME)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_QUANTITY))
        );
    }
}
