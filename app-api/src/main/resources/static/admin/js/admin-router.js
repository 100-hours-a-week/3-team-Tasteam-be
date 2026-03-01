const ADMIN_ROUTE_ALIASES = {
	dashboard: 'dashboard',
	restaurants: 'restaurants',
	'restaurant-create': 'restaurants/create',
	'restaurant-edit': 'restaurants/edit',
	'restaurant-menu': 'restaurants/menu',
	promotions: 'promotions',
	'promotion-create': 'promotions/create',
	'promotion-edit': 'promotions/edit',
	announcements: 'announcements',
	'announcement-create': 'announcements/create',
	'announcement-edit': 'announcements/edit',
	groups: 'groups',
	reports: 'reports',
	reviews: 'reviews',
	jobs: 'jobs',
	dummy: 'dummy'
};

const ROUTE_DEFINITIONS = {
	login: { view: () => window.loginView, requiresAuth: false, showLayout: false },
	dashboard: { view: () => window.dashboardView, requiresAuth: true, showLayout: true },
	restaurants: { view: () => window.restaurantsView, requiresAuth: true, showLayout: true },
	'restaurants/create': { view: () => window.restaurantFormView, requiresAuth: true, showLayout: true },
	'restaurants/edit': { view: () => window.restaurantEditView, requiresAuth: true, showLayout: true },
	'restaurants/menu': { view: () => window.restaurantMenuView, requiresAuth: true, showLayout: true },
	promotions: { view: () => window.promotionsView, requiresAuth: true, showLayout: true },
	'promotions/create': { view: () => window.promotionFormView, requiresAuth: true, showLayout: true },
	'promotions/edit': { view: () => window.promotionEditView, requiresAuth: true, showLayout: true },
	announcements: { view: () => window.announcementsView, requiresAuth: true, showLayout: true },
	'announcements/create': { view: () => window.announcementFormView, requiresAuth: true, showLayout: true, mode: 'create' },
	'announcements/edit': { view: () => window.announcementFormView, requiresAuth: true, showLayout: true, mode: 'edit' },
	groups: { view: () => window.groupsView, requiresAuth: true, showLayout: true },
	reports: { view: () => window.reportsView, requiresAuth: true, showLayout: true },
	reviews: { view: () => window.reviewsView, requiresAuth: true, showLayout: true },
	jobs: { view: () => window.jobsView, requiresAuth: true, showLayout: true },
	dummy: { view: () => window.dummyView, requiresAuth: true, showLayout: true },
	'not-found': { view: () => window.notFoundView, requiresAuth: true, showLayout: true }
};

function normalizeRouteSegment(routeSegment = '') {
	return routeSegment
		.replace(/^\/+|\/+$/g, '')
		.replace(/\.html$/, '');
}

function normalizePath(pathname = window.location.pathname) {
	const parsed = window.AdminUtils?.parsePathname ? window.AdminUtils.parsePathname(pathname) : pathname;

	if (parsed === '/admin' || parsed === '/admin/' || parsed === '/admin/index.html') {
		return 'login';
	}
	if (parsed === '/admin/pages' || parsed === '/admin/pages/' || parsed === '/admin/pages/index.html') {
		return 'dashboard';
	}
	if (parsed.startsWith('/admin/pages/')) {
		return parsed.slice('/admin/pages/'.length);
	}
	if (parsed.startsWith('/admin/')) {
		return parsed.slice('/admin/'.length);
	}
	return parsed.replace(/^\/+/, '');
}

function isAuthed() {
	return window.AdminUtils && typeof window.AdminUtils.isAuthed === 'function'
		? window.AdminUtils.isAuthed()
		: Boolean(localStorage.getItem('authToken'));
}

function getRouteState(search = window.location.search) {
	return Object.fromEntries(new URLSearchParams(search).entries());
}

function normalizeRoute(pathname = window.location.pathname) {
	const raw = normalizeRouteSegment(normalizePath(pathname));
	if (!raw) {
		return 'login';
	}
	return ADMIN_ROUTE_ALIASES[raw] || raw;
}

function resolveRoute(pathname = window.location.pathname) {
	const routeKey = normalizeRoute(pathname);
	const route = ROUTE_DEFINITIONS[routeKey];
	if (route) {
		return {
			key: routeKey,
			route,
			activeNav: routeKey.includes('/') ? routeKey.split('/')[0] : routeKey
		};
	}

	return {
		key: 'not-found',
		route: ROUTE_DEFINITIONS['not-found'],
		activeNav: routeKey.includes('/') ? routeKey.split('/')[0] : routeKey
	};
}

function getView(routeConfig) {
	if (!routeConfig || typeof routeConfig.view !== 'function') {
		return null;
	}
	const view = routeConfig.view();
	if (!view || typeof view.render !== 'function' || typeof view.mount !== 'function') {
		return null;
	}
	return view;
}

let currentUnmount = null;
let currentRouteKey = 'login';
let unbindLayout = null;

function clearCurrentRouteState() {
	if (typeof currentUnmount === 'function') {
		currentUnmount();
		currentUnmount = null;
	}
	if (typeof unbindLayout === 'function') {
		unbindLayout();
		unbindLayout = null;
	}
}

async function renderCurrentView(pathname = window.location.pathname, search = window.location.search) {
	const appRoot = document.getElementById('admin-app-root');
	if (!appRoot) {
		return;
	}

	const resolved = resolveRoute(pathname);
	const state = getRouteState(search);
	let route = resolved.route || ROUTE_DEFINITIONS['not-found'];
	const routeKey = resolved.key;

	if (!route.requiresAuth && isAuthed() && routeKey === 'login') {
		navigate('/admin/pages/dashboard.html', { replace: true });
		return;
	}

	if (route.requiresAuth && !isAuthed()) {
		if (pathname !== '/admin/' && pathname !== '/admin') {
			navigate('/admin/', { replace: true });
		}
		return;
	}

	clearCurrentRouteState();
	appRoot.innerHTML = '';

	let contentRoot = appRoot;
	if (route.showLayout) {
		contentRoot = renderAdminLayout(appRoot, resolved.activeNav || '');
		unbindLayout = bindAdminLayout(appRoot, navigate, window.logout);
	}

	const view = getView(route);
	if (!view) {
		route = ROUTE_DEFINITIONS['not-found'];
		const fallbackView = getView(route);
		if (!fallbackView) {
			contentRoot = appRoot;
			contentRoot.innerHTML = '<div class="content" style="padding:20px;">요청한 페이지를 불러오지 못했습니다.</div>';
			return;
		}
		if (route.showLayout) {
			contentRoot = renderAdminLayout(appRoot, resolved.activeNav || '');
			unbindLayout = bindAdminLayout(appRoot, navigate, window.logout);
		}
		fallbackView.render(contentRoot, state);
		const fallbackMount = await Promise.resolve(fallbackView.mount({
			...state,
			route: 'not-found',
			mode: route.mode
		}));
		currentUnmount = typeof fallbackMount === 'function'
			? fallbackMount
			: (typeof fallbackView.unmount === 'function' ? fallbackView.unmount : null);
		currentRouteKey = 'not-found';
		return;
	}

	view.render(contentRoot, state);
	const mountResult = await Promise.resolve(view.mount({
		...state,
		route: routeKey,
		mode: route.mode
	}));
	currentUnmount = typeof mountResult === 'function' ? mountResult : (typeof view.unmount === 'function' ? view.unmount : null);
	currentRouteKey = routeKey;
}

function normalizeDestination(path) {
	if (!path || typeof path !== 'string') {
		return '/admin/';
	}
	if (path.startsWith('http://') || path.startsWith('https://')) {
		return path;
	}
	if (path.startsWith('/admin/')) {
		return path;
	}
	return `/admin/${path.replace(/^\/+/, '')}`;
}

function navigate(path, options = {}) {
	const destination = normalizeDestination(path);
	const target = new URL(destination, window.location.origin);

	if (!target.pathname.startsWith('/admin/')) {
		window.location.href = destination;
		return;
	}

	const next = target.pathname + target.search + target.hash;
	const current = window.location.pathname + window.location.search + window.location.hash;

	if (options.replace) {
		window.history.replaceState(null, '', next);
	} else if (next !== current) {
		window.history.pushState(null, '', next);
	}

	renderCurrentView(target.pathname, target.search);
}

function startAdminRouter() {
	window.addEventListener('popstate', () => {
		renderCurrentView(window.location.pathname, window.location.search);
	});
	renderCurrentView(window.location.pathname, window.location.search);
}

window.AdminRouter = {
	start: startAdminRouter,
	resolveRoute,
	navigate,
	getCurrentRoute: () => currentRouteKey,
	getRouteState
};
