package org.aksw.iguana.utils;

import java.util.Collection;

public class ThreadJoiner implements Runnable{

	Collection<Thread> threads;
	
	public ThreadJoiner(Collection<Thread> t){
		threads = t;
	}
	
	
	public void start(){
		for(Thread t : threads){
			try {
				t.stop();
				t.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		Thread.currentThread().stop();
	}
	
	@Override
	public void run() {
		start();
	}

	
	
}
