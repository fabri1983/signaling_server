package org.fabri1983.signaling.core.task;

public interface ITask<U> extends Runnable {

	String getId();

	U getUserFrom();
	
}
