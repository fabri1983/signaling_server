package org.fabri1983.signaling.core.task;

public class DummyTaskManager<U> implements ITaskManager<U> {

	private ITask<U> dummyTask;
	
	public DummyTaskManager() {
		dummyTask = new DummyTask<U>();
	}
	
	@Override
	public ITask<U> get(String sessionId) {
		return dummyTask;
	}

	@Override
	public void schedule(ITask<U> task) {
	}

	@Override
	public void remove(ITask<U> task) {
	}
	
	@Override
	public void reschedule(ITask<U> task) {
	}
	
}
