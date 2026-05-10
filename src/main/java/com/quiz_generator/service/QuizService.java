package com.quiz_generator.service;

import com.quiz_generator.domain.entity.*;
import com.quiz_generator.domain.model.AiGeneratedQuestion;
import com.quiz_generator.domain.model.CreateQuizRequest;
import com.quiz_generator.domain.model.SubmitQuizRequest;
import com.quiz_generator.repository.QuizAttemptRepository;
import com.quiz_generator.repository.QuizRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final EmailService emailService;
    private final OpenAiQuizGenerationService openAiQuizGenerationService;

    public QuizService(QuizRepository quizRepository, QuizAttemptRepository quizAttemptRepository, EmailService emailService, OpenAiQuizGenerationService openAiQuizGenerationService) {
        this.quizRepository = quizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.emailService = emailService;
        this.openAiQuizGenerationService = openAiQuizGenerationService;
    }

    @Transactional
    public Quiz createQuiz(CreateQuizRequest request, User user) {
        validateCreateQuizRequest(request);

        Quiz quiz = new Quiz();
        quiz.setUser(user);
        quiz.setTopic(request.getTopic().trim());
        quiz.setDifficulty(request.getDifficulty());
        quiz.setQuestionCount(request.getQuestionCount());
        quiz.setSourceText(normalizeSourceText(request.getSourceText()));

        var generatedQuestions = openAiQuizGenerationService.generateQuestions(request);

        if (generatedQuestions.size() != request.getQuestionCount()) {
            throw new IllegalArgumentException("AI did not generate the expected number of questions");
        }

        for (var generated : generatedQuestions) {
            validateGeneratedQuestion(generated);

            Question question = new Question();
            question.setQuestionText(generated.getQuestionText().trim());
            question.setOptionA(generated.getOptionA().trim());
            question.setOptionB(generated.getOptionB().trim());
            question.setOptionC(generated.getOptionC().trim());
            question.setOptionD(generated.getOptionD().trim());
            question.setCorrectAnswer(generated.getCorrectAnswer().trim().toUpperCase());
            question.setExplanation(generated.getExplanation() == null ? null : generated.getExplanation().trim());

            quiz.addQuestion(question);
        }

        return quizRepository.save(quiz);
    }

    @Transactional(readOnly = true)
    public Quiz getQuizForUser(Long quizId, User user) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found"));

        if (!quiz.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You do not have access to this quiz");
        }

        return quiz;
    }

    @Transactional(readOnly = true)
    public java.util.List<Quiz> getQuizzesForUser(User user) {
        return quizRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional
    public QuizAttempt submitQuiz(Long quizId, SubmitQuizRequest request, User user) {
        Quiz quiz = getQuizForUser(quizId, user);

        if (request == null || request.getAnswers() == null) {
            throw new IllegalArgumentException("Answers are required");
        }

        int score = 0;
        int totalQuestions = quiz.getQuestions().size();

        QuizAttempt quizAttempt = new QuizAttempt();
        quizAttempt.setQuiz(quiz);
        quizAttempt.setUser(user);
        quizAttempt.setTotalQuestions(totalQuestions);

        for (Question question : quiz.getQuestions()) {
            String selectedAnswer = request.getAnswers().get(question.getId());

            if (selectedAnswer == null || selectedAnswer.isBlank()) {
                throw new IllegalArgumentException("All questions must have an answer");
            }

            boolean isCorrect = question.getCorrectAnswer().equalsIgnoreCase(selectedAnswer.trim());
            if (isCorrect) {
                score++;
            }

            QuestionAttempt questionAttempt = new QuestionAttempt();
            questionAttempt.setQuestion(question);
            questionAttempt.setSelectedAnswer(selectedAnswer.trim().toUpperCase());
            questionAttempt.setIsCorrect(isCorrect);

            quizAttempt.addQuestionAttempt(questionAttempt);
        }

        quizAttempt.setScore(score);
        quizAttempt.setFeedback(buildMockFeedback(score, totalQuestions));

        QuizAttempt savedAttempt = quizAttemptRepository.save(quizAttempt);

        emailService.sendQuizResultEmail(
                user.getEmail(),
                user.getUsername(),
                quiz.getTopic(),
                savedAttempt.getScore(),
                savedAttempt.getTotalQuestions(),
                savedAttempt.getFeedback()
        );

        return savedAttempt;
    }

    @Transactional(readOnly = true)
    public QuizAttempt getQuizAttemptForUser(Long attemptId, User user) {
        return quizAttemptRepository.findByIdAndUser(attemptId, user)
                .orElseThrow(() -> new IllegalArgumentException("Quiz attempt not found"));
    }

    private String buildMockFeedback(int score, int totalQuestions) {
        if (score == totalQuestions) {
            return "Excellent result. You answered all questions correctly.";
        }
        if (score >= Math.ceil(totalQuestions * 0.7)) {
            return "Good job. You have a solid understanding of this topic.";
        }
        if (score >= Math.ceil(totalQuestions * 0.4)) {
            return "Decent attempt. Review the explanations and try again.";
        }
        return "You should review this topic again and retry the quiz.";
    }

    private void validateCreateQuizRequest(CreateQuizRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Invalid quiz request");
        }

        if (isBlank(request.getTopic())) {
            throw new IllegalArgumentException("Topic is required");
        }

        if (request.getTopic().trim().length() > 255) {
            throw new IllegalArgumentException("Topic must be at most 255 characters");
        }

        if (request.getDifficulty() == null) {
            throw new IllegalArgumentException("Difficulty is required");
        }

        if (request.getQuestionCount() == null) {
            throw new IllegalArgumentException("Question count is required");
        }

        if (request.getQuestionCount() < 1 || request.getQuestionCount() > 10) {
            throw new IllegalArgumentException("Question count must be between 1 and 10");
        }

        if (request.getSourceText() != null && request.getSourceText().length() > 5000) {
            throw new IllegalArgumentException("Source text must be at most 5000 characters");
        }
    }

    private String normalizeSourceText(String sourceText) {
        if (sourceText == null || sourceText.trim().isEmpty()) {
            return null;
        }
        return sourceText.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String buildQuestionText(String topic, int index) {
        return "Mock question " + index + " about " + topic + "?";
    }

    private void validateGeneratedQuestion(AiGeneratedQuestion q) {
        if (q.getQuestionText() == null || q.getQuestionText().isBlank()) {
            throw new IllegalArgumentException("AI returned an invalid question");
        }

        if (q.getOptionA() == null || q.getOptionA().isBlank()
                || q.getOptionB() == null || q.getOptionB().isBlank()
                || q.getOptionC() == null || q.getOptionC().isBlank()
                || q.getOptionD() == null || q.getOptionD().isBlank()) {
            throw new IllegalArgumentException("AI returned invalid answer options");
        }

        if (q.getCorrectAnswer() == null
                || !java.util.List.of("A", "B", "C", "D").contains(q.getCorrectAnswer().trim().toUpperCase())) {
            throw new IllegalArgumentException("AI returned an invalid correct answer");
        }
    }
}
