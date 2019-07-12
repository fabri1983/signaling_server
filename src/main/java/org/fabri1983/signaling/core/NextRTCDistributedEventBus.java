package org.fabri1983.signaling.core;

import org.nextrtc.signalingserver.api.NextRTCEventBus;
import org.nextrtc.signalingserver.api.dto.NextRTCEvent;
import org.nextrtc.signalingserver.domain.EventContext;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

/**
 * This class intended to handle a distributed event bus using a message broker solution over 
 * the already implemented {@link NextRTCEventBus} which is not distributed.
 */
public class NextRTCDistributedEventBus extends NextRTCEventBus implements MessageListener<NextRTCEvent> {

	private ITopic<NextRTCEvent> hzcTopic;
	
    public NextRTCDistributedEventBus(ITopic<NextRTCEvent> hzcTopic) {
        super();
        this.hzcTopic = hzcTopic;
    }

    @Override
    public void post(NextRTCEvent event) {
        super.post(event);
        hzcTopic.publish(event);
    }

    @Override
    public void register(Object listeners) {
        super.register(listeners);
        hzcTopic.addMessageListener(this);
    }
    
    @Override
    public void onMessage(Message<NextRTCEvent> message) {
    	NextRTCEvent event = message.getMessageObject();
    	// EventContext (extends NextRTCEvent) has references to manager objects, which are not serialized, so after 
    	// deserialization phase we have to populate those managers
    	event = populateManagers(event);
    	post(event);
    }

	private NextRTCEvent populateManagers(NextRTCEvent event) {
		if (event instanceof EventContext) {
			// TODO complete here
			return event;
		} else {
			return event;
		}
	}
}
