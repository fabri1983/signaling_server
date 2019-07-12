package org.fabri1983.signaling.core.population;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @param <C> conversation id type
 * @param <S> session id type
 * @param <U> user id type
 */
public class ConversationPopulation<C, S, U> {
	
	private static final Logger log = LoggerFactory.getLogger(ConversationPopulation.class);
	
	private Map<U, WeakReference<Session>> sessionByUserId;
	private Map<S, U> userIdBySessionId;
	private Map<C, Set<S>> setOfSessionIdsByConversationId;
	private Map<S, C> roomBySessionId;
	private int maxParticipantsPerRoom;
	
	public ConversationPopulation(int maxParticipantsPerRoom) {
		this.sessionByUserId = new ConcurrentHashMap<>();
		this.userIdBySessionId = new ConcurrentHashMap<>();
		this.setOfSessionIdsByConversationId = new ConcurrentHashMap<>();
		this.roomBySessionId = new ConcurrentHashMap<>();
		this.maxParticipantsPerRoom = maxParticipantsPerRoom;
	}
	
	public IConversationPopulationActuator<C, S, U> at(C conversationId) {
		return new ConversationPopulationActuator<C, S, U>(this, conversationId);
	}

	public void addSessionByUserId(Session session, S sessionId, U userId) {
		if (userId == null || "".equals(userId.toString())) {
			return;
		}
		sessionByUserId.put(userId, new WeakReference<>(session));
		userIdBySessionId.put(sessionId, userId);
	}
	
	public Session getSessionByUserId(U userId) {
		return Optional.ofNullable(sessionByUserId.get(userId)).map( w -> w.get() ).orElse(null);
	}
	
	public U getUserIdBySessionId(S sessionId) {
		return userIdBySessionId.get(sessionId);
	}
	
	public Set<S> getSessionIdsByConversationId(C conversationId) {
		if (conversationId == null) {
			return Collections.emptySet();
		}
		Set<S> sessions = setOfSessionIdsByConversationId.get(conversationId);
		return sessions == null? Collections.emptySet() : sessions;
	}
	
	public void addSessionIdAtConversationId(C conversationId, S sessionId) {
		if (conversationId == null) {
			return;
		}
		
		Set<S> sessionIds = setOfSessionIdsByConversationId.get(conversationId);
		if (sessionIds == null) {
			sessionIds = new HashSet<S>();
			sessionIds.add(sessionId);
			setOfSessionIdsByConversationId.put(conversationId, sessionIds);
		} else {
			sessionIds.add(sessionId);
		}
		
		log.info("Room {} has now {} participant/s.", conversationId, sessionIds.size());
		roomBySessionId.put(sessionId, conversationId);
	}
	
	public boolean removeSessionIdByConversationId(C conversationId, S sessionId) {
		removeSessionForUser(sessionId);
		
		if (conversationId == null) {
			return false;
		}
		
		Set<S> sessionIds = setOfSessionIdsByConversationId.get(conversationId);
		if (sessionIds != null) {
			sessionIds.remove(sessionId);
			// if set is empty then remove it from the map of rooms
			if (sessionIds.isEmpty()) {
				Object sessionIdObj = sessionId;
				setOfSessionIdsByConversationId.remove(sessionIdObj);
			}
			
			log.info("Removed countable participant on session {} from room {}. Room has {} participants", 
					sessionId, conversationId, sessionIds.size());
			return true;
		}
		
		return false;
	}

	public boolean removeConversationIdBySessionId(S sessionId) {
		C conversationId = roomBySessionId.get(sessionId);
		return removeSessionIdByConversationId(conversationId, sessionId);
	}
	
	public int getMaxParticipantsPerRoom() {
		return maxParticipantsPerRoom;
	}

	private void removeSessionForUser(S sessionId) {
		U userId = userIdBySessionId.remove(sessionId);
		if (userId != null) {
			sessionByUserId.remove(userId);
		}
	}
	
}
