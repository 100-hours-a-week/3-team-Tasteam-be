function renderLogin(container) {
    if (!container) {
        return;
    }

    container.innerHTML = `
        <div class="login-container">
            <div class="login-box">
                <h1>Tasteam Admin</h1>
                <form id="loginForm">
                    <div class="form-group">
                        <label for="email">아이디</label>
                        <input type="text" id="email" name="email" required>
                    </div>
                    <div class="form-group">
                        <label for="password">비밀번호</label>
                        <input type="password" id="password" name="password" required>
                    </div>
                    <button type="submit" class="btn btn-primary">로그인</button>
                    <div id="error-message" class="error-message"></div>
                </form>
            </div>
        </div>
    `;
}

function mountLogin() {
    initLoginForm();
}

function unmountLogin() {}

window.loginView = {
    render: renderLogin,
    mount: mountLogin,
    unmount: unmountLogin
};
