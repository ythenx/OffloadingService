package com.symlab.dandelion;
import com.symlab.dandelion.lib.OffloadableMethod;

interface IOffloadingService {
	void addTaskToQueue(in OffloadableMethod offloadableMethod);
	String getDeviceId();
	void startDiscovery();
	void stopDiscovery();
	//void stopService();
}