let cleanup = [];

function render(container) {
    if (!container) {
        return;
    }

    const pageLinks = [
        {
            title: '음식점 관리',
            description: '음식점 등록, 수정, 메뉴 관리, 운영시간 설정',
            href: '/admin/pages/restaurants.html'
        },
        {
            title: '그룹 관리',
            description: '그룹 생성, 신청 조회 및 승인',
            href: '/admin/pages/groups.html'
        },
        {
            title: '프로모션 관리',
            description: '이벤트 및 프로모션 등록, 수정, 노출 관리',
            href: '/admin/pages/promotions.html'
        },
        {
            title: '공지사항 관리',
            description: '공지사항 등록, 수정, 삭제',
            href: '/admin/pages/announcements.html'
        },
        {
            title: '리뷰 관리',
            description: '리뷰 조회 및 삭제',
            href: '/admin/pages/reviews.html'
        },
        {
            title: '신고 관리',
            description: '신고 내역 조회 및 처리',
            href: '/admin/pages/reports.html'
        },
        {
            title: '배치 작업',
            description: '이미지 최적화 등 배치 작업 수동 실행',
            href: '/admin/pages/jobs.html'
        }
    ];

    container.innerHTML = `
        <div class="content-header">
            <h1>대시보드</h1>
        </div>
        <div class="dashboard-cards">
            ${pageLinks.map((link) => `
                <div class="card">
                    <h3>${link.title}</h3>
                    <p>${link.description}</p>
                    <a href="${link.href}" class="btn btn-primary">바로가기</a>
                </div>
            `).join('')}
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
