package org.fabri1983.signaling.core.task;

import org.fabri1983.signaling.util.TriConsumer;
import org.nextrtc.signalingserver.domain.Member;

public class TaskActuator<U> {
	
	private ITaskManager<U> taskManager;
	private Member member;
	private U userFrom;
	private TriConsumer<ITask<U>, ITaskManager<U>, U> actionIfExist;
	private TriConsumer<ITask<U>, ITaskManager<U>, U> actionIfNotExist;
	private TriConsumer<Member, ITaskManager<U>, U> actionElsse;
	private boolean runIfExist;
	private boolean runIfNotExist;
	private boolean runElsse;
	
	/**
	 * Private constructor so only using method {@link TaskActuator#getTask(String)} allows the user 
	 * to create an instance.
	 */
	private TaskActuator() {
	}
	
	public static <U> TaskActuator<U> getTask(ITaskManager<U> taskManager, Member member, U userFrom) {
		TaskActuator<U> jobActuator = new TaskActuator<U>();
		jobActuator.taskManager = taskManager;
		jobActuator.member = member;
		jobActuator.userFrom = userFrom;
		return jobActuator;
	}
	
	public TaskActuator<U> ifExist(TriConsumer<ITask<U>, ITaskManager<U>, U> action) {
		this.runIfExist = true;
		this.actionIfExist = action;
		return this;
	}
	
	public TaskActuator<U> ifNotExist(TriConsumer<ITask<U>, ITaskManager<U>, U> action) {
		this.runIfNotExist = true;
		this.actionIfNotExist = action;
		return this;
	}
	
	public TaskActuator<U> elsse(TriConsumer<Member, ITaskManager<U>, U> action) {
		this.runElsse = true;
		this.actionElsse = action;
		return this;
	}
	
	public void go() {
		ITask<U> task = taskManager.get(member.getSession().getId());
		
		if (runIfExist) {
			if (task != null) {
				actionIfExist.accept(task, taskManager, userFrom);
			}
			else if (runElsse) {
				actionElsse.accept(member, taskManager, userFrom);
			}
		} else if (runIfNotExist) {
			if (task == null) {
				actionIfNotExist.accept(task, taskManager, userFrom);
			}
			else if (runElsse) {
				actionElsse.accept(member, taskManager, userFrom);
			}
		}
	}
}
