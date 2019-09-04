package org.fabri1983.signaling.core.distributed;

import org.nextrtc.signalingserver.api.dto.NextRTCEvent;

public class DistributedEventConditionResolver {

	public void post(NextRTCEvent event, Runnable runner) {
		// continue if event type is one of following
		switch (event.type()) {
		case SESSION_CLOSED:
		case CONVERSATION_CREATED:
		case CONVERSATION_DESTROYED:
		case UNEXPECTED_SITUATION:
		case MEMBER_JOINED:
		case MEMBER_LEFT:
		case MEDIA_LOCAL_STREAM_REQUESTED:
		case MEDIA_LOCAL_STREAM_CREATED:
		case MEDIA_STREAMING:
		case TEXT:
			withConditions(event, runner);
		default:
			break;
		}
	}

	private void withConditions(NextRTCEvent event, Runnable runner) {
		// Perform any condition here to stop execution of the runner
		
		// do what it has to do
		runner.run();
	}

}
