let authNavigator = null;

function setAuthNavigator(navigate) {
    authNavigator = navigate;
}

function getAuthToken() {
    return localStorage.getItem('authToken');
}

function isLoginRoute(pathname = window.location.pathname) {
    return pathname === '/admin' || pathname === '/admin/' || pathname === '/admin/index.html';
}

function goToLogin() {
    if (typeof authNavigator === 'function') {
        authNavigator('/admin/');
        return;
    }
    window.location.href = '/admin/';
}

function checkAuth() {
    if (!getAuthToken() && !isLoginRoute()) {
        goToLogin();
    }
}

function logout() {
    localStorage.removeItem('authToken');
    goToLogin();
}

async function handleLoginSubmit(event) {
    event.preventDefault();

    const form = event.target;
    const emailInput = form.querySelector('#email');
    const passwordInput = form.querySelector('#password');
    const errorDiv = form.querySelector('#error-message');

    if (!emailInput || !passwordInput || !errorDiv) {
        return;
    }

    const username = emailInput.value;
    const password = passwordInput.value;

    try {
        const response = await fetch('/api/v1/admin/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || '로그인에 실패했습니다.');
        }

        const result = await response.json();
        if (result.data && result.data.accessToken) {
            localStorage.setItem('authToken', result.data.accessToken);
            if (typeof authNavigator === 'function') {
                authNavigator('/admin/pages/dashboard.html');
                return;
            }
            window.location.href = '/admin/pages/dashboard.html';
            return;
        }

        errorDiv.textContent = '로그인 정보가 올바르지 않습니다.';
    } catch (error) {
        errorDiv.textContent = error.message || '로그인 중 오류가 발생했습니다.';
    }
}

function initLoginForm() {
    const loginForm = document.getElementById('loginForm');
    if (!loginForm) {
        return;
    }

    loginForm.addEventListener('submit', handleLoginSubmit);
}

function getAdminAuth() {
    return {
        checkAuth,
        logout,
        initLoginForm,
        setAuthNavigator
    };
}

window.checkAuth = checkAuth;
window.logout = logout;
window.initLoginForm = initLoginForm;
window.setAuthNavigator = setAuthNavigator;
window.getAdminAuth = getAdminAuth;
