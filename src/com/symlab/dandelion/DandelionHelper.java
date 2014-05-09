package com.symlab.dandelion;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.symlab.dandelion.lib.MethodPackage;
import com.symlab.dandelion.lib.OffloadableMethod;
import com.symlab.dandelion.lib.ResultTicket;

public class DandelionHelper {

	private static final String TAG = "DandelionHelper";
	
	private IOffloadingService mService;
	private Context	mContext;
	private Intent offloadingServiceIntent;
	private String appName;
	
	private boolean serviceBound;
	
	private ServiceConnection serviceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(TAG, "Service Binding...");
			mService = IOffloadingService.Stub.asInterface(service);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
			Log.d(TAG, "Service Broken");
		}
		
	};
	
	public DandelionHelper(Context context) {
		this.mContext 	= context;
		offloadingServiceIntent = new Intent(context, OffloadingService.class);
		appName = context.getPackageName();
		serviceBound = false;
	}
	
	public void initializeOHelper() {
		Log.d(TAG, "Call initialize");
		startOffloadingService();
		if (!serviceBound) {
			Log.d(TAG, "Bind Service");
			mContext.bindService(offloadingServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
			serviceBound = true;
		}
		
	}
	
	boolean startOffloadingService() {
		if (!OffloadingService.serviceStarted) {
			Log.d(TAG, "Start Service");
			mContext.startService(offloadingServiceIntent);
			OffloadingService.setServiceOn();
		}
		if (OffloadingService.serviceStarted)
			return true;
		else
			return false;
	}
	
	public void tearDownOHelper() {
		if (serviceBound) {
			mContext.unbindService(serviceConnection);
			serviceBound = false;
			mService = null;
			Log.d(TAG, "Service Unbinding...");
		}
	}
	
	boolean stopOffloadingService() {
		if (OffloadingService.serviceStarted) {
			Log.d(TAG, "Stop Service");
			
			mContext.stopService(offloadingServiceIntent);
			OffloadingService.setServiceOff();
		}
		if (!OffloadingService.serviceStarted)
			return true;
		else
			return false;
	}
	
	public ResultTicket postTask(MethodPackage methodPack, Class<?> reutrnType) {
		OffloadableMethod offloadableMethod = new OffloadableMethod(mContext, appName, methodPack, reutrnType);
		try {
			mService.addTaskToQueue(offloadableMethod);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return offloadableMethod.resultTicket;
	}
	
	public String myId() {
		String ret = "";
		try {
			if (mService != null)
				ret +=  mService.getDeviceId();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	public void startDiscovery() {
		if (mService != null) {
			try {
				mService.startDiscovery();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void stopDiscovery() {
		if (mService != null) {
			try {
				mService.stopDiscovery();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

}
