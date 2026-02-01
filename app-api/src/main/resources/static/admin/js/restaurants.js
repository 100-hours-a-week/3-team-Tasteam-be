let currentPage = 0;
const pageSize = 20;

checkAuth();

async function loadRestaurants(page = 0) {
    try {
        const params = {
            page,
            size: pageSize,
            sort: 'createdAt,desc'
        };

        const searchName = document.getElementById('searchName').value;
        const searchAddress = document.getElementById('searchAddress').value;

        if (searchName) params.name = searchName;
        if (searchAddress) params.address = searchAddress;

        const result = await getRestaurants(params);
        displayRestaurants(result.data);
        displayPagination(result.data);
    } catch (error) {
        alert('음식점 목록을 불러오는데 실패했습니다: ' + error.message);
    }
}

function displayRestaurants(data) {
    const tbody = document.getElementById('restaurantList');

    if (!data.content || data.content.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="loading">등록된 음식점이 없습니다.</td></tr>';
        return;
    }

    tbody.innerHTML = data.content.map(restaurant => `
        <tr>
            <td>${restaurant.id}</td>
            <td>${restaurant.name}</td>
            <td>${restaurant.address}</td>
            <td>${(restaurant.foodCategories || []).join(', ')}</td>
            <td>${new Date(restaurant.createdAt).toLocaleDateString()}</td>
            <td>
                ${restaurant.deletedAt
                    ? '<span class="badge badge-deleted">삭제됨</span>'
                    : '<span class="badge badge-active">활성</span>'}
            </td>
            <td>
                <button class="btn btn-secondary" onclick="location.href='/admin/pages/restaurant-edit.html?id=${restaurant.id}'">수정</button>
            </td>
        </tr>
    `).join('');
}

function displayPagination(data) {
    const pagination = document.getElementById('pagination');
    const totalPages = data.totalPages;
    currentPage = data.number;

    let html = '';

    if (currentPage > 0) {
        html += `<button onclick="loadRestaurants(${currentPage - 1})">이전</button>`;
    }

    for (let i = Math.max(0, currentPage - 2); i < Math.min(totalPages, currentPage + 3); i++) {
        html += `<button class="${i === currentPage ? 'active' : ''}" onclick="loadRestaurants(${i})">${i + 1}</button>`;
    }

    if (currentPage < totalPages - 1) {
        html += `<button onclick="loadRestaurants(${currentPage + 1})">다음</button>`;
    }

    pagination.innerHTML = html;
}

function searchRestaurants() {
    loadRestaurants(0);
}

document.addEventListener('DOMContentLoaded', () => {
    loadRestaurants();
});
