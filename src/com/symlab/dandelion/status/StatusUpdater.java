package com.symlab.dandelion.status;

import android.os.Handler;

import com.symlab.dandelion.network.ConnectedDeviceList;
import com.symlab.dandelion.network.NetworkInterface;

public class StatusUpdater {

	private static final String TAG = "StatusUpdater";
	
	private NetworkInterface netInt;
	private ConnectedDeviceList deviceList;
	private StatusTable statusTable;
	
	private Handler handler;
	
	private int timeInterval;
	
	private Runnable task = new Runnable() {

		@Override
		public void run() {
			netInt.discoverService();
			statusTable.updateStatus(netInt, deviceList);
			handler.postDelayed(this,timeInterval); 
		}
		
	};
	
	public StatusUpdater(NetworkInterface netInt, ConnectedDeviceList deviceList, StatusTable statusTable, int timeInterval) {
		this.netInt = netInt;
		this.deviceList = deviceList;
		this.statusTable = statusTable;
		this.timeInterval = timeInterval;
		handler = new Handler( );
	}
	
	public void startUpdate() {
		handler.removeCallbacks(task);
		handler.post(task); 
	}
	
	public void stopUpdate() {
		handler.removeCallbacks(task);
		//netInt.stopDiscovery();
	}
}
