package com.symlab.dandelion;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.symlab.dandelion.lib.OffloadableMethod;
import com.symlab.dandelion.lib.TaskQueue;
import com.symlab.dandelion.network.ConnectedDeviceList;
import com.symlab.dandelion.status.StatusTable;

public class DecisionMaker {
	
	private static final String TAG = "DecisionMaker";
	
	private StatusTable statustable;
	

	public DecisionMaker(StatusTable st) {
		statustable = st;
	}
	
	public List<String> globalOptimalDecision(LinkedList<OffloadableMethod> taskQueue){
		List <String> devices=new ArrayList<String>();
		return devices;
	}
	
	public List<String> greedyDecision(TaskQueue taskQueue, ConnectedDeviceList deviceList){
		LinkedList<OffloadableMethod> bufferQueue = new LinkedList<OffloadableMethod>();
		
		List <String> devices=new ArrayList<String>();
		
		statustable.sortDescStatus("cpuIdleness");
		int count=0;
		for(int i=0; i<deviceList.size(); i++){
			if(deviceList.getJobStatusList().get(i)==true){
				devices.add(deviceList.getDeviceList().get(i));
			}
		}
		
		return devices;
	}
	
}
