package com.dooqu.quiz.common;

import com.dooqu.quiz.utils.ThreadUtil;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Skill {
    static Logger logger = Logger.getLogger(Skill.class.getSimpleName());

    public static interface SkillCallback {
        void invoke(int skillIndex);
    }

    protected PipedOutputStream streamPruducer;
    protected PipedInputStream streamConsumer;
    protected volatile boolean contentWriteComplete;
    private volatile boolean running;
    protected LinkedHashSet<Client> sessionList;
    protected Thread consumeThread;
    protected long contentSizeTotal;
    protected int contentCapacity;
    protected String skillId;
    protected SkillCallback callback;

    public Skill() {
        this(1280 * 32, null);
    }

    public Skill(Client... clients) {
        this(1280 * 32, clients);
    }

    public Skill(long contentCapacity, Client... clients) {
        sessionList = new LinkedHashSet<Client>();
        contentSizeTotal = Long.MAX_VALUE;
        consumeThread = null;
        containSessions(clients);
    }

    public void containSessions(Client... clients) {
        synchronized (sessionList) {
            sessionList.clear();
            if (clients != null && clients.length > 0) {
                for (int i = 0; i < clients.length; i++) {
                    clients[i].setCurrentSkill(this);
                }
            }
        }
    }

    protected void initStream() {
        streamConsumer = new PipedInputStream(1280 * 32);
        try {
            streamPruducer = new PipedOutputStream(streamConsumer);
        } catch (IOException ex) {
            logger.log(Level.WARNING, ex.toString());
        }
    }

    protected void setContentSize(long sizeTotal) {
        contentSizeTotal = sizeTotal;
    }

    protected void setContentSize(boolean completed, long sizeTotal) {
        contentWriteComplete = completed;
        contentSizeTotal = sizeTotal;
    }

    protected long getContentSize() {
        return contentSizeTotal;
    }

    protected abstract boolean onStart();

    protected void produceSkillData(byte[] byteDataArray, int offset, int length) {
        try {
            streamPruducer.write(byteDataArray, offset, length);
            streamPruducer.flush();
        } catch (IOException ex) {
        }
    }

    protected void attachSession(Client session) {
        synchronized (sessionList) {
            if (sessionList.contains(session) == false) {
                sessionList.add(session);
            }
        }
    }

    protected void detachSession(Client session) {
        synchronized (sessionList) {
            if (sessionList.contains(session) == true) {
                sessionList.remove(session);
                if (sessionList.size() <= 0) {
                    //暂时去掉，因为在while循环了做了判定
                    //stopRunning();
                }
            }
        }
    }

    protected void stopRunning() {
        running = false;
    }

    protected boolean isRunning() {
        return running;
    }


    public void start() {
        if (isRunning()) {
            stop();
        }
        if (onStart() == true) {
            running = true;
            skillId = UUID.randomUUID().toString().replace("-", "");
            initStream();
            pushSkillStream();
        }
    }

    static class SkillConsumerThread extends Thread {
        protected WeakReference<Skill> skillWeakReference;

        public SkillConsumerThread(Skill skill) {
            super("SkillConsumerThread");
            skillWeakReference = new WeakReference<>(skill);
        }

        @Override
        public void run() {
            super.run();
            Skill skill = skillWeakReference.get();
            if (skill == null) {
                return;
            }
            skill.onBeforePushStream();
            byte[] buffer = new byte[160];
            int bytesReaded = -1;
            int bytesTotal = 0;

            do {
                skill = skillWeakReference.get();
                if (skill == null) {
                    break;
                }
                int bytesAvailable = 0;
                try {
                    bytesAvailable = skill.streamConsumer.available();
                } catch (IOException ex) {
                }
                if (bytesAvailable <= 0) {
                    ThreadUtil.safeSleep(5);
                    continue;
                }
                try {
                    bytesReaded = skill.streamConsumer.read(buffer, 0, buffer.length);
                    if (bytesReaded > 0) {
                        bytesTotal += bytesReaded;
                        //System.out.println("consumer thread write to session" + bytesReaded + ",total=" + bytesTotal);
                        synchronized (skill.sessionList) {
                            if (skill.sessionList.size() <= 0) {
                                //如果列表没有人了
                                break;
                            }
                            Iterator<Client> iterator = skill.sessionList.iterator();
                            while (iterator.hasNext()) {
                                Client currSession = iterator.next();
                                if (currSession != null && currSession.isOpen()) {
                                    currSession.sendBinary(buffer, 0, bytesReaded);
                                }
                            }
                        }
                        if (skill.contentWriteComplete && bytesTotal >= skill.contentSizeTotal) {
                            break;
                        } else {
                            ThreadUtil.safeSleep(9);
                        }
                    }
                } catch (IOException ex) {
                    break;
                }
            } while (skill.isRunning());
            boolean isRunning = skill.isRunning();
            skill.stopRunning();//让生产者那边停止写入
            skill.onSkillComplete(skill.contentWriteComplete ? 0 : (isRunning) ? -1 : 1, 0);
        }
    }

    protected void pushSkillStream() {
        /*consumeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                onBeforSkillStreamPush();
                byte[] buffer = new byte[160];
                int bytesReaded = -1;
                int bytesTotal = 0;

                do {
                    int bytesAvailable = 0;
                    try {
                        bytesAvailable = streamConsumer.available();
                    } catch (IOException ex) {
                    }
                    if (bytesAvailable <= 0) {
                        ThreadUtil.safeSleep(5);
                        continue;
                    }
                    try {
                        bytesReaded = streamConsumer.read(buffer, 0, buffer.length);
                        if (bytesReaded > 0) {
                            bytesTotal += bytesReaded;
                            System.out.println("consumer thread write to session" + bytesReaded + ",total=" + bytesTotal);
                            synchronized (sessionList) {
                                if (sessionList.size() <= 0) {
                                    //如果列表没有人了
                                    break;
                                }
                                Iterator<Client> iterator = sessionList.iterator();
                                while (iterator.hasNext()) {
                                    Client currSession = iterator.next();
                                    if (currSession != null && currSession.isOpen()) {
                                        currSession.sendBinary(buffer, 0, bytesReaded);
                                    }
                                }
                            }
                            if (contentWriteComplete && bytesTotal >= contentSizeTotal) {
                                break;
                            }
                            else {
                                ThreadUtil.safeSleep(9);
                            }
                        }
                    } catch (IOException ex) {
                        break;
                    }
                } while (isRunning());
                boolean isRunning = isRunning();
                stopRunning();//让生产者那边停止写入
                onSkillComplete(contentWriteComplete? 0 : (isRunning)? -1 : 1, 0);

            }
        });*/
        consumeThread = new SkillConsumerThread(this);
        consumeThread.start();
    }

    protected void onSkillComplete(int code, int skillIndex) {
        logger.log(Level.INFO, this.toString() + ".onSkillComplete(code=" + code + ", skillIndex=" + skillIndex);
    }

    protected abstract void onStop();

    protected void onBeforePushStream() {
        synchronized (sessionList) {
            for (Client client : sessionList) {
                client.sendText("SKL " + skillId);
            }
        }
    }

    public void stop() {
        System.out.println("stop");
        stopRunning();
        if (consumeThread != null) {
            try {
                consumeThread.join();
            } catch (InterruptedException ex) {
            }
        }
        onStop();
        sessionList.clear();
        try {
            streamPruducer.close();
        } catch (IOException ex) {
        }

        try {
            streamConsumer.close();
        } catch (IOException ex) {
        }
    }
}
