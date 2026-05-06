package com.quiz.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

public class DatabaseModels {

    @Document(collection = "users")
    public static class AppUser {
        @Id public String id;
        public boolean isSuspended = false;
        public long suspensionEndDate = 0; 
        public String suspensionReason = "";
        public String username;
        public String password;
        public String role; 
        public String assignedClass; 
        public boolean mustChangePassword; 

        public AppUser() {}
        public AppUser(String username, String password, String role, String assignedClass, boolean mustChangePassword) {
            this.username = username; this.password = password; this.role = role;
            this.assignedClass = assignedClass; this.mustChangePassword = mustChangePassword;
        }
    }

    @Document(collection = "subjects")
    public static class Subject {
        @Id public String id;
        public String name; 
        public String classLevel; 

        public Subject() {}
    }

    @Document(collection = "quizzes")
    public static class Quiz {
        @Id public String id;
        public String subjectId;
        public String title;
        public int timeLimitMinutes;
        public boolean isOpen = false; 
    }

    @Document(collection = "questions")
    public static class Question {
        @Id public String id;
        public String quizId; 
        public String text;
        public List<String> options;
        public int correctIndex;

        public Question() {}
    }

    @Document(collection = "attempts")
    public static class QuizAttempt {
        @Id public String id;
        public String studentUsername;
        public String quizId;
        public int score;
        public int totalQuestions;
    }
}