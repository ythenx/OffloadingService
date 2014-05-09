package com.symlab.testoffloading;

import java.util.ArrayList;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.symlab.dandelion.DandelionHelper;
import com.symlab.dandelion.db.DatabaseQuery;
import com.symlab.dandelion.lib.MethodPackage;
import com.symlab.dandelion.lib.ResultTicket;
import com.symlab.offloadingservice.R;

public class CalSumActivity extends Activity {
	
	private static final String TAG = "TestMain";
	
	private TextView tv;
	private EditText startNumText;
	private EditText endNumText;
	private Button run;
	private Button search;
	
	private DandelionHelper dHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.cal_sum);
		
		tv = (TextView)findViewById(R.id.outputField);
		tv.setMovementMethod(new ScrollingMovementMethod());
		startNumText = (EditText) findViewById(R.id.startNum);
		endNumText = (EditText) findViewById(R.id.endNum);
		run = (Button) findViewById(R.id.run);
		run.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				try {
					runTask(v);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
		});
		search = (Button) findViewById(R.id.search);
		search.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				try {
					searchNode(v);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		});
		String a = BluetoothAdapter.getDefaultAdapter().getAddress();
		println("My Address: " + a);
		Log.e(TAG, "My Address: " + a);
		//showDB(this);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
        	//controller.tearDown();
        	//Intent intent = new Intent(this, com.symlab.dandelion4.MainActivity.class);
			//startActivity(intent);
        	this.finish();
        }
        return super.onKeyDown(keyCode, event);
    }
	
	@Override
	protected void onStart() {
		super.onStart();
		dHelper = new DandelionHelper(this);
		dHelper.initializeOHelper();
	}
	
	@Override
	protected void onStop() {
		dHelper.tearDownOHelper();
		dHelper = null;
		super.onStop();
		
	}

	
	public void runTask(View v) throws InterruptedException {
		long startNum = 1L;//Long.parseLong(startNumText.getText().toString());
		long endNum = Long.parseLong(endNumText.getText().toString());
		clear_screen();
		println("Summing up...");
		println(String.format("StartNum: %d\nEndNum: %d", startNum, endNum));
		
		final int num = Integer.parseInt(startNumText.getText().toString());
		final Long[] result = new Long[num];
		TestTask[] task = new TestTask[num];
		final ResultTicket[] rt = new ResultTicket[num];
		final Class<?>[] paramTypes = {long.class, long.class};
		
		final Long startTime = System.nanoTime();
		for (int i = 0; i < num; i++) {
			result[i] = 0L;
					
			task[i] = new TestTask();
			Object[] paramValues = {startNum, endNum};
			rt[i] = dHelper.postTask(new MethodPackage(task[i], "compute_sum", paramTypes, paramValues), Long.class);
		}
		Log.e(TAG, "Task submitted********");
		for (int j = 0; j < num; j++) {
			final int i = j;
			new Thread(new Runnable(){
				public void run() {
					Log.d(TAG, "The Result is " + rt[i].getResult());
					runOnUiThread(new Runnable(){
						public void run() {
								println("The " + i + " result: " + rt[i].getResult());

							Long estimatedTime = System.nanoTime() - startTime;
							println("Elapsed time: " + estimatedTime/1000000 + "ms");
						}
					});
				}
			}).start();
		}
		
	}
	
	private void showDB(final CalSumActivity activity) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				while(true) {
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					DatabaseQuery query = new DatabaseQuery(activity);
					//String classMethodName = receiver.getClass().toString() + "#" + method.getName();
					final ArrayList<String> queryString = query.getData();
					activity.runOnUiThread(new Runnable(){
	
						@Override
						public void run() {
							//activity.clear_screen();
							for (int i = 0; i < queryString.size(); i++) {
								activity.print(queryString.get(i) + " ");
							}
							activity.println("");
						}
						
					});
				}
			}
			
		}).start();
	}
	
	public void searchNode(View v) {
		//dHelper.testStart();
	}
	
	public void println(String s) {
		tv.append(s + "\n");
	}
	
	public void print(String s) {
		tv.append(s);
	}
	
	public void clear_screen() {
		tv.setText("");
	}
	
	@Override
	public void onDestroy() {
		
		super.onDestroy();
	}

}
