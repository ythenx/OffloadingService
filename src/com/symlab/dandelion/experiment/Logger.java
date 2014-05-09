package com.symlab.dandelion.experiment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.os.Environment;

public class Logger {

	File logFileDir = Environment.getExternalStorageDirectory();
	File logFile = new File(logFileDir, "dandelion_log.txt");
	
	public static void log(String record) {
		Logger logger = new Logger();
		if (!logger.logFile.exists()) {
			try {
				logger.logFile.createNewFile();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(logger.logFile, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		BufferedWriter writer = new BufferedWriter(fileWriter);
		try {
			writer.write(record);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
