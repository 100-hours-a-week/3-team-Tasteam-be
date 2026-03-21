function renderNotFound(container) {
    if (!container) {
        return;
    }

    container.innerHTML = `
        <div class="content-header">
            <h1>요청한 페이지를 찾을 수 없습니다.</h1>
        </div>
        <div class="card">
            <p>요청하신 관리자 페이지가 존재하지 않습니다.</p>
            <a href="/admin/pages/dashboard.html" class="btn btn-primary not-found-link">대시보드로 이동</a>
        </div>
    `;
}

function mountNotFound() {}

function unmountNotFound() {}

window.notFoundView = {
    render: renderNotFound,
    mount: mountNotFound,
    unmount: unmountNotFound
};
