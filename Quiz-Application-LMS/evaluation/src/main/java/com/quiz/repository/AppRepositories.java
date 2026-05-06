package com.quiz.repository;

import com.quiz.model.DatabaseModels;
import com.quiz.model.DatabaseModels.*;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public class AppRepositories {
    
    public interface UserRepository extends MongoRepository<AppUser, String> {
        AppUser findByUsername(String username);
        AppUser findByUsernameAndPassword(String username, String password);
        List<AppUser> findByRole(String role);
    }

    public interface SubjectRepository extends MongoRepository<Subject, String> {
        List<Subject> findByClassLevel(String classLevel);
    }

    public interface QuizRepository extends MongoRepository<DatabaseModels.Quiz, String> {
        List<DatabaseModels.Quiz> findBySubjectId(String subjectId);
    }

    public interface QuestionRepository extends MongoRepository<DatabaseModels.Question, String> {
        List<DatabaseModels.Question> findByQuizId(String quizId); 
    }

    public interface QuizAttemptRepository extends MongoRepository<DatabaseModels.QuizAttempt, String> {
        List<DatabaseModels.QuizAttempt> findByStudentUsername(String studentUsername);
    }
}