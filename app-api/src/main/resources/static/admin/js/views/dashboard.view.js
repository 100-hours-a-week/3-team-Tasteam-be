let cleanup = [];

function render(container) {
    if (!container) {
        return;
    }

    const sections = [
        {
            title: '콘텐츠 운영',
            description: '서비스에 노출되는 콘텐츠를 등록, 수정, 배포하는 업무 영역',
            items: [
                {
                    title: '음식점 관리',
                    description: '음식점 등록, 수정, 메뉴 관리, 운영시간 설정',
                    href: '/admin/pages/restaurants.html'
                },
                {
                    title: '프로모션 관리',
                    description: '이벤트/배너 등록, 노출 기간 설정, 발행 상태 관리',
                    href: '/admin/pages/promotions.html'
                },
                {
                    title: '공지사항 관리',
                    description: '공지사항 작성, 수정, 게시 현황 관리',
                    href: '/admin/pages/announcements.html'
                }
            ]
        },
        {
            title: '품질 / 리스크',
            description: '사용자 콘텐츠 품질과 신고/리뷰 이슈를 모니터링하는 영역',
            items: [
                {
                    title: '리뷰 관리',
                    description: '리뷰 검색, 내용 확인, 부적절 리뷰 삭제 처리',
                    href: '/admin/pages/reviews.html'
                },
                {
                    title: '신고 관리',
                    description: '신고 접수 내역 조회 및 상태 변경 처리',
                    href: '/admin/pages/reports.html'
                }
            ]
        },
        {
            title: '운영 작업',
            description: '시스템 운영 및 유지보수성 향상을 위한 관리 영역',
            items: [
                {
                    title: '그룹 관리',
                    description: '그룹 생성, 가입 타입 설정, 그룹 데이터 운영',
                    href: '/admin/pages/groups.html'
                },
                {
                    title: '배치 작업',
                    description: '이미지 최적화/정리 작업 실행과 결과 확인',
                    href: '/admin/pages/jobs.html'
                }
            ]
        }
    ];

    container.innerHTML = `
        <div class="content-header">
            <h1>대시보드</h1>
        </div>
        <section class="dashboard-hero card">
            <h2>업무 목적 중심으로 필요한 관리 화면에 빠르게 진입하세요.</h2>
            <p>자주 쓰는 기능을 운영 목적에 맞춰 분류했습니다. 각 영역에서 바로 작업을 시작할 수 있습니다.</p>
        </section>
        <div class="dashboard-sections">
            ${sections.map((section) => `
                <section class="dashboard-section">
                    <header class="dashboard-section-header">
                        <h2>${section.title}</h2>
                        <p>${section.description}</p>
                    </header>
                    <div class="dashboard-cards">
                        ${section.items.map((item) => `
                            <article class="card dashboard-card">
                                <h3>${item.title}</h3>
                                <p>${item.description}</p>
                                <a href="${item.href}" class="btn btn-primary">바로가기</a>
                            </article>
                        `).join('')}
                    </div>
                </section>
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
