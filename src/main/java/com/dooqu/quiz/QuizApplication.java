package com.dooqu.quiz;

import com.dooqu.quiz.common.GameContainer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class QuizApplication {
    static GameContainer quizGameContainer = new QuizGameContainer();

    public QuizApplication() {
        quizGameContainer.onLoad();
    }

    public static void main(String[] args) {
        SpringApplication.run(QuizApplication.class, args);

    }
}
