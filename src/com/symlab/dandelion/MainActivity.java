package com.symlab.dandelion;

import com.symlab.offloadingservice.R;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	public static final String TAG = "Main";
	//private IOffloadingService mService;
	//private Intent intent;
	private TextView tv;
	
	private Button startButton;
	private Button stopButton;
	
	//private boolean serviceStarted = false;
	private DandelionHelper dHelper;
	
	private Handler handler;
	/*
	private Runnable changeState = new Runnable() {

		@Override
		public void run() {
			serviceStarted = !serviceStarted;
		}
		
	};
	*/

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		tv = (TextView) findViewById(R.id.screen);
		tv.setMovementMethod(new ScrollingMovementMethod());
		startButton = (Button) findViewById(R.id.startButton);
		stopButton = (Button) findViewById(R.id.stopButton);
		if (OffloadingService.serviceStarted) {
			disableButton(startButton);
			enableButton(stopButton);
		}
		else {
			disableButton(stopButton);
			enableButton(startButton);
		}
		CheckBox helping = (CheckBox) findViewById(R.id.HelpingCheckBox);
		helping.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					dHelper.startDiscovery();
				}
				else {
					dHelper.stopDiscovery();
				}
				
			}			
		});
		handler = new Handler();
		dHelper = new DandelionHelper(this);
		setTitle(BluetoothAdapter.getDefaultAdapter().getAddress());
		//intent = new Intent(this, com.symlab.dandelion.OffloadingService.class);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void start_service(View v) {
		if (dHelper.startOffloadingService()) {
			//startService(intent);
			//print("Service Running");
			disableButton(startButton);
			enableButton(stopButton);
			
			//serviceStarted = true;
			//handler.postDelayed(changeState, 1000);
		}
		dHelper.initializeOHelper();
	}
	
	public void stop_service(View v) {
		dHelper.tearDownOHelper();
		if (dHelper.stopOffloadingService()) {
			//stopService(intent);
			//print("Service Stopped");
			disableButton(stopButton);
			enableButton(startButton);
			//serviceStarted = false;
			//handler.postDelayed(changeState, 1000);
		}
	}
	
	public void test1(View v) {
		Intent intent = new Intent(this, com.symlab.testoffloading.FaceDetectionActivity.class);
		startActivity(intent);
	}
	
	public void test2(View v) {
		Intent intent = new Intent(this, com.symlab.testoffloading.NQueensActivity.class);
		startActivity(intent);
	}
	
	public void test3(View v) {
		Intent intent = new Intent(this, com.symlab.testoffloading.SudokuActivity.class);
		startActivity(intent);
	}
	
	
	private void enableButton(Button b) {
		b.setClickable(true);
		//b.setBackgroundColor(Color.parseColor("#F0F0F0"));
		b.setTextColor(Color.BLACK);
	}
	
	private void disableButton(Button b) {
		b.setClickable(false);
		//b.setBackgroundColor(Color.parseColor("#808080"));
		b.setTextColor(Color.LTGRAY);
	}
	
	public void print(String s) {
        String current = tv.getText().toString();
        tv.setText(current + "\n" + s);
	}
	
	public void clear() {
		tv.setText("");
	}

}
