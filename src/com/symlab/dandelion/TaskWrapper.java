package com.symlab.dandelion;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import com.symlab.dandelion.lib.ByteFile;
import com.symlab.dandelion.lib.ConnectionLostException;
import com.symlab.dandelion.lib.Constants;
import com.symlab.dandelion.lib.Offloadable;
import com.symlab.dandelion.lib.OffloadableMethod;
import com.symlab.dandelion.lib.OffloadingNetworkException;
import com.symlab.dandelion.lib.RemoteNodeException;
import com.symlab.dandelion.lib.ResultContainer;
import com.symlab.dandelion.lib.TaskQueue;
import com.symlab.dandelion.network.ConnectedDeviceList;
import com.symlab.dandelion.network.DataPackage;
import com.symlab.dandelion.network.NetworkInterface;
import com.symlab.dandelion.profilers.DeviceProfiler;
import com.symlab.dandelion.profilers.Profiler;
import com.symlab.dandelion.profilers.ProgramProfiler;

public class TaskWrapper implements Callable<Object> {
	private static final String TAG = "TaskWrapper";
	
	private Context context;
	private NetworkInterface networkInterface;
	private PackageManager packageManager;
	private OffloadableMethod method;
	private String target;
	private ConnectedDeviceList deviceList;
	private TaskQueue queue;
	
	private Long mPureExecutionDuration;
	private boolean remotely;
	
	public TaskWrapper(Context context, NetworkInterface networkInterface, PackageManager packageManager, OffloadableMethod method, String target, final ConnectedDeviceList deviceList, final TaskQueue queue) {
		this.context = context;
		this.networkInterface = networkInterface;
		this.packageManager = packageManager;
		this.method = method;
		this.target = target;
		this.deviceList = deviceList;
		this.queue = queue;
		remotely = (target != null);
		Log.d(TAG, "Set Task " + method.methodPackage.methodName + " to " + target + "***");
	}

	@Override
	public Object call() throws Exception {
		Object ret = execute();
		if (ret != null)
			method.resultTicket.setResultReady();
		else
			throw new Exception("*****null result returned!*****");
		return ret;
	}
	
	private Object execute() {
		Object result = null;
		String classMethodName = method.methodPackage.receiver.getClass().toString() + "#" + method.methodPackage.methodName;

		ProgramProfiler progProfiler = new ProgramProfiler(classMethodName);
		DeviceProfiler devProfiler = new DeviceProfiler(context);
		
		try {
			if (remotely) {

				//NetworkProfiler netProfiler = new NetworkProfiler(mContext);
				Log.d(TAG, "Executing Remotely...");
				//Profiler profiler = new Profiler(mContext, progProfiler, netProfiler, devProfiler);
				//establishConnection();
				// Start tracking execution statistics for the method
				//profiler.startExecutionInfoTracking();
				try {
					result = sendAndExecute();
					deviceList.setJobStatusToFree(target);
				} catch (RemoteNodeException e) {
					//remotely = false;
					result = executeLocally();
				} catch (OffloadingNetworkException e) {
					Log.e(TAG, "Network Error, Executing Locally...");
					result = executeLocally();
					//Log.e(TAG, "Network Error, Adding task to task queue...");
					//queue.enqueue(method);
				}
				// Collect execution statistics
				//profiler.stopAndLogExecutionInfoTracking(mPureExecutionDuration);
				//lastLogRecord = profiler.lastLogRecord;
				//return result;
			} else { // Execute locally
				//NetworkProfiler netProfiler = null;
				Log.d(TAG, "Executing Locally...");
				//Profiler profiler = new Profiler(context, progProfiler, null, devProfiler);
				
				// Start tracking execution statistics for the method
				
				//profiler.startExecutionInfoTracking();
				Long startTime = System.nanoTime();
				result = executeLocally();
				mPureExecutionDuration = System.nanoTime() - startTime;
				//Log.d("PowerDroid-Profiler", "LOCAL " + m.getName()+ ": Actual Invocation duration - " + mPureExecutionDuration/ 1000 + "us");
				// Collect execution statistics
				//profiler.stopAndLogExecutionInfoTracking(mPureExecutionDuration);
				
				//lastLogRecord = profiler1.lastLogRecord;
				//return result;
			}
			
		
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	private Object executeLocally() {
		Object result = null;
		Method m;
		try {
			Log.i(TAG, "executeLocally...");
			Class<?> temp = method.methodPackage.receiver.getClass();
			//Log.wtf(TAG, "executeLocally Error2");
			m = temp.getDeclaredMethod(method.methodPackage.methodName, method.methodPackage.paraTypes);
			//Log.wtf(TAG, "executeLocally Error3");
			m.setAccessible(true);
			//Log.wtf(TAG, "executeLocally Error4");
			result = m.invoke(method.methodPackage.receiver, method.methodPackage.paraValues); 
			Log.i(TAG, "executeLocally No Error");
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	private Object sendAndExecute() throws RemoteNodeException, OffloadingNetworkException {
		DataPackage response = null;
		Object result = null;
		try {
			String apkName = packageManager.getApplicationInfo(method.appName, 0).sourceDir;
			File apkFile = new File(apkName);
			long lastModified = apkFile.lastModified();
			Log.e(TAG, "lastModified: " + lastModified);
			Log.d(TAG, "Start Offloading");
			try {
				networkInterface.send(target, DataPackage.obtain(Constants.INIT_OFFLOAD, method.appName + "#" + lastModified));
			} catch (ConnectionLostException e) {
				deviceList.removeNode(target);
				throw new OffloadingNetworkException("Failed to send INIT_OFFLOAD", e);
			}
			Log.d(TAG, apkName);
			try {
				response = networkInterface.receive(target);
			} catch (ConnectionLostException e) {
				Log.e(TAG, "ConnectionLoss 1");
				//deviceList.removeNode(target);
				throw new OffloadingNetworkException("No response for INIT_OFFLOAD", e);
			}
			if (response.what == Constants.APK_REQUEST) {
				
				FileInputStream fin = new FileInputStream(apkFile);
				BufferedInputStream bis = new BufferedInputStream(fin);
				byte[] tempArray = new byte[(int) apkFile.length()];
				bis.read(tempArray, 0, tempArray.length);
				bis.close();
				Log.d(TAG, "Send APK");
				try {
					networkInterface.send(target, DataPackage.obtain(Constants.APK_SEND, new ByteFile(tempArray)));
				} catch (ConnectionLostException e) {
					deviceList.removeNode(target);
					throw new OffloadingNetworkException("Failed to send APK", e);
				}
				try {
					response = networkInterface.receive(target);
				} catch (ConnectionLostException e) {
					Log.e(TAG, "ConnectionLoss 2");
					//deviceList.removeNode(target);
					throw new OffloadingNetworkException("No response for APK sent", e);
				}
			}
			if (response.what == Constants.READY) {
				Log.d(TAG, "Send Offloading Task");
				try {
					networkInterface.send(target, DataPackage.obtain(Constants.EXECUTE, method.methodPackage));
				} catch (ConnectionLostException e) {
					Log.v(TAG, "Send Offloading Task Error, deleting node");
					deviceList.removeNode(target);
					Log.e(TAG, "Send Offloading Task Error");
					throw new OffloadingNetworkException("Failed to send Method", e);
				}
				try {
					response = networkInterface.receive(target);
				} catch (ConnectionLostException e) {
					Log.e(TAG, "ConnectionLoss 3");
					throw new OffloadingNetworkException("No response for offloading result", e);
				}
				if (response.what == Constants.RESULT) {
					ResultContainer container = (ResultContainer) response.data;
					Class<?>[] pTypes = { Offloadable.class };
					try {
						// Use the copyState method that must be defined for all Remoteable
						// classes to copy the state of relevant fields to the local object
						method.methodPackage.receiver.getClass().getMethod("copyState", pTypes).invoke(method.methodPackage.receiver, container.caller);
					} catch (NullPointerException e) {
						// Do nothing - exception happened remotely and hence there is
						// no objet state returned.
						// The exception will be returned in the function result anyway.
						Log.d(TAG, "Exception received from remote server - " + container.result);
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (NoSuchMethodException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (container.isExceptionOrError) {
						throw (RemoteNodeException) container.result;
					} else {
						result = container.result;
					}
				}
			}
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

}
