package org.fabri1983.signaling.core.population;

import java.util.Set;

public class ConversationPopulationActuator implements IConversationPopulationActuator{

	private ConversationPopulation population;
	private String conversationId;
	
	public ConversationPopulationActuator(ConversationPopulation population, String conversationId) {
		this.population = population;
		this.conversationId = conversationId;
	}
	
	@Override
	public boolean hasReachedMax(String sessionId) {
		Set<String> sessions = population.getSessionIdsByConversationId(conversationId);
		return sessions.size() >= population.getMaxParticipantsPerRoom();
	}
	
}
