package com.symlab.dandelion.lib;

import java.util.LinkedList;

public class TaskQueue {

	private LinkedList<OffloadableMethod> queue;
	
	public TaskQueue() {
		queue = new LinkedList<OffloadableMethod>();
	}
	
	public void enqueue(OffloadableMethod m) {
		synchronized (queue) {
			queue.addLast(m);
		}
	}
	
	public OffloadableMethod dequeue() {
		synchronized (queue) {
			return queue.removeFirst();
		}
	}
	
	public int queueSize() {
		synchronized (queue) {
			return queue.size();
		}
	}
	
	public void clearQueue() {
		queue.clear();
	}
	public LinkedList<OffloadableMethod> getQueue(){
		return queue;
	}
}
