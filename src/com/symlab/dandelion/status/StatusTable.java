package com.symlab.dandelion.status;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.util.Log;

import com.symlab.dandelion.lib.ConnectionLostException;
import com.symlab.dandelion.lib.Constants;
import com.symlab.dandelion.network.ConnectedDeviceList;
import com.symlab.dandelion.network.DataPackage;
import com.symlab.dandelion.network.NetworkInterface;

public class StatusTable {

	private static final String TAG = "StatusTable";
	private CopyOnWriteArrayList<TableRow> table;
	
	public StatusTable() {
		table = new CopyOnWriteArrayList<TableRow>();
	}
/*	
	private void addStatus(String id, Status status) {
		table.addIfAbsent(new TableRow(id, status));
	}
	
	private void removeById(String id) {
		for(int i=0; i<table.size(); i++){
			if (id.equals(table.get(i).id)) {
				table.remove(i);
			}
		}
	}
*/	
	private void clearData(){
		table.clear();
	}
	
	public void updateStatus(NetworkInterface networkInterface, ConnectedDeviceList deviceList) {
		Log.d(TAG, "Start updating status...");
		//CountDownLatch latch = new CountDownLatch(t.length);
		clearData();
		ExecutorService updatePool = Executors.newCachedThreadPool();
		for(int i = 0; i < deviceList.size(); i++) {
			String key = deviceList.getDeviceList().get(i);
			Log.d(TAG, "Updating " + key);
			updatePool.execute(new UpdateStatusThread(key, networkInterface));
			//latch.countDown();
		}
		//latch.await(10L, TimeUnit.SECONDS);
	}
	
	public CopyOnWriteArrayList<TableRow> getStatusTable() {
		return table;
	}
	
	public Status getStatusById(String id) {
		Status ret = null;
		for(int i=0; i<table.size(); i++){
			if (id.equals(table.get(i).id)) {
				ret = table.get(i).status;
			}
		}
		return ret;
	}
	
	public void sortDescStatus(String index){
		float[] array = new float[table.size()];
		if(index.compareTo("cpuIdleness")==0){
			for(int i=0; i<table.size(); i++){
				array[i] = table.get(i).status.cpuIdleness;
			}
		}else if(index.compareTo("batteryPercentage")==0){
			for(int i=0; i<table.size(); i++){
				array[i] = table.get(i).status.batteryPercentage;
			}
		}else if(index.compareTo("memoryFree")==0){
			for(int i=0; i<table.size(); i++){
				array[i] = table.get(i).status.memoryFree;
			}
		}
		
		for(int j=1; j< array.length; j++){
			float key =  array[j];
			TableRow temp = new TableRow(table.get(j).id, table.get(j).status);

			int i=j-1;
			while(i>=0 && array[i]<key){
				array[i+1]=array[i];
				table.set(i+1, table.get(i));
				i--;
			}
			array[i+1]=key;
			table.set(i+1, temp);
		}
	}
	
	private class TableRow implements Comparable<TableRow> {
		public String id;
		public Status status;
		
		public TableRow(String id, Status status) {
			this.id = id;
			this.status = status;
		}

		@Override
		public int compareTo(TableRow another) {
			return id.compareTo(another.id);
		}
		
		@Override
		public boolean equals (Object another) {
			return id.equals(((TableRow)another).id);
		}
	}
	
	private class UpdateStatusThread implements Runnable {
		
		String target;
		NetworkInterface networkInterface;
		
		public UpdateStatusThread(String target, NetworkInterface networkInterface) {
			this.target = target;
			this.networkInterface = networkInterface;
		}

		@Override
		public void run() {
			try {
				Status status;
				networkInterface.send(target, DataPackage.obtain(Constants.REQUEST_STATUS));
				status = (Status) networkInterface.receive(target).data;
				if (status != null) {
					Log.d(TAG, String.format("Device ID: %s", target));
					Log.d(TAG, String.format("CPU Idelness: %f%%\nBattery: %f%%\nMemory Free: %fMB", status.cpuIdleness, status.batteryPercentage, status.memoryFree));
					table.add(new TableRow(target, status));
				}
				else {
					Log.e(TAG, "Failed to update status");
				}
			} catch (ConnectionLostException e) {
				Log.e(TAG, "Failed to update status");
			}
		}
		
	}
}
