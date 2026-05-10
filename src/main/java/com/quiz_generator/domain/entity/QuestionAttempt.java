package com.quiz_generator.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "question_attempts")
public class QuestionAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_attempt_id", nullable = false)
    private QuizAttempt quizAttempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "selected_answer", nullable = false, length = 1)
    private String selectedAnswer;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    public QuestionAttempt() {
    }

    public Long getId() {
        return id;
    }

    public QuizAttempt getQuizAttempt() {
        return quizAttempt;
    }

    public Question getQuestion() {
        return question;
    }

    public String getSelectedAnswer() {
        return selectedAnswer;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setQuizAttempt(QuizAttempt quizAttempt) {
        this.quizAttempt = quizAttempt;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public void setSelectedAnswer(String selectedAnswer) {
        this.selectedAnswer = selectedAnswer;
    }

    public void setIsCorrect(Boolean correct) {
        isCorrect = correct;
    }
}
