/*
 * $Id: ServiceActivity.java 1042 2008-12-23 01:27:21Z jasta00 $
 *
 * Copyright (C) 2008 Josh Guilfoyle <jasta@devtcg.org>
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 */

package org.devtcg.demo.keepalive;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

/**
 * Utility class to create activities which critically depend on a service 
 * connection.
 */
public abstract class ServiceActivity<T extends IInterface> extends Activity
{
	public static final String TAG = "ServiceActivity";

	/**
	 * When connected, references the service interface as a convenience to
	 * subclasses.
	 */
	protected T mService = null;

	/**
	 * Convenience feature to allow subclasses to defer UI
	 * construction/assignment until the service is connected, and then manage
	 * hiding/showing the UI to match the service state.
	 * 
	 * @see onInitUI
	 */
	private boolean mShowingUI = false;

	@Override
	protected void onStart()
	{
		super.onStart();
		
		Log.d(TAG, "onStart(): Binding service...");
		
		if (bindService() == false)
			onServiceFatal();
	}

	@Override
	protected void onStop()
	{
		Log.d(TAG, "onStop(): Unbinding service...");
		
		if (mService != null)
			onDetached();

		showUI(false);

		unbindService();
		mService = null;

		super.onStop();
	}

	protected boolean isShown()
	{
		return mShowingUI;
	}

	protected void showUI(boolean display)
	{
		if (mShowingUI == false)
		{
			if (display == true)
			{
				onInitUI();
				mShowingUI = true;
			}
		}
		else
		{
			/* XXX: There must be a less intrusive way to figure out what
			 * the top-level content view is? */
			View v = ((ViewGroup)getWindow().getDecorView()).getChildAt(0);
			v.setVisibility(display ? View.VISIBLE : View.GONE);
		}
	}

	/**
	 * Called when the activity needs to display the UI for the first
	 * time.  Initialization is similar to {@link Activity#onCreate}.
	 */
	protected abstract void onInitUI();
	
	/**
	 * Called when the activity is attached to the service, and not necessarily
	 * the first time it connects.  You should register listeners and fit
	 * the UI to the service state here.
	 */
	protected abstract void onAttached();

	/**
	 * Called when the activity is detached from the service.  You should
	 * unregister listeners and tidy up internal state accordingly.
	 */
	protected abstract void onDetached();

	protected abstract Intent getServiceIntent();
	protected abstract T getServiceInterface(IBinder service);

	private boolean bindService()
	{
		Intent i = getServiceIntent();

		/* Start the service first to ensure that it survives after our
		 * activity unbinds (if it wants to). */
		ComponentName name = startService(i);

		if (name == null)
			return false;

		boolean bound = bindService(new Intent().setComponent(name),
		  mConnection, BIND_AUTO_CREATE);

		return bound;
	}

	private void unbindService()
	{
		unbindService(mConnection);
	}
	
	private final ServiceConnection mConnection = new ServiceConnection()
	{
		public void onServiceConnected(ComponentName name, IBinder service)
        {
			mService = getServiceInterface(service);

			showUI(true);
			onAttached();
        }

		public void onServiceDisconnected(ComponentName name)
        {
			onServiceFatal();
			mService = null;
        }
	};

	/**
	 * Fatal error attempting to either start or bind to the service specified
	 * by {@link getServiceIntent}.  Will not retry.  Default implementation is
	 * to throw up an error and finish().
	 */
	protected void onServiceFatal()
	{
		Log.e(TAG, "Unable to start service: " + getServiceIntent());

		(new AlertDialog.Builder(this))
		  .setIcon(android.R.drawable.ic_dialog_alert)
		  .setTitle("Sorry!")
		  .setMessage("Unexpected fatal error!")
		  .create().show();

		finish();
	}
}
