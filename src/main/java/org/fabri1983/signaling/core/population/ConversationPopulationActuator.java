package org.fabri1983.signaling.core.population;

import java.util.Set;

public class ConversationPopulationActuator<C, S, U> implements IConversationPopulationActuator<C, S, U>{

	private ConversationPopulation<C, S, U> population;
	private C conversationId;
	
	public ConversationPopulationActuator(ConversationPopulation<C, S, U> population, C conversationId) {
		this.population = population;
		this.conversationId = conversationId;
	}
	
	@Override
	public boolean hasReachedMax(S sessionId) {
		Set<S> sessions = population.getSessionIdsByConversationId(conversationId);
		return sessions.size() >= population.getMaxParticipantsPerRoom();
	}
	
}
