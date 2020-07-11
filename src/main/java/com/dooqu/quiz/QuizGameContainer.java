package com.dooqu.quiz;

import com.dooqu.quiz.asr.ASRIntent;
import com.dooqu.quiz.common.GameContainer;
import com.dooqu.quiz.common.Client;
import com.dooqu.quiz.common.Skill;
import com.dooqu.quiz.data.Subject;
import com.dooqu.quiz.skills.AnswerResponseSkill;
import com.dooqu.quiz.skills.SubjectTTSSkill;
import com.dooqu.quiz.skills.XiaoiceTTSSkill;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.Reader;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QuizGameContainer extends GameContainer {
    static Logger logger = Logger.getLogger(GameClient.class.getSimpleName());
    List<Subject> subjectPool;
    Subject currentSubject;
    int index = 0;
    Random random = new Random();
    @Override
    public void onLoad() {
        super.onLoad();
        logger.log(Level.INFO, "onLoad");
        Reader reader = null;
        try {
            reader = Resources.getResourceAsReader("mybatis-config.xml");
        }
        catch (Exception ex) {
            System.out.println(ex.toString());
        }
        SqlSessionFactory sessionFactory = new SqlSessionFactoryBuilder().build(reader);
        SqlSession session=sessionFactory.openSession();
        subjectPool = session.selectList("listAllSubjects");
        session.close();

        logger.log(Level.INFO, "list subjects size is:" + subjectPool.size());

    }

    @Override
    public void onUnload() {
        super.onUnload();
    }

    @Override
    public void onClientJoin(Client client) {
        super.onClientJoin(client);
        index = 0;
        logger.log(Level.INFO, "onClientJoin:" + client.getId());
        QuizProcessInfo quizProcessInfo = new QuizProcessInfo();
        client.setProcessInfo(quizProcessInfo);
        client.setGameContainer(this);
        client.sendText("JRM");
    }

    @Override
    public void onClientLeave(Client client, int reason) {
        super.onClientLeave(client, reason);
        client.setCurrentSkill(null);
        client.setGameContainer(null);
        client.setProcessInfo(null);
        client.sendText("LRM");
        logger.log(Level.INFO, "onClientLeave:" + client.getId() + ",reason=" + reason);
    }

    protected void onGameStart(Client client) {
        client.sendText("STT");
        currentSubject = subjectPool.get(random.nextInt(subjectPool.size() - 1));
        SubjectTTSSkill skill = new SubjectTTSSkill(currentSubject, index++, client) {
            @Override
            protected void onSkillComplete(int code, int skillIndex) {
                super.onSkillComplete(code, skillIndex);
                if(code == 0 && client.isGoingToLeave() == false && client.isOpen()) {
                    client.startASRSession();
                }
                this.close();
            }
        };
        skill.start();
    }

    @Override
    public void onSessionBinaryData(Client session, byte[] byteArray) {
        super.onSessionBinaryData(session, byteArray);
    }

    @Override
    public void onSessionASRResult(Client client, boolean success, boolean isFinalResult, String asrResultString) {
        System.out.println("QuizGameContainer.onSessionASRResult:" + client.getId() + ",asrResultString = " + asrResultString);
        if(success == false || client.isOpen() == false || client.isGoingToLeave()) {
            System.out.println("客户端已经离开");
            return;
        }
        ASRIntent intent = ASRIntent.parse(asrResultString);
        onClientIntent(client, isFinalResult, intent);
    }

    protected void onClientIntent(Client client, boolean isFinalResult, ASRIntent intent) {
        long startTime = System.currentTimeMillis();
        client.sendText("URS " + intent.getAction() + " " + (isFinalResult? intent.getMatchedString() : intent.getActionString()));
        System.out.println("QuizGameContainer.onClientIntent: isFinalResult=" + isFinalResult + "intent=" + intent.getActionString());
        if(isFinalResult) {
            System.out.println("公布答案");
            new AnswerResponseSkill(currentSubject.getKey(), intent.getAction(), client) {
                @Override
                protected void onSkillComplete(int code, int skillIndex) {
                    super.onSkillComplete(code, skillIndex);
                    if (code == 0 && client.isOpen() && client.isGoingToLeave() == false) {
                        if (index >= 5) {
                            client.sendText("STP");
                            index = 0;
                        }
                        else {
                            onGameStart(client);
                        }
                    }
                }
            }.start();
        }
    }

    @Override
    public void onSessionTextData(Client client, String dataString) {
        super.onSessionTextData(client, dataString);
        switch (dataString) {
            case "STT":
                QuizProcessInfo quizProcessInfo = ((QuizProcessInfo)client.getProcessInfo());
                if(quizProcessInfo != null && quizProcessInfo.isGaming() == false) {
                    onGameStart(client);
                }
                break;
        }
    }

    protected void onSingleSkillComplete(Client client, Skill skill, int code, int skillIndex) {
        if(code == 0) {
            client.startASRSession();
        }
    }
}
