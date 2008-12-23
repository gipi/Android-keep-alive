/*
 * $Id: ServiceActivity.java 937 2008-10-23 04:24:00Z jasta00 $
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
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

/**
 * Utility class to create activities which critically depend on a service 
 * connection.
 */
public abstract class ServiceActivity extends Activity
  implements ServiceConnection
{
	public static final String TAG = "ServiceActivity";

	/**
	 * Convenience feature to allow ServiceActivity subclasses to defer
	 * UI construction/assignment until the service is connected, and then
	 * manage hiding/showing the UI to match the service state.
	 * 
	 * @see onInitUI
	 */
	protected boolean mHasUI = false;

	@Override
	public void onStart()
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

		unbindService();
		displayUI(false);

		super.onStop();
	}
	
	protected boolean hasUI()
	{
		return mHasUI;
	}
	
	protected void displayUI(boolean display)
	{
		if (mHasUI == false)
		{
			if (display == true)
			{
				onInitUI();
				mHasUI = true;
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
	 * time.
	 */
	protected abstract void onInitUI();

	private boolean bindService()
	{
		Intent i = getServiceIntent();

		/* I don't remember why we start first, then bind.  I picked it up
		 * somewhere back in M5 days, so perhaps it no longer applies. */
		ComponentName name = startService(i);

		if (name == null)
			return false;

		boolean bound = bindService(new Intent().setComponent(name),
		  this, BIND_AUTO_CREATE);

		return bound;
	}

	private void unbindService()
	{
		unbindService(this);
	}

	protected abstract Intent getServiceIntent();

	/**
	 * Fatal error attempting to either start or bind to the service specified
	 * by {@link getServiceIntent}.  Will not retry.  Default implementation is
	 * to throw up an error and finish().
	 */
	protected void onServiceFatal()
	{
		Log.e(TAG, "Unable to start service: " + getServiceIntent());
		finish();
	}
}
