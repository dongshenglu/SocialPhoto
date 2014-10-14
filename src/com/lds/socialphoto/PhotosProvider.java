package com.lds.socialphoto;

import static com.lds.socialphoto.Constants.*;
import static android.provider.BaseColumns._ID;

import java.util.Arrays;
import java.util.HashSet;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class PhotosProvider extends ContentProvider {

	private static final int PHOTOS = 10;
	private static final int PHOTOS_ID = 20;
	
	private PhotosData photoData;
	
	private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	  static {
	    sURIMatcher.addURI(AUTHORITY, TABLE_NAME, PHOTOS);
	    sURIMatcher.addURI(AUTHORITY, TABLE_NAME + "/#", PHOTOS_ID);
	  }
	  
	public PhotosProvider() {
		
	}

	@Override
	public boolean onCreate() {
	    photoData = new PhotosData( getContext() );
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		
	    SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

	    // check if the caller has requested a column which does not exists
	    checkColumns(projection);

	    // Set the table
	    queryBuilder.setTables( TABLE_NAME );

	    int uriType = sURIMatcher.match(uri);
	    switch ( uriType ) {
		    case PHOTOS:
		      break;
		    case PHOTOS_ID:
		      // adding the ID to the original query
		      queryBuilder.appendWhere( _ID + "=" + uri.getLastPathSegment());
		      break;
		    default:
		      throw new IllegalArgumentException("Unknown URI: " + uri);
	    }

	    SQLiteDatabase db = photoData.getWritableDatabase();
	    Cursor cursor = queryBuilder.query(db, projection, selection,
	        selectionArgs, null, null, sortOrder);
	    // make sure that potential listeners are getting notified
	    cursor.setNotificationUri(getContext().getContentResolver(), uri);
	    
		return cursor;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		int uriType = sURIMatcher.match(uri);
	    SQLiteDatabase sqlDB = photoData.getWritableDatabase();
	    long id = 0;
	    switch ( uriType ) {
	    	case PHOTOS:
	    		id = sqlDB.insert( TABLE_NAME, null, values );
	    		break;
	    	default:
	    		throw new IllegalArgumentException("Unknown URI: " + uri);
	    }
	    Uri newUri = ContentUris.withAppendedId( CONTENT_URI, id );
	    getContext().getContentResolver().notifyChange( newUri, null );
	    return newUri;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int uriType = sURIMatcher.match(uri);
	    SQLiteDatabase sqlDB = photoData.getWritableDatabase();
	    int rowsDeleted = 0;
	    switch (uriType) {
	    case PHOTOS:
	    	rowsDeleted = sqlDB.delete( TABLE_NAME, selection, selectionArgs );
	    	break;
	    case PHOTOS_ID:
	    	String id = uri.getLastPathSegment();
	    	rowsDeleted = sqlDB.delete(TABLE_NAME, appendRowId( selection, id ), selectionArgs );
	    	break;
	    default:
	    	throw new IllegalArgumentException("Unknown URI: " + uri);
	    }
	    getContext().getContentResolver().notifyChange( uri, null );
	    return rowsDeleted;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int uriType = sURIMatcher.match(uri);
	    SQLiteDatabase sqlDB = photoData.getWritableDatabase();
	    
	    int rowsUpdated = 0;
	    switch ( uriType ) {
	    	case PHOTOS:
	    		rowsUpdated = sqlDB.update( TABLE_NAME, values, selection, selectionArgs );
	    		break;
	    	case PHOTOS_ID:
		    	String id = uri.getLastPathSegment();
		    	rowsUpdated = sqlDB.update(TABLE_NAME, values, appendRowId( selection, id ), selectionArgs );		    	
		      	break;
	    	default:
	    		throw new IllegalArgumentException("Unknown URI: " + uri);
	    	}
	    getContext().getContentResolver().notifyChange(uri, null);
	    return rowsUpdated;
	}

	private void checkColumns(String[] projection) {
	    String[] available = { PHOTO_ID, TITLE, TAGS, THUMBNAIL_URL, THUMBNAIL, KEYWORD, DESCRIPTION, LATITUDE, LONGITUDE, LARGE_URL, _ID };
	    
	    if ( projection != null ) {
	      HashSet<String> requestedColumns = new HashSet<String>( Arrays.asList( projection ) );
	      HashSet<String> availableColumns = new HashSet<String>( Arrays.asList( available ) );
	      // check if all columns which are requested are available
	      if ( !availableColumns.containsAll( requestedColumns ) ) {
	        throw new IllegalArgumentException("Unknown columns in projection");
	      }
	    }
	  }
	
	private String appendRowId( String selection, String id ) {
	      return _ID + "=" + id
	            + ( !TextUtils.isEmpty( selection ) ? " AND (" + selection + ')' : PHOTO_EMPTY);
	   }
	
}
