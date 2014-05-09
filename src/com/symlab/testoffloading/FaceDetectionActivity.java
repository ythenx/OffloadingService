package com.symlab.testoffloading;

import java.io.File;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.symlab.dandelion.DandelionHelper;
import com.symlab.dandelion.lib.MethodPackage;
import com.symlab.dandelion.lib.ResultTicket;
import com.symlab.offloadingservice.R;

public class FaceDetectionActivity extends Activity {

	private static final String TAG = "FaceDetectionActivity";
	
	private TextView tv;
	private EditText maxFaceNum;
	private Button detect;
	
	private DandelionHelper dHelper;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.face_detection);
		tv = (TextView)findViewById(R.id.output);
		tv.setMovementMethod(new ScrollingMovementMethod());
		maxFaceNum = (EditText) findViewById(R.id.maxFaces);
		detect = (Button) findViewById(R.id.detect);
		detect.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				try {
					detect(v);
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
	
	public void detect(View v) {
		final int maxFace = Integer.parseInt(maxFaceNum.getText().toString());
		clear_screen();
		println("Detecting faces, max num for each photo: " + maxFace);
		File sdCardRoot = Environment.getExternalStorageDirectory();
		File photoDir = new File(sdCardRoot, "testPhotos");
		final int fileNum = photoDir.listFiles().length;
		Log.d(TAG, "Num of files: " + fileNum);
		if (fileNum == 0) return;
		
		TestFaceDetection2[] tasks = new TestFaceDetection2[fileNum];
		final ResultTicket[] rt = new ResultTicket[fileNum];
		final Integer[] result = new Integer[fileNum];
		final Class<?>[] paramTypes = {int.class};
		
		
		final Long startTime = System.nanoTime();
		for (int i = 0; i < fileNum; i++) {
			File file = photoDir.listFiles()[i];
			if (!file.isFile() || !file.getName().matches("^\\w+\\.jpg")) continue;
			result[i] = 0;
			tasks[i] = new TestFaceDetection2();
			tasks[i].getImage(file.getAbsoluteFile());
			Object[] paramValues = {maxFace};
			rt[i] = dHelper.postTask(new MethodPackage(tasks[i], "detect_faces", paramTypes, paramValues), Integer.class);
		}
		Log.e(TAG, "Task submitted********");
		for (int j = 0; j < fileNum; j++) {
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
