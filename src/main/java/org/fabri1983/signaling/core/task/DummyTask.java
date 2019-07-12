package org.fabri1983.signaling.core.task;

public class DummyTask<U> implements ITask<U> {

	@Override
	public void run() {
	}

	@Override
	public String getId() {
		return "-99-";
	}

	@Override
	public U getUserFrom() {
		return null;
	}
	
}
