package com.symlab.testoffloading;

import java.io.File;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;

import com.symlab.dandelion.lib.ByteFile;
import com.symlab.dandelion.lib.Offloadable;

public class TestFaceDetection2 extends Offloadable {
	
	private static final long serialVersionUID = 7617110052835429481L;
	private static final String TAG = "TestFaceDetection";
	//transient private Bitmap sourceImage;
	//private Face[] faces;
	private ByteFile file;
	
	public Integer detect_faces(int num) {
		//File temp = new File();
		//file.writeToFile(temp);
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inPreferredConfig = Bitmap.Config.RGB_565;
		//Bitmap bitmap = BitmapFactory.decodeFile(temp.getAbsolutePath(), opts);
		Bitmap bitmap = BitmapFactory.decodeByteArray(file.toByteArray(), 0, file.length(), opts);
		
		FaceDetector fd = new FaceDetector(bitmap.getWidth(), bitmap.getHeight(), num);
		Face[] faces = new Face[num];
		int foundNum = 0;
		foundNum = fd.findFaces(bitmap, faces);
		file.recycle();
		return foundNum;
	}
	
	public void getImage(File imageToCheck) {
		file = ByteFile.createFromFile(imageToCheck);
	}

	@Override
	public void copyState(Offloadable state) {
		
	}

}
