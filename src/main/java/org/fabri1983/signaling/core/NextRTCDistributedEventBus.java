package org.fabri1983.signaling.core;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import org.fabri1983.signaling.util.NextRTCEventWrapper;
import org.nextrtc.signalingserver.api.NextRTCEventBus;
import org.nextrtc.signalingserver.api.dto.NextRTCEvent;
import org.nextrtc.signalingserver.cases.ExchangeSignalsBetweenMembers;
import org.nextrtc.signalingserver.cases.LeftConversation;
import org.nextrtc.signalingserver.domain.MessageSender;

/**
 * This class intended to handle a distributed event bus using a message broker solution over 
 * the already implemented {@link NextRTCEventBus} which is not distributed.
 */
public class NextRTCDistributedEventBus extends NextRTCEventBus implements MessageListener<NextRTCEventWrapper> {

	private ITopic<NextRTCEventWrapper> hzcTopic;
	private NextRTCEventBus eventBus;
	private LeftConversation leftConversation;
	private MessageSender messageSender;
	private ExchangeSignalsBetweenMembers exchange;
	
    public NextRTCDistributedEventBus(ITopic<NextRTCEventWrapper> hzcTopic, NextRTCEventBus eventBus, 
			LeftConversation leftConversation, MessageSender messageSender, 
			ExchangeSignalsBetweenMembers exchange) {
        super();
        this.hzcTopic = hzcTopic;
    }

    @Override
    public void post(NextRTCEvent event) {
        super.post(event);
        NextRTCEventWrapper eventWrapper = NextRTCEventWrapper.wrap(event);
        hzcTopic.publish(eventWrapper);
    }

    @Override
    public void register(Object listeners) {
        super.register(listeners);
        hzcTopic.addMessageListener(this);
    }
    
    @Override
    public void onMessage(Message<NextRTCEventWrapper> message) {
    	NextRTCEventWrapper eventWrapper = message.getMessageObject();
    	NextRTCEvent event = NextRTCEventWrapper.unwrapNow(eventWrapper, eventBus, leftConversation, 
    			messageSender, exchange);
    	post(event);
    }

}
