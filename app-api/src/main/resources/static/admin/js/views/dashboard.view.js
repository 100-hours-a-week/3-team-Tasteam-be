let cleanup = [];

function render(container) {
    if (!container) {
        return;
    }

    container.innerHTML = `
        <div class="content-header">
            <h1>대시보드</h1>
        </div>
        <div class="dashboard-cards">
            <div class="card">
                <h3>음식점 관리</h3>
                <p>음식점 등록, 수정, 메뉴 관리, 운영시간 설정</p>
                <a href="/admin/pages/restaurants.html" class="btn btn-primary">바로가기</a>
            </div>
            <div class="card">
                <h3>그룹 관리</h3>
                <p>그룹 생성, 신청 조회 및 승인</p>
                <a href="/admin/pages/groups.html" class="btn btn-primary">바로가기</a>
            </div>
            <div class="card">
                <h3>프로모션 관리</h3>
                <p>이벤트 및 프로모션 등록, 수정, 노출 관리</p>
                <a href="/admin/pages/promotions.html" class="btn btn-primary">바로가기</a>
            </div>
            <div class="card">
                <h3>공지사항 관리</h3>
                <p>공지사항 등록, 수정, 삭제</p>
                <a href="/admin/pages/announcements.html" class="btn btn-primary">바로가기</a>
            </div>
            <div class="card">
                <h3>배치 작업</h3>
                <p>이미지 최적화 등 배치 작업 수동 실행</p>
                <a href="/admin/pages/jobs.html" class="btn btn-primary">바로가기</a>
            </div>
        </div>
    `;
}

function mount() {
    cleanup = [];
}

function unmount() {
    cleanup.forEach((remove) => remove());
    cleanup = [];
}

window.dashboardView = { render, mount, unmount };
