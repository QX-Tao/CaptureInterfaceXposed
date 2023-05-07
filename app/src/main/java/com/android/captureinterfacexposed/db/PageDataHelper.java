package com.android.captureinterfacexposed.db;

import static android.content.ContentValues.TAG;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class PageDataHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "page_data.db";
    private static final int DATABASE_VERSION = 1;

    // Pages table
    private static final String TABLE_PAGES = "pages";
    private static final String COLUMN_PAGES_ID = "mid";
    private static final String COLUMN_PAGES_PKG_NAME = "pkg_name";
    private static final String COLUMN_PAGES_APP_NAME = "app_name";
    private static final String COLUMN_PAGES_PAGE_NUM = "page_num";

    // Collect table
    private static final String TABLE_COLLECT = "collect";
    private static final String COLUMN_COLLECT_ID = "mid";
    private static final String COLUMN_COLLECT_PAGE_COLLECT_DATA = "page_collect_data";
    private static final String COLUMN_COLLECT_PAGE_COLLECT_NUM = "page_collect_num";

    public PageDataHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_PAGES_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_PAGES + "("
                + COLUMN_PAGES_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_PAGES_PKG_NAME + " TEXT UNIQUE,"
                + COLUMN_PAGES_APP_NAME + " TEXT,"
                + COLUMN_PAGES_PAGE_NUM + " INTEGER" + ")";
        db.execSQL(CREATE_PAGES_TABLE);

        String CREATE_COLLECT_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_COLLECT + "("
                + COLUMN_COLLECT_ID + " INTEGER,"
                + COLUMN_COLLECT_PAGE_COLLECT_DATA + " TEXT NOT NULL,"
                + COLUMN_COLLECT_PAGE_COLLECT_NUM + " INTEGER,"
                + "PRIMARY KEY(" + COLUMN_COLLECT_ID + "," + COLUMN_COLLECT_PAGE_COLLECT_DATA + "),"
                + "FOREIGN KEY(" + COLUMN_COLLECT_ID + ") REFERENCES " + TABLE_PAGES + "(" + COLUMN_PAGES_ID + ")" + " ON DELETE CASCADE" + ")";
        db.execSQL(CREATE_COLLECT_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COLLECT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PAGES);
        onCreate(db);
    }

    // clearDatabase
    public void clearDatabase() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_PAGES);
        db.execSQL("DELETE FROM " + TABLE_COLLECT);
        db.close();
    }

    // Add a new page to the pages table
    public long addPage(String pkgName,String appName, int pageNum) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PAGES_PKG_NAME, pkgName);
        values.put(COLUMN_PAGES_APP_NAME, appName);
        values.put(COLUMN_PAGES_PAGE_NUM, pageNum);
        long id = db.insert(TABLE_PAGES, null, values);
        db.close();
        return id;
    }

    // Get all pages
    public List<Page> getAllPages(){
        ArrayList<Page> pages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_PAGES, null)) {
            if (cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_PAGES_ID));
                    String pkgName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PAGES_PKG_NAME));
                    String appName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PAGES_APP_NAME));
                    String pageNum = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PAGES_PAGE_NUM));
                    Page page = new Page(id, pkgName, appName, pageNum);
                    pages.add(page);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while getting all pages: " + e.getMessage());
        }
        return pages;
    }

    // Get the pageID (mid) by its package name (pkgName)
    public long getPageIdByPkgName(String pkgName) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_PAGES_ID};
        String selection = COLUMN_PAGES_PKG_NAME + " = ?";
        String[] selectionArgs = {pkgName};
        Cursor cursor = db.query(TABLE_PAGES, columns, selection, selectionArgs, null, null, null);
        int pageId = -1;
        if (cursor.moveToFirst()) {
            pageId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PAGES_ID));
        }
        cursor.close();
        db.close();
        return pageId;
    }

    // Get the pageNum by its package name (pkgName)
    public int getPageNumByPkgName(String pkgName) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_PAGES_PAGE_NUM};
        String selection = COLUMN_PAGES_PKG_NAME + " = ?";
        String[] selectionArgs = {pkgName};
        Cursor cursor = db.query(TABLE_PAGES, columns, selection, selectionArgs, null, null, null);
        int pageNum = -1;
        if (cursor.moveToFirst()) {
            pageNum = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PAGES_PAGE_NUM));
        }
        cursor.close();
        db.close();
        return pageNum;
    }

    // Get the page number by its ID (mid)
    public int getPageNumById(long pageId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_PAGES_PAGE_NUM};
        String selection = COLUMN_PAGES_ID + " = ?";
        String[] selectionArgs = {String.valueOf(pageId)};
        Cursor cursor = db.query(TABLE_PAGES, columns, selection, selectionArgs, null, null, null);
        int pageNum = -1;
        if (cursor.moveToFirst()) {
            pageNum = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PAGES_PAGE_NUM));
        }
        cursor.close();
        db.close();
        return pageNum;
    }

    // Increment the page number by 1 for the given page ID (mid)
    public void incrementPageNumById(long pageId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String selection = COLUMN_PAGES_ID + " = ?";
        String[] selectionArgs = {String.valueOf(pageId)};
        db.execSQL("UPDATE " + TABLE_PAGES + " SET " + COLUMN_PAGES_PAGE_NUM + " = " + COLUMN_PAGES_PAGE_NUM + " + 1 WHERE " + selection, selectionArgs);
        db.close();
    }

    // Increment the page_num for a given pkg_name in the pages table
    public void incrementPageNumByPkgName(String pkgName) {
        SQLiteDatabase db = this.getWritableDatabase();
        String selection = COLUMN_PAGES_PKG_NAME + " = ?";
        String[] selectionArgs = {pkgName};
        db.execSQL("UPDATE " + TABLE_PAGES + " SET " + COLUMN_PAGES_PAGE_NUM + " = " + COLUMN_PAGES_PAGE_NUM + " + 1 WHERE " + selection, selectionArgs);
        db.close();
    }

    // delete page and collect data for given a pageId
    public void delPageAndCollectData(long mid){
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // delete from collect table
            db.delete(TABLE_COLLECT, COLUMN_COLLECT_ID + "=?", new String[]{String.valueOf(mid)});
            // delete from pages table
            db.delete(TABLE_PAGES, COLUMN_PAGES_ID + "=?", new String[]{String.valueOf(mid)});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("PageDataHelper", "Error deleting page and collect data with mid: " + mid, e);
        } finally {
            db.endTransaction();
        }
    }

    // Add a new collect to the collect table
    public long addCollect(long pageId, String pageCollectData, int pageCollectNum) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_COLLECT_ID, pageId);
        values.put(COLUMN_COLLECT_PAGE_COLLECT_DATA, pageCollectData);
        values.put(COLUMN_COLLECT_PAGE_COLLECT_NUM, pageCollectNum);
        long id = db.insert(TABLE_COLLECT, null, values);
        db.close();
        return id;
    }

    // Get all page_collect_data values for a given page ID (mid) from the collect table
    public ArrayList<String> getCollectPageCollectDataByMid(long pageId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_COLLECT_PAGE_COLLECT_DATA};
        String selection = COLUMN_COLLECT_ID + " = ?";
        String[] selectionArgs = {String.valueOf(pageId)};
        Cursor cursor = db.query(TABLE_COLLECT, columns, selection, selectionArgs, null, null, null);
        ArrayList<String> pageCollectDataList = new ArrayList<>();
        while (cursor.moveToNext()) {
            String pageCollectData = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COLLECT_PAGE_COLLECT_DATA));
            pageCollectDataList.add(pageCollectData);
        }
        cursor.close();
        db.close();
        return pageCollectDataList;
    }

    // Get all_page_collect for given a pageId
    public List<PageCollect> getPageCollectsByMid(long pageId){
        ArrayList<PageCollect> pageCollects = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_COLLECT + " WHERE " + COLUMN_COLLECT_ID + " = " + pageId, null)) {
            if (cursor.moveToFirst()) {
                do {
                    String pageCollectData = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COLLECT_PAGE_COLLECT_DATA));
                    String pageCollectNum = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COLLECT_PAGE_COLLECT_NUM));
                    PageCollect pageCollect = new PageCollect(pageCollectData, pageCollectNum);
                    pageCollects.add(pageCollect);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while getting pageCollects: " + e.getMessage());
        }
        return pageCollects;
    }

    // update pageNum by given a pageId
    public void updatePageNumByPageId(long pageId, int pageNum) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PAGES_PAGE_NUM, pageNum);
        db.update(TABLE_PAGES, values, COLUMN_PAGES_ID + " = ?", new String[]{String.valueOf(pageId)});
        db.close();
    }


    // Get the page collect number by its ID (mid) and collect data
    public int getPageCollectNumByIdAndData(long pageId, String collectData) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_COLLECT_PAGE_COLLECT_NUM};
        String selection = COLUMN_COLLECT_ID + " = ? AND " + COLUMN_COLLECT_PAGE_COLLECT_DATA + " = ?";
        String[] selectionArgs = {String.valueOf(pageId), collectData};
        Cursor cursor = db.query(TABLE_COLLECT, columns, selection, selectionArgs, null, null, null);
        int pageCollectNum = -1;
        if (cursor.moveToFirst()) {
            pageCollectNum = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COLLECT_PAGE_COLLECT_NUM));
        }
        cursor.close();
        db.close();
        return pageCollectNum;
    }

    // Increment the page collect number by 1 for the given page ID (mid) and collect data
    public void incrementPageCollectNumByIdAndData(long pageId, String collectData) {
        SQLiteDatabase db = this.getWritableDatabase();
        String selection = COLUMN_COLLECT_ID + " = ? AND " + COLUMN_COLLECT_PAGE_COLLECT_DATA + " = ?";
        String[] selectionArgs = {String.valueOf(pageId), collectData};
        db.execSQL("UPDATE " + TABLE_COLLECT + " SET " + COLUMN_COLLECT_PAGE_COLLECT_NUM + " = " + COLUMN_COLLECT_PAGE_COLLECT_NUM + " + 1 WHERE " + selection, selectionArgs);
        db.close();
    }

    // delete collect data by given a mid and pageCollectData
    public void deleteCollectRow(long mid, String pageCollectData) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_COLLECT, COLUMN_COLLECT_ID + " = ? AND " + COLUMN_COLLECT_PAGE_COLLECT_DATA + " = ?", new String[] { String.valueOf(mid), pageCollectData });
        db.close();
    }


    public static class Page{
        public final long mid;
        public final String packageName;
        public final String appName;
        public final String pageNum;
        Page(long mid, String packageName,String appName, String pageNum){
            this.mid = mid;
            this.packageName = packageName;
            this.appName = appName;
            this.pageNum = pageNum;
        }
    }

    public static class PageCollect{
        public final String pageCollectData;
        public final String pageCollectNum;
        PageCollect(String pageCollectData, String pageCollectNum){
            this.pageCollectData = pageCollectData;
            this.pageCollectNum = pageCollectNum;
        }
    }

}
