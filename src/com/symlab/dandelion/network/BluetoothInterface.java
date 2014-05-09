package com.symlab.dandelion.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.symlab.dandelion.NodeServer;
import com.symlab.dandelion.lib.ConnectionLostException;
import com.symlab.dandelion.lib.Constants;
import com.symlab.dandelion.lib.DynamicObjectInputStream;

public class BluetoothInterface implements NetworkInterface {
	
	private static final String TAG = "BluetoothInterface**";
	
	private static final String NAME = "DandelionBluetooth";
	private static final UUID DANDELION_UUID = UUID.fromString("36ca5460-c301-11e3-8a33-0800200c9a66");
	
	private final BluetoothAdapter mAdapter;
	private BluetoothServerSocket bss;
	private boolean receiverRegistered = false;
	
	private BluetoothServiceServer serverThread;
	
	private boolean occupied = false;
	private Boolean isServerShutdown = true;
	private boolean resolved = false;
	private Object lock;
	
	private Context mContext;
	private ConnectedDeviceList mDeviceList;
	private CopyOnWriteArrayList<BluetoothDevice> tempDeviceList;
	private final String [] fullDeviceList = { 
			//"E4:B0:21:73:CB:B7",
			//"CC:C3:EA:0E:B4:70",
			//"F8:E0:79:26:51:BD",
			"CC:C3:EA:0E:B4:82",
			//"F0:08:F1:39:75:48",
			//"CC:C3:EA:10:1A:E5",
			//"E4:B0:21:FD:C1:14",
			//"CC:C3:EA:0E:B4:86",
			//"F8:E0:79:31:E5:7D"
			
	};
	
	private HashMap<String, BluetoothSocket> bsMap;
	
	public BluetoothInterface(Context context, ConnectedDeviceList deviceList) {
		mContext = context;
		mDeviceList = deviceList;
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		tempDeviceList = new CopyOnWriteArrayList<BluetoothDevice>();
		if (mAdapter == null) {
			Log.e(TAG, "Device does not support Bluetooth");
			return;
		}
		//Log.e(TAG, "My Scan Mode is " + mAdapter.getScanMode());
		if (mAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			askForDiscoverable();
		}
		bsMap = new HashMap<String, BluetoothSocket>();
		startServer();
	}
	
	private void askForDiscoverable() {
		Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
		discoverableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mContext.startActivity(discoverableIntent);
	}
	
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        // When discovery finds a device
	        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
	            // Get the BluetoothDevice object from the Intent
	            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	            // Add the name and address to an array adapter to show in a ListView
	            tempDeviceList.addIfAbsent(device);

	        } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action) && !resolved) {
	        	resolved = true;
	        	resolveConnections();
	        }
	    }
	};
	
	private void registerReceiver() {
		if (receiverRegistered) return;
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
		intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		mContext.registerReceiver(mReceiver, intentFilter);
		receiverRegistered = true;		
	}
	
	private void resolveConnections() {
		ExecutorService pool = Executors.newCachedThreadPool();
		//mAdapter.cancelDiscovery();
		//Log.e(TAG, "" + tempDeviceList.size());

		for (String device : fullDeviceList) {
			//Log.e(TAG, "Device***" + device);
			if (bsMap.containsKey(device)) {
				try {
					bsMap.get(device).close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				bsMap.remove(device);
			}
			if (device.equals(getMacAsId())) continue;
			if (mDeviceList.containsNode(device)) {
				pool.execute(new ResolveExistingTask(device));
			}
			else {
				pool.execute(new ResolveTask(device));
			}
			
		}
	}
	
	private class ResolveExistingTask implements Runnable {
		ExecutorService pool = Executors.newCachedThreadPool();
		private String device;
		
		public ResolveExistingTask(String device) {
			this.device = device;
		}
		@Override
		public void run() {
			DataPackage response;
			try {
				send(device, DataPackage.obtain(Constants.PING));
				response = receive(device);
				if (response == null || response.what != Constants.PONG) {
					mDeviceList.removeNode(device);
					pool.execute(new ResolveTask(device));
				}
			} catch (ConnectionLostException e) {
				mDeviceList.removeNode(device);
				pool.execute(new ResolveTask(device));
			}
			
		}	
	}
	
	private class ResolveTask implements Runnable {
		private BluetoothDevice mDevice;
		private BluetoothSocket bs = null;
		private ObjectInputStream ois = null;
		private ObjectOutputStream oos = null;
		
		public ResolveTask(String device) {
			mDevice = mAdapter.getRemoteDevice(device);
		}
		
		@Override
		public void run() {
			try {
				try {
					bs = mDevice.createInsecureRfcommSocketToServiceRecord(DANDELION_UUID);
				} catch (IOException e) {
					Log.e(TAG, "Cannot create socket to " + mDevice.getAddress());
					e.printStackTrace();
				}
				try {
					bs.connect();
					Log.d(TAG, "Connected to " + mDevice.getAddress());
				} catch (IOException e) {
					Log.e(TAG, "Cannot connect to device " + mDevice.getAddress());
					//Log.e(TAG, "*****"  + (e.getMessage()!=null?e.getMessage():e.toString()) + "*****");
					return;
				}
				try {
					//oos = new ObjectOutputStream(new BufferedOutputStream(bs.getOutputStream()));
					oos = new ObjectOutputStream(bs.getOutputStream());
					//Log.d(TAG, "Setup Output stream to " + mDevice.getAddress());
					//ois = new ObjectInputStream(new BufferedInputStream(bs.getInputStream()));
					ois = new ObjectInputStream(bs.getInputStream());
					//Log.d(TAG, "Setup Input stream to " + mDevice.getAddress());
				} catch (IOException e) {
					Log.e(TAG, "Cannot setup streams to device " + mDevice.getAddress());
					return;
				}
				mDeviceList.addIfAbsent(bs.getRemoteDevice().getAddress(), ois, oos);
				bsMap.put(bs.getRemoteDevice().getAddress(), bs);
				Log.d(TAG, "Streams to " + mDevice.getAddress() + " established");
			} catch (Exception e){				
				e.printStackTrace();
			}
			
		}
	}
	
	private void startServer() {
		synchronized (isServerShutdown) {
			if (isServerShutdown) {
				serverThread = new BluetoothServiceServer();
				isServerShutdown = false;
				serverThread.start();
			}
		}
	}
	
	@Override
	public void shutdownServer() {
		try {
			synchronized (isServerShutdown) {
				if (!isServerShutdown) {
					isServerShutdown = true;
					bss.close();
					serverThread = null;
				}
			}
		} catch (IOException e) {
			
		}
	}
	
	private class BluetoothServiceServer extends Thread {
		
		private BluetoothSocket mSocket = null;
		private DynamicObjectInputStream ois = null;
		private ObjectOutputStream oos = null;

		@Override
		public void run() {
/*			try {
				bss = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, DANDELION_UUID);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
*/
			while(!isServerShutdown) {
				try {
					//if (bss == null) break;
					bss = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, DANDELION_UUID);
					Log.d(TAG, "Waiting for connection");
					mSocket = bss.accept();
					if (mSocket != null) {
						Log.d(TAG, "Socket connected...");
						//ois = new DynamicObjectInputStream(new BufferedInputStream(mSocket.getInputStream()));
						ois = new DynamicObjectInputStream(mSocket.getInputStream());
						Log.d(TAG, "Server Setup Input stream to " + mSocket.getRemoteDevice().getAddress());
						//oos = new ObjectOutputStream(new BufferedOutputStream(mSocket.getOutputStream()));
						oos = new ObjectOutputStream(mSocket.getOutputStream());
						Log.d(TAG, "Server Setup Output stream to " + mSocket.getRemoteDevice().getAddress());
						Thread t = new NodeServer(new ServerStreams(ois, oos), mContext);
						t.start();
						try {
							t.join();
							//mSocket.close();
							bss.close();
							
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					else {
						Log.d(TAG, "NULL Socket connected");
					}
				} catch (IOException e) {
					Log.e(TAG, "ServerSocket clossed");
				}
				
				/*
				occupied = true;
				synchronized(lock) {
		            while (occupied) {
						try {
							lock.wait();
						} catch (InterruptedException e) {
							break;
						}
		            }
		        }
				*/
			}
		}
	}
	
	public void resumeServer() {
		synchronized(lock) {
			occupied = false;
			lock.notifyAll();
		}
    }

	@Override
	public void send(String target, DataPackage data) throws ConnectionLostException {
		ObjectOutputStream oos = mDeviceList.getOutputStream(target);
		if (oos == null) {
			Log.e(TAG, "Node Data lost");
			throw new ConnectionLostException("Cannot find OutputStream", null);
		}
		try {
			Long startTime = System.nanoTime();
			oos.writeObject(data);
			oos.flush();
			Long transmissionTime = System.nanoTime() - startTime;
			Log.d(TAG, "Transmit what: " + data.what + " Takes " + transmissionTime/1000000 + "ms");
		} catch (IOException e) {
			Log.e(TAG, "Offloader Transmission Error");
			//Log.e(TAG, e.toString() + " " + e.getMessage());
			//e.printStackTrace();
			//ConnectionLostException cle = new ConnectionLostException("Send lost****", e);
			//Log.e(TAG, "Create new ConnectionLostException");
			//String errorMessage = e.getMessage();
			throw new ConnectionLostException(((e.getMessage()==null)?"":e.getMessage()), e);
		} 
	}

	@Override
	public DataPackage receive(String target) throws ConnectionLostException {
		ObjectInputStream ois = mDeviceList.getInputStream(target);
		DataPackage ret = null;
		if (ois == null) {
			Log.e(TAG, "Node Data lost");
			throw new ConnectionLostException("Cannot find InputStream", null);
		}
		try {
			ret = (DataPackage) ois.readObject();
			if (ret == null) {
				send(target, DataPackage.obtain(Constants.PING));
				if (((DataPackage) ois.readObject()).what != Constants.PONG) {
					mDeviceList.removeNode(target);
					throw new ConnectionLostException("PING not response properly", null);
				}
			}
		} catch (OptionalDataException e) {
			throw new ConnectionLostException(((e.getMessage()==null)?"":e.getMessage()), e);
		} catch (ClassNotFoundException e) {
			throw new ConnectionLostException(((e.getMessage()==null)?"":e.getMessage()), e);
		} catch (IOException e) {
			throw new ConnectionLostException(((e.getMessage()==null)?"":e.getMessage()), e);
		}
		return ret;
	}
	

	@Override
	public void discoverService() {
		resolveConnections();
		/*
		Set<BluetoothDevice> pairedDevices = mAdapter.getBondedDevices();
		// If there are paired devices
		if (pairedDevices.size() > 0) {
		    // Loop through paired devices
		    for (BluetoothDevice device : pairedDevices) {
		        // Add the name and address to an array adapter to show in a ListView
		    	tempDeviceList.addIfAbsent(device);
		    }
		}
		registerReceiver();
		resolved = false;
		mAdapter.startDiscovery();
		*/
	}

	@Override
	public void stopDiscovery() {
		mAdapter.cancelDiscovery();
		
	}

	@Override
	public String getMacAsId() {
		return mAdapter.getAddress();
	}

}
