package com.dooqu.quiz.common;

import com.dooqu.quiz.utils.StreamUtils;
import com.dooqu.quiz.utils.ThreadUtils;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Skill {
    static Logger logger = Logger.getLogger(Skill.class.getSimpleName());
    static int bytesSizePerMillionSecond = 16;
    static int FRAME_DATA_SIZE = bytesSizePerMillionSecond * 10;
    static int FRAME_DATA_WRITE_TO_USER = 160 * 2;

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
    protected int byteBufferSize;
    protected String skillId;
    protected SkillCallback callback;

    public Skill() {
        this(1280 * 32, null);
    }

    public Skill(Client... clients) {
        this(1280 * 32, clients);
    }

    public Skill(int byteBufferSize, Client... clients) {
        skillId = UUID.randomUUID().toString().replace("-", "");
        this.byteBufferSize = byteBufferSize;
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

    protected void initializePipedStream() {
        streamConsumer = new PipedInputStream(byteBufferSize);
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
            close();
        }
        initializePipedStream();
        if (onStart() == true) {
            running = true;
            onBeforePushStream();
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
            byte[] buffer = new byte[FRAME_DATA_WRITE_TO_USER];
            int bytesReaded = -1;
            int bytesReadedTotal = 0;
            int sleepTimeTotal = 0;
            long skillStartTime = System.currentTimeMillis();
            Skill currentSkill = null;
            do {
                currentSkill = skillWeakReference.get();
                if (currentSkill == null) {
                    break;
                }
                try {
                    bytesReaded = currentSkill.streamConsumer.read(buffer, 0, buffer.length);
                    if (bytesReaded > 0) {
                        bytesReadedTotal += bytesReaded;
                        synchronized (currentSkill.sessionList) {
                            if (currentSkill.sessionList.size() <= 0) {
                                break;  //have no clients.
                            }
                            Iterator<Client> iterator = currentSkill.sessionList.iterator();
                            while (iterator.hasNext()) {
                                Client currSession = iterator.next();
                                if (currSession != null && currSession.isOpen()) {
                                    currSession.sendBinary(buffer, 0, bytesReaded);
                                }
                            }
                        }
                        if (currentSkill.contentWriteComplete && bytesReadedTotal >= currentSkill.contentSizeTotal) {
                            //如果生产线程的数据已经全部写入 && 当前从生产者线程读到的数据数量 == contentSizeTotal
                            //那么说明全部数据都已经读取完成。
                            break;
                        }
                        else {
/*                            long timeSpan = System.currentTimeMillis() - skillStartTime;
                            long bytesShouldWrited = timeSpan * 16;
                            long bytesModified = bytesReadedTotal - bytesShouldWrited;
                            if(bytesModified > 16) {
                                long sleetTime = (long)Math.ceil((double)bytesModified / 16d);
                                sleepTimeTotal += sleetTime;
                                System.out.println("timeSpan=" + timeSpan + ", bytesShouldWrited=" + bytesShouldWrited + ", bytesReadedTotal=" + bytesReadedTotal + ", bytesModified=" + (bytesModified) + ",sleepTime=" + sleetTime);
                                ThreadUtil.safeSleep(sleetTime);
                            }
                            else {
                                System.out.println("not sleept");
                            }*/
                            long timeUseTotal = (long) ((double) bytesReadedTotal / (double) bytesSizePerMillionSecond);
                            long timeSpanNow = System.currentTimeMillis() - skillStartTime;
                            long timeDiff = timeUseTotal - timeSpanNow;
                            if (timeDiff > 0) {
                                ThreadUtils.safeSleep(timeDiff);
                            }
                        }
                    }
                } catch (IOException ex) {
                    logger.log(Level.WARNING, "error at thread that write data to client: " + ex.toString());
                    break;
                }
            } while (currentSkill.isRunning() && bytesReaded != -1);
            System.out.println("consumer thread over: TimeSpanTotal=" + (System.currentTimeMillis() - skillStartTime) + ",bytesReadedTotal=" + bytesReadedTotal + ",sleepTimeTotal=" + sleepTimeTotal);
            boolean isRunning = currentSkill.isRunning();
            currentSkill.stopRunning();//让生产者那边停止写入
            currentSkill.onSkillComplete(currentSkill.contentWriteComplete ? 0 : (isRunning) ? -1 : 1, 0);
        }
    }

    protected void pushSkillStream() {
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

    public void close() {
        System.out.println("stop");
        stopRunning();
        ThreadUtils.safeJoin(consumeThread);
        onStop();
        sessionList.clear();
        StreamUtils.safeClose(streamPruducer);
        StreamUtils.safeClose(streamConsumer);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        logger.log(Level.INFO, "Skill.finalize()");
    }
}
