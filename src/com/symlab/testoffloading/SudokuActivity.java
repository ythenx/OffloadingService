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
import android.widget.TextView;

public class SudokuActivity extends Activity {

	private static final String TAG = "SudokuActivity";
	
	private TextView tv;
	private Button solve;
	
	private DandelionHelper dHelper;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sudoku);
		tv = (TextView)findViewById(R.id.output3);
		tv.setMovementMethod(new ScrollingMovementMethod());
		solve = (Button) findViewById(R.id.solve);
		solve.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				try {
					solve(v);
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
	
	public void solve(View v) {
		final int num = 10;
		clear_screen();
		
		final String[] input = { "006", "073", "102", "131", "149", "217", "235",
				"303", "345", "361", "378", "422", "465", "514", "521", "548",
				"582", "658", "679", "743", "752", "784", "818", "883" };
		
		Sudoku[] tasks = new Sudoku[num];
		final ResultTicket[] rt = new ResultTicket[num];
		final Integer[] result = new Integer[num];
		final Class<?>[] paramTypes = {};
		
		
		final Long startTime = System.nanoTime();
		for (int i = 0; i < num; i++) {

			result[i] = 0;
			tasks[i] = new Sudoku();

			Object[] paramValues = {};
			rt[i] = dHelper.postTask(new MethodPackage(tasks[i], "hasSolution", paramTypes, paramValues), Boolean.class);
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
