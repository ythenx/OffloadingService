package com.symlab.dandelion.lib;

import java.util.ArrayList;

import com.symlab.dandelion.db.DatabaseQuery;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

public class OffloadableMethod implements Parcelable {
	
	public String appName;
	public MethodPackage methodPackage;
	public ResultTicket resultTicket;
	
	public long execDuration;
	public long energyConsumption;
	public long recordQuantity;
	
	public OffloadableMethod(Context context, String appName, Offloadable receiver, String methodName, Class<?>[] paraTypes, Object[] paraValue, Class<?> reutrnType) {
		this.appName = appName;
		methodPackage = new MethodPackage(receiver, methodName, paraTypes, paraValue);
		resultTicket = new ResultTicket(reutrnType);
		//this.resultListener = resultListener;
		DatabaseQuery query = new DatabaseQuery(context);
		String classMethodName = receiver.getClass().toString() + "#" + methodName;
		ArrayList<String> queryString = query.getData(new String[] {"execDuration", "energyConsumption", "recordQuantity"}, "methodName = ?", 
				new String[] {classMethodName} , null, null, "execDuration", " ASC");
		boolean noResult = (queryString.size() == 0);
		execDuration = noResult ? 0 : Long.parseLong(queryString.get(0));
		energyConsumption = noResult ? 0 : Long.parseLong(queryString.get(1));
		recordQuantity = noResult ? 0 : Long.parseLong(queryString.get(2));
	}
	
	public OffloadableMethod(Context context, String appName, MethodPackage methodPackage, Class<?> reutrnType) {
		this.appName = appName;
		this.methodPackage = methodPackage;
		resultTicket = new ResultTicket(reutrnType);
		//this.resultListener = resultListener;
		DatabaseQuery query = new DatabaseQuery(context);
		String classMethodName = methodPackage.receiver.getClass().toString() + "#" + methodPackage.methodName;
		ArrayList<String> queryString = query.getData(new String[] {"execDuration", "energyConsumption", "recordQuantity"}, "methodName = ?", 
				new String[] {classMethodName} , null, null, "execDuration", " ASC");
		boolean noResult = (queryString.size() == 0);
		execDuration = noResult ? 0 : Long.parseLong(queryString.get(0));
		energyConsumption = noResult ? 0 : Long.parseLong(queryString.get(1));
		recordQuantity = noResult ? 0 : Long.parseLong(queryString.get(2));
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(appName);
		out.writeSerializable(methodPackage);
		out.writeParcelable(resultTicket, flags);
		out.writeLong(execDuration);
		out.writeLong(energyConsumption);
		out.writeLong(recordQuantity);
	}
	
	public static final Parcelable.Creator<OffloadableMethod> CREATOR = new Parcelable.Creator<OffloadableMethod>() {

		@Override
		public OffloadableMethod createFromParcel(Parcel in) {
			return new OffloadableMethod(in);
		}

		@Override
		public OffloadableMethod[] newArray(int size) {
			return new OffloadableMethod[size];
		}
		
	};
	
	private OffloadableMethod(Parcel in) {
		appName = in.readString();
		methodPackage = (MethodPackage) in.readSerializable();
		resultTicket = in.readParcelable(null);
		execDuration = in.readLong();
		energyConsumption = in.readLong();
		recordQuantity = in.readLong();
	}

}
