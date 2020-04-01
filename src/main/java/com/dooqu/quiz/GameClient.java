package com.dooqu.quiz;

import com.dooqu.quiz.asr.ASRProvider;
import com.dooqu.quiz.common.Client;
import com.dooqu.quiz.common.GameContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
@RestController
@ServerEndpoint("/service/{userid}")
public class GameClient extends Client {
    static Set<GameClient> sessions = new LinkedHashSet<>();
    protected javax.websocket.Session session;
    ASRProvider asrProvider;
    protected boolean isGoingToLeave;
    static Logger logger = Logger.getLogger(GameClient.class.getSimpleName());

    public GameClient() {
        asrProvider = new ASRProvider() {
            @Override
            protected void onASRResult(boolean success, String resultString) {
                super.onASRResult(success, resultString);
                GameContainer currentGameGameContainer = getGameContainer();
                if (currentGameGameContainer != null && isOpen()) {
                    currentGameGameContainer.onSessionASRResult(GameClient.this, success, (success) ? resultString : null);
                }
                sendText("ASR 0");
            }

            @Override
            protected void onASRReady() {
                super.onASRReady();
                GameClient.this.sendText("ASR 1");
            }
        };
    }


    @Override
    public void startASRSession() {
        if (this.isOpen()) {
            asrProvider.newSession();
        }
    }


    @Override
    public void sendBinary(byte[] buffer, int offset, int length) {
        if (this.isOpen()) {
            this.session.getAsyncRemote().sendBinary(ByteBuffer.wrap(buffer, offset, length));
        }
    }


    @Override
    public boolean isOpen() {
        return this.session.isOpen();
    }

    public void close() {
        if (this.session.isOpen()) {
            try {
                this.session.close();
            } catch (IOException ex) {
                System.out.println(ex.toString());
            }
        }
    }


    @Override
    public void setLeavingMode() {
        isGoingToLeave = true;
    }


    @Override
    public boolean isGoingToLeave() {
        return isGoingToLeave;
    }


    @Override
    public void sendText(String message) {
        this.session.getAsyncRemote().sendText(message);
    }


    @OnOpen
    public void onOpen(javax.websocket.Session session, @PathParam("userid") String userId) {
        this.session = session;
        onJoin();
    }


    @OnMessage
    public void onBinaryMessage(byte[] dataByteArray, javax.websocket.Session session) {
        if (isOpen() && isGoingToLeave() == false && asrProvider.isConnected()) {
            asrProvider.sendPCMFrame(dataByteArray, dataByteArray.length);
        }
    }


    @OnMessage
    public void onTextMessage(String dataString, Session session) {
        GameContainer gameContainer = this.getGameContainer();
        if (gameContainer != null) {
            gameContainer.onSessionTextData(this, dataString);
        }
    }


    @OnClose
    public void onClose(javax.websocket.Session session) {
        onLeave(1000);
    }


    @OnError
    public void onError(Session session, Throwable error) {
        logger.log(Level.WARNING,"onError");
        error.printStackTrace();
    }


    public void onJoin() {
        logger.log(Level.INFO, "GameClient.onJoin");
        this.setId(session.getId());
        synchronized (sessions) {
            sessions.add(this);
        }
        QuizApplication.quizGameContainer.joinSession(this);
    }


    public void onLeave(int reason) {
        logger.log(Level.INFO, "GameClient.onLeave:" + reason);
        GameContainer currentGameContainer = this.getGameContainer();
        currentGameContainer.removeSession(this, 0);
        synchronized (sessions) {
            sessions.remove(this);
        }
        asrProvider.close(1000, "socket closed");
    }
}
