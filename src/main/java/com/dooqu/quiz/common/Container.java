package com.dooqu.quiz.common;

public interface Container {
    void onLoad();
    void onUnload();
    void onClientJoin(Client client);
    void onClientLeave(Client client, int reason);
}
