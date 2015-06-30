package com.lds.socialphoto;

import static com.lds.socialphoto.Constants.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class PhotosDownloader {    
    private Context mContext;

    public PhotosDownloader(Context context) {
        mContext = context;
    }

    public void startDownload( Intent intent ) {
        String photoUrlStr = intent.getStringExtra(PhotoDownloadService.DOWNLOAD_FROM_URL);
        PhotosWebServiceHandler.PhotoDetails photoDetailData = new PhotosWebServiceHandler.PhotoDetails();
        photoDetailData.id = intent.getStringExtra(PHOTODETAILS_ID);
        photoDetailData.tags = intent.getStringExtra(PHOTODETAILS_TAG);
        photoDetailData.title = intent.getStringExtra(PHOTODETAILS_TITLE);
        photoDetailData.description = intent.getStringExtra(PHOTODETAILS_DSC);
        photoDetailData.largeUrl = intent.getStringExtra(PHOTODETAILS_LARGE_URL);
        photoDetailData.thumbnailUrl = intent.getStringExtra(PHOTODETAILS_THUMB_URL);
        photoDetailData.searchKeyword = intent.getStringExtra(PHOTODETAILS_KEYWORD);
        photoDetailData.latitude = intent.getDoubleExtra(PHOTODETAILS_LATITUDE, 0);
        photoDetailData.longitude = intent.getDoubleExtra(PHOTODETAILS_LONGITUDE, 0);
        final boolean isFirstPhoto = intent.getExtras().getBoolean(PHOTO_SEARCH_FIRST_PHOTO);
        final boolean isLastPhotoInPage = intent.getExtras().getBoolean(PHOTO_SEARCH_LAST_PHOTO_INPAGE);
        final boolean isLastPage = intent.getExtras().getBoolean(PHOTO_SEARCH_LAST_PAGE);

        URL photoUrl;
        try {
                photoUrl = new URL(photoUrlStr);
                HttpURLConnection urlConnection = (HttpURLConnection) photoUrl.openConnection();                                             
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
            if (e.getMessage() != null) { Log.e(TAG, e.getMessage()); } 
        }

        Uri uri = saveToDataBase(photoDetailData);        
        if (isFirstPhoto || isLastPhotoInPage) {             
            Message retMsg = Message.obtain();            
            retMsg.what = isFirstPhoto ? REQUEST_STATUS_FIRST_PHOTO_LOADED :
                (isLastPage ? REQUEST_STATUS_ALL_PAGES_LOADED : REQUEST_STATUS_ONE_PAGE_LOADED);
            if (!uri.toString().isEmpty()) { retMsg.arg1 = RES_OK; } else { retMsg.arg1 = RES_NO_FOUND; }
            Messenger messenger = (Messenger) intent.getExtras().get(PhotoDownloadService.MESSENGER);
            try { messenger.send(retMsg); } 
            catch (RemoteException e) { 
                if (e.getMessage() != null) { Log.e(TAG, e.getMessage()); } 
            }
        }
    }

   private Uri saveToDataBase(PhotosWebServiceHandler.PhotoDetails photoData) {
       ContentValues values = new ContentValues();
       values.put(PHOTO_ID, photoData.id);
       values.put(TITLE, photoData.title);                  
       values.put(THUMBNAIL_URL, photoData.thumbnailUrl);
       values.put(THUMBNAIL, photoData.thumbnailImage);
       values.put(KEYWORD, photoData.searchKeyword);
       values.put(DESCRIPTION, photoData.description);
       values.put(LARGE_URL, photoData.largeUrl);
       values.put(LATITUDE,  photoData.latitude);
       values.put(LONGITUDE, photoData.longitude);                  
       values.put(TAGS, photoData.tags);
       return mContext.getContentResolver().insert(CONTENT_URI, values); 
   }
}
