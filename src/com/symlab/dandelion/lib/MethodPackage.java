package com.symlab.dandelion.lib;

import java.io.Serializable;

public class MethodPackage implements Serializable {

	private static final long serialVersionUID = 1234736759181295962L;

	public Offloadable receiver;
	public String methodName;
	public Class<?>[] paraTypes;
	public Object[] paraValues;
	
	public MethodPackage(Offloadable receiver, String methodName, Class<?>[] paraTypes, Object[] paraValues) {
		this.receiver = receiver;
		this.methodName = methodName;
		this.paraTypes = paraTypes;
		this.paraValues = paraValues;
	}
}
