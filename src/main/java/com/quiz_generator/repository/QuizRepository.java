package com.quiz_generator.repository;

import com.quiz_generator.domain.entity.Quiz;
import com.quiz_generator.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByUserOrderByCreatedAtDesc(User user);
}
