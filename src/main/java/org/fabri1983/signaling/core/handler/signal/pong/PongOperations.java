package org.fabri1983.signaling.core.handler.signal.pong;

import org.fabri1983.signaling.core.messagesender.ErrorMessageSender;
import org.fabri1983.signaling.core.task.ITask;
import org.fabri1983.signaling.core.task.ITaskManager;
import org.fabri1983.signaling.util.TriConsumer;
import org.nextrtc.signalingserver.domain.Member;
import org.nextrtc.signalingserver.repository.MemberRepository;

public class PongOperations {

	public static <T extends ITask<String>, S extends ITaskManager<String>> TriConsumer<T, S, String> reschedule() {
		return (task, manager, userFrom) -> {
			manager.reschedule(task);
		};
	}

	public static <T extends Member, S extends ITaskManager<String>> TriConsumer<T, S, String> createAndSchedule(
			ErrorMessageSender errorSender, MemberRepository members) {
		return (member, manager, userFrom) -> {
			ITask<String> task = new PongCloseAllTask(member, userFrom, manager, errorSender, members);
			manager.schedule(task);
		};
	}

}
