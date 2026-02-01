function checkAuth() {
    const token = localStorage.getItem('authToken');
    if (!token && !window.location.pathname.endsWith('index.html') && !window.location.pathname.endsWith('/admin/')) {
        window.location.href = '/admin/index.html';
    }
}

function logout() {
    localStorage.removeItem('authToken');
    window.location.href = '/admin/index.html';
}

document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const email = document.getElementById('email').value;
            const password = document.getElementById('password').value;
            const errorDiv = document.getElementById('error-message');

            try {
                const result = await login(email, password);
                if (result.data && result.data.accessToken) {
                    localStorage.setItem('authToken', result.data.accessToken);
                    window.location.href = '/admin/pages/dashboard.html';
                } else {
                    errorDiv.textContent = '로그인 정보가 올바르지 않습니다.';
                }
            } catch (error) {
                errorDiv.textContent = error.message;
            }
        });
    }

    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', (e) => {
            e.preventDefault();
            logout();
        });
    }
});
