package org.example;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    public Session get(String sessionId){
        if (sessionId == null){
            return null;
        }
        return sessions.get(sessionId);
    }

    public Session create(){
        String sessionId = UUID.randomUUID().toString();
        Session session = new Session(sessionId);
        sessions.put(sessionId, session);
        return session;
    }

    public void delete(String sessionId){
        if (sessionId != null){
            sessions.remove(sessionId);
        }
    }
}
