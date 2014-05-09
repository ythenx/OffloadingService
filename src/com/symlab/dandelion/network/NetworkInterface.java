package com.symlab.dandelion.network;

import com.symlab.dandelion.lib.ConnectionLostException;

public interface NetworkInterface {
	
	void send(String target, DataPackage data) throws ConnectionLostException;
	
	DataPackage receive(String target) throws ConnectionLostException;
	
	void discoverService();
	
	void stopDiscovery();
	
	void shutdownServer();
	
	String getMacAsId();
	
	
	
}
