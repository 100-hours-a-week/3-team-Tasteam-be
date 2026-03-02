const API_BASE_URL = '/api/v1';

let authFailureHandler = null;

function setApiUnauthorizedHandler(handler) {
    authFailureHandler = handler;
}

function getAuthHeaders() {
    const token = localStorage.getItem('authToken');
    return {
        'Content-Type': 'application/json',
        Authorization: token ? `Bearer ${token}` : ''
    };
}

async function apiRequest(url, options = {}) {
    const response = await fetch(API_BASE_URL + url, {
        ...options,
        headers: {
            ...getAuthHeaders(),
            ...options.headers
        }
    });

    if (response.status === 401) {
        localStorage.removeItem('authToken');
        if (typeof authFailureHandler === 'function') {
            authFailureHandler();
        } else {
            window.location.href = '/admin/';
        }
        throw new Error('인증이 필요합니다.');
    }

    const responseContentType = response.headers.get('content-type') || '';
    if (!response.ok) {
        let errorMessage = '요청 처리 중 오류가 발생했습니다.';
        try {
            if (responseContentType.includes('application/json')) {
                const error = await response.json();
                errorMessage = error.message || errorMessage;
            } else {
                const text = await response.text();
                if (text) {
                    errorMessage = text;
                }
            }
        } catch (e) {
            // ignore parse errors and use default message
        }
        const error = new Error(errorMessage);
        error.status = response.status;
        error.body = responseContentType.includes('application/json') ? null : errorMessage;
        throw error;
    }

    if (response.status === 204) {
        return null;
    }

    const contentType = response.headers.get('content-type') || '';
    if (!contentType.includes('application/json')) {
        const text = await response.text();
        return text ? { data: text } : null;
    }

    return response.json();
}

async function login(email, password) {
    const response = await fetch(API_BASE_URL + '/auth/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ email, password })
    });

    if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || '로그인에 실패했습니다.');
    }

    return response.json();
}

async function getRestaurants(params = {}) {
    const queryString = new URLSearchParams(params).toString();
    return apiRequest(`/admin/restaurants?${queryString}`);
}

async function getRestaurant(id) {
    return apiRequest(`/admin/restaurants/${id}`);
}

async function createRestaurant(data) {
    return apiRequest('/admin/restaurants', {
        method: 'POST',
        body: JSON.stringify(data)
    });
}

async function updateRestaurant(id, data) {
    return apiRequest(`/admin/restaurants/${id}`, {
        method: 'PATCH',
        body: JSON.stringify(data)
    });
}

async function deleteRestaurant(id) {
    return apiRequest(`/admin/restaurants/${id}`, {
        method: 'DELETE'
    });
}

async function getFoodCategories() {
    return apiRequest('/food-categories');
}

async function createFoodCategory(data) {
    return apiRequest('/admin/food-categories', {
        method: 'POST',
        body: JSON.stringify(data)
    });
}

async function getRestaurantMenus(restaurantId) {
    return apiRequest(`/admin/restaurants/${restaurantId}/menus`);
}

async function createMenuCategory(restaurantId, data) {
    return apiRequest(`/admin/restaurants/${restaurantId}/menus/categories`, {
        method: 'POST',
        body: JSON.stringify(data)
    });
}

async function createMenu(restaurantId, data) {
    return apiRequest(`/admin/restaurants/${restaurantId}/menus`, {
        method: 'POST',
        body: JSON.stringify(data)
    });
}

async function createPresignedUploads(purpose, files) {
    const allowedContentTypes = new Set(['image/jpeg', 'image/jpg', 'image/png', 'image/webp']);

    const getAllowedContentTypeFromFileName = (fileName) => {
        const normalizedName = (fileName || '').trim().toLowerCase();
        const lastDot = normalizedName.lastIndexOf('.');
        const extension = lastDot >= 0 ? normalizedName.substring(lastDot + 1) : '';

        const extensionMap = {
            jpg: 'image/jpeg',
            jpeg: 'image/jpeg',
            png: 'image/png',
            webp: 'image/webp'
        };

        return extensionMap[extension];
    };

    const getFileContentType = (file) => {
        const explicitType = (file.type || '').trim().toLowerCase();
        if (explicitType && explicitType !== 'application/octet-stream') {
            return explicitType;
        }

        return getAllowedContentTypeFromFileName(file.name);
    };

    const filesWithType = files.map(file => ({
        fileName: file.name,
        contentType: getFileContentType(file),
        size: file.size
    }));

    const invalidType = filesWithType.find((item) => !item.contentType || !allowedContentTypes.has(item.contentType));
    if (invalidType) {
        throw new Error('지원하지 않는 이미지 파일 형식입니다.');
    }

    const request = {
        purpose,
        files: filesWithType
    };

    return apiRequest('/files/uploads/presigned', {
        method: 'POST',
        body: JSON.stringify(request)
    });
}

async function uploadToPresigned(presignedItem, file) {
    const formData = new FormData();
    Object.entries(presignedItem.fields || {}).forEach(([key, value]) => {
        formData.append(key, value);
    });
    formData.append('file', file);

    const response = await fetch(presignedItem.url, {
        method: 'POST',
        body: formData
    });

    if (!response.ok) {
        throw new Error('파일 업로드에 실패했습니다.');
    }
}

async function getReviews(params = {}) {
    const queryString = new URLSearchParams(params).toString();
    return apiRequest(`/admin/reviews?${queryString}`);
}

async function adminDeleteReview(id) {
    return apiRequest(`/admin/reviews/${id}`, {
        method: 'DELETE'
    });
}

async function getAdminReports(params = {}) {
    const queryString = new URLSearchParams(params).toString();
    return apiRequest(`/admin/reports?${queryString}`);
}

async function getAdminReport(id) {
    return apiRequest(`/admin/reports/${id}`);
}

async function updateReportStatus(id, status) {
    return apiRequest(`/admin/reports/${id}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ status })
    });
}

async function geocodeAddress(query) {
    const params = new URLSearchParams({ query });
    return apiRequest(`/admin/geocoding?${params.toString()}`);
}

async function getGroups(params = {}) {
    const queryString = new URLSearchParams(params).toString();
    return apiRequest(`/admin/groups?${queryString}`);
}

async function createGroup(data) {
    return apiRequest('/admin/groups', {
        method: 'POST',
        body: JSON.stringify(data)
    });
}

async function seedDummyData(params = {}) {
    return apiRequest('/admin/dummy/seed', {
        method: 'POST',
        body: JSON.stringify(params)
    });
}

async function getDataCounts() {
    return apiRequest('/admin/dummy/count');
}

async function deleteDummyData() {
    return apiRequest('/admin/dummy', {
        method: 'DELETE'
    });
}

const api = {
    get: async (url) => {
        const result = await apiRequest(url, { method: 'GET' });
        return result?.data || result;
    },
    post: async (url, data) => {
        const result = await apiRequest(url, {
            method: 'POST',
            body: JSON.stringify(data)
        });
        return result?.data || result;
    },
    patch: async (url, data) => {
        const result = await apiRequest(url, {
            method: 'PATCH',
            body: JSON.stringify(data)
        });
        return result?.data || result;
    },
    delete: async (url) => {
        return await apiRequest(url, { method: 'DELETE' });
    }
};

window.setApiUnauthorizedHandler = setApiUnauthorizedHandler;
window.apiRequest = apiRequest;
window.api = api;
window.login = login;
window.getRestaurants = getRestaurants;
window.getRestaurant = getRestaurant;
window.createRestaurant = createRestaurant;
window.updateRestaurant = updateRestaurant;
window.deleteRestaurant = deleteRestaurant;
window.getFoodCategories = getFoodCategories;
window.createFoodCategory = createFoodCategory;
window.getRestaurantMenus = getRestaurantMenus;
window.createMenuCategory = createMenuCategory;
window.createMenu = createMenu;
window.createPresignedUploads = createPresignedUploads;
window.uploadToPresigned = uploadToPresigned;
window.getReviews = getReviews;
window.adminDeleteReview = adminDeleteReview;
window.getAdminReports = getAdminReports;
window.getAdminReport = getAdminReport;
window.updateReportStatus = updateReportStatus;
window.geocodeAddress = geocodeAddress;
window.getGroups = getGroups;
window.createGroup = createGroup;
window.seedDummyData = seedDummyData;
window.getDataCounts = getDataCounts;
window.deleteDummyData = deleteDummyData;
