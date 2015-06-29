package com.lds.socialphoto;

import static com.lds.socialphoto.Constants.*;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class PhotosWebServiceHandler {

	// Search photo
	private final static String FLICKR_SEARCH_URL = "https://api.flickr.com/services/rest/?&method=flickr.photos.search&api_key=f7a3d616960aa3ba9def87d5ff7dc160";
	
	// Fetch different size pictures
	private final static String FLICKR_GET_PICTURE_URL = "https://api.flickr.com/services/rest/?&method=flickr.photos.getSizes&api_key=f7a3d616960aa3ba9def87d5ff7dc160";

	private final static String URL_LATITUDE = "&lat=";
	private final static String URL_LONGITUDE = "&lon=";
	private final static String URL_PHOTO_ID = "&photo_id=";
	private final static String URL_SUFFIX = "&format=json";
	private final static String URL_SEARCH_AMOUNT_PERPAGE = "&per_page=";
	private final static String URL_SEARCH_PAGE_INDEX = "&page=";
	private final static String URL_SEARCH_TEXT_PARAM = "&text=";
	private final static String URL_SEARCH_TAGS = "&extras=tags";
	private final static String URL_SEARCH_DESCRIPTION = "&extras=description";
	private final static String URL_SEARCH_TAGS_DESCRIPTION = "&extras=description%2C+tags";
	
	private final static String RESPONSE_PREFIX = "jsonFlickrApi(";
	
	// JSON Node names
    private static final String TAG_PHOTOS = "photos";
    private static final String TAG_TOTAL = "total";
    private static final String TAG_PHOTO_ARRAY = "photo";
    private static final String TAG_PHOTO_ID = "id";
    private static final String TAG_PHOTO_TITLE = "title";
    private static final String TAG_PHOTO_TAGS = "tags";
    private static final String TAG_PHOTO_DESCRIPTION = "description";
    private static final String TAG_PHOTO_DES_CONTENT = "_content";
    
    private static final String TAG_PHOTO_SIZES = "sizes";
    private static final String TAG_PHOTO_SIZE = "size";
    private static final String TAG_PHOTO_LABEL = "label";
    private static final String TAG_PHOTO_SOURCE = "source";
    
    // 4000 is MAX search result returned from Flickr, set 200 for saving time on testing.
    private static final int MAX_RESULT_FROM_FLICKR = 200;
    private static final int PERPAGE = 5;
    
    public static class PhotoDetails {
    	public String id;
    	public String title;
    	public String thumbnailUrl;
    	public String largeUrl;
    	public String tags;
    	public String description;
    	public String searchKeyword;
    	public byte[] thumbnailImage;
    	public double longitude;
    	public double latitude;
    }
	
    private Context mContext;    
    private static String mSearchKeyword = new String(PHOTO_EMPTY);
    private String mExtraParam = new String(PHOTO_EMPTY);
    private String mLocationParam = new String(PHOTO_EMPTY);
    private ArrayList<Integer> mSearchRules = new ArrayList<Integer>(SEARCH_RULE_COUNT);
	
    private static double mLongitude = 0.0;
    private static double mLatitude = 0.0;
    private boolean mIsTitleSelected = false;
    private boolean mIsTagsSelected = false;
    private boolean mIsDescriptionSelected = false;
    private boolean mLocationEnabled = false;
    private Messenger mMessenger;  	
    
	public PhotosWebServiceHandler(Context context) {
		mContext = context;
	}

	public void getTotalPhotos(Intent intent) {
		if (configureSearchRules(intent)) {			
			int total = fetchTotalPhotos();
			String methodUrl = FLICKR_SEARCH_URL + URL_SEARCH_TEXT_PARAM 
		  			  + mSearchKeyword + mLocationParam + mExtraParam + URL_SUFFIX 
		  			  + URL_SEARCH_AMOUNT_PERPAGE + PERPAGE + URL_SEARCH_PAGE_INDEX;
			
			Message retMsg = Message.obtain();
			retMsg.what = REQUEST_STATUS_TOTAL_PHOTOS;
			retMsg.obj = methodUrl;
			retMsg.arg1 = total;
			retMsg.arg2 = total / PERPAGE;
			Messenger messenger = (Messenger) intent.getExtras().get(PHOTO_MESSENGER);
			try {
				messenger.send( retMsg );
	    	} catch (RemoteException e) {
	    		if (e.getMessage() != null) { Log.e(TAG, e.getMessage()); }
	    	}
		}
	}

	public void fetchPhotoJson( Intent intent ) {
	  	String url = intent.getStringExtra(PHOTO_SEARCH_URL);
	  	int pageIndex = intent.getIntExtra(PHOTO_SEARCH_PAGE_INDEX, 1);
	  	int totalPages = intent.getIntExtra(PHOTO_SEARCH_TOTAL_PAGES, 1);
	  	mMessenger = (Messenger) intent.getExtras().get(PHOTO_MESSENGER);
	  	fetchPhotoMetaData(url, pageIndex, totalPages);	  	
	}
	
	private boolean configureSearchRules(Intent intent) {
  	  	boolean success = true;
  	  	if (intent == null)  { return false; }
  	  	mSearchKeyword = intent.getStringExtra(PHOTO_SEARCH_KEYWORD);
  	  	if ( mSearchKeyword.isEmpty() ) { return success; }
  	  	
  	  	mSearchRules = intent.getIntegerArrayListExtra(PHOTO_SEARCH_RULE);
  	  	mIsTagsSelected = false;
  	  	mIsDescriptionSelected = false;
  	  	mIsTitleSelected = false;
  	  	for(int ii = 0; ii < mSearchRules.size(); ++ii) {
  	  		if ( mSearchRules.get(ii) == 0 ) { mIsTitleSelected = true; }
  	  		else if ( mSearchRules.get(ii) == 1 ) { mIsTagsSelected = true; } 
  	  		else if ( mSearchRules.get(ii) == 2 ) { mIsDescriptionSelected = true; }	    		    
  	  	}
		  
  	  	mExtraParam = PHOTO_EMPTY;
  	  	if (mIsTagsSelected && mIsDescriptionSelected) { mExtraParam = URL_SEARCH_TAGS_DESCRIPTION; }
  	  	else if (mIsTagsSelected) { mExtraParam = URL_SEARCH_TAGS; } 
  	  	else if (mIsDescriptionSelected) { mExtraParam = URL_SEARCH_DESCRIPTION; }
  	  	mLocationParam = PHOTO_EMPTY;
  	  	mLocationEnabled = false;
  	  	mLocationEnabled = intent.getBooleanExtra(PHOTO_LOCATION_ENABLED, false);
  	  	if ( mLocationEnabled ) {
  	  		mLongitude = intent.getDoubleExtra(LONGITUDE, 0.0);
  	  		mLatitude = intent.getDoubleExtra(LATITUDE, 0.0);
  	  		mLocationParam = URL_LATITUDE + mLatitude + URL_LONGITUDE + mLongitude;
  	  	}
	  
  	  	return success;
	}
	
	private int fetchTotalPhotos()
    {
    	String jsonResponse = callFlickrRestAPI(FLICKR_SEARCH_URL + URL_SEARCH_TEXT_PARAM 
    			  + mSearchKeyword + mLocationParam + mExtraParam + URL_SUFFIX
    			  + URL_SEARCH_AMOUNT_PERPAGE + PERPAGE);
    	int total = 0;
  	  	try {
  	  		jsonResponse = ParseFlickrJsonResponse(jsonResponse);
  	  		if (jsonResponse.length() > 0) {
  	  			String str = jsonResponse.substring(0, RESPONSE_PREFIX.length());
  	  			if (str.equals(RESPONSE_PREFIX) ) {
  	  				jsonResponse = jsonResponse.replace(RESPONSE_PREFIX, PHOTO_EMPTY);
  	  			}
  	  			if (jsonResponse.charAt(jsonResponse.length()-1) == ')') {
  	  				jsonResponse = jsonResponse.substring(0, jsonResponse.length()-1);
  	  			}
  	  		} else { return 0; }
  		  
  	  		JSONObject jsonRootObject = new JSONObject( jsonResponse );
  	  		JSONObject photos = null;
  	  		if ( jsonRootObject.has(TAG_PHOTOS) ) {
  	  			photos = jsonRootObject.getJSONObject(TAG_PHOTOS);
  	  			if ( photos == null ) { return 0; }
  	  			total = Integer.parseInt(photos.getString(TAG_TOTAL)); 			  
  	  		}
  	  	} catch (JSONException e) { 
  	  		if (e.getMessage() != null) { Log.e(TAG, e.getMessage()); } 
  	  	}
  	  	return total;
    }

	private boolean fetchPhotoMetaData(String url, int pageIndex, int totalPages) {
  	  	boolean success = false;
  	  	String jsonResponse = callFlickrRestAPI(url);  	  
  	  	success = parsePhotoJsonResponese( jsonResponse, pageIndex, totalPages );
  	  	return success;
    }
    
    private String callFlickrRestAPI( String methodUrl ) {
  	  	StringBuilder builder = new StringBuilder();
  	  	InputStream in = null;
  	    try {
  	    	URL url = new URL(methodUrl);
  	    	HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
  	    	in = new BufferedInputStream(urlConnection.getInputStream());
  	    	BufferedReader reader = new BufferedReader(new InputStreamReader(in));
  	    	String line;
  	    	while ((line = reader.readLine()) != null) {
  	    		builder.append(line);
                }
  	    } catch (Exception e ) { 
  	    	if (e.getMessage() != null) { Log.e(TAG, e.getMessage()); } 
  	    }
  	    return builder.toString();
    }
    
    private boolean parsePhotoJsonResponese(String jsonResponse, int pageIndex, int totalPages) {
  	  	boolean success = true;
  	  	try {
  	  		jsonResponse = ParseFlickrJsonResponse(jsonResponse);
  	  		if (jsonResponse.length() > 0) {
  	  			String str = jsonResponse.substring(0, RESPONSE_PREFIX.length());
  	  			if (str.equals(RESPONSE_PREFIX) ) {
  				  jsonResponse = jsonResponse.replace(RESPONSE_PREFIX, PHOTO_EMPTY);
  	  			}
  	  			if (jsonResponse.charAt(jsonResponse.length()-1) == ')') {
  	  				jsonResponse = jsonResponse.substring(0, jsonResponse.length()-1);
  	  			}
  	  		} else {
  	  			success = false;
  	  			return success;
  	  		}
  		  
  	  		JSONObject jsonRootObject = new JSONObject( jsonResponse );
  	  		JSONObject photos = null;
  	  		JSONArray photoDetails = null;
  	  		if ( jsonRootObject.has(TAG_PHOTOS) ) {
  	  			photos = jsonRootObject.getJSONObject(TAG_PHOTOS);
  	  			if ( photos == null ) { return false; }
  	  			int total = Integer.parseInt(photos.getString(TAG_TOTAL));
  	  			total = total > MAX_RESULT_FROM_FLICKR ? MAX_RESULT_FROM_FLICKR : total; 
				  
  	  			if ( photos.has(TAG_PHOTO_ARRAY) ) {
  	  				photoDetails = photos.getJSONArray(TAG_PHOTO_ARRAY);
  	  				if ( photoDetails == null ) { return false; }
  	  				for (int i = 0; i < photoDetails.length(); i++) {
  	  					PhotoDetails photoDetailData = new PhotoDetails();
  	  					JSONObject photoData = photoDetails.getJSONObject(i);			                          
  	  					photoDetailData.id = photoData.getString(TAG_PHOTO_ID);
  	  					if (mIsTitleSelected) { photoDetailData.title = photoData.getString(TAG_PHOTO_TITLE); }
  	  					else { photoDetailData.title = PHOTO_EMPTY; }
  	  					if (mIsTagsSelected) {
  	  						photoDetailData.tags = photoData.getString(TAG_PHOTO_TAGS); } 
  	  					else { photoDetailData.tags = PHOTO_EMPTY; }
		                
  	  					if (mIsDescriptionSelected && photoData.has(TAG_PHOTO_DESCRIPTION)) {
  	  						JSONObject photoDescription = photoData.getJSONObject(TAG_PHOTO_DESCRIPTION);
  	  						photoDetailData.description = photoDescription.getString(TAG_PHOTO_DES_CONTENT);
		              	} else { photoDetailData.description = PHOTO_EMPTY; }
		                
		                String jsonRes = callFlickrRestAPI(FLICKR_GET_PICTURE_URL + URL_PHOTO_ID + photoDetailData.id + URL_SUFFIX);
		                photoDetailData.thumbnailUrl = ParsePhotoURL( jsonRes, "Thumbnail" );
		                
		                String jsonRes_largeUrl = callFlickrRestAPI(FLICKR_GET_PICTURE_URL + URL_PHOTO_ID + photoDetailData.id + URL_SUFFIX);
		                photoDetailData.largeUrl = ParsePhotoURL( jsonRes_largeUrl, "Large" );
		                photoDetailData.searchKeyword = mSearchKeyword;
		                photoDetailData.latitude = mLatitude;
		                photoDetailData.longitude = mLongitude;
		                boolean isFirstPhoto = (pageIndex == 1 && i == 0);
		                boolean isLastPhotoInPage = (i+1 == photoDetails.length());
		                boolean isLastPage = (pageIndex == totalPages);
		                PhotoDownloadService.startDownload(photoDetailData.thumbnailUrl, mContext, photoDetailData, mMessenger, isFirstPhoto, isLastPhotoInPage, isLastPage);
		            }
			  } else { success = false; }
	  	} else { success = false; }
  			 	    	  
  	  	} catch (JSONException e) {
  	  		if (e.getMessage() != null) { Log.e(TAG, e.getMessage()); }
  	  		success = false;
  	  	}
  	  
  	  return success;
	}
    
   private String ParseFlickrJsonResponse(String response) {
	   if (response.length() > 0) {
		   String str = response.substring(0, RESPONSE_PREFIX.length());
		   if (str.equals(RESPONSE_PREFIX) ) {
			   response = response.replace(RESPONSE_PREFIX, PHOTO_EMPTY);
		   }
		   if (response.charAt(response.length()-1) == ')') {
			   response = response.substring(0, response.length()-1);
		   }
	   }
  	  return response;
    }
    
    private String ParsePhotoURL(String jsonResponse, String imageSize ) {
    	String imageUrl = PHOTO_EMPTY;
  	  	jsonResponse = ParseFlickrJsonResponse(jsonResponse);
  	  	try {
  	  		JSONObject jsonRootObject = new JSONObject( jsonResponse );
  	  		JSONObject photoSizes = null;
  	  		JSONArray photoSizeDetails = null;
  	  		if ( jsonRootObject.has(TAG_PHOTO_SIZES) ) {
  	  			photoSizes = jsonRootObject.getJSONObject(TAG_PHOTO_SIZES);
  	  			if ( photoSizes != null && photoSizes.has(TAG_PHOTO_SIZE) ) {
  	  				photoSizeDetails = photoSizes.getJSONArray(TAG_PHOTO_SIZE);
  	  				if ( photoSizeDetails != null ) {
  	  					for (int i = 0; i < photoSizeDetails.length(); i++) {
  	  						JSONObject imageData = photoSizeDetails.getJSONObject(i);
	    					if (imageData.getString(TAG_PHOTO_LABEL).equals(imageSize)) {
	    						imageUrl = imageData.getString(TAG_PHOTO_SOURCE);
	    						break;
	    					}
  	  					}
  				  }
  			  }
  		  }
  	  	} catch (JSONException e) { 
  	  		if (e.getMessage() != null) { Log.e(TAG, e.getMessage()); } 
  	  	}
  	  	return imageUrl;
    }

}
