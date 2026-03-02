const EMPTY_NOOP = () => {};

let routeNavigator = null;
let featureFlags = {
    dummyEnabled: true
};

function setFeatureFlag(name, enabled) {
    if (!name) {
        return;
    }

    featureFlags[name] = Boolean(enabled);
}

function setFeatureFlags(flags = {}) {
    featureFlags = {
        ...featureFlags,
        ...flags
    };
}

function isFeatureEnabled(name) {
    return Boolean(featureFlags[name]);
}

async function refreshAdminFeatureFlags() {
    const enabled = await isDummyEndpointAvailable();
    setFeatureFlag('dummyEnabled', enabled);
    return featureFlags;
}

async function isDummyEndpointAvailable() {
    if (!window.getDataCounts || !isAuthed()) {
        return false;
    }

    try {
        await window.getDataCounts();
        return true;
    } catch (error) {
        if (error && typeof error.status === 'number') {
            return error.status !== 404;
        }
        return false;
    }
}

function setRouteNavigator(navigate) {
    routeNavigator = typeof navigate === 'function' ? navigate : null;
}

function navigateTo(path) {
    if (typeof routeNavigator === 'function') {
        routeNavigator(path);
        return;
    }
    window.location.href = path;
}

function toKoreanDateTime(isoString) {
    if (!isoString) {
        return '-';
    }

    const date = new Date(isoString);
    return date.toLocaleString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function toKoreanDate(isoString) {
    if (!isoString) {
        return '-';
    }

    const date = new Date(isoString);
    return date.toLocaleDateString('ko-KR');
}

function toDatetimeLocal(isoString) {
    if (!isoString) {
        return '';
    }

    const date = new Date(isoString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');

    return `${year}-${month}-${day}T${hours}:${minutes}`;
}

function escapeHtml(text) {
    const node = document.createElement('div');
    node.textContent = text ?? '';
    return node.innerHTML;
}

function getQueryState() {
    return new URLSearchParams(window.location.search);
}

function getRouteState() {
    return Object.fromEntries(getQueryState().entries());
}

function parsePathname(pathname = window.location.pathname) {
    return (pathname || '').split('?')[0].split('#')[0];
}

function getPathSegments(pathname = window.location.pathname) {
    const normalized = parsePathname(pathname).replace(/^\//, '');
    return normalized ? normalized.split('/').filter(Boolean) : [];
}

function isAdminPath(pathname = window.location.pathname) {
    return getPathSegments(pathname)[0] === 'admin';
}

function addListener(element, eventName, handler) {
    if (!element || !eventName || typeof handler !== 'function') {
        return EMPTY_NOOP;
    }

    element.addEventListener(eventName, handler);
    return () => element.removeEventListener(eventName, handler);
}

function addDelegatedListener(root, selector, eventName, handler) {
    if (!root || !selector || typeof handler !== 'function') {
        return EMPTY_NOOP;
    }

    const wrapped = (event) => {
        const target = event.target?.closest?.(selector);
        if (target && root.contains(target)) {
            handler(event, target);
        }
    };

    root.addEventListener(eventName, wrapped);
    return () => root.removeEventListener(eventName, wrapped);
}

function getAdminAuthToken() {
    return localStorage.getItem('authToken');
}

function isAuthed() {
    return Boolean(getAdminAuthToken());
}

window.AdminUtils = {
    setRouteNavigator,
    setFeatureFlag,
    setFeatureFlags,
    isFeatureEnabled,
    refreshAdminFeatureFlags,
    navigateTo,
    toKoreanDateTime,
    toKoreanDate,
    toDatetimeLocal,
    escapeHtml,
    getRouteState,
    getQueryState,
    parsePathname,
    getPathSegments,
    isAdminPath,
    addListener,
    addDelegatedListener,
    isAuthed
};
