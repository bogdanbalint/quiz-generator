package com.quiz_generator.repository;

import com.quiz_generator.domain.entity.Quiz;
import com.quiz_generator.domain.entity.QuizAttempt;
import com.quiz_generator.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByUserOrderByCompletedAtDesc(User user);

    Optional<QuizAttempt> findByIdAndUser(Long id, User user);

    List<QuizAttempt> findByQuizAndUserOrderByCompletedAtDesc(Quiz quiz, User user);
}
