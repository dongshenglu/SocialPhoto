package com.lds.socialphoto;

import static com.lds.socialphoto.Constants.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import android.os.Messenger;
import android.os.Process;
import android.util.Log;
import android.content.Context;
import android.content.Intent;

public class PhotoDownloadService extends PhotosConcurrencyBaseService {
    private PhotosDownloader mDownloader;
    private static boolean isShutDownExecutorService = false;
    public static final int MAX_CONCURRENT_DOWNLOADS = 30;
    public static ExecutorService DOWNLOAD_THREAD_POOL = null;

    public PhotoDownloadService() {
    	if (DOWNLOAD_THREAD_POOL == null) {
	    	DOWNLOAD_THREAD_POOL = Executors.newFixedThreadPool(
	                MAX_CONCURRENT_DOWNLOADS, new ThreadFactory(){
	                    int counter = 0;
	                    @Override
	                    public Thread newThread(Runnable r) {
	                        counter++;
	                        Thread t = new Thread(r);
	                        Process.setThreadPriority(
	                                Process.THREAD_PRIORITY_BACKGROUND);
	                        t.setName("photos_download"+counter);
	                        return t;
	                    }
	                });
    	}
        preStart(DOWNLOAD_THREAD_POOL);
    }

    public static final String DOWNLOAD_FROM_URL = "from_url";
    public static final String MESSENGER = "messenger";

    public static int startDownload(String url, Context ctx, PhotosWebServiceHandler.PhotoDetails photoDetailData, 
            Messenger messenger, boolean isFirstPhoto, boolean isLastPhotoInPage, boolean isLastPage) {
        Intent intent = new Intent(ctx, PhotoDownloadService.class);
        intent.putExtra(PhotoDownloadService.DOWNLOAD_FROM_URL, url);
        intent.putExtra(PhotoDownloadService.REQUEST_ID, url.hashCode());
        intent.putExtra(PHOTODETAILS_ID, photoDetailData.id);
        intent.putExtra(PHOTODETAILS_TITLE, photoDetailData.title);
        intent.putExtra(PHOTODETAILS_TAG, photoDetailData.tags);
        intent.putExtra(PHOTODETAILS_LARGE_URL, photoDetailData.largeUrl);
        intent.putExtra(PHOTODETAILS_THUMB_URL, photoDetailData.thumbnailUrl);
        intent.putExtra(PHOTODETAILS_DSC, photoDetailData.description);
        intent.putExtra(PHOTODETAILS_KEYWORD, photoDetailData.searchKeyword);
        intent.putExtra(PHOTODETAILS_LATITUDE, photoDetailData.latitude);
        intent.putExtra(PHOTODETAILS_LONGITUDE, photoDetailData.longitude);
        intent.putExtra(PhotoDownloadService.MESSENGER, messenger);
        intent.putExtra(PHOTO_SEARCH_FIRST_PHOTO, isFirstPhoto);
        intent.putExtra(PHOTO_SEARCH_LAST_PHOTO_INPAGE, isLastPhotoInPage);
        intent.putExtra(PHOTO_SEARCH_LAST_PAGE, isLastPage);
        ctx.startService(intent);
        return url.hashCode();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mDownloader = new PhotosDownloader(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mDownloader.startDownload(intent);
    }

    @Override
    public void onDestroy() {     
        Log.i(TAG, "PhotoDownloadService destroyed");
        if (isShutDownExecutorService) {
            DOWNLOAD_THREAD_POOL.shutdown();
            if (terminateThreadPool(DOWNLOAD_THREAD_POOL)) {
            	DOWNLOAD_THREAD_POOL = null;
            }
        }
        super.onDestroy();
    }

    public static void configureExecutorService(boolean isShutdown) {
        isShutDownExecutorService = isShutdown;
    }
}
