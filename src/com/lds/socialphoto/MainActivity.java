package com.lds.socialphoto;

import static com.lds.socialphoto.Constants.*;
import static android.provider.BaseColumns._ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;

import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.ListFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity implements
		ActionBar.OnNavigationListener {
	
	public static final String PREFS_NAME = "SocialPhotoPrefFile";
	public static final String PREFS_DROPLISTITEM = "dropListItem";
	public static final String PREFS_DROPLISTITEM_COUNT = "dropListItemCount";
	public static final String SAVE_KEY_DROPLIST = "droplist";
	
	public static final String TAG_DEBUG = "SocialPhoto: ";
	
	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * current dropdown position.
	 */
	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
	private static ArrayAdapter<String> mSpinnerAdapter;
	private static List<CharSequence> mItemList;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Set up the action bar to show a dropdown list.
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST); 
		
		if (savedInstanceState != null && savedInstanceState.containsKey(SAVE_KEY_DROPLIST)) {			
			mItemList = savedInstanceState.getCharSequenceArrayList(SAVE_KEY_DROPLIST);
		} else if (savedInstanceState == null) {
			mItemList = 
					new ArrayList<CharSequence>(Arrays.asList(new String[] { getString(R.string.title_section1) }));
			
			// Restore preferences
		    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		    int count = settings.getInt(PREFS_DROPLISTITEM_COUNT, 0);
		    for (int ii = 1; ii <= count; ++ii) {
		    	String item = settings.getString(PREFS_DROPLISTITEM + ii, PHOTO_EMPTY );
		    	mItemList.add(item);
		      }
		}
		
		mSpinnerAdapter = new ArrayAdapter(actionBar.getThemedContext(),
				android.R.layout.simple_list_item_1,
				android.R.id.text1, mItemList );
		
		// Set up the dropdown list navigation in the action bar.
		actionBar.setListNavigationCallbacks(
		// Specify a SpinnerAdapter to populate the dropdown list.
				mSpinnerAdapter, this);
		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
		
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// Restore the previously serialized current dropdown position.
		if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
			getSupportActionBar().setSelectedNavigationItem(
					savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
		}
		if (savedInstanceState.containsKey(SAVE_KEY_DROPLIST)) {			
			mItemList = savedInstanceState.getCharSequenceArrayList(SAVE_KEY_DROPLIST);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// Serialize the current dropdown position.
		outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getSupportActionBar()
				.getSelectedNavigationIndex());
		
		outState.putCharSequenceArrayList(SAVE_KEY_DROPLIST, (ArrayList<CharSequence>)mItemList); 
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		switch ( id ) {
	        case R.id.action_settings:
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onNavigationItemSelected(int position, long id) {
		// When the given dropdown item is selected, show its contents in the
		// container view.
		getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.container,
						ImageListFragment.newInstance(position + 1)).commit();
		return true;
	}
	
	
	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class ImageListFragment extends ListFragment 
		implements LoaderManager.LoaderCallbacks<Cursor>,
		GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		
		public static enum SearchState {
			SEARCH_NOT_START,
			SEARCH_START,
			SEARCH_IN_PROCESSING,
			SERCH_FAILED,
			SEARCH_COMPLETE
		}
		
		private SearchState mSearchState = SearchState.SEARCH_NOT_START;

		private static final String ARG_SECTION_NUMBER = "section_number";
		private static String[] FROM = { TITLE, THUMBNAIL };
		private static int[] TO = { R.id.photoname, R.id.thumbnail };
		
		private SimpleCursorAdapter mAdapter;
		
		private String mSearchString = PHOTO_EMPTY;
		private String mCurrentSearchKeyword = PHOTO_EMPTY;
		private String mSavedSearchKeyword = PHOTO_EMPTY;
		
		private int mdropDownItemIndex = 0;
		
		private ProgressDialog mProgressDialog = null;
		private AlertDialog mInfoDialog = null;
		private AlertDialog.Builder mSettingDialog = null;
		private ArrayList<Integer> mSelectedItems = new ArrayList<Integer>();
		LocationClient mLocationClient = null;		
		private Location mCurrentLocation;
		
		
		/**
		 * Returns a new instance of this fragment for the given section number.
		 */
		public static ImageListFragment newInstance(int sectionNumber) {
			ImageListFragment fragment = new ImageListFragment();
			Bundle args = new Bundle();
			args.putInt(ARG_SECTION_NUMBER, sectionNumber);
			fragment.setArguments(args);
			return fragment;
		}

		public ImageListFragment() {
		}

		/*
		 * Implementation of override virtual functions
		 */
		
		@Override
		 public void onCreate(Bundle savedInstanceState) {
		  super.onCreate(savedInstanceState);
		  
		  	Bundle bundle = this.getArguments();
		  	// start from 1
		  	mdropDownItemIndex = bundle.getInt(ARG_SECTION_NUMBER);
		  	mSavedSearchKeyword = PHOTO_EMPTY;
		  	if ( mdropDownItemIndex > 1 ) {
		  		mSavedSearchKeyword = (String)mSpinnerAdapter.getItem(mdropDownItemIndex-1);
		  	} 
		  	
		  	if ( getActivity() != null ) {
		  		mLocationClient = new LocationClient( getActivity(), this, this);
		  	
		  		buildDialogs();
		  		
			
		  		getPhotos();
		  	}
		 }
	    
		private void buildDialogs() {
			
			mProgressDialog = new ProgressDialog( getActivity() );
	  		mProgressDialog.setMessage( getResources().getString(R.string.searchProgressText)  );
	  		mProgressDialog.setCancelable(false);
	  		mProgressDialog.setIndeterminate(true);
	  		mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
	  		    @Override
	  		    public void onClick(DialogInterface dialog, int which) {
	  		    	if ( getActivity() != null ) {
    					getActivity().stopService( new Intent( getActivity(), PhotoService.class));
    					mSearchState = SearchState.SEARCH_NOT_START;
    					mCurrentSearchKeyword = PHOTO_EMPTY;
    				}
	  		    }
	  		});

	  		mInfoDialog = new AlertDialog.Builder( getActivity() ).create();
	  		
	  		boolean[] checkedItems = { true, true, true };
	  		mSelectedItems.add(0);
	  		mSelectedItems.add(1);
	  		mSelectedItems.add(2);
	  		mSettingDialog = new AlertDialog.Builder( getActivity() );
	  		mSettingDialog.setMultiChoiceItems(R.array.setting_array, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
	  			@Override
	  			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
	  				if (isChecked) {
	  					mSelectedItems.add(which);
	  				} else if (mSelectedItems.contains(which)) { 
	  					mSelectedItems.remove(Integer.valueOf(which));
	  				}
	  			}
	  		})
	  		.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
	               @Override
	               public void onClick(DialogInterface dialog, int id) {
	               }
	           })
	           .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
	               @Override
	               public void onClick(DialogInterface dialog, int id) {
	               }
	           });
	  		
	  		mSettingDialog.create();
		}

		@Override
		public void onDestroyView ()
		{	
			if ( mLocationClient.isConnected() || mLocationClient.isConnecting() )
				mLocationClient.disconnect();
			
			if ( mSearchState == SearchState.SEARCH_IN_PROCESSING || mSearchState == SearchState.SEARCH_START )
				getActivity().stopService( new Intent( getActivity(), PhotoService.class));
			
			super.onDestroyView();
			if ( mProgressDialog != null ) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
			
			if ( mInfoDialog != null ) {
				mInfoDialog.dismiss();
				mInfoDialog = null;
			}			
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			
			final EditText searchBox = (EditText) rootView.findViewById(R.id.searchBox);
			if ( mdropDownItemIndex > 1 )
			{
				searchBox.setVisibility(View.GONE);
			}
			  
			searchBox.addTextChangedListener( new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					
				}
			
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				
				}
			
				@Override
				public void afterTextChanged(Editable s) {
					mSearchString = s.toString();
				}
			});
			
			ImageButton settingButton = (ImageButton) rootView.findViewById( R.id.searchSettings );
			if ( mdropDownItemIndex > 1 )
			{
				settingButton.setVisibility(View.GONE);
			} else {
				settingButton.setOnClickListener( new View.OnClickListener() {
					@Override
				    public void onClick(View v) {
						if ( mSettingDialog != null )
							mSettingDialog.show();
				    }
				});
			}
			  
			ImageButton searchButton = (ImageButton) rootView.findViewById( R.id.searchPhoto );
			if ( mdropDownItemIndex > 1 )
			{
				searchButton.setVisibility(View.GONE);
			} else {
				searchButton.setOnClickListener( new View.OnClickListener() {
					@Override
				    public void onClick(View v) {
				    	openSearch();
				    }
				});
				
				mLocationClient.connect();
			}

			return rootView;
		}
		
		@Override
		public void onListItemClick(ListView l, View v, int position, long id) 
		{
			if ( getActivity() == null ) {
				return;
			}
			
			Intent intent = new Intent( getActivity(), PhotoActivity.class);
			
			Uri photoUri = Uri.parse(CONTENT_URI + "/" + id);
		    intent.putExtra("photoUri", photoUri);
			getActivity().startActivity(intent);
	    }
		
		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			String[] projection = { _ID, TITLE, THUMBNAIL, KEYWORD };
			String selection = "KEYWORD=?";
			CursorLoader cursorLoader = null;
			if ( mdropDownItemIndex == 1 ) {
				String[] selectionArgs = { mSearchString};
		    	cursorLoader = new CursorLoader( getActivity(), 
		    		CONTENT_URI, projection, selection, selectionArgs, null );
			}
			else {
				String[] selectionArgs = { mSavedSearchKeyword };
				cursorLoader = new CursorLoader( getActivity(), 
			    		CONTENT_URI, projection, selection, selectionArgs, null );
			}

		    return cursorLoader;
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			mAdapter.swapCursor(data);
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			mAdapter.swapCursor(null);
		}
		
		@Override
		public void onConnectionFailed(ConnectionResult connectionResult) {
			if (connectionResult.hasResolution()) {
				showMessage("Location connection was failed, please try again.");
				
	        } else {
	        	showMessage("Location connection was failed.");
	        }
	    }

		@Override
		public void onConnected(Bundle arg0) {
			Toast.makeText( getActivity(), "Connected", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onDisconnected() {
			Toast.makeText( getActivity(), "Disconnected. Please re-connect.",
	                Toast.LENGTH_SHORT).show();
		}
	    
		/*
		* Implementation of private functions
		*/		
		
		private Handler sMainActHandler = new Handler() {
	        
			public void handleMessage( Message msg ) {
	        	super.handleMessage( msg );
	        	
	        	switch ( msg.what ) {
	        		case REQUEST_STATUS_ONE_PAGE_LOADED:
	        			if (mProgressDialog != null)
                            mProgressDialog.hide();
	        			
	        			mSearchState = SearchState.SERCH_FAILED;
	        			if (msg.arg1 == RES_OK) {
	        				mSearchState = SearchState.SEARCH_IN_PROCESSING;
	        				if ( isAdded() ) {
	        					restartLoader();
	        				}
	        				if ( getActivity() != null ) {
		        				Toast.makeText(
			        				    getActivity(), 
			        				    "Page" +  Integer.toString(msg.arg2) + " was loaded", 
			        				    Toast.LENGTH_LONG ).show();
	        				}
	        			} else if (msg.arg1 == RES_FAILED) {
	        				showMessage("Search was failed");
	        				mCurrentSearchKeyword = PHOTO_EMPTY;
	        				if ( getActivity() != null ) {
	        					getActivity().stopService( new Intent( getActivity(), PhotoService.class));
	        				}
	        			}
	        			
	        			break;
	        		case REQUEST_STATUS_ALL_PAGES_LOADED:
	        			int currentPage = msg.arg2;
        				if ( currentPage == 1 ) {
	        				if (mProgressDialog != null)
	                            mProgressDialog.hide();
        				}
        				
        				mSearchState = SearchState.SERCH_FAILED;
        				if (msg.arg1 == RES_OK) {
	        				mSearchState = SearchState.SEARCH_COMPLETE;
	        				if ( isAdded() ) {
	        					restartLoader();
	        				}
	        				
	        				mItemList.add(mSearchString);
	        				mSpinnerAdapter.notifyDataSetChanged();
	        				SaveNavigationDropListItem();
	        				
	        				if ( getActivity() != null ) {
		        				Toast.makeText(
			        				    getActivity(), 
			        				    "All pages have been loaded", 
			        				    Toast.LENGTH_LONG ).show();
	        				}
	        				
	        			} else if (msg.arg1 == RES_NO_FOUND ) {
	        				showMessage("Sorry, no matched result was found.");
	        			}
        				else if (msg.arg1 == RES_FAILED) {
	        				showMessage("Search was failed");
	        			}
        				
        				mCurrentSearchKeyword = PHOTO_EMPTY;
        				if ( getActivity() != null ) {
        					getActivity().stopService( new Intent( getActivity(), PhotoService.class));
        				}
	        			
	        			break;
	        		default:
	        			break;
	        	}
	        	
	        }
	    };
	    
	    final Messenger sMessenger = new Messenger(sMainActHandler);
	    
	    private boolean openSearch()
	    {
	    	if ( mSearchString.isEmpty() || getActivity() == null )
	    		return false;
	    	
	    	if ( mSearchState  == SearchState.SEARCH_START 
	    			|| mSearchState == SearchState.SEARCH_IN_PROCESSING ) {
	    		getActivity().stopService( new Intent( getActivity(), PhotoService.class));
	    		if ( !mCurrentSearchKeyword.isEmpty() && mSearchState == SearchState.SEARCH_IN_PROCESSING ) {
	    			mItemList.add(mCurrentSearchKeyword);
    				mSpinnerAdapter.notifyDataSetChanged();
    				SaveNavigationDropListItem();
	    		}
	    	}
	    	
	    	double longitude = 0.0;
	    	double latitude = 0.0;
	    	boolean locationEnabled = false;
	    	mCurrentLocation = mLocationClient.getLastLocation();
	    	if (mCurrentLocation != null) {
	    		longitude = mCurrentLocation.getLongitude();
	    		latitude = mCurrentLocation.getLatitude();
	    		locationEnabled = true;
	    	}
	    	
	    	Intent intent = new Intent( getActivity(), PhotoService.class );
			intent.putExtra( PHOTO_SEARCH_KEYWORD, mSearchString );
			intent.putExtra( PHOTO_MESSENGER, sMessenger );
			if (locationEnabled) {
				intent.putExtra( PHOTO_LOCATION_ENABLED, true );
				intent.putExtra( LATITUDE, latitude );
				intent.putExtra( LONGITUDE, longitude );
			} else
			{
				intent.putExtra( PHOTO_LOCATION_ENABLED, false );
			}
				
			intent.putIntegerArrayListExtra( PHOTO_SEARCH_RULE, mSelectedItems );
			
			mCurrentSearchKeyword = mSearchString;			
			getActivity().startService( intent );
			
			mSearchState = SearchState.SEARCH_START; 
					
			if (mProgressDialog != null)
                mProgressDialog.show();
			
			return true;
	    }
	    
		protected void restartLoader() {
			getLoaderManager().restartLoader( 0, null, this );
		}

		private void getPhotos() {
			getLoaderManager().initLoader( 0, null, this );
			mAdapter = new SimpleCursorAdapter( getActivity(), R.layout.rowlayout, null, FROM, TO, 0 );
			
			mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
	            @Override
				public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
	                if (view.getId() == R.id.thumbnail) {
	                	byte[] byteImage = cursor.getBlob(columnIndex);
	                	int length = byteImage.length;
	                	if ( length > 0 ) {
	                		((ImageView)view).setImageBitmap(BitmapFactory.decodeByteArray( byteImage, 0,
	                				length));
	                	}
	                    return true; //true because the data was bound to the view
	                }
	                
	                return false;
	            }
	        });
			
		    setListAdapter( mAdapter );
		}

	    void showMessage(String messsage )
	    {
	    	mInfoDialog.setMessage( messsage );
	    	AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {				
				@Override
				protected void onPreExecute() {
                    if (mInfoDialog != null)                    	
                        mInfoDialog.show();
				}
					
				@Override
				protected Void doInBackground(Void... arg0) {
					try {
						Thread.sleep( 3000 );
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					return null;
				}
				
				@Override
				protected void onPostExecute(Void result) {
                    try {
                       
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
				}
					
			};
			task.execute((Void[])null);
	    }

	    void SaveNavigationDropListItem() {
	    	if ( getActivity() == null )
	    		return;
	    	
	    	SharedPreferences settings = getActivity().getSharedPreferences(PREFS_NAME, 0);
			SharedPreferences.Editor editor = settings.edit();
			int count = mItemList.size();
			Log.d( TAG_DEBUG, "SharePreference item count : " + count );
			editor.putInt(PREFS_DROPLISTITEM_COUNT, count - 1 );
			// the first item is "New search" not need to store
			for (int ii = 1; ii < count; ++ii) {
				editor.putString(PREFS_DROPLISTITEM + ii, mItemList.get(ii).toString() );
				Log.d( TAG_DEBUG, "SharePreference item " + ii + " : " + mItemList.get(ii).toString() );
			}
			  
			// Commit the edits!
			editor.commit();
	    }

	    
	} // ImageListFragment

	
} // MainActivity
