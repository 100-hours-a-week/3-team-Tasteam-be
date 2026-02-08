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
                const response = await fetch('/api/v1/admin/auth/login', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        username: email,
                        password: password
                    })
                });

                if (response.ok) {
                    const result = await response.json();
                    if (result.data && result.data.accessToken) {
                        localStorage.setItem('authToken', result.data.accessToken);
                        window.location.href = '/admin/pages/dashboard.html';
                    } else {
                        errorDiv.textContent = '로그인 정보가 올바르지 않습니다.';
                    }
                } else {
                    const error = await response.json();
                    errorDiv.textContent = error.message || '로그인에 실패했습니다.';
                }
            } catch (error) {
                errorDiv.textContent = '로그인 중 오류가 발생했습니다.';
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
