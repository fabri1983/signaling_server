package org.fabri1983.signaling.core.distributed;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import org.fabri1983.signaling.core.distributed.serialization.NextRTCEventWrapper;
import org.fabri1983.signaling.core.population.ConversationPopulation;
import org.nextrtc.signalingserver.api.NextRTCEventBus;
import org.nextrtc.signalingserver.api.dto.NextRTCEvent;
import org.nextrtc.signalingserver.repository.ConversationRepository;
import org.nextrtc.signalingserver.repository.MemberRepository;

/**
 * This class intended to handle a distributed event bus using a message broker
 * solution over the already implemented {@link NextRTCEventBus} which is not
 * distributed.
 */
public class NextRTCDistributedEventBus extends NextRTCEventBus implements MessageListener<NextRTCEventWrapper> {

	private ITopic<NextRTCEventWrapper> hzcTopic;
	private ConversationPopulation population;
	private ConversationRepository conversationRepository;
	private MemberRepository members;
	private DistributedEventTypeResolver eventTypeResolver = new DistributedEventTypeResolver();

	public NextRTCDistributedEventBus(ITopic<NextRTCEventWrapper> hzcTopic, ConversationPopulation population,
			ConversationRepository conversationRepository, MemberRepository members) {
		super();
		this.hzcTopic = hzcTopic;
		this.population = population;
		this.conversationRepository = conversationRepository;
		this.members = members;
	}

	@Override
	public void post(NextRTCEvent event) {
		super.post(event);
		// only post on some specific events
		eventTypeResolver.post(event.type(), () -> {
			NextRTCEventWrapper eventWrapper = NextRTCEventWrapper.wrap(event, population);
			hzcTopic.publish(eventWrapper);
		});
	}

	@Override
	public void register(Object listeners) {
		super.register(listeners);
		hzcTopic.addMessageListener(this);
	}

	@Override
	public void onMessage(Message<NextRTCEventWrapper> message) {
		NextRTCEventWrapper eventWrapper = message.getMessageObject();
		NextRTCEvent event = NextRTCEventWrapper.unwrapNow(eventWrapper, this, population, 
				conversationRepository, members);
		post(event);
	}

}
