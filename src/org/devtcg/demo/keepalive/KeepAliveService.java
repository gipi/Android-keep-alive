package org.devtcg.demo.keepalive;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
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
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class KeepAliveService extends Service
{
	public static final String TAG = "KeepAliveService";

	private static final String HOST = "jasta.dyndns.org";
	private static final int PORT = 50000;

	private static final String ACTION_START = "org.devtcg.demo.keepalive.START";
	private static final String ACTION_STOP = "org.devtcg.demo.keepalive.STOP";
	private static final String ACTION_KEEPALIVE = "org.devtcg.demo.keepalive.KEEP_ALIVE";

	private ConnectionThread mConnection;
	
	private static final long KEEP_ALIVE_INTERVAL = 1000 * 60 * 28;
	
	private static final long INITIAL_RETRY_INTERVAL = 1000 * 5;
	private static final long MAXIMUM_RETRY_INTERVAL = 1000 * 60 * 2;
	private long mStartTime;

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

		if (intent.getAction().equals(ACTION_START) == true)
		{
			start();
		}
		else if (intent.getAction().equals(ACTION_STOP) == true)
		{
			stop();
			stopSelf();
		}
	}
	
	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}

	public synchronized void start()
	{
		if (mConnection != null)
		{
			Log.w(TAG, "Attempt to start connection that is already active");
			return;
		}
		
		mStartTime = System.currentTimeMillis();
		mConnection = new ConnectionThread(HOST, PORT);
		mConnection.start();

		IntentFilter f = new IntentFilter();
		f.addAction(ACTION_KEEPALIVE);
		registerReceiver(mKeepAliveReceiver, f);

		Intent i = new Intent();
		i.setAction(ACTION_KEEPALIVE);
		PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager)getSystemService(ALARM_SERVICE);
		alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP,
		  mStartTime + KEEP_ALIVE_INTERVAL, KEEP_ALIVE_INTERVAL, pi);
	}

	public synchronized void stop()
	{
		if (mConnection == null)
		{
			Log.w(TAG, "Attempt to stop connection not active.");
			return;
		}

		Intent i = new Intent();
		i.setAction(ACTION_KEEPALIVE);
		PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager)getSystemService(ALARM_SERVICE);
		alarmMgr.cancel(pi);

		unregisterReceiver(mKeepAliveReceiver);

		mConnection.abort();
		mConnection = null;
	}
	
	public void reschedule()
	{
		long interval =
		  mPrefs.getLong("retryInterval", INITIAL_RETRY_INTERVAL);

		long now = System.currentTimeMillis();
		long elapsed = now - mStartTime;

		if (elapsed < interval)
			interval = Math.min(interval * 10, MAXIMUM_RETRY_INTERVAL);
		else
			interval = INITIAL_RETRY_INTERVAL;

		Log.i(TAG, "Waiting " + interval + "ms before retrying connection...");

		mPrefs.edit().putLong("retryInterval", interval).commit();

		AlarmManager alarmMgr = (AlarmManager)getSystemService(ALARM_SERVICE);

		Intent i = new Intent();
		i.setClass(this, KeepAliveService.class);
		i.setAction(ACTION_START);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);

		alarmMgr.set(AlarmManager.RTC_WAKEUP, now + interval, pi);
	}

	private BroadcastReceiver mKeepAliveReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			Log.i(TAG, "Hello.");

			synchronized(KeepAliveService.this) {
				try {
					if (mConnection != null)
						mConnection.sendKeepAlive();
				} catch (IOException e) {}
			}
		}
	};

	private class ConnectionThread extends Thread
	{
		private Socket mSocket;
		private final String mHost;
		private final int mPort;

		public ConnectionThread(String host, int port)
		{
			mHost = host;
			mPort = port;
		}

		public void run()
		{
			boolean shutdown = false;
			
			try {
				Log.i(TAG, "Retrying connection...");

				synchronized(this) {
					mSocket = new Socket();
				}

				mSocket.connect(new InetSocketAddress(mHost, mPort), 20000);

				Log.i(TAG, "Established.");

				InputStream in = mSocket.getInputStream();
				OutputStream out = mSocket.getOutputStream();

				byte[] b = new byte[1024];
				int n;

				while ((n = in.read(b)) >= 0)
					out.write(b, 0, n);

				Log.e(TAG, "Server closed connection!");
			} catch (IOException e) {
				Log.e(TAG, "Exception occurred", e);
			}
			
			synchronized(this) {
				if (mSocket.isClosed() == true)
					shutdown = true;
				else
				{
					try {
						mSocket.close();
					} catch (IOException e) {}
				}

				mSocket = null;
			}

			if (shutdown == true)
				Log.i(TAG, "Shutting down...");
			else
			{
				synchronized(KeepAliveService.this) {
					mConnection = null;
				}

				reschedule();
				stop();
				stopSelf();
			}
		}

		public void sendKeepAlive()
		  throws IOException
		{
			Socket s;
			
			synchronized(this) {
				s = mSocket;
			}

			Date d = new Date();
			s.getOutputStream().write((d.toString() + "\n").getBytes());
		}

		public void abort()
		{
			Log.i(TAG, "Abort requested!");

			interrupt();

			synchronized(this) {
				try {
					if (mSocket != null)
						mSocket.close();
				} catch (IOException e) {}
			}
		}
	}
}
