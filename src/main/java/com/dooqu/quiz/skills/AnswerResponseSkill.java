package com.dooqu.quiz.skills;

import com.dooqu.quiz.common.Client;

import java.util.Random;

public class AnswerResponseSkill extends XiaoiceTTSSkill {
    static String[] rightKeyResponseStrings = {"恭喜你，答对了。", "回答正确。", "厉害，答对了。"};
    static String[] errorKeyResponseStrings = {"很遗憾，答错了，", "哎呀答错了，", "不对哦，"};

    static Random random = new Random();
    public AnswerResponseSkill(int rightKey, int userKey, Client... sessions) {
        super(generateResponseString(rightKey, userKey), sessions);
    }

    public static String generateResponseString(int rightKey, int userKey) {
        if(rightKey == userKey) {
            return rightKeyResponseStrings[random.nextInt(rightKeyResponseStrings.length)];
        }
        else {
            return errorKeyResponseStrings[random.nextInt(errorKeyResponseStrings.length)] + "正确选项是，选项" + (rightKey + 1) + "。";
        }
    }
}
