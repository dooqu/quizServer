package com.dooqu.quiz.data;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Subject {
    protected String title;
    protected String optionA;
    protected String optionB;
    protected String optionC;
    protected String optionD;
    protected String optionE;

    protected List<String> options;
    protected int correctIndex;
    protected int index;
    protected int key;

    @Override
    public String toString() {
        String subString = "第" + (index + 1) + "题，" + title + "。";

        for(int i = 0; i < options.size(); i++ ) {
            subString += "选项" + (i + 1) + "，" + options.get(i) + "；";
        }
        subString += "请回答。";

        return subString;
    }
}
