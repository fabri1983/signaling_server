package org.fabri1983.signaling.core.distributed;

import org.nextrtc.signalingserver.api.NextRTCEvents;

public class DistributedEventTypeResolver {

	public void post(NextRTCEvents eventType, Runnable runner) {
		switch (eventType) {
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
			runner.run();
		default:
			break;
		}
	}

}
