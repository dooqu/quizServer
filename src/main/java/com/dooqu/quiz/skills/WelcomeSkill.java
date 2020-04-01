package com.dooqu.quiz.skills;

import com.dooqu.quiz.common.Client;

import java.util.Random;

public class WelcomeSkill extends XiaoiceTTSSkill {
    static Random random = new Random();
    static String[] bytes = new String[]{"选手们准备好，竞赛马上开始了！", "准备好了吗！精彩比赛马上开始！", "玩家已经就绪！准备开始比赛！"};

    public WelcomeSkill(Client... sessions) {
        super(generateRandomBye(), sessions);
    }

    static String generateRandomBye() {
        return bytes[random.nextInt(bytes.length)];
    }
}
