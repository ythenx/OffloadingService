package com.symlab.dandelion.lib;

public interface Constants {
	int INIT_OFFLOAD = 1;
	int APK_REQUEST = 2;
	int APK_SEND = 3;
	int READY = 4;
	int EXECUTE = 5;
	int RESULT = 6;
	
	int REQUEST_STATUS = 7;
	int RESPONSE_STATUS = 8;
	
	
	
	int PING = 101;
	int PONG = 102;
	
	int BLUETOOTH = 30;
	int WIFI = 31;
	
	int NETWORK_TYPE = BLUETOOTH;
	
	int BUFFER = 8192;
	
	int PROFILER_UPDATE_PERIOD = 1000;
	
	int SERVER_PORT = 54321;
	
	int DECISION_MAKER_THRESHOLD = 60;
	
	int UPDATE_INTERVAL = 60000;
}
