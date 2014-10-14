package com.lds.socialphoto;

import static android.provider.BaseColumns._ID;
import static com.lds.socialphoto.Constants.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class PhotoActivity extends Activity {
	
	private Uri mPhotoUri;
	private TextView mTitle;
	private TextView mDescription;
	private TextView mTag;
	private TextView mLatitude;
	private TextView mLongitude;
	private Bitmap mBitmapImg = null;
	private ImageView mPhotoImage;
	private ProgressDialog mProgressDialog = null;
	private ImageButton mShareButton = null;
	private String mLargeUrl = PHOTO_EMPTY;
	
	public PhotoActivity() {
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);  
		setContentView(R.layout.activity_photo);
			
		Bundle extras = getIntent().getExtras();
		
		// check from the saved Instance
		mPhotoUri = (savedInstanceState == null) ? null : (Uri) savedInstanceState
	        .getParcelable("photoUri");

	    // Or passed from the other activity
	    if (extras != null) {
	    	mPhotoUri = extras.getParcelable("photoUri");
	    }
		
		mTitle = (TextView) findViewById(R.id.title);
		mDescription = (TextView) findViewById(R.id.description);
		mTag = (TextView) findViewById(R.id.tag);
		mLatitude = (TextView) findViewById(R.id.latitude);
		mLongitude = (TextView) findViewById(R.id.longitude);
		
		mShareButton = (ImageButton)findViewById(R.id.button_share);
		mShareButton.setOnClickListener(new View.OnClickListener() {
	    	public void onClick(View v) {
	    		onShareButtonSelected(v);
	    	}
	    });
		mProgressDialog = new ProgressDialog( this );
		mProgressDialog.setMessage( "Loading..." );
		mProgressDialog.setCancelable(false);
		mProgressDialog.setIndeterminate(true);

		updateUI();
		
	}

	protected void onShareButtonSelected(View v) {

		Intent shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND);
		shareIntent.putExtra(Intent.EXTRA_TEXT, mLargeUrl);
		shareIntent.setType("text/plain");
		
		this.startActivity(Intent.createChooser(shareIntent, "Share To"));
		
	}

	private void updateUI() {

		String[] projection = { _ID, TITLE, DESCRIPTION, TAGS, LATITUDE, LONGITUDE, LARGE_URL };
		
	    Cursor cursor = getContentResolver().query(mPhotoUri, projection, null, null,
	        null);
	    if (cursor != null) {
	      cursor.moveToFirst();
	      String title = cursor.getString(cursor.getColumnIndexOrThrow(TITLE));
	      if ( !title.isEmpty() ) {
	    	  mTitle.setText( getResources().getString(R.string.photo_title) + ": " + title);
	      } else {
	    	  mTitle.setVisibility(View.GONE);
	      }
	      
	      String description = cursor.getString(cursor.getColumnIndexOrThrow(DESCRIPTION));
	      if ( !description.isEmpty() ) {
	    	  mDescription.setText( getResources().getString(R.string.photo_description) + ": " + description);
	      } else {
	    	  mDescription.setVisibility(View.GONE);
	      }
	      
	      String tags = cursor.getString(cursor.getColumnIndexOrThrow(TAGS));
	      if ( !tags.isEmpty() ) {
	    	  mTag.setText( getResources().getString(R.string.photo_tags) + ": " + tags);
	      } else {
	    	  mTag.setVisibility(View.GONE);
	      }

	      double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(LATITUDE));
	      if ( latitude == 0.0 ){
	    	  mLatitude.setVisibility(View.GONE);
	      } else {
	    	  mLatitude.setText( getResources().getString(R.string.photo_latitude) + ": " + latitude);
	      }
	      
	      double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(LONGITUDE));
	      if ( longitude == 0.0 ) {
	    	  mLongitude.setVisibility(View.GONE);
	      } else {
	    	  mLongitude.setText( getResources().getString(R.string.photo_longitude) + ": " + longitude);
	      }
	      
	      mLargeUrl = cursor.getString(cursor.getColumnIndexOrThrow(LARGE_URL));

	      mPhotoImage = (ImageView)findViewById(R.id.imageView1);
	      new LoadImage().execute(mLargeUrl);

			
	      // always close the cursor
	      cursor.close();
	    }
		
	}

	public class LoadImage extends AsyncTask<String, Void, Void> {

		@Override
		protected void onPreExecute() {
			if (mProgressDialog != null)
                mProgressDialog.show();
		}
		
        @Override
        protected Void doInBackground(String... urlStrings) {
            try {
            	URL url = new URL( urlStrings[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();   
                conn.setDoInput(true);   
                conn.connect();     
                InputStream is = conn.getInputStream();
                mBitmapImg = BitmapFactory.decodeStream(is); 
            }
            catch (IOException e)
            {       
                e.printStackTrace();  
            }

            return null;   
        }
            
        @Override       
        protected void onPostExecute(Void result) {
            if (mProgressDialog!=null) {
            	mProgressDialog.hide();
            }
            mPhotoImage.setImageBitmap(mBitmapImg);        	
        }
	}
	
	@Override
	protected void onRestart() 
	{
		super.onRestart();
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if ( mProgressDialog != null ) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
	}
	
	
}
