package org.devtcg.demo.keepalive;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.util.Log;

public class KeepAliveService extends Service
{
	public static final String TAG = "KeepAliveService";

	private static final String HOST = "jasta.dyndns.org";
	private static final int PORT = 50000;

	private static final String ACTION_START = "org.devtcg.demo.keepalive.START";
	private static final String ACTION_STOP = "org.devtcg.demo.keepalive.STOP";
	private static final String ACTION_KEEPALIVE = "org.devtcg.demo.keepalive.KEEP_ALIVE";
	private static final String ACTION_RECONNECT = "org.devtcg.demo.keepalive.RECONNECT";

	private ConnectivityManager mConnMan;

	private boolean mStarted;
	private ConnectionThread mConnection;

	private static final long KEEP_ALIVE_INTERVAL = 1000 * 60 * 28;
	
	private static final long INITIAL_RETRY_INTERVAL = 1000 * 5;
	private static final long MAXIMUM_RETRY_INTERVAL = 1000 * 60 * 2;

	private SharedPreferences mPrefs;

	public static void actionStart(Context ctx)
	{
		Intent i = new Intent(ctx, KeepAliveService.class);
		i.setAction(ACTION_START);
		ctx.startService(i);
	}

	public static void actionStop(Context ctx)
	{
		Intent i = new Intent(ctx, KeepAliveService.class);
		i.setAction(ACTION_STOP);
		ctx.startService(i);
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		mPrefs = getSharedPreferences(TAG, MODE_PRIVATE);

		mConnMan =
		  (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
	}

	@Override
	public void onDestroy()
	{
		stop();
	}

	@Override
	public void onStart(Intent intent, int startId)
	{
		super.onStart(intent, startId);

		if (intent.getAction().equals(ACTION_STOP) == true)
		{
			stop();
			stopSelf();
		}
		else if (intent.getAction().equals(ACTION_START) == true)
			start();
		else if (intent.getAction().equals(ACTION_KEEPALIVE) == true)
			keepAlive();
		else if (intent.getAction().equals(ACTION_RECONNECT) == true)
			reconnectIfNecessary();
	}
	
	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}

	private synchronized void start()
	{
		if (mStarted == true)
		{
			Log.w(TAG, "Attempt to start connection that is already active");
			return;
		}

		mStarted = true;

		registerReceiver(mConnectivityChanged,
		  new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

		mConnection = new ConnectionThread(HOST, PORT);
		mConnection.start();
	}

	private synchronized void stop()
	{
		if (mStarted == false)
		{
			Log.w(TAG, "Attempt to stop connection not active.");
			return;
		}

		mStarted = false;
		
		unregisterReceiver(mConnectivityChanged);

		if (mConnection != null)
		{
			mConnection.abort();
			mConnection = null;
		}
	}

	private synchronized void keepAlive()
	{
		Log.i(TAG, "Hello.");

		try {
			if (mConnection != null)
				mConnection.sendKeepAlive();
		} catch (IOException e) {}
	}

	private void startKeepAlives()
	{
		Intent i = new Intent();
		i.setClass(this, KeepAliveService.class);
		i.setAction(ACTION_KEEPALIVE);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager)getSystemService(ALARM_SERVICE);
		alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP,
		  System.currentTimeMillis() + KEEP_ALIVE_INTERVAL,
		  KEEP_ALIVE_INTERVAL, pi);
	}

	private void stopKeepAlives()
	{
		Intent i = new Intent();
		i.setClass(this, KeepAliveService.class);
		i.setAction(ACTION_KEEPALIVE);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager)getSystemService(ALARM_SERVICE);
		alarmMgr.cancel(pi);
	}

	public void scheduleReconnect(long startTime)
	{
		long interval =
		  mPrefs.getLong("retryInterval", INITIAL_RETRY_INTERVAL);

		long now = System.currentTimeMillis();
		long elapsed = now - startTime;

		if (elapsed < interval)
			interval = Math.min(interval * 10, MAXIMUM_RETRY_INTERVAL);
		else
			interval = INITIAL_RETRY_INTERVAL;

		Log.i(TAG, "Waiting " + interval + "ms before retrying connection...");

		mPrefs.edit().putLong("retryInterval", interval).commit();

		Intent i = new Intent();
		i.setClass(this, KeepAliveService.class);
		i.setAction(ACTION_RECONNECT);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager)getSystemService(ALARM_SERVICE);
		alarmMgr.set(AlarmManager.RTC_WAKEUP, now + interval, pi);
	}

	private synchronized void reconnectIfNecessary()
	{
		if (mStarted == true && mConnection == null)
		{
			mConnection = new ConnectionThread(HOST, PORT);
			mConnection.start();			
		}
	}

	private BroadcastReceiver mConnectivityChanged = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			NetworkInfo info = (NetworkInfo)intent.getParcelableExtra
			  (ConnectivityManager.EXTRA_NETWORK_INFO);

			Log.v(TAG, "ConnectivityChanged: info=" + info);

			if (info != null && info.isConnected() == true)
				reconnectIfNecessary();
		}
	};

	private class ConnectionThread extends Thread
	{
		private final Socket mSocket;
		private final String mHost;
		private final int mPort;

		public ConnectionThread(String host, int port)
		{
			mHost = host;
			mPort = port;
			mSocket = new Socket();
		}

		public boolean isConnected()
		{
			return mSocket.isConnected();
		}

		private boolean isNetworkAvailable()
		{
			NetworkInfo info = mConnMan.getActiveNetworkInfo();
			if (info == null)
				return false;

			return info.isConnected();
		}

		public void run()
		{
			Socket s = mSocket;
			
			long startTime = System.currentTimeMillis();
			
			try {
				Log.i(TAG, "[Re]trying connection...");

				s.connect(new InetSocketAddress(mHost, mPort), 20000);
				s.setSoTimeout((int)KEEP_ALIVE_INTERVAL + 120000);

				Log.i(TAG, "Established.");

				startKeepAlives();

				InputStream in = s.getInputStream();
				OutputStream out = s.getOutputStream();

				byte[] b = new byte[1024];
				int n;

				while ((n = in.read(b)) >= 0)
					out.write(b, 0, n);

				Log.i(TAG, "Server closed connection unexpectedly!");
			} catch (IOException e) {
				Log.e(TAG, "Exception occurred", e);
			} finally {
				stopKeepAlives();

				if (s.isClosed() == true)
					Log.i(TAG, "Shutting down...");
				else
				{
					try {
						s.close();
					} catch (IOException e) {}

					synchronized(KeepAliveService.this) {
						mConnection = null;
					}

					/* If our local interface is still up then the connection
					 * failure must have been something intermittent.  Try
					 * our connection again later (the wait grows with each
					 * successive failure).  Otherwise we will try to
					 * reconnect when the local interface comes back. */
					if (isNetworkAvailable() == true)
						scheduleReconnect(startTime);
				}
			}
		}

		public void sendKeepAlive()
		  throws IOException
		{
			Socket s = mSocket;

			Date d = new Date();
			s.getOutputStream().write((d.toString() + "\n").getBytes());
		}

		public void abort()
		{
			Log.i(TAG, "Abort requested!");

			interrupt();

			synchronized(this) {
				try {
					mSocket.close();
				} catch (IOException e) {}
			}
		}
	}
}
