package com.quiz_generator.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void sendWelcomeEmail(String toEmail, String username) {
        try {
            Context context = new Context();
            context.setVariable("username", username);

            String htmlContent = templateEngine.process("email/welcome-email", context);
            sendHtmlEmail(toEmail, "Welcome to QuizGen", htmlContent);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}", toEmail, e);
        }
    }

    public void sendQuizResultEmail(String toEmail,
                                    String username,
                                    String topic,
                                    int score,
                                    int totalQuestions,
                                    String feedback) {
        try {
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("topic", topic);
            context.setVariable("score", score);
            context.setVariable("totalQuestions", totalQuestions);
            context.setVariable("feedback", feedback);

            String htmlContent = templateEngine.process("email/quiz-result-email", context);
            sendHtmlEmail(toEmail, "Your Quiz Result - " + topic, htmlContent);
        } catch (Exception e) {
            log.error("Failed to send quiz result email to {}", toEmail, e);
        }
    }

    private void sendHtmlEmail(String toEmail, String subject, String htmlContent) throws Exception {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

        helper.setFrom(fromAddress);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(mimeMessage);
        log.info("Email sent to {} with subject {}", toEmail, subject);
    }
}
