package com.lds.socialphoto;

import android.net.Uri;
import android.provider.BaseColumns;

public interface Constants extends BaseColumns {
	   public static final String TABLE_NAME = "photos"; 
	   public static final String AUTHORITY = "com.lds.socialphoto.photos";
	   public static final Uri CONTENT_URI = Uri.parse("content://"
	         + AUTHORITY + "/" + TABLE_NAME);
	   

	   // Columns in the Photo database
	   public static final String PHOTO_ID = "photoId";
	   public static final String TITLE = "title";
	   public static final String KEYWORD = "keyword";
	   public static final String LATITUDE = "latitude";
	   public static final String LONGITUDE = "longitude";
	   public static final String THUMBNAIL_URL = "thumbnailUrl";
	   public static final String THUMBNAIL = "thumbnail";
	   public static final String LARGE_URL = "largeUrl";
	   public static final String DESCRIPTION = "description";
	   public static final String TAGS = "tags";
	   public static final String SAVED_RECORD = "saved";
	   public static final String NEW_RECORD = "new record";
	   
	   public static final String ROW_ID = "rowId";
	   
	   public static final String TAG = "com.lds.socialphoto";
	   
	   public static final String PHOTO_SEARCH_KEYWORD = "searchKeyword";
	   public static final String PHOTO_MESSENGER = "messenger";
	   public static final String PHOTO_LOCATION_ENABLED = "location_enabled";
	   public static final String PHOTO_SEARCH_RULE = "searchRule";	   
	   public static final int SEARCH_RULE_COUNT = 3;
	   
	   public static final int RES_OK = 1001;
	   public static final int RES_NO_FOUND = 1002;
	   public static final int RES_FAILED = 1003;
	   
	   public static final int REQUEST_STATUS_ONE_PAGE_LOADED = 2001;
	   public static final int REQUEST_STATUS_ALL_PAGES_LOADED = 2002;
	   
	   public static final String PHOTO_EMPTY = "";
	   
	}
