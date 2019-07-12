package org.fabri1983.signaling.core.handler.signal.pong;

import org.fabri1983.signaling.core.messagesender.ErrorMessageSender;
import org.fabri1983.signaling.core.task.ITask;
import org.fabri1983.signaling.core.task.ITaskManager;
import org.fabri1983.signaling.util.TriConsumer;
import org.nextrtc.signalingserver.domain.Member;
import org.nextrtc.signalingserver.repository.MemberRepository;

public class PongOperations {

	public static <T extends ITask<U>, S extends ITaskManager<U>, U> TriConsumer<T, S, U> reschedule() {
		return (task, manager, userFrom) -> {
			manager.reschedule(task);
		};
	}

	public static <T extends Member, S extends ITaskManager<U>, U> TriConsumer<T, S, U> createAndSchedule(
			ErrorMessageSender errorSender, MemberRepository members) {
		return (member, manager, userFrom) -> {
			ITask<U> task = new PongCloseAllTask<U>(member, userFrom, manager, errorSender, members);
			manager.schedule(task);
		};
	}

}
