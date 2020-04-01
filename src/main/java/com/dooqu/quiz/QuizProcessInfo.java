package com.dooqu.quiz;

import com.dooqu.quiz.common.ProcessInfo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizProcessInfo implements ProcessInfo {
    protected boolean gaming;
    protected QuizMatch matchRomm;
}
