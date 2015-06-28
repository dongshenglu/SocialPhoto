package com.lds.socialphoto;

import static com.lds.socialphoto.Constants.*;

import java.util.concurrent.Executor;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public abstract class PhotosConcurrencyBaseService extends Service {

	public static final String REQUEST_ID = "request_id";
	private final Executor mExecutor;
	private final Handler mCompletionHandler = new completionHandler();
	private int counter;

	public PhotosConcurrencyBaseService(Executor executor) {
		this.mExecutor = executor;
	}

	protected abstract void onHandleIntent(Intent intent);

	@Override
	public int onStartCommand(final Intent intent, int flags, int startId) {
		counter++;
		mExecutor.execute( new Runnable() {
			@Override
			public void run() {
				try {
					onHandleIntent(intent);
				} finally {
					mCompletionHandler.sendMessage(Message.obtain(mCompletionHandler));
				}
				
			}
		});
		
		return START_NOT_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent intent) { return null; }

	@SuppressLint("HandlerLeak")
	private class completionHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			if (--counter == 0) {
				Log.i(TAG, "service stop");
				stopSelf();
			} else {
				Log.i(TAG, counter + " remaining tasks");
			}
		}
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		Log.i(TAG, "PhotosConcurrencyBaseService destroyed");
	}
}
