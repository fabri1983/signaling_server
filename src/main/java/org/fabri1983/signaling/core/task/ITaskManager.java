package org.fabri1983.signaling.core.task;

public interface ITaskManager<U> {

	ITask<U> get(String sessionId);

	void schedule(ITask<U> task);

	void remove(ITask<U> task);
	
	void reschedule(ITask<U> task);
	
}
