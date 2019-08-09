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

public class ConversationPopulation {
	
	private static final Logger log = LoggerFactory.getLogger(ConversationPopulation.class);
	
	private Map<String, WeakReference<Session>> sessionByUserId;
	private Map<String, String> userIdBySessionId;
	private Map<String, Set<String>> setOfSessionIdsByConversationId;
	private Map<String, String> roomBySessionId;
	private int maxParticipantsPerRoom;
	
	public ConversationPopulation(int maxParticipantsPerRoom) {
		this.sessionByUserId = new ConcurrentHashMap<>();
		this.userIdBySessionId = new ConcurrentHashMap<>();
		this.setOfSessionIdsByConversationId = new ConcurrentHashMap<>();
		this.roomBySessionId = new ConcurrentHashMap<>();
		this.maxParticipantsPerRoom = maxParticipantsPerRoom;
	}
	
	public IConversationPopulationActuator at(String conversationId) {
		return new ConversationPopulationActuator(this, conversationId);
	}

	public void addSessionByUserId(Session session, String sessionId, String userId) {
		if (userId == null || "".equals(userId)) {
			return;
		}
		sessionByUserId.put(userId, new WeakReference<>(session));
		userIdBySessionId.put(sessionId, userId);
	}
	
	public Session getSessionByUserId(String userId) {
		return Optional.ofNullable(sessionByUserId.get(userId)).map( w -> w.get() ).orElse(null);
	}
	
	public String getUserIdBySessionId(String sessionId) {
		return userIdBySessionId.get(sessionId);
	}
	
	public Set<String> getSessionIdsByConversationId(String conversationId) {
		if (conversationId == null) {
			return Collections.emptySet();
		}
		Set<String> sessions = setOfSessionIdsByConversationId.get(conversationId);
		return sessions == null? Collections.emptySet() : sessions;
	}
	
	public void addSessionIdAtConversationId(String conversationId, String sessionId) {
		if (conversationId == null) {
			return;
		}
		
		Set<String> sessionIds = setOfSessionIdsByConversationId.get(conversationId);
		if (sessionIds == null) {
			sessionIds = new HashSet<String>();
			sessionIds.add(sessionId);
			setOfSessionIdsByConversationId.put(conversationId, sessionIds);
		} else {
			sessionIds.add(sessionId);
		}
		
		log.info("Room {} has now {} participant/s.", conversationId, sessionIds.size());
		roomBySessionId.put(sessionId, conversationId);
	}
	
	public boolean removeSessionIdByConversationId(String conversationId, String sessionId) {
		removeSessionForUser(sessionId);
		
		if (conversationId == null) {
			return false;
		}
		
		Set<String> sessionIds = setOfSessionIdsByConversationId.get(conversationId);
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

	public boolean removeConversationIdBySessionId(String sessionId) {
		String conversationId = roomBySessionId.get(sessionId);
		return removeSessionIdByConversationId(conversationId, sessionId);
	}
	
	public int getMaxParticipantsPerRoom() {
		return maxParticipantsPerRoom;
	}

	private void removeSessionForUser(String sessionId) {
		String userId = userIdBySessionId.remove(sessionId);
		if (userId != null) {
			sessionByUserId.remove(userId);
		}
	}
	
}
