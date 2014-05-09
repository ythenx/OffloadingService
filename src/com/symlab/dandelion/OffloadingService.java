package com.symlab.dandelion;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.Toast;

import com.symlab.dandelion.lib.Constants;
import com.symlab.dandelion.lib.OffloadableMethod;
import com.symlab.dandelion.lib.TaskQueue;
import com.symlab.dandelion.network.BluetoothInterface;
import com.symlab.dandelion.network.ConnectedDeviceList;
import com.symlab.dandelion.network.NetworkInterface;
import com.symlab.dandelion.status.DeviceStatus;
import com.symlab.dandelion.status.StatusTable;
import com.symlab.dandelion.status.StatusUpdater;

public class OffloadingService extends Service {
	
	private static final String TAG = "OffloadingService";
	
	private PackageManager packageManager;// = getPackageManager();
	private ExecutorService executor = Executors.newCachedThreadPool();
	private String myId;
	private NetworkInterface networkInterface;
	private ConnectedDeviceList deviceList;
	private TaskQueue taskQueue;
	private TaskQueueHandler taskQueueHandler;
	private StatusTable statusTable;
	private StatusUpdater statusUpdater;
	
	private OffloadingService me;// = this;
	
	public static boolean serviceStarted = false;
	
	private final IOffloadingService.Stub mBinder = new IOffloadingService.Stub() {

		@Override
		public void addTaskToQueue(OffloadableMethod offloadableMethod)
				throws RemoteException {
			taskQueue.enqueue(offloadableMethod);
		}

		@Override
		public String getDeviceId() throws RemoteException {
			return myId;
		}

		@Override
		public void startDiscovery() throws RemoteException {
			statusUpdater.startUpdate();
			Toast.makeText(me, "Starting Service", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void stopDiscovery() throws RemoteException {
			statusUpdater.stopUpdate();
			Toast.makeText(me, "Starting Service", Toast.LENGTH_SHORT).show();
		}
		/*
		@Override
		public void stopService() throws RemoteException {
			stopService();
		}
		*/

	};
	
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		Toast.makeText(this, "Starting Service", Toast.LENGTH_SHORT).show();
		deviceList = new ConnectedDeviceList();
		taskQueue = new TaskQueue();
		statusTable = new StatusTable();
		switch(Constants.NETWORK_TYPE) {
		case Constants.BLUETOOTH:
			networkInterface = new BluetoothInterface(this, deviceList);
			myId = networkInterface.getMacAsId();
			break;
		case Constants.WIFI:
			networkInterface = null;
			myId = "";
			break;
		}
		packageManager = getPackageManager();
		taskQueueHandler = new TaskQueueHandler(this, networkInterface, executor, taskQueue, deviceList, statusTable, packageManager);
		executor.execute(taskQueueHandler);
		statusUpdater = new StatusUpdater(networkInterface, deviceList, statusTable, Constants.UPDATE_INTERVAL);
		//statusUpdater.startUpdate();
		
		me = this;
		
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}
	
	public static synchronized void setServiceOn() {
		serviceStarted = true;
	}
	
	public static synchronized void setServiceOff() {
		serviceStarted = false;
	}
	/*
	private void stopService() {
		networkInterface.shutdownServer();
		stopSelf();
	}
	*/
	@Override
	public void onDestroy() {
		DeviceStatus.newInstance(this).tearDown();
		networkInterface.shutdownServer();
		super.onDestroy();
	}

}
