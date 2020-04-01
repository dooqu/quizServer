package com.dooqu.quiz.common;

import java.util.*;

public class GameContainer implements Container{
    Map<String, Client> sessions = new LinkedHashMap<String, Client>();

    public void onLoad() {
    }

    public void onUnload() {
    }

    @Override
    public void onClientJoin(Client session) {
    }

    @Override
    public void onClientLeave(Client session, int reason) {
    }

    public void onSessionBinaryData(Client session, byte[] byteArray) {
    }

    public void onSessionASRResult(Client client, boolean success, String asrResultString) {

    }

    public void onSessionTextData(Client client, String dataString) {

    }

    public int joinSession(Client session) {
        synchronized (sessions) {
            if (sessions.containsKey(session.getId())) {
                return 1;
            }
            sessions.put(session.getId(), session);
            session.setGameContainer(this);
            onClientJoin(session);
        }
        return 0;
    }

    public void removeSession(Client session, int code) {
        synchronized (sessions) {
            if (sessions.containsKey(session.getId())) {
                sessions.remove(session.getId());
                onClientLeave(session, code);
                session.setGameContainer(null);
                session.setCurrentSkill(null);
            }
        }
    }
}
