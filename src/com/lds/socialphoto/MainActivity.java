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
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
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
        ActionBar.OnNavigationListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    public static final String PREFS_NAME = "SocialPhotoPrefFile";
    public static final String PREFS_DROPLISTITEM = "dropListItem";
    public static final String PREFS_DROPLISTITEM_COUNT = "dropListItemCount";
    public static final String SAVE_KEY_DROPLIST = "droplist";
    public static final String TAG_DEBUG = "SocialPhoto: ";
    public static final boolean DEBUG_ENABLE = false;

    /**
     * The serialization (saved instance state) Bundle key representing the
     * current drop down position.
     */
    private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
    private static ArrayAdapter<String> mSpinnerAdapter;
    private static List<String> sItemList;
    private LocationClient mLocationClient = null;
    private AlertDialog mInfoDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up the action bar to show a drop down list.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        if (savedInstanceState != null && savedInstanceState.containsKey(SAVE_KEY_DROPLIST)) {
            sItemList = savedInstanceState.getStringArrayList(SAVE_KEY_DROPLIST);
        } else if (savedInstanceState == null) {
            sItemList = new ArrayList<String>(Arrays.asList(new String[] { getString(R.string.title_section1) }));

            // Restore preferences
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            int count = settings.getInt(PREFS_DROPLISTITEM_COUNT, 0);
            for (int ii = 1; ii <= count; ++ii) {
                String item = settings.getString(PREFS_DROPLISTITEM + ii, PHOTO_EMPTY );
                sItemList.add(item);
              }
        }

        // Set up the drop down list navigation in the action bar.
        mSpinnerAdapter = new ArrayAdapter<String>(actionBar.getThemedContext(),
                                                   android.R.layout.simple_list_item_1,
                                                   android.R.id.text1);
        mSpinnerAdapter.addAll(sItemList);
        actionBar.setListNavigationCallbacks(mSpinnerAdapter, this);

        mLocationClient = new LocationClient(this, this, this);
        mInfoDialog = new AlertDialog.Builder(this).create();
    }

    @Override
    public void onResume() { super.onResume(); }

    @Override
    public void onPause() { 
        super.onPause();
        if (isFinishing()) {
            if ( mLocationClient.isConnected() || mLocationClient.isConnecting() ) {
                mLocationClient.disconnect();
            }
            if ( mInfoDialog != null ) {
                mInfoDialog.dismiss();
                mInfoDialog = null;
            }
        }
    }

    @Override
    protected void onDestroy() {
    	super.onDestroy();
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Restore the previously serialized current drop down position.
        if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
            getSupportActionBar().setSelectedNavigationItem(
                    savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
        }
        if (savedInstanceState.containsKey(SAVE_KEY_DROPLIST)) {
            sItemList = savedInstanceState.getStringArrayList(SAVE_KEY_DROPLIST);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Serialize the current drop down position.
        outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getSupportActionBar()
                .getSelectedNavigationIndex());
        outState.putStringArrayList(SAVE_KEY_DROPLIST, (ArrayList<String>)sItemList);
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
        getSupportFragmentManager().beginTransaction().replace(R.id.container,
                ImageListFragment.newInstance(position + 1)).commit();
        return true;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            showMessage("Location connection was failed, please try again.");
        } else { showMessage("Location connection was failed."); }
    }

    @Override
    public void onConnected(Bundle arg0) {
        if (DEBUG_ENABLE) {
            Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDisconnected() {
        if (DEBUG_ENABLE) {
            Toast.makeText(this, "Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();
        }
    }

    private void connectLocationService() {
        mLocationClient.connect();
    }

    private Location getCurrentLocation() {
        return mLocationClient.getLastLocation();
    }

    private void resetActionBar() {
        sItemList.clear();
        sItemList.add(getString(R.string.title_section1));
        mSpinnerAdapter.clear();
        mSpinnerAdapter.addAll(sItemList);
    }

    private void showMessage(String messsage ) {
        mInfoDialog.setMessage( messsage );
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                if (mInfoDialog != null) { mInfoDialog.show(); }
            }

            @Override
            protected Void doInBackground(Void... arg0) {
                try { Thread.sleep( 3000 ); }
                catch (InterruptedException e) {
                    if (e.getMessage() != null) { Log.e(TAG, e.getMessage()); }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                try { mInfoDialog.dismiss();} 
                catch (IllegalArgumentException e) { 
                    if (e.getMessage() != null) { Log.e(TAG, e.getMessage()); }
                }
            }
        };
        task.execute((Void[])null);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class ImageListFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

        public static enum SearchState {
            SEARCH_NOT_START,
            SEARCH_START,
            SEARCH_IN_PROCESSING,
            SEARCH_FAILED,
            SEARCH_CANCELLED,
            SEARCH_STOPPED,
            SEARCH_COMPLETE
        }

        private static SearchState sSearchState = SearchState.SEARCH_NOT_START;
        private static final String ARG_SECTION_NUMBER = "section_number";
        private static String[] FROM = { TITLE, THUMBNAIL };
        private static int[] TO = { R.id.photoname, R.id.thumbnail };
        private SimpleCursorAdapter mAdapter;

        private static String sSearchString = PHOTO_EMPTY;
        private static String sSavedSearchKeyword = PHOTO_EMPTY;   
        private int mdropdownItemIndex = 0;

        private ImageButton mSearchButton;
        private ProgressDialog mProgressDialog = null;
        private AlertDialog.Builder mSettingDialog = null;
        private ArrayList<Integer> mSelectedItems = new ArrayList<Integer>();
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

        public ImageListFragment() {}

        /**
         * Implementation of override virtual functions
         */
        @Override
        public void onAttach(Activity activity) { super.onAttach(activity); }

        @Override
        public void onResume() {
            super.onResume();
            ((UIHandler) sMainActHandler).attach(this, mProgressDialog);
            PhotoWebService.configureExecutorService(false);
        }

        @Override 
        public void onPause() {
            super.onPause();
            ((UIHandler) sMainActHandler).detach();
            if (getActivity().isFinishing()) {
            	PhotoWebService.configureExecutorService(true);
                getActivity().stopService(new Intent(getActivity(), PhotoWebService.class));
                sSearchState = SearchState.SEARCH_STOPPED;
                sSearchString = PHOTO_EMPTY;
            }
        }

        @Override
        public void onDetach() { super.onDetach(); }

        @Override
         public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
              Bundle bundle = this.getArguments();
              // start from 1
              mdropdownItemIndex = bundle.getInt(ARG_SECTION_NUMBER);
              sSavedSearchKeyword = PHOTO_EMPTY;
              if ( mdropdownItemIndex > 1 ) {
                  sSavedSearchKeyword = (String)mSpinnerAdapter.getItem(mdropdownItemIndex-1);
                  sSavedSearchKeyword = parseSearchKeyword(sSavedSearchKeyword);
              }
              if ( getActivity() != null ) {
                  buildDialogs();
                  getPhotos();
              }
         }

        @Override
        public void onDestroyView () {
            super.onDestroyView();
            if ( mProgressDialog != null ) {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }
        }

        @Override
        public void onDestroy() {
        	super.onDestroy();
        }
        
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            final EditText searchBox = (EditText) rootView.findViewById(R.id.searchBox);
            if ( mdropdownItemIndex > 1 ) { searchBox.setVisibility(View.GONE);}

            searchBox.addTextChangedListener( new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void afterTextChanged(Editable s) { sSearchString = s.toString().trim(); }
            });

            ImageButton settingButton = (ImageButton) rootView.findViewById( R.id.searchSettings );
            if ( mdropdownItemIndex > 1 ) {
                settingButton.setVisibility(View.GONE);
            } else {
                settingButton.setOnClickListener( new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { if ( mSettingDialog != null ) { mSettingDialog.show(); }}
                });
            }
              
            mSearchButton = (ImageButton) rootView.findViewById( R.id.searchPhoto );
            if ( mdropdownItemIndex > 1 ) {
                mSearchButton.setVisibility(View.GONE);
            } else {
                mSearchButton.setOnClickListener( new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (openSearch()) { ((ImageButton)v).setImageResource(R.drawable.search_stop);}
                            else { ((ImageButton)v).setImageResource(android.R.drawable.ic_menu_search); }
                    }
                });
                ((MainActivity)getActivity()).connectLocationService();
            }
            return rootView;
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            if ( getActivity() == null ) { return; }
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
            if ( mdropdownItemIndex == 1 ) {
                String[] selectionArgs = { sSearchString};
                cursorLoader = new CursorLoader( getActivity(), 
                    CONTENT_URI, projection, selection, selectionArgs, null );
            }
            else {
                String[] selectionArgs = { sSavedSearchKeyword };
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

        /**
        * Implementation of private functions
        */
        private void buildDialogs() {
            mProgressDialog = new ProgressDialog(getActivity());
              mProgressDialog.setMessage(getResources().getString(R.string.searchProgressText));
              mProgressDialog.setCancelable(false);
              mProgressDialog.setIndeterminate(true);
              mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                      if ( getActivity() != null ) {
                        getActivity().stopService( new Intent( getActivity(), PhotoWebService.class));
                        sSearchState = SearchState.SEARCH_CANCELLED;
                        resetSearchButtonImage();
                    }
                  }
              });

              boolean[] checkedItems = { true, true, true, false };
              mSelectedItems.add(0);
              mSelectedItems.add(1);
              mSelectedItems.add(2);
              mSelectedItems.add(3);
              mSettingDialog = new AlertDialog.Builder( getActivity() );
              mSettingDialog.setMultiChoiceItems(R.array.setting_array, checkedItems, 
                      new DialogInterface.OnMultiChoiceClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                      if (isChecked) { mSelectedItems.add(which); } 
                      else if (mSelectedItems.contains(which)) { mSelectedItems.remove(Integer.valueOf(which)); }
                  }
              })
              .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int id) {
                       if (mSelectedItems.get(3) == 3) {
                           clearNavigationDropListItem();                           
                       }
                   }
               })
               .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int id) {}
               });
              mSettingDialog.create();
        }

        private void resetSearchButtonImage() {
            mSearchButton.setImageResource(android.R.drawable.ic_menu_search);
        }

        private String parseSearchKeyword(final String searchString) {
            String result = searchString;
            int startPos = result.indexOf("-(");
              int endPos = result.indexOf(")");
              if (startPos > 0 && endPos > startPos && endPos == result.length()-1) {
                  boolean isAllDigit = true;
                  for(int ii=startPos+2; ii < endPos; ii++) {
                      char c = result.charAt(ii); 
                       if (!(c >= '0' && c <= '9')) { isAllDigit = false; break; }
                  }
                  if (isAllDigit) { return result.substring(0, startPos); }
              }              
              return result;
        }

        final private static Handler sMainActHandler = new UIHandler();
        final static Messenger sMessenger = new Messenger(sMainActHandler);
        private static class UIHandler extends Handler {
            ProgressDialog progressDialog;
            Context context;
            ImageListFragment fragment;
            int totalPages = 2;
            int pageIndex = 1;
            String methodUrl ="";
            boolean isKeyWordSaved = false;

            public void attach(ImageListFragment listFragment, ProgressDialog progressDlg) {
                this.fragment = listFragment;
                this.context = listFragment.getActivity();
                this.progressDialog = progressDlg;
            }

            public void detach() {
                this.fragment = null;
                this.context = null;
                this.progressDialog = null;
            }

            private void handleSearchNotFound() {
                sSearchState = SearchState.SEARCH_FAILED;
                ((MainActivity)fragment.getActivity()).showMessage(
                        "Sorry, no matched result was found, please check the internet connenction.");
            }

            private void saveSearchKeyword() {
                if (!isKeyWordSaved) {
                    isKeyWordSaved = true;
                    int counter = 0;
                    for(String item: sItemList) {
                        String keyword = fragment.parseSearchKeyword(item);
                        if (keyword.equals(sSearchString)) { counter++; }
                    }
                    if (counter == 0) { sItemList.add(sSearchString); }
                        else if (counter > 0) { sItemList.add(sSearchString + "-(" + counter + ")"); }
                    mSpinnerAdapter.notifyDataSetChanged();
                    fragment.saveNavigationDropListItem();
                }
            }

            @SuppressLint("NewApi")
            public void handleMessage( Message msg ) {
                super.handleMessage( msg );
                switch ( msg.what ) {
                    case REQUEST_STATUS_TOTAL_PHOTOS:
                        totalPages = msg.arg2;
                        if (totalPages == 0) {
                            if (progressDialog != null) { progressDialog.hide(); }
                            handleSearchNotFound();
                            break;
                        }
                        if (sSearchState == SearchState.SEARCH_CANCELLED 
                                || sSearchState == SearchState.SEARCH_STOPPED) { break; }

                        isKeyWordSaved = false;
                        pageIndex = 1;
                        methodUrl = (String)msg.obj;
                        Intent intent = new Intent( context, PhotoWebService.class );
                        intent.putExtra(PHOTO_SEARCH_ACTION, PHOTO_SEARCH);
                        intent.putExtra(PHOTO_SEARCH_PAGE_INDEX, pageIndex);
                        intent.putExtra(PHOTO_SEARCH_TOTAL_PAGES, totalPages);
                        intent.putExtra(PHOTO_SEARCH_URL, methodUrl+pageIndex);
                        intent.putExtra( PHOTO_MESSENGER, sMessenger );
                        context.startService(intent);
                        if (fragment != null) { fragment.restartLoader(); }
                        break;
                    case REQUEST_STATUS_FIRST_PHOTO_LOADED:
                        if (progressDialog != null) { progressDialog.hide(); }
                        saveSearchKeyword();
                        break;
                    case REQUEST_STATUS_ONE_PAGE_LOADED:
                        if (sSearchState == SearchState.SEARCH_CANCELLED 
                            || sSearchState == SearchState.SEARCH_STOPPED) { break; }

                        if (msg.arg1 == RES_OK) {                            
                            sSearchState = SearchState.SEARCH_IN_PROCESSING;
                            if (DEBUG_ENABLE) {
                                if (context != null) {
                                    Toast.makeText(
                                            context, 
                                            "Page" +  Integer.toString(pageIndex) + " was loaded", 
                                            Toast.LENGTH_LONG ).show();
                                }
                            }
                        } else if (msg.arg1 == RES_NO_FOUND) { handleSearchNotFound(); }
                        
                        if ( sSearchState == SearchState.SEARCH_IN_PROCESSING 
                                && ++pageIndex <= totalPages ) {
                            intent = new Intent(context, PhotoWebService.class);
                            intent.putExtra(PHOTO_SEARCH_ACTION, PHOTO_SEARCH);
                            intent.putExtra(PHOTO_SEARCH_PAGE_INDEX, pageIndex);
                            intent.putExtra(PHOTO_SEARCH_TOTAL_PAGES, totalPages);
                            intent.putExtra(PHOTO_SEARCH_URL, methodUrl+pageIndex);
                            intent.putExtra(PHOTO_MESSENGER, sMessenger);
                            context.startService(intent);
                        }
                        break;
                    case REQUEST_STATUS_ALL_PAGES_LOADED: 
                        if (msg.arg1 == RES_OK) {
                            sSearchState = SearchState.SEARCH_COMPLETE;
                            if (DEBUG_ENABLE) {
                                if (context != null) {
                                    Toast.makeText(context, "All pages have been loaded", Toast.LENGTH_LONG ).show();
                                }
                            }
                        } else if (msg.arg1 == RES_NO_FOUND ) { handleSearchNotFound(); }                        
                        if (context != null) { context.stopService( new Intent(context, PhotoWebService.class)); }
                        fragment.resetSearchButtonImage();
                        break;
                    default:
                        break;
                }
            }
        };

        private boolean openSearch() {
            if (sSearchString.isEmpty() || getActivity() == null) { return false; }            
            if ( sSearchState  == SearchState.SEARCH_START 
                    || sSearchState == SearchState.SEARCH_IN_PROCESSING ) {
                getActivity().stopService( new Intent( getActivity(), PhotoWebService.class));
                sSearchState = SearchState.SEARCH_STOPPED;
                return false;
            }

            double longitude = 0.0;
            double latitude = 0.0;
            boolean locationEnabled = false;
            mCurrentLocation = ((MainActivity)getActivity()).getCurrentLocation();
            if (mCurrentLocation != null) {
                longitude = mCurrentLocation.getLongitude();
                latitude = mCurrentLocation.getLatitude();
                locationEnabled = true;
            }

            Intent intent = new Intent( getActivity(), PhotoWebService.class );
            intent.putExtra("action", "fetchTotal");
            intent.putExtra( PHOTO_SEARCH_KEYWORD, sSearchString );
            intent.putExtra("totalPage", 5);
            intent.putExtra( PHOTO_MESSENGER, sMessenger );
            if (locationEnabled) {
                intent.putExtra( PHOTO_LOCATION_ENABLED, true );
                intent.putExtra( LATITUDE, latitude );
                intent.putExtra( LONGITUDE, longitude );
            } else {
                intent.putExtra( PHOTO_LOCATION_ENABLED, false );
            }
            intent.putIntegerArrayListExtra( PHOTO_SEARCH_RULE, mSelectedItems );    
            getActivity().startService( intent );
            sSearchState = SearchState.SEARCH_START;                     
            if (mProgressDialog != null) { mProgressDialog.show(); }

            return true;
        }

        private void restartLoader() {
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
                            ((ImageView)view).setImageBitmap(BitmapFactory.decodeByteArray(byteImage, 0, length));
                        }
                        return true; //true because the data was bound to the view
                    }
                    return false;
                }
            });
            setListAdapter( mAdapter );
        }

        private void saveNavigationDropListItem() {
            if (getActivity() == null) { return; }
            SharedPreferences settings = getActivity().getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            int count = sItemList.size();
            Log.d(TAG_DEBUG, "SharePreference item count : " + count );
            editor.putInt(PREFS_DROPLISTITEM_COUNT, count - 1 );
            // the first item is "New search" not need to store
            for (int ii = 1; ii < count; ++ii) {
                editor.putString(PREFS_DROPLISTITEM + ii, sItemList.get(ii).toString());
                Log.d(TAG_DEBUG, "SharePreference item " + ii + " : " + sItemList.get(ii).toString());
            }
            editor.commit();
        }

        private void clearNavigationDropListItem() {
            if (getActivity() == null) { return; }
            SharedPreferences settings = getActivity().getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.clear();
            editor.commit();
            ((MainActivity) getActivity()).resetActionBar();
            getActivity().getContentResolver().delete(CONTENT_URI, "", null);
        }
        
    } // ImageListFragment
} // MainActivity
