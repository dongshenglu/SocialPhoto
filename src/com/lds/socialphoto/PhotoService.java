package com.lds.socialphoto;

import static com.lds.socialphoto.Constants.*;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;


public class PhotoService extends Service {
	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;
	
	// Search photo
	private final static String FLICKR_SEARCH_URL = "https://api.flickr.com/services/rest/?&method=flickr.photos.search&api_key=f7a3d616960aa3ba9def87d5ff7dc160";
	
	// Fetch different size pictures
	private final static String FLICKR_GET_PICTURE_URL = "https://api.flickr.com/services/rest/?&method=flickr.photos.getSizes&api_key=f7a3d616960aa3ba9def87d5ff7dc160";

	private final static String URL_LATITUDE = "&lat=";
	private final static String URL_LONGITUDE = "&lon=";
	private final static String URL_PHOTO_ID = "&photo_id=";
	private final static String URL_SUFFIX = "&format=json";
	private final static String URL_SEARCH_AMOUNT_PERPAGE = "&per_page=20";
	private final static String URL_SEARCH_PAGE_INDEX = "&page=";
	private final static String URL_SEARCH_TEXT_PARAM = "&text=";
	private final static String URL_SEARCH_TAGS = "&extras=tags";
	private final static String URL_SEARCH_DESCRIPTION = "&extras=description";
	private final static String URL_SEARCH_TAGS_DESCRIPTION = "&extras=description%2C+tags";
	
	private final static String RESPONSE_PREFIX = "jsonFlickrApi(";
	
	// JSON Node names
    private static final String TAG_PHOTOS = "photos";
    //private static final String TAG_PAGE = "page";
    private static final String TAG_PERPAGE = "perpage";
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
	
	public PhotoService() {
	}

	// Handler that receives messages from the thread
	private final class ServiceHandler extends Handler {
		
		private class PhotoDetails {
	    	public String id;
	    	public String title;
	    	public String thumbnailUrl;
	    	public String largeUrl;
	    	public String tags;
	    	public String description;
	    	public byte[] thumbnailImage;
	    	
	    }
		
		private int mNextPage = 1;
	    private int mLastPage = MAX_RESULT_FROM_FLICKR / PERPAGE;
	    
		List<PhotoDetails> mPhotoList = null;
		String mSearchKeyword = new String(PHOTO_EMPTY);
		String mExtraParam = new String(PHOTO_EMPTY);
		String mLocationParam = new String(PHOTO_EMPTY);
		ArrayList<Integer> mSearchRules = new ArrayList<Integer>(SEARCH_RULE_COUNT);
		
		double mLongitude = 0.0;
  	  	double mLatitude = 0.0;
  	  	boolean mIsTitleSelected = false;
  	  	boolean mIsTagsSelected = false;
  	  	boolean mIsDescriptionSelected = false;
  	  	boolean mLocationEnabled = false;
		
		public ServiceHandler(Looper looper) {
	          super(looper);
	          mPhotoList = new ArrayList<PhotoDetails>(PERPAGE);
	      }
		
	      @Override
	      public void handleMessage( Message msg ) {
	    	  
	    	  mPhotoList.clear();
	    	  mNextPage = 1;
	    	  mLastPage = MAX_RESULT_FROM_FLICKR / PERPAGE;
	    	  
	    	  Intent intent = (Intent) msg.obj;
	    	  
	    	  if ( configureSearchRules(intent) ) {

		    	  while ( mNextPage <= mLastPage ) {
		    		  int nextPage = mNextPage;
			    	  boolean ret = fetchPhotoMetaData();  
			    	  // Send message back to activity
			    	  Message retMsg = Message.obtain();
	
			    	  if (mNextPage < mLastPage) {
			    		  retMsg.what = REQUEST_STATUS_ONE_PAGE_LOADED;
			    	  }
			    	  else {
			    		  retMsg.what = REQUEST_STATUS_ALL_PAGES_LOADED;			    		  
			    	  }
			    	  mNextPage++;
			    	  
			    	  if ( ret ) {  
			    		  if ( mLastPage >= 1 )
			    			  retMsg.arg1 = RES_OK;
			    		  else
			    			  retMsg.arg1 = RES_NO_FOUND;
			    	  }
			    	  else
			    		  retMsg.arg1 = RES_FAILED;
			    	  
			    	  retMsg.arg2 = nextPage;
			    	  
			    	  Messenger messenger = ( Messenger ) intent.getExtras().get(PHOTO_MESSENGER);
			    	  try {
			    		  messenger.send( retMsg );
			    	  } catch (RemoteException e) {
			    		  System.out.println(e.getMessage());
			    	  }
		    	  }
	    	  }
	    		
	    	  stopSelf(msg.arg1);
	    	  Log.i(TAG, "Service stopped start id := " + msg.arg1 );
	    	  
	      }
	      
	      private boolean configureSearchRules(Intent intent) {
	    	  boolean success = true;
	    	  if (intent == null)  return false;
	    	  
	    	  mSearchKeyword = intent.getStringExtra(PHOTO_SEARCH_KEYWORD);
	    	  if ( mSearchKeyword.isEmpty() )
	    		  return success;
	    	  
			  mSearchRules = intent.getIntegerArrayListExtra(PHOTO_SEARCH_RULE);
			  mIsTagsSelected = false;
			  mIsDescriptionSelected = false;
			  mIsTitleSelected = false;
			  for(int ii = 0; ii < mSearchRules.size(); ++ii) {
				  if ( mSearchRules.get(ii) == 0 ) {
					  mIsTitleSelected = true;
				  }
				  else if ( mSearchRules.get(ii) == 1 ) {
					  mIsTagsSelected = true;
				  } else if ( mSearchRules.get(ii) == 2 ) {
					  mIsDescriptionSelected = true;
				  }	    		    
			  }
				  
			  mExtraParam = PHOTO_EMPTY;
			  if (mIsTagsSelected && mIsDescriptionSelected) {
				  mExtraParam = URL_SEARCH_TAGS_DESCRIPTION;
			  } else if (mIsTagsSelected) {
				  mExtraParam = URL_SEARCH_TAGS;
			  } else if (mIsDescriptionSelected) {
				  mExtraParam = URL_SEARCH_DESCRIPTION;
			  }
			  
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

		private boolean fetchPhotoMetaData()
	      {
	    	  boolean success = false;

	    	  String jsonResponse = callFlickrRestAPI(FLICKR_SEARCH_URL + URL_SEARCH_TEXT_PARAM 
	    			  + mSearchKeyword + mLocationParam + mExtraParam + URL_SUFFIX 
	    			  + URL_SEARCH_AMOUNT_PERPAGE + URL_SEARCH_PAGE_INDEX + mNextPage);
	    	  
	    	  success = parsePhotoJsonResponese( jsonResponse );
	    	  saveToDataBase();
	    	  
	    	  return success;
	      }
	      
	      private String callFlickrRestAPI( String methodUrl ) 
	      {
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
		    	      System.out.println(e.getMessage());
		    	}
	    	    
	    	    return builder.toString();
	      }
	      
	     private boolean parsePhotoJsonResponese( String jsonResponse )
	      {
	    	  boolean success = true;
	    	  try {
	    		  
	    		  jsonResponse = ParseFlickrJsonResponse(jsonResponse);
	    		  
	    		  if (jsonResponse.length() > 0)
	    		  {
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
	    			  if ( photos == null ) {
	    				  success = false;
		    			  return success;
	    			  }
    				  
	    			  // int pageIndex = Integer.parseInt(photos.getString(TAG_PAGE));
	    			  int perPage = Integer.parseInt(photos.getString(TAG_PERPAGE));
	    			  int total = Integer.parseInt(photos.getString(TAG_TOTAL));
	    			  total = total > MAX_RESULT_FROM_FLICKR ? MAX_RESULT_FROM_FLICKR : total; 
	    			  mLastPage = total / perPage;
    				  
    				  if ( photos.has(TAG_PHOTO_ARRAY) ) {
	    				  photoDetails = photos.getJSONArray(TAG_PHOTO_ARRAY);
	    				  if ( photoDetails == null ) {
	    					  success = false;
	    	    			  return success;
	    				  }
		    				  
    					  for (int i = 0; i < photoDetails.length(); i++) {
	    					  PhotoDetails photoDetailData = new PhotoDetails();
	                          JSONObject photoData = photoDetails.getJSONObject(i);			                          
	                          photoDetailData.id = photoData.getString(TAG_PHOTO_ID);
	                          
	                          if (mIsTitleSelected) {
	                        	  photoDetailData.title = photoData.getString(TAG_PHOTO_TITLE);
	                          }
	                          else { 
	                        	  photoDetailData.title = PHOTO_EMPTY;
	                          }
	                          
	                          if (mIsTagsSelected) {
	                        	  photoDetailData.tags = photoData.getString(TAG_PHOTO_TAGS);
	                          }
	                          else { 
	                        	  photoDetailData.tags = PHOTO_EMPTY;
	                          }
	                          
	                          if (mIsDescriptionSelected && photoData.has(TAG_PHOTO_DESCRIPTION)) {
	                        	  JSONObject photoDescription = photoData.getJSONObject(TAG_PHOTO_DESCRIPTION);
	                        	  photoDetailData.description = photoDescription.getString(TAG_PHOTO_DES_CONTENT);
	                          } else {
	                        	  photoDetailData.description = PHOTO_EMPTY;
	                          }
	                          
	                          String jsonRes = callFlickrRestAPI(FLICKR_GET_PICTURE_URL + URL_PHOTO_ID + photoDetailData.id + URL_SUFFIX);
	                          photoDetailData.thumbnailUrl = ParsePhotoURL( jsonRes, "Thumbnail" );
	                          
	                          String jsonRes_largeUrl = callFlickrRestAPI(FLICKR_GET_PICTURE_URL + URL_PHOTO_ID + photoDetailData.id + URL_SUFFIX);
	                          photoDetailData.largeUrl = ParsePhotoURL( jsonRes_largeUrl, "Large" );
	                          
	                          URL thumbnail_url;
	                          try {
	                        	  thumbnail_url = new URL( photoDetailData.thumbnailUrl );
	                        	  HttpURLConnection urlConnection = (HttpURLConnection) thumbnail_url.openConnection();			                        	     
	                        	  urlConnection.setDoInput(true);   
	                        	  urlConnection.connect();     
	                              InputStream is = urlConnection.getInputStream();
	                              
	                              ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	                              int nRead;
	                              byte[] data = new byte[16384];
	                              while ((nRead = is.read(data, 0, data.length)) != -1) {
	                                buffer.write(data, 0, nRead);
	                              }
	                              buffer.flush();
	                              photoDetailData.thumbnailImage = buffer.toByteArray();
	                              
	                          } catch (Exception e) {
	                        	  System.out.println(e.getMessage());
	                          }
	                          
	                          mPhotoList.add( photoDetailData );
	    				  }
    				  }else {
	    			  		success = false;
	    			  	}
    			  	} else {
    			  		success = false;
    			  	}
	    			 	    	  
	    	  	} catch (JSONException e) {
	    	  		System.out.println(e.getMessage());
	    	  		success = false;
	    	  	}
	    	  
	    	  return success;
			}
	      
	     private String ParseFlickrJsonResponse(String response)
	      {
	    	  if (response.length() > 0)
    		  {
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
	      
	      private String ParsePhotoURL(String jsonResponse, String imageSize )
	      {
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
		    					  if (imageData.getString(TAG_PHOTO_LABEL).equals(imageSize))
		    					  {
		    						  imageUrl = imageData.getString(TAG_PHOTO_SOURCE);
		    						  break;
		    					  }
		    				  }
	    				  }
	    			  }
	    		  }
	    		  
	    	  } catch (JSONException e) {
	    	  		System.out.println(e.getMessage());
	    	  	}
	    	  
	    	  return imageUrl;
	      }
	      
	     private void saveToDataBase()
	      {
			  for (PhotoDetails photoData : mPhotoList) {
				  ContentValues values = new ContentValues();
				  values.put( PHOTO_ID, photoData.id );
				  values.put( TITLE, photoData.title );				  
			      values.put( THUMBNAIL_URL, photoData.thumbnailUrl );
			      values.put( THUMBNAIL, photoData.thumbnailImage );
			      values.put( KEYWORD, mSearchKeyword );
			      values.put( DESCRIPTION, photoData.description );
			      values.put( LARGE_URL, photoData.largeUrl );
			      values.put( LATITUDE,  mLatitude );
			      values.put( LONGITUDE, mLongitude );			      
			      values.put( TAGS, photoData.tags );
			      
			      getContentResolver().insert( CONTENT_URI, values ); 
			  }
	      }
	      
	  } // ServiceHandler


	@Override
	  public void onCreate() {
		Log.i ( TAG, "Service onCreate" );
		
		HandlerThread thread = new HandlerThread( "PhotoService" );
	    thread.start();

	    mServiceLooper = thread.getLooper();
	    mServiceHandler = new ServiceHandler( mServiceLooper );
	}
	

	@Override
	public int onStartCommand( Intent intent, int flags, int startId ) {
		Log.i(TAG, "Service onStartCommand");
		Message msg = mServiceHandler.obtainMessage();
	    msg.arg1 = startId;
	    msg.obj = intent;
	    mServiceHandler.sendMessage(msg);
	    
	    return Service.START_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	  public void onDestroy() {
		Log.i(TAG, "Service onDestroy");
		if (mServiceLooper != null)
			mServiceLooper.quit();
	}

}
