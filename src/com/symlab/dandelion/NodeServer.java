package com.symlab.dandelion;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.content.Context;
import android.util.Log;

import com.symlab.dandelion.lib.ByteFile;
import com.symlab.dandelion.lib.Constants;
import com.symlab.dandelion.lib.MethodPackage;
import com.symlab.dandelion.lib.RemoteNodeException;
import com.symlab.dandelion.lib.ResultContainer;
import com.symlab.dandelion.network.DataPackage;
import com.symlab.dandelion.network.ServerStreams;
import com.symlab.dandelion.status.DeviceStatus;

public class NodeServer extends Thread {
	
	ServerStreams sstreams;
	Context context;
	ExecutorService workerPool;
	ExecutorService pool;

	public NodeServer(ServerStreams ss, Context c) {
		sstreams = ss;
		context = c;
		workerPool = Executors.newFixedThreadPool(1);
		pool = Executors.newCachedThreadPool();
	}
	
	@Override
	public void run() {
		DataPackage receive = DataPackage.obtain(-1);
		String apkFilePath = "";
		File dexFile = null;
		boolean connectionloss = false;
		while(receive != null) {
			try {
				receive = sstreams.receive();
				Log.d("NodeServer", "Received message: " + (receive != null?receive.what:"null"));
			} catch (IOException e) {
				connectionloss = true;
				Log.e("NodeServer", "Connection Loss");
			} 
			if (connectionloss || receive == null) break;
			switch (receive.what) {
			case Constants.PING:
				sstreams.send(DataPackage.obtain(Constants.PONG));
				break;
			case Constants.INIT_OFFLOAD:
				String appName_hashCode = (String) receive.data;
				String[] temp = appName_hashCode.split("#");
				String appName = temp[0].trim();
				long lastModified = Long.parseLong(temp[1].trim());
				Log.e("NodeServer", "Received lastModified: " + lastModified);
				Log.d("NodeServer", "AppName***" + appName);
				File dexOutputDir = context.getDir("dex", 0);
				apkFilePath = dexOutputDir.getAbsolutePath() + "/" + appName + ".apk";
				Log.d("NodeServer", apkFilePath);
				if (apkPresent(apkFilePath, lastModified)) {
					sstreams.send(DataPackage.obtain(Constants.READY));
				}
				else {
					sstreams.send(DataPackage.obtain(Constants.APK_REQUEST));
				}
				break;
			case Constants.APK_SEND:
				ByteFile bf = (ByteFile) receive.data;
				//Log.e("NodeServer", "1");
				dexFile = new File(apkFilePath);
				//Log.e("NodeServer", dexFile.getAbsolutePath());
				try {
					FileOutputStream fout = new FileOutputStream(dexFile);
					//Log.e("NodeServer", "3");
					BufferedOutputStream bout = new BufferedOutputStream(fout, Constants.BUFFER);
					//Log.e("NodeServer", "4");
					bout.write(bf.toByteArray());
					//Log.e("NodeServer", "5");
					bout.close();
					//Log.e("NodeServer", "6");
					sstreams.addDex(dexFile);
					//Log.e("NodeServer", "7");
					sstreams.send(DataPackage.obtain(Constants.READY));
					//Log.e("NodeServer", "8");
				} catch (IOException e){
					
				}
				break;
			case Constants.EXECUTE:
				MethodPackage methodPack;
				methodPack = MethodPackage.class.cast(receive.data);
				Future<ResultContainer> future = workerPool.submit(new Worker(methodPack));
				pool.execute(new SendResult(future));
				break;
			case Constants.REQUEST_STATUS:
				sstreams.send(DataPackage.obtain(Constants.RESPONSE_STATUS, DeviceStatus.newInstance(context).readStatus()));
				break;
			}
		}
		sstreams.tearDownStream();
		return;
	}
	
	private class Worker implements Callable<ResultContainer> {
		private MethodPackage methodPack;
		
		
		public Worker(MethodPackage mp) {
			methodPack = mp;
		}

		@Override
		public ResultContainer call() {
			ResultContainer ret = null;
			Object result = null;
			Long execDuration = null;
			try {
				Method method = methodPack.receiver.getClass().getDeclaredMethod(methodPack.methodName, methodPack.paraTypes);
				method.setAccessible(true);
				Long startExecTime = System.nanoTime();
				result = method.invoke(methodPack.receiver, methodPack.paraValues);
				execDuration = System.nanoTime() - startExecTime;
				ret = new ResultContainer(false, methodPack.receiver, result, execDuration, 0f); 
			} catch (NoSuchMethodException e) {
				ret = new ResultContainer(true, methodPack.receiver, new RemoteNodeException(e.getMessage(), e), 0L, 0f); 
			} catch (InvocationTargetException e) {
				ret = new ResultContainer(true, methodPack.receiver, new RemoteNodeException(e.getMessage(), e), 0L, 0f); 
			} catch (IllegalAccessException e) {
				ret = new ResultContainer(true, methodPack.receiver, new RemoteNodeException(e.getMessage(), e), 0L, 0f); 
			} 
			Log.e("Worker", "Remote Result: " + ret.result);
			return ret;
			
		}
		
	}
	
	private class SendResult implements Runnable {
		private Future<ResultContainer> future;
		
		public SendResult(Future<ResultContainer> f) {
			future = f;
		}
		
		@Override
		public void run() {
			try {
				ResultContainer result = future.get();
				sstreams.send(DataPackage.obtain(Constants.RESULT, result));
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	private boolean apkPresent(String filename, long lastModified) {
		//return false;
		File apkFile = new File(filename);
		if (apkFile.exists()) {
			Log.e("NodeServer", "lastModified: " + apkFile.lastModified());
			if (lastModified == apkFile.lastModified())
				return true;
			else
				return true;
		}
		else {
			return false;
		}
	}
}
