package com.symlab.testoffloading;

import com.symlab.dandelion.DandelionHelper;
import com.symlab.dandelion.lib.MethodPackage;
import com.symlab.dandelion.lib.ResultTicket;
import com.symlab.offloadingservice.R;

import android.app.Activity;
import android.os.Bundle;

import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class NQueensActivity extends Activity {
private static final String TAG = "NQueensActivity";
	
	private TextView tv;
	private EditText puzzleSize;
	private Button count;
	
	private DandelionHelper dHelper;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.nqueens);
		tv = (TextView)findViewById(R.id.output2);
		tv.setMovementMethod(new ScrollingMovementMethod());
		puzzleSize = (EditText) findViewById(R.id.puzzleSize);
		count = (Button) findViewById(R.id.count);
		count.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				try {
					count(v);
				} catch (Exception e) {
					Log.e(TAG, (e.getMessage()!=null?e.getMessage():e.toString()));
				}
			}
			
		});
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
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
	
	public void count(View v) {
		final int num = 10;
		final int N = Integer.parseInt(puzzleSize.getText().toString());
		clear_screen();
		println("Finding solutions for " + N + "-queens puzzle.");
		
		
		NQueens[] tasks = new NQueens[num];
		final ResultTicket[] rt = new ResultTicket[num];
		final Integer[] result = new Integer[num];
		final Class<?>[] paramTypes = {int.class};
		
		
		final Long startTime = System.nanoTime();
		for (int i = 0; i < num; i++) {

			result[i] = 0;
			tasks[i] = new NQueens();

			Object[] paramValues = {N};
			rt[i] = dHelper.postTask(new MethodPackage(tasks[i], "solveNQueens", paramTypes, paramValues), Integer.class);
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
	
	public void println(String s) {
		tv.append(s + "\n");
	}
	
	public void print(String s) {
		tv.append(s);
	}
	
	public void clear_screen() {
		tv.setText("");
	}
}
