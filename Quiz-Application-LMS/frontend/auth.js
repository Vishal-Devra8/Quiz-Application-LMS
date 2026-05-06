const API_BASE = "http://localhost:8080/api/auth";


// Temporary variable to hold username if they are forced to reset password
let pendingUsername = ""; 

async function login() {
    const user = document.getElementById('username').value;
    const pass = document.getElementById('password').value;
    const errorDiv = document.getElementById('login-error');

    if (!user || !pass) {
        showError(errorDiv, "Please enter both username and password.");
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: user, password: pass })
        });
        
        const data = await response.json();

        if (data.success) {
            // Check if this is a newly created account that needs a password reset
            if (data.mustChangePassword) {
                pendingUsername = data.username;
                document.getElementById('login-section').classList.add('hidden');
                document.getElementById('reset-section').classList.remove('hidden');
                return;
            }

            // If no reset needed, save their session and route them!
            sessionStorage.setItem("username", data.username);
            sessionStorage.setItem("role", data.role);
            sessionStorage.setItem("assignedClass", data.assignedClass); // <--- ADD THIS LINE!
            
            if (data.role === "HOD") window.location.href = "hod-dashboard.html";
            else if (data.role === "TEACHER") window.location.href = "teacher-dashboard.html";
            else if (data.role === "STUDENT") window.location.href = "student-dashboard.html";

        } else {
            // --- NEW SUSPENSION LOGIC ADDED HERE ---
            if (data.isSuspended) {
                showError(errorDiv, data.message); // Displays the exact ban reason and days left
            } else {
                showError(errorDiv, "Invalid credentials. Please try again.");
            }
        }
    } catch (error) {
        showError(errorDiv, "Critical Error: Cannot reach Java Backend Server.");
    }
}

async function changePassword() {
    const newPass = document.getElementById('new-password').value;
    const errorDiv = document.getElementById('reset-error');

    if (newPass.length < 5) {
        showError(errorDiv, "Password must be at least 5 characters long.");
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/change-password`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: pendingUsername, newPassword: newPass })
        });

        if (response.ok) {
            alert("Password updated successfully! Please login with your new password.");
            window.location.reload(); // Send them back to normal login
        }
    } catch (error) {
        showError(errorDiv, "Error updating password.");
    }
}

function showError(element, message) {
    element.innerText = message;
    element.style.display = "block";
    setTimeout(() => { element.style.display = "none"; }, 3000);
}