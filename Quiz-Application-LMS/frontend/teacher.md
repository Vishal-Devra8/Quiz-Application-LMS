<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Teacher Dashboard | LMS</title>
    <link rel="stylesheet" href="styles.css">
    <style>
        .dashboard-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; max-width: 1000px; width: 100%; align-items: start; }
        .panel { background: var(--card-bg); padding: 25px; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.05); }
        h3 { border-bottom: 2px solid var(--bg-color); padding-bottom: 10px; margin-top: 0; }
        select { width: 100%; padding: 12px; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 1rem; margin-bottom: 15px;}
    </style>
</head>
<body style="padding: 40px 20px; align-items: flex-start;">

    <div style="max-width: 1000px; width: 100%;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
            <h2>Faculty Dashboard: <span id="teacher-name"></span></h2>
            <button class="btn" style="width: auto; background: var(--danger);" onclick="logout()">Logout</button>
        </div>

        <div class="dashboard-grid">
            
            <div class="panel">
                <h3>1. Register New Student</h3>
                <div class="form-group"><input type="text" id="s-user" placeholder="Student Username"></div>
                <div class="form-group"><input type="password" id="s-pass" placeholder="Initial Password"></div>
                <div class="form-group"><input type="text" id="s-class" placeholder="Class (e.g., 1st Year MCA)"></div>
                <button class="btn btn-success" onclick="createStudent()">Add Student</button>
                <div id="student-alert" class="alert"></div>
            </div>

            <div class="panel">
                <h3>2. Create Subject & Rules</h3>
                <div class="form-group"><input type="text" id="sub-name" placeholder="Subject Name (e.g., Data Science)"></div>
                <div class="form-group"><input type="text" id="sub-class" placeholder="Target Class (e.g., 1st Year MCA)"></div>
                <div class="form-group"><input type="number" id="sub-time" placeholder="Time Limit (in Minutes)"></div>
                <button class="btn btn-success" onclick="createSubject()">Save Subject</button>
                <div id="subject-alert" class="alert"></div>
            </div>

            <div class="panel" style="grid-column: span 2;">
                <h3>3. Add Questions to Subject Bank</h3>
                
                <div class="form-group">
                    <label>Select Subject</label>
                    <select id="q-subject"></select> </div>
                
                <div class="form-group"><input type="text" id="q-text" placeholder="Enter the question text..."></div>
                
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px;">
                    <input type="text" id="q-opt0" placeholder="Option 1">
                    <input type="text" id="q-opt1" placeholder="Option 2">
                    <input type="text" id="q-opt2" placeholder="Option 3">
                    <input type="text" id="q-opt3" placeholder="Option 4">
                </div>
                
                <div class="form-group" style="margin-top: 15px;">
                    <label>Correct Answer</label>
                    <select id="q-correct">
                        <option value="0">Option 1</option>
                        <option value="1">Option 2</option>
                        <option value="2">Option 3</option>
                        <option value="3">Option 4</option>
                    </select>
                </div>
                <button class="btn btn-success" onclick="createQuestion()">Save Question to Database</button>
                <div id="question-alert" class="alert"></div>
            </div>
            
        </div>
    </div>

    <script>
        const API_BASE = "http://localhost:8080/api/teacher";

        // AUTH GUARD
        if(sessionStorage.getItem("role") !== "TEACHER") window.location.href = "login.html";
        document.getElementById("teacher-name").innerText = sessionStorage.getItem("username");

        function logout() {
            sessionStorage.clear();
            window.location.href = "login.html";
        }

        // LOAD SUBJECTS INTO DROPDOWN ON BOOT
        async function fetchSubjects() {
            try {
                const res = await fetch(`${API_BASE}/subjects`);
                const subjects = await res.json();
                const select = document.getElementById("q-subject");
                select.innerHTML = subjects.map(sub => `<option value="${sub.id}">${sub.name} (${sub.classLevel})</option>`).join('');
            } catch (e) { console.error("Could not fetch subjects."); }
        }
        fetchSubjects(); // Run immediately
// CREATE STUDENT
        async function createStudent() {
            const payload = { 
                username: document.getElementById("s-user").value, 
                password: document.getElementById("s-pass").value, 
                assignedClass: document.getElementById("s-class").value 
            };
            const res = await fetch(`${API_BASE}/create-student`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload) });
            const data = await res.json();
            
            if(res.ok) {
                showAlert("student-alert", data.message, true);
                // Clear the boxes
                document.getElementById("s-user").value = "";
                document.getElementById("s-pass").value = "";
                document.getElementById("s-class").value = "";
            } else {
                showAlert("student-alert", "Failed to add student.", false);
            }
        }

        // CREATE SUBJECT
        async function createSubject() {
            const payload = { 
                name: document.getElementById("sub-name").value, 
                classLevel: document.getElementById("sub-class").value, 
                timeLimitMinutes: parseInt(document.getElementById("sub-time").value) 
            };
            const res = await fetch(`${API_BASE}/create-subject`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload) });
            
            if(res.ok) {
                showAlert("subject-alert", "Subject Created! You can now add questions to it.", true);
                fetchSubjects(); // Refresh dropdown
                // Clear the boxes
                document.getElementById("sub-name").value = "";
                document.getElementById("sub-class").value = "";
                document.getElementById("sub-time").value = "";
            }
        }

        // CREATE QUESTION
        async function createQuestion() {
            const subId = document.getElementById("q-subject").value;
            if(!subId) return showAlert("question-alert", "You must create a subject first!", false);

            const payload = {
                subjectId: subId,
                text: document.getElementById("q-text").value,
                options: [
                    document.getElementById("q-opt0").value, document.getElementById("q-opt1").value,
                    document.getElementById("q-opt2").value, document.getElementById("q-opt3").value
                ],
                correctIndex: parseInt(document.getElementById("q-correct").value)
            };

            const res = await fetch(`${API_BASE}/create-question`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload) });
            
            if(res.ok) {
                showAlert("question-alert", "Question Bank Updated!", true);
                // Clear ALL the boxes
                document.getElementById("q-text").value = ""; 
                document.getElementById("q-opt0").value = "";
                document.getElementById("q-opt1").value = "";
                document.getElementById("q-opt2").value = "";
                document.getElementById("q-opt3").value = "";
                document.getElementById("q-correct").value = "0"; // Reset dropdown
            }
        }
       
        function showAlert(id, msg, isSuccess) {
            const el = document.getElementById(id);
            el.innerText = msg;
            el.className = `alert text-center ${isSuccess ? 'alert-success' : 'alert-error'}`;
            el.style.display = "block";
            setTimeout(() => el.style.display = "none", 4000);
        }
    </script>
</body>
</html>