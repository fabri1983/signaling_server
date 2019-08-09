package org.fabri1983.signaling.core.distributed;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import org.fabri1983.signaling.core.distributed.serialization.NextRTCEventWrapper;
import org.fabri1983.signaling.core.population.ConversationPopulation;
import org.nextrtc.signalingserver.api.NextRTCEventBus;
import org.nextrtc.signalingserver.api.dto.NextRTCEvent;
import org.nextrtc.signalingserver.cases.ExchangeSignalsBetweenMembers;
import org.nextrtc.signalingserver.cases.LeftConversation;
import org.nextrtc.signalingserver.domain.MessageSender;
import org.nextrtc.signalingserver.repository.MemberRepository;

/**
 * This class intended to handle a distributed event bus using a message broker solution over 
 * the already implemented {@link NextRTCEventBus} which is not distributed.
 */
public class NextRTCDistributedEventBus extends NextRTCEventBus implements MessageListener<NextRTCEventWrapper> {

	private ITopic<NextRTCEventWrapper> hzcTopic;
	private LeftConversation leftConversation;
	private MessageSender messageSender;
	private ExchangeSignalsBetweenMembers exchange;
	private ConversationPopulation population;
	private MemberRepository members;
	
    public NextRTCDistributedEventBus(ITopic<NextRTCEventWrapper> hzcTopic, MessageSender messageSender, 
    		ExchangeSignalsBetweenMembers exchange, ConversationPopulation population, 
    		MemberRepository members) {
        super();
        this.hzcTopic = hzcTopic;
        this.messageSender = messageSender;
        this.exchange = exchange;
        this.population = population;
        this.members = members;
    }

    public void setLeftConversation(LeftConversation leftConversation) {
		this.leftConversation = leftConversation;
	}

	@Override
    public void post(NextRTCEvent event) {
        super.post(event);
        NextRTCEventWrapper eventWrapper = NextRTCEventWrapper.wrap(event, population);
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
    	NextRTCEvent event = NextRTCEventWrapper.unwrapNow(eventWrapper, this, leftConversation, 
    			messageSender, exchange, population, members);
    	post(event);
    }

}
