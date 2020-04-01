package com.dooqu.quiz.common;

public abstract class Client {
    protected ProcessInfo processInfo;
    protected String id;
    protected long behaviorId;
    protected Skill currentSkill;
    protected GameContainer gameContainer;
    protected int defaultMediaTypeSupported;

    public void setProcessInfo(ProcessInfo processInfo) {
        this.processInfo = processInfo;
    }

    public ProcessInfo getProcessInfo() {
        return processInfo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCurrentSkill(Skill skill) {
        if (currentSkill == skill) {
            return;
        }
        if (currentSkill != null) {
            currentSkill.detachSession(this);
        }
        if (skill != null) {
            skill.attachSession(this);
        }
        this.currentSkill = skill;
    }

    public GameContainer getGameContainer() {
        return this.gameContainer;
    }

    public void setGameContainer(GameContainer gameContainer) {
        this.gameContainer = gameContainer;
    }

    public abstract void sendBinary(byte[] buffer, int offset, int length);

    public abstract void sendText(String message);

    public abstract boolean isOpen();

    public abstract void startASRSession();

    public abstract void close();

    public abstract void setLeavingMode();

    public abstract boolean isGoingToLeave();

    public int getDefaultMediaTypeSupported() {
        return defaultMediaTypeSupported;
    }

    public void setDefaultMediaTypeSupported(int type) {
        defaultMediaTypeSupported = type;
    }
}
