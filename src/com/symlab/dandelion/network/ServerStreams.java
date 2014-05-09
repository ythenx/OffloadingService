package com.symlab.dandelion.network;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;

import com.symlab.dandelion.lib.DynamicObjectInputStream;

public class ServerStreams {
	
	private DynamicObjectInputStream objIn;
	private ObjectOutputStream objOut;

	public ServerStreams(DynamicObjectInputStream ois, ObjectOutputStream oos) {
		objIn = ois;
		objOut = oos;
	}
	
	public void send(DataPackage data) {
		try {
			synchronized (objOut) {
				objOut.writeObject(data);
				objOut.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public DataPackage receive() throws IOException {
		DataPackage ret = null;
		try {
			ret = (DataPackage) objIn.readObject();
		} catch (OptionalDataException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} 
		return ret;
	}
	
	public void addDex(File dexFile) {
		objIn.addDex(dexFile);
	}
	
	public void tearDownStream() {
		try {
			objIn.close();
			objOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
