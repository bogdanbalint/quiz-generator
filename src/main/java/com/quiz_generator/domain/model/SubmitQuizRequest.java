package com.quiz_generator.domain.model;

import java.util.HashMap;
import java.util.Map;

public class SubmitQuizRequest {

    private Map<Long, String> answers = new HashMap<>();

    public SubmitQuizRequest() {
    }

    public Map<Long, String> getAnswers() {
        return answers;
    }

    public void setAnswers(Map<Long, String> answers) {
        this.answers = answers;
    }
}
