package com.quiz;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import com.quiz.model.DatabaseModels.*;
import com.quiz.repository.AppRepositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@SpringBootApplication
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@EnableMongoRepositories(basePackages = "com.quiz", considerNestedRepositories = true)
public class QuizBackendApplication {  
   
    @Autowired QuizRepository quizRepo;
    @Autowired UserRepository userRepo;
    @Autowired SubjectRepository subjectRepo;
    @Autowired QuestionRepository questionRepo;
    @Autowired QuizAttemptRepository attemptRepo;

    public static void main(String[] args) {
        SpringApplication.run(QuizBackendApplication.class, args);
    }

    // --- 1. SYSTEM SEEDER (Creates the HOD) ---
    @Bean
    CommandLineRunner initDatabase(UserRepository userRepo) {
        return args -> {
            if (userRepo.findByRole("HOD").isEmpty()) {
                userRepo.save(new AppUser("hod_admin", "admin123", "HOD", "ALL", false));
                System.out.println("✅ Supreme HOD Account Created!");
            }
        };
    }

    // --- 2. AUTHENTICATION & PASSWORD RESET API ---
   @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, String> creds) {
        AppUser user = userRepo.findByUsernameAndPassword(creds.get("username"), creds.get("password"));
        Map<String, Object> res = new HashMap<>();
        
        if (user != null) {
            // --- NEW SUSPENSION LOGIC ---
            if (user.isSuspended) {
                if (System.currentTimeMillis() < user.suspensionEndDate) {
                    // Ban is still active! Calculate remaining days.
                    long daysLeft = (user.suspensionEndDate - System.currentTimeMillis()) / (1000 * 60 * 60 * 24) + 1;
                    res.put("success", false);
                    res.put("isSuspended", true);
                    res.put("message", "ACCOUNT SUSPENDED. " + daysLeft + " days remaining. Reason: " + user.suspensionReason);
                    res.put("assignedClass", user.assignedClass); // <--- ADD THIS LINE!
                    return res;
                } else {
                    // Time is up. Lift the ban automatically!
                    user.isSuspended = false;
                    user.suspensionEndDate = 0;
                    user.suspensionReason = "";
                    userRepo.save(user);
                }
            }

            res.put("success", true);
            res.put("role", user.role);
            res.put("mustChangePassword", user.mustChangePassword);
            res.put("username", user.username);
        } else {
            res.put("success", false);
            res.put("message", "Invalid username or password");
        }
        return res;
    }

    @PostMapping("/auth/change-password")
    public Map<String, String> changePassword(@RequestBody Map<String, String> data) {
        AppUser user = userRepo.findByUsername(data.get("username"));
        user.password = data.get("newPassword");
        user.mustChangePassword = false;
        userRepo.save(user);
        return Collections.singletonMap("message", "Password Updated Successfully!");
    }

   // --- 3. HOD API (Creates Teachers) ---
    @PostMapping("/hod/create-teacher")
    public Map<String, String> createTeacher(@RequestBody Map<String, String> payload) {
        // Manually build the user to avoid JSON mapping errors
        AppUser teacher = new AppUser(
            payload.get("username"),
            payload.get("password"),
            "TEACHER",
            payload.get("assignedClass"),
            true
        );
        userRepo.save(teacher);
        return Collections.singletonMap("message", "Teacher Created. They must reset password on login.");
    }
    @GetMapping("/hod/teachers")
    public List<AppUser> getAllTeachers() {
        return userRepo.findByRole("TEACHER");
    }

    @GetMapping("/hod/students")
    public List<AppUser> getAllStudents() {
        return userRepo.findByRole("STUDENT");
    }
@DeleteMapping("/hod/delete-user/{username}")
    public Map<String, String> deleteUser(@PathVariable String username) {
        AppUser user = userRepo.findByUsername(username);
        if (user != null && !user.role.equals("HOD")) { // Security: Prevent deleting the HOD!
            userRepo.delete(user);
            return Collections.singletonMap("message", "User permanently deleted.");
        }
        return Collections.singletonMap("message", "Error: User not found or protected.");
    }

    @PostMapping("/hod/suspend-user")
    public Map<String, String> suspendUser(@RequestBody Map<String, Object> payload) {
        String username = (String) payload.get("username");
        int days = Integer.parseInt(payload.get("days").toString());
        String reason = (String) payload.get("reason");

        AppUser user = userRepo.findByUsername(username);
        if (user != null && !user.role.equals("HOD")) {
            user.isSuspended = true;
            // Calculate future date in milliseconds (Current Time + Days converted to ms)
            user.suspensionEndDate = System.currentTimeMillis() + ((long) days * 24 * 60 * 60 * 1000);
            user.suspensionReason = reason;
            userRepo.save(user);
            return Collections.singletonMap("message", "User suspended successfully.");
        }
        return Collections.singletonMap("message", "Error: User not found or protected.");
    }
    @PostMapping("/hod/unsuspend-user")
    public Map<String, String> unsuspendUser(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        AppUser user = userRepo.findByUsername(username);
        
        if (user != null) {
            user.isSuspended = false;
            user.suspensionEndDate = 0;
            user.suspensionReason = "";
            userRepo.save(user);
            return Collections.singletonMap("message", "Suspension lifted successfully.");
        }
        return Collections.singletonMap("message", "Error: User not found.");
    }
    // --- 4. TEACHER API (Manages Students, Subjects, Questions) ---
    @PostMapping("/teacher/create-student")
    public Map<String, String> createStudent(@RequestBody Map<String, String> payload) {
        AppUser student = new AppUser(
            payload.get("username"),
            payload.get("password"),
            "STUDENT",
            payload.get("assignedClass"),
            true
        );
        userRepo.save(student);
        return Collections.singletonMap("message", "Student Created Successfully.");
    }
    @GetMapping("/teacher/students")
    public List<AppUser> getStudents() {
        return userRepo.findByRole("STUDENT");
    }

   // --- COURSE MANAGER (SUBJECTS) ---
    @PostMapping("/teacher/create-subject")
    public Subject createSubject(@RequestBody Map<String, String> payload) {
        Subject sub = new Subject();
        sub.name = payload.get("name");
        sub.classLevel = payload.get("classLevel");
        return subjectRepo.save(sub);
    }
@GetMapping("/teacher/subjects")
    public List<Subject> getAllSubjects() {
        return subjectRepo.findAll();
    }
    @DeleteMapping("/teacher/delete-subject/{id}")
    public Map<String, String> deleteSubject(@PathVariable String id) {
        subjectRepo.deleteById(id);
        return Collections.singletonMap("message", "Course deleted.");
    }
    @DeleteMapping("/teacher/delete-student/{username}")
    public Map<String, String> deleteStudent(@PathVariable String username) {
        AppUser user = userRepo.findByUsername(username);
        if (user != null && user.role.equals("STUDENT")) {
            userRepo.delete(user);
            return Collections.singletonMap("message", "Student removed from roster.");
        }
        return Collections.singletonMap("message", "Error finding student.");
    }

    // --- QUIZ MANAGER (NEW) ---
    @PostMapping("/teacher/create-quiz")
    public Quiz createQuiz(@RequestBody Map<String, Object> payload) {
        Quiz q = new Quiz();
        q.subjectId = (String) payload.get("subjectId");
        q.title = (String) payload.get("title");
        q.timeLimitMinutes = Integer.parseInt(payload.get("timeLimitMinutes").toString());
        q.isOpen = false; // Defaults to closed so students can't cheat!
        return quizRepo.save(q);
    }

    @GetMapping("/teacher/quizzes")
    public List<Quiz> getAllQuizzes() {
        return quizRepo.findAll();
    }

    @PostMapping("/teacher/toggle-quiz/{id}")
    public Map<String, String> toggleQuiz(@PathVariable String id) {
        Quiz q = quizRepo.findById(id).orElse(null);
        if (q != null) {
            q.isOpen = !q.isOpen; // Flips true to false, or false to true
            quizRepo.save(q);
            return Collections.singletonMap("message", q.isOpen ? "Quiz is now OPEN" : "Quiz is now CLOSED");
        }
        return Collections.singletonMap("message", "Error finding quiz.");
    }

    @DeleteMapping("/teacher/delete-quiz/{id}")
    public Map<String, String> deleteQuiz(@PathVariable String id) {
        quizRepo.deleteById(id);
        return Collections.singletonMap("message", "Quiz deleted.");
    }

    // --- QUESTION BANK (UPDATED) ---
    @PostMapping("/teacher/create-question")
    public Question createQuestion(@RequestBody Map<String, Object> payload) {
        Question q = new Question();
        q.quizId = (String) payload.get("quizId"); // Now binds to a Quiz, not a Subject
        q.text = (String) payload.get("text");
        
        @SuppressWarnings("unchecked")
        List<String> opts = (List<String>) payload.get("options");
        q.options = opts;
        
        q.correctIndex = (Integer) payload.get("correctIndex");
        return questionRepo.save(q);
    }

    // --- STUDENT PERFORMANCE ANALYTICS (NEW) ---
    @GetMapping("/teacher/student-performance/{username}")
    public List<QuizAttempt> getStudentPerformance(@PathVariable String username) {
        return attemptRepo.findByStudentUsername(username);
    }
  // --- 5. STUDENT API (Takes Quizzes & Views Scores) ---
    
    @GetMapping("/student/subjects")
    public List<Subject> getSubjectsForStudent(@RequestParam(required = false) String classLevel) {
        // Failsafe: Just return ALL subjects to the student, ignoring the class level
        return subjectRepo.findAll();
    }

    // NEW: Fetches only OPEN quizzes for a specific subject
    @GetMapping("/student/quizzes/{subjectId}")
    public List<Quiz> getOpenQuizzesForSubject(@PathVariable String subjectId) {
        List<Quiz> allQuizzes = quizRepo.findBySubjectId(subjectId);
        List<Quiz> openQuizzes = new ArrayList<>();
        for(Quiz q : allQuizzes) {
            if(q.isOpen) openQuizzes.add(q); // Only send quizzes the Teacher has opened!
        }
        return openQuizzes;
    }

    // NEW: Fetches questions for the specific quiz
    @GetMapping("/student/questions/{quizId}")
    public List<Question> getQuizQuestions(@PathVariable String quizId) {
        return questionRepo.findByQuizId(quizId);
    }

    // UPDATED: Evaluates based on quizId and saves the grade!
    @PostMapping("/student/evaluate")
    public Map<String, Object> evaluateQuiz(@RequestBody Map<String, Object> payload) {
        String username = (String) payload.get("username");
        String quizId = (String) payload.get("quizId");

        @SuppressWarnings("unchecked")
        Map<String, Integer> answers = (Map<String, Integer>) payload.get("answers");

        // 1. Fetch questions using the new Quiz ID
        List<Question> dbQuestions = questionRepo.findByQuizId(quizId);

        // 2. Calculate Score
        int score = 0;
        for (Question q : dbQuestions) {
            if (answers.containsKey(q.id)) {
                int studentAnswer = Integer.parseInt(String.valueOf(answers.get(q.id)));
                if (studentAnswer == q.correctIndex) {
                    score++;
                }
            }
        }

        // 3. Save the grade to the database
        QuizAttempt attempt = new QuizAttempt();
        attempt.studentUsername = username;
        attempt.quizId = quizId;
        attempt.score = score;
        attempt.totalQuestions = dbQuestions.size();
        attemptRepo.save(attempt);

        Map<String, Object> res = new HashMap<>();
        res.put("score", score);
        res.put("total", dbQuestions.size());
        return res;
    }

    @GetMapping("/student/performance")
    public List<QuizAttempt> getPerformance(@RequestParam String username) {
        return attemptRepo.findByStudentUsername(username);
    }

    @GetMapping("/questions")
    public List<Question> getAllQuestions() {
        return questionRepo.findAll();
    }
}