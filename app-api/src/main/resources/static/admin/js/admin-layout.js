const MENU_ITEMS = [
    { route: 'dashboard', label: '대시보드', href: '/admin/pages/dashboard.html' },
    { route: 'restaurants', label: '음식점 관리', href: '/admin/pages/restaurants.html' },
    { route: 'groups', label: '그룹 관리', href: '/admin/pages/groups.html' },
    { route: 'promotions', label: '프로모션 관리', href: '/admin/pages/promotions.html' },
    { route: 'announcements', label: '공지사항 관리', href: '/admin/pages/announcements.html' },
    { route: 'reviews', label: '리뷰 관리', href: '/admin/pages/reviews.html' },
    { route: 'reports', label: '신고 관리', href: '/admin/pages/reports.html' },
    { route: 'jobs', label: '배치 작업', href: '/admin/pages/jobs.html' },
    { route: 'notifications', label: '알림 발송', href: '/admin/pages/notifications.html' },
    { route: 'dummy', label: '더미 데이터', href: '/admin/pages/dummy.html' }
];

function getMenuItems() {
    const hasDummyFeature = typeof window.AdminUtils?.isFeatureEnabled === 'function'
        ? window.AdminUtils.isFeatureEnabled('dummyEnabled')
        : true;

    if (hasDummyFeature) {
        return MENU_ITEMS;
    }

    return MENU_ITEMS.filter((item) => item.route !== 'dummy');
}

function renderAdminLayout(container, activeRoute = '') {
    const normalizedActive = activeRoute || 'dashboard';
    const menuHtml = getMenuItems().map((menu) => {
        const isActive = menu.route === normalizedActive;
        return `<li><a href="${menu.href}" data-route="${menu.route}" class="${isActive ? 'active' : ''}">${menu.label}</a></li>`;
    }).join('');

    container.innerHTML = `
        <div class="admin-layout">
            <nav class="sidebar">
                <div class="sidebar-header">
                    <h2>Tasteam Admin</h2>
                </div>
                <ul class="nav-menu">
                    ${menuHtml}
                    <li><a href="#" id="logoutBtn" class="logout-btn">로그아웃</a></li>
                </ul>
            </nav>
            <main class="content">
                <div id="admin-page-content"></div>
            </main>
        </div>
    `;

    return container.querySelector('#admin-page-content');
}

function bindAdminLayout(container, routerNavigate, onLogout) {
    if (!container) {
        return () => {};
    }

    const onLinkClick = (event) => {
        const anchor = event.target.closest('a');
        if (!anchor || !container.contains(anchor)) {
            return;
        }

        if (anchor.id === 'logoutBtn' || anchor.getAttribute('href') === '#') {
            event.preventDefault();
            if (typeof onLogout === 'function') {
                onLogout();
            }
            return;
        }

        const routeHref = anchor.getAttribute('href');
        if (!routeHref) {
            return;
        }

        if (routeHref.startsWith('/admin/')) {
            const isRelativeHash = routeHref.startsWith('/admin/#');
            if (isRelativeHash) {
                return;
            }

            event.preventDefault();
            if (typeof routerNavigate === 'function') {
                routerNavigate(routeHref);
            }
        }
    };

    container.addEventListener('click', onLinkClick);
    return () => {
        container.removeEventListener('click', onLinkClick);
    };
}

window.renderAdminLayout = renderAdminLayout;
window.bindAdminLayout = bindAdminLayout;
