package com.lds.socialphoto;

import static com.lds.socialphoto.Constants.TAG;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import android.os.Process;
import android.util.Log;
import android.content.Intent;

public class PhotoWebService extends PhotosConcurrencyBaseService {

	private PhotosWebServiceHandler mWebServiceHandler;
    public static final int MAX_CONCURRENT_DOWNLOADS = 10;
    public static final Executor WEBSERVICE_THREAD_POOL =
            Executors.newFixedThreadPool(
                MAX_CONCURRENT_DOWNLOADS, new ThreadFactory(){
                	int counter = 0;
                    @Override
                    public Thread newThread(Runnable r) {
                    	counter++;
                        Thread t = new Thread(r);
                        Process.setThreadPriority(
                                Process.THREAD_PRIORITY_BACKGROUND);
                        t.setName("photos_webservice"+counter);
                        return t;
                    }
                });

	public PhotoWebService() {
		super(WEBSERVICE_THREAD_POOL);
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		mWebServiceHandler = new PhotosWebServiceHandler(this);
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		String action = intent.getStringExtra("action");
		if (action.equals("fetchTotal")) {
			mWebServiceHandler.getTotalPhotos(intent);
		} else if (action.equals("search")) {
			mWebServiceHandler.fetchPhotoJson(intent);
		}
	}	    
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		this.stopService(new Intent(this, PhotoDownloadService.class));
		Log.i(TAG, "PhotoWebService destroyed");
	}
}
