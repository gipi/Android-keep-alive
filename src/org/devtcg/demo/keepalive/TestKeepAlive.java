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
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class TestKeepAlive extends Activity
{
	public static final String TAG = "TestKeepAlive";

	private final OnClickListener mClicked = new OnClickListener()
	{
		public void onClick(View v)
		{
			switch (v.getId())
			{
			case R.id.start:
				KeepAliveService.actionStart(TestKeepAlive.this);
				break;
			case R.id.stop:
				KeepAliveService.actionStop(TestKeepAlive.this);
				break;
			case R.id.ping:
				KeepAliveService.actionPing(TestKeepAlive.this);
				break;
			}
		}
	};

	@Override
	public void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);
		setContentView(R.layout.main);

		findViewById(R.id.start).setOnClickListener(mClicked);
		findViewById(R.id.stop).setOnClickListener(mClicked);
		findViewById(R.id.ping).setOnClickListener(mClicked);
	}
}
