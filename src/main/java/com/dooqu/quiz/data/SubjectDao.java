package com.dooqu.quiz.data;

import java.util.List;

public interface SubjectDao {
    //@Select("select title, option_a, option_b, option_c, option_d, option_e, `key`  from game_ask where option_e <> '' limit 1000")
    List<Subject> listAllSubjects();
}
