package com.symlab.dandelion.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.util.Log;

public class ConnectedDeviceList {
	
	private CopyOnWriteArrayList<String> list;
	//indicate whether the offloadee is busy or not:true is free, false is busy
	private ArrayList<Boolean> jobStatus;
	private HashMap<String, ObjectInputStream> inputstreams;
	private HashMap<String, ObjectOutputStream> outputstreams;
	
	public ConnectedDeviceList() {
		list = new CopyOnWriteArrayList<String>();
		jobStatus = new ArrayList<Boolean>();
		inputstreams = new HashMap<String, ObjectInputStream>();
		outputstreams = new HashMap<String, ObjectOutputStream>();
	}
	
	public ObjectInputStream getInputStream(String key) {
		if (!list.contains(key)) return null;
		else return inputstreams.get(key);
	}

	public ObjectOutputStream getOutputStream(String key) {
		if (!list.contains(key)) return null;
		else return outputstreams.get(key);
	}
	
	public synchronized void addIfAbsent(String key, ObjectInputStream is, ObjectOutputStream os) {
		if (!list.contains(key)) {
			list.add(key);
			jobStatus.add(true);
			inputstreams.put(key, is);
			outputstreams.put(key, os);
		} else {
			inputstreams.remove(key);
			inputstreams.put(key, is);
			outputstreams.remove(key);
			outputstreams.put(key, os);
		}
	}
	
	public synchronized void removeNode(String key) {
		if (!list.contains(key)) return;
		int index = list.indexOf(key);
		//Log.v("ConnectedDeviceList", "removeNode1");
		list.remove(index);
		//Log.v("ConnectedDeviceList", "removeNode2");
		jobStatus.remove(index);
		//Log.v("ConnectedDeviceList", "removeNode3");
		try {
			inputstreams.get(key).close();
		} catch (IOException e) {
			Log.e("ConnectedDeviceList", "inputstream already closed");
		} finally {
			inputstreams.remove(key);
			try {
				outputstreams.get(key).close();
			} catch (IOException e) {
				Log.e("ConnectedDeviceList", "outputstream already closed");
			} finally {
				outputstreams.remove(key);
			}
		}
	}
	
	public boolean containsNode(String key) {
		return list.contains(key);
	}
	
	public void clear(){
		list.clear();
		jobStatus.clear();
		inputstreams.clear();
		outputstreams.clear();
	}
	
	public int size() {
		return list.size();
	}
	
	public List<String> getDeviceList() {
		return list;
	}
	
	public List<Boolean> getJobStatusList() {
		return jobStatus;
	}
	
	public void setJobStatusToBusy(String target){
		if (target == null) return;
		jobStatus.set(list.indexOf(target), false);
	}
	
	public void setJobStatusToFree(String target){
		if (target == null) return;
		synchronized(jobStatus){
			jobStatus.set(list.indexOf(target), true);
		}
	}
}
