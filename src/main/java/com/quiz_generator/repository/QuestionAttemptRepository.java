package com.quiz_generator.repository;

import com.quiz_generator.domain.entity.QuestionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionAttemptRepository extends JpaRepository<QuestionAttempt, Long> {
}
