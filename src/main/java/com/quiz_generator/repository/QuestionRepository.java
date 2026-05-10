package com.quiz_generator.repository;

import com.quiz_generator.domain.entity.Question;
import com.quiz_generator.domain.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByQuiz(Quiz quiz);
}
