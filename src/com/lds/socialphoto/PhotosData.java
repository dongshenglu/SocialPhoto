package com.lds.socialphoto;

import static com.lds.socialphoto.Constants.*;
import static android.provider.BaseColumns._ID;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PhotosData extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "photos.db";
	private static final int DATABASE_VERSION = 1;
	
	// Database creation sql statement
	private static final String DATABASE_CREATE = "create table "
				+ TABLE_NAME + "(" + _ID
				+ " integer primary key autoincrement, " 
				+ PHOTO_ID + " TEXT NOT NULL, " 
				+ TITLE + " TEXT NOT NULL, " 
				+ TAGS  + " TEXT NOT NULL, "
				+ THUMBNAIL_URL + " TEXT NOT NULL, "
				+ THUMBNAIL + " BLOB , "
				+ KEYWORD + " TEXT NOT NULL, " 
				+ DESCRIPTION + " TEXT NOT NULL, " 
				+ LATITUDE + " REAL, " 
				+ LONGITUDE + " REAL, " 
				+ LARGE_URL + " TEXT NOT NULL " + " );";
	  
	public PhotosData(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);

	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL( DATABASE_CREATE );
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
	    onCreate( db );
	}

}
