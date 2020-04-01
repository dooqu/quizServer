package com.dooqu.quiz.skills;

import com.dooqu.quiz.common.Client;

import java.util.Random;

public class SayByeSkill extends XiaoiceTTSSkill{

    static Random random = new Random();
    static String[] bytes = new String[]{"好的，我也准备学习了，拜拜", "好的，拜拜", "正好我有也有事，先下了"};

    public SayByeSkill(Client... sessions) {
        super(generateRandomBye(), sessions);
    }

    static String generateRandomBye() {
        return bytes[random.nextInt(bytes.length)];
    }
}
