package com.symlab.dandelion;

import java.util.List;
import java.util.concurrent.ExecutorService;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import com.symlab.dandelion.lib.OffloadableMethod;
import com.symlab.dandelion.lib.TaskQueue;
import com.symlab.dandelion.network.ConnectedDeviceList;
import com.symlab.dandelion.network.NetworkInterface;
import com.symlab.dandelion.status.StatusTable;

public class TaskQueueHandler implements Runnable {
	
	private static final String TAG = "TaskQueueHandler";
	//private ArrayList<TaskWrapper<?>> list;
	private boolean paused = false;
	private boolean stopped = false;
	private Object lock;
	
	private Context context;
	private NetworkInterface networkInterface;
	private TaskQueue queue;
	private ConnectedDeviceList deviceList;
	//private StatusTable statusTable;
	private PackageManager packageManager;
	
	private ExecutorService executor;

	private DecisionMaker dmaker;
	
	public TaskQueueHandler(Context context, NetworkInterface networkInterface, ExecutorService executor, 
			TaskQueue queue,  ConnectedDeviceList deviceList, StatusTable statusTable, PackageManager packageManager) {
		lock = new Object();
		this.context = context;
		this.networkInterface = networkInterface;
		this.executor = executor;
		this.queue = queue;
		this.deviceList = deviceList;
		dmaker = new DecisionMaker(statusTable);
		this.packageManager = packageManager;
		//list = new ArrayList<TaskWrapper<?>>();
	}
	
	private void submitTask(OffloadableMethod offloadableMethod, String target) {
		TaskWrapper taskWrapper = new TaskWrapper(context, networkInterface, packageManager, offloadableMethod, target, deviceList, queue);
		offloadableMethod.resultTicket.setHolder(executor.submit(taskWrapper));
		//offloadableMethod.resultTicket.setResultReady();
		deviceList.setJobStatusToBusy(target);
	}
	
	@Override
	public void run() {
		Log.d(TAG, "Start TaskQueue Handler");
		while (!stopped && !Thread.currentThread().isInterrupted()) {
			while (queue.queueSize() != 0) {
				Log.e(TAG, "************************************************************");
//				int qSize = queue.queueSize();
//				if(deviceList.size()*qSize<Constants.DECISION_MAKER_THRESHOLD){
//					List<String> devices = dmaker.globalOptimalDecision(queue);
//					for(int i=0; i<qSize; i++){
//						
//						submitTask(queue.dequeue(),devices.get(i));
//					}
//				}
//				else{
				List<String> devices = dmaker.greedyDecision(queue, deviceList);
				for(int i=0; i<devices.size() && queue.queueSize() != 0; i++){
					Log.e(TAG, "Device: " + devices.get(i));
					submitTask(queue.dequeue(), devices.get(i));
				}

				while (queue.queueSize() != 0) {
					submitTask(queue.dequeue(), null);
				}

			}
			//queue.clearQueue();
			
			
	        synchronized(lock) {
	            while (paused) {
					try {
						lock.wait();
					} catch (InterruptedException e) {
						break;
					}
	            }
	        }
		}

	}
	
	public void pause() {
		synchronized(lock) {
			paused = true;
		}
	}
	
	public void resumeThread() {
		synchronized(lock) {
			paused = false;
			lock.notifyAll();
		}
    }
}
