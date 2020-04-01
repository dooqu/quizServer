package com.dooqu.quiz.skills;

import com.dooqu.quiz.data.Subject;
import com.dooqu.quiz.common.Client;

public class SubjectTTSSkill extends XiaoiceTTSSkill {
    public SubjectTTSSkill(Subject subject, int index, Client... client) {
        super(generateSubjectStatement(subject, index), client);
    }

    static String generateSubjectStatement(Subject subject, int index) {
        String s =  "第" + (index + 1) + "题，" + subject.getTitle() + "。";
        s += ("".equals(subject.getOptionA().trim()) ==  false)? "选项1，" + subject.getOptionA() + "；" : "";
        s += ("".equals(subject.getOptionB().trim()) ==  false)? "选项2，" + subject.getOptionB() + "；": "";
        s += ("".equals(subject.getOptionC().trim()) ==  false)? "选项3，" + subject.getOptionC() + "；": "";
        s += ("".equals(subject.getOptionD().trim()) ==  false)? "选项4，" + subject.getOptionD() + "；": "";
        s += ("".equals(subject.getOptionE().trim()) ==  false)? "选项5，" + subject.getOptionE() + "；": "";
        s += "，请开始答题。";
        return s;
    }
}
