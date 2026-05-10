package com.quiz_generator.controller;

import com.quiz_generator.domain.entity.Quiz;
import com.quiz_generator.domain.entity.QuizAttempt;
import com.quiz_generator.domain.entity.User;
import com.quiz_generator.domain.model.CreateQuizRequest;
import com.quiz_generator.domain.model.Difficulty;
import com.quiz_generator.domain.model.SubmitQuizRequest;
import com.quiz_generator.repository.UserRepository;
import com.quiz_generator.service.CustomUserDetails;
import com.quiz_generator.service.QuizService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/quizzes")
public class QuizController {

    private final QuizService quizService;
    private final UserRepository userRepository;

    public QuizController(QuizService quizService, UserRepository userRepository) {
        this.quizService = quizService;
        this.userRepository = userRepository;
    }

    @GetMapping("/create")
    public String showCreateQuizPage(Model model) {
        if (!model.containsAttribute("createQuizRequest")) {
            CreateQuizRequest request = new CreateQuizRequest();
            request.setQuestionCount(5);
            request.setDifficulty(Difficulty.MEDIUM);
            model.addAttribute("createQuizRequest", request);
        }

        model.addAttribute("difficulties", Difficulty.values());
        return "create-quiz";
    }

    @PostMapping("/create")
    public String createQuiz(@ModelAttribute("createQuizRequest") CreateQuizRequest createQuizRequest,
                             @AuthenticationPrincipal CustomUserDetails currentUser,
                             Model model) {
        try {
            User user = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

            Quiz quiz = quizService.createQuiz(createQuizRequest, user);
            return "redirect:/quizzes/" + quiz.getId();
        } catch (IllegalArgumentException ex) {
            model.addAttribute("quizError", ex.getMessage());
            model.addAttribute("difficulties", Difficulty.values());
            return "create-quiz";
        }
    }

    @GetMapping("/{id}")
    public String showQuizDetails(@PathVariable("id") Long id,
                                  @AuthenticationPrincipal CustomUserDetails currentUser,
                                  Model model) {
        try {
            User user = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

            Quiz quiz = quizService.getQuizForUser(id, user);
            model.addAttribute("quiz", quiz);
            return "quiz-details";
        } catch (IllegalArgumentException ex) {
            return "redirect:/dashboard";
        }
    }

    @GetMapping
    public String showQuizHistory(@AuthenticationPrincipal CustomUserDetails currentUser,
                                  Model model) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        model.addAttribute("quizzes", quizService.getQuizzesForUser(user));
        return "quiz-history";
    }

    @GetMapping("/{id}/solve")
    public String showSolveQuizPage(@PathVariable("id") Long id,
                                    @AuthenticationPrincipal CustomUserDetails currentUser,
                                    Model model) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        Quiz quiz = quizService.getQuizForUser(id, user);

        SubmitQuizRequest submitQuizRequest = new SubmitQuizRequest();
        model.addAttribute("quiz", quiz);
        model.addAttribute("submitQuizRequest", submitQuizRequest);
        return "solve-quiz";
    }

    @PostMapping("/{id}/solve")
    public String submitQuiz(@PathVariable("id") Long id,
                             @ModelAttribute("submitQuizRequest") SubmitQuizRequest submitQuizRequest,
                             @AuthenticationPrincipal CustomUserDetails currentUser,
                             Model model) {
        try {
            User user = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

            QuizAttempt attempt = quizService.submitQuiz(id, submitQuizRequest, user);
            return "redirect:/quizzes/attempts/" + attempt.getId();
        } catch (IllegalArgumentException ex) {
            User user = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

            Quiz quiz = quizService.getQuizForUser(id, user);
            model.addAttribute("quiz", quiz);
            model.addAttribute("quizError", ex.getMessage());
            return "solve-quiz";
        }
    }

    @GetMapping("/attempts/{attemptId}")
    public String showQuizResult(@PathVariable("attemptId") Long attemptId,
                                 @AuthenticationPrincipal CustomUserDetails currentUser,
                                 Model model) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        QuizAttempt attempt = quizService.getQuizAttemptForUser(attemptId, user);
        model.addAttribute("attempt", attempt);
        return "quiz-result";
    }
}