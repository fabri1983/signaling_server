package org.fabri1983.signaling.core.task;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TaskManager<U> implements ITaskManager<U> {

	private int timerDelay;
	private ScheduledExecutorService scheduler;
	private Map<String, ITask<U>> taskMap;
	private Map<String, ScheduledFuture<ITask<U>>> futureTaskMap;
	
	public TaskManager(int timerDelay) {
		this.timerDelay = timerDelay;
		scheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
		taskMap = new HashMap<>();
		futureTaskMap = new HashMap<>();
	}

	public ITask<U> get(String sessionId) {
		// sanity clean: if futureTask is cancelled/done then remove it from futureMap
		ScheduledFuture<ITask<U>> futureTask = futureTaskMap.get(sessionId);
		if (futureTask != null && (futureTask.isCancelled() || futureTask.isDone())) {
			futureTaskMap.remove(sessionId);
		}
		return taskMap.get(sessionId);
	}

	@SuppressWarnings("unchecked")
	public void schedule(ITask<U> task) {
		ScheduledFuture<ITask<U>> futureTask = (ScheduledFuture<ITask<U>>) scheduler
				.scheduleAtFixedRate(task, timerDelay, timerDelay, TimeUnit.MILLISECONDS);
		String sessionId = task.getId();
		taskMap.put(sessionId, task);
		futureTaskMap.put(sessionId, futureTask);
	}

	public void remove(ITask<U> task) {
		if (task == null) {
			return;
		}
		
		String sessionId = task.getId();
		ScheduledFuture<ITask<U>> futureTask = futureTaskMap.remove(sessionId);
		if (futureTask != null) {
			futureTask.cancel(false);
		}
		taskMap.remove(sessionId);
	}
	
	public void reschedule(ITask<U> task) {
		remove(task);
		schedule(task);
	}
	
}
