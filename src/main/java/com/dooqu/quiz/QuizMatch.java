package com.dooqu.quiz;

import com.dooqu.quiz.common.Client;
import com.dooqu.quiz.data.Subject;

import java.util.*;

public class QuizMatch {
    static int MATCH_ROOM_CAPACITY = 2;
    static int SUBJECT_COUNT_PER_MATCH = 5;
    static Random randomInMatch = new Random();
    LinkedHashSet<Client> clients;
    List<Subject> subjects;
    int subjectIndex;
    String matchId;

    public QuizMatch() {
        matchId = UUID.randomUUID().toString().replace("-","");
        clients = new LinkedHashSet<>();
        subjects = new ArrayList<Subject>();
    }

    public String getMatchId() {
        return matchId;
    }

    public boolean joinClient(Client client) {
        if(clients.contains(client) || clients.size() >= MATCH_ROOM_CAPACITY) {
            return false;
        }
        clients.add(client);
        return true;
    }

    public boolean removeClient(Client client) {
        if(clients.contains(client)) {
            clients.remove(client);
            return true;
        }
        return false;
    }

    public boolean contains(Client client) {
        return clients.contains(client);
    }


    public void resetSubjects(List<Subject> subjectPool) {
        subjectIndex = 0;
        subjects.clear();
        if(subjectPool != null && subjectPool.size() > SUBJECT_COUNT_PER_MATCH) {
            for(int i = 0; i < SUBJECT_COUNT_PER_MATCH; i++ ) {
                int randomIndex = randomInMatch.nextInt(subjectPool.size() - 1);
                subjects.add(subjectPool.get(randomIndex));
            }
        }
    }
}
