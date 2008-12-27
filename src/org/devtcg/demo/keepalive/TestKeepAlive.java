/*
 * $Id$
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
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class TestKeepAlive extends Activity
{
	public static final String TAG = "TestKeepAlive";

	private Button mStart;
	private Button mStop;

	private final OnClickListener mStartClick = new OnClickListener()
	{
		public void onClick(View v)
		{
			KeepAliveService.actionStart(TestKeepAlive.this);
		}
	};

	private final OnClickListener mStopClick = new OnClickListener()
	{
		public void onClick(View v)
		{
			KeepAliveService.actionStop(TestKeepAlive.this);
		}
	};

	@Override
	public void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);
		setContentView(R.layout.main);

		mStart = (Button)findViewById(R.id.start);
		mStart.setOnClickListener(mStartClick);

		mStop = (Button)findViewById(R.id.stop);
		mStop.setOnClickListener(mStopClick);
	}
}
