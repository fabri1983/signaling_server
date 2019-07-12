package org.fabri1983.signaling.core.population;

public interface IConversationPopulationActuator<C, S, U> {

	boolean hasReachedMax(S sessionId);
	
}
