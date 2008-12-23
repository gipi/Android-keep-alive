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
        setContentView(R.layout.main);

        mStart = (Button)findViewById(R.id.start);
        mStart.setOnClickListener(mStartClick);

        mStop = (Button)findViewById(R.id.stop);
        mStop.setOnClickListener(mStopClick);
	}
}