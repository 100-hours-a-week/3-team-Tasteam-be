const API_BASE_URL = '/api/v1';

function getAuthHeaders() {
    const token = localStorage.getItem('authToken');
    return {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
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
        window.location.href = '/admin/index.html';
        throw new Error('인증이 필요합니다.');
    }

    if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || '요청 처리 중 오류가 발생했습니다.');
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
