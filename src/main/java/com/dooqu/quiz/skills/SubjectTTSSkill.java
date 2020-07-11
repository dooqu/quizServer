package com.dooqu.quiz.skills;

import com.dooqu.quiz.data.Subject;
import com.dooqu.quiz.common.Client;

public class SubjectTTSSkill extends XiaoiceTTSSkill {
    Subject subject;
    int subjectIndex;

    public SubjectTTSSkill(Subject subject, int index, Client... client) {
        super(generateSubjectStatement(subject, index), 1000, client);
        this.subject = subject;
        this.subjectIndex = index;

    }

    static String generateSubjectStatement(Subject subject, int index) {
        String s = "第" + (index + 1) + "题，" + subject.getTitle() + "。";
        s += ("".equals(subject.getOptionA().trim()) == false) ? "选项一、" + subject.getOptionA() + "；" : "";
        s += ("".equals(subject.getOptionB().trim()) == false) ? "选项二、" + subject.getOptionB() + "；" : "";
        s += ("".equals(subject.getOptionC().trim()) == false) ? "选项三、" + subject.getOptionC() + "；" : "";
        s += ("".equals(subject.getOptionD().trim()) == false) ? "选项四、" + subject.getOptionD() + "；" : "";
        s += ("".equals(subject.getOptionE().trim()) == false) ? "选项五、" + subject.getOptionE() + "；" : "";
        s += "，请开始答题。";
        return s;
    }

    @Override
    protected void onBeforePushStream() {
        super.onBeforePushStream();
        synchronized (sessionList) {
            for (Client client : sessionList) {
                client.sendText("TIT " + subjectIndex + " " + subject.getTitle().replace(" ", "%20"));
                if ("".equals(subject.getOptionA().trim()) == false) {
                    client.sendText("OPT 0 " + subject.getOptionA());
                }
                if ("".equals(subject.getOptionB().trim()) == false) {
                    client.sendText("OPT 1 " + subject.getOptionB());
                }
                if ("".equals(subject.getOptionC().trim()) == false) {
                    client.sendText("OPT 2 " + subject.getOptionC());
                }
                if ("".equals(subject.getOptionD().trim()) == false) {
                    client.sendText("OPT 3 " + subject.getOptionD());
                }
                if ("".equals(subject.getOptionE().trim()) == false) {
                    client.sendText("OPT 4 " + subject.getOptionE());
                }
            }
        }
    }
}
