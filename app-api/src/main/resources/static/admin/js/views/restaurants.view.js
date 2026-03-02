let cleanup = [];
let currentPage = 0;
const pageSize = 20;

function getRestaurantFilterState(state = {}) {
	const searchName = state.searchName || '';
	const searchAddress = state.searchAddress || '';
	const page = Number.parseInt(state.page || '0', 10) || 0;
	return {
		searchName,
		searchAddress,
		page
	};
}

function applyRestaurantFiltersFromState(state = {}) {
	const { searchName, searchAddress } = getRestaurantFilterState(state);
	const searchNameInput = document.getElementById('searchName');
	const searchAddressInput = document.getElementById('searchAddress');

	if (searchNameInput) {
		searchNameInput.value = searchName;
	}
	if (searchAddressInput) {
		searchAddressInput.value = searchAddress;
	}
}

function getRestaurantFiltersFromInputs() {
	return {
		searchName: document.getElementById('searchName')?.value?.trim() || '',
		searchAddress: document.getElementById('searchAddress')?.value?.trim() || ''
	};
}

function getRestaurantListPath(page = 0, state = {}) {
	const filters = getRestaurantFilterState({ ...state, page });
	const query = new URLSearchParams({
		page: String(filters.page)
	});
	if (filters.searchName) {
		query.set('searchName', filters.searchName);
	}
	if (filters.searchAddress) {
		query.set('searchAddress', filters.searchAddress);
	}
	return `/admin/pages/restaurants.html?${query.toString()}`;
}

function render(container, state = {}) {
    if (!container) {
        return;
    }

    container.innerHTML = `
        <div class="content-header">
            <h1>음식점 관리</h1>
            <a href="/admin/pages/restaurant-create.html" class="btn btn-primary" id="restaurants-create-btn">음식점 등록</a>
        </div>

        <div class="search-bar">
            <input type="text" id="searchName" placeholder="음식점 이름 검색" value="${state.searchName || ''}">
            <input type="text" id="searchAddress" placeholder="주소 검색" value="${state.searchAddress || ''}">
            <button class="btn btn-secondary" id="searchRestaurants">검색</button>
        </div>

        <div class="table-container">
            <table class="data-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>음식점명</th>
                        <th>주소</th>
                        <th>음식 카테고리</th>
                        <th>등록일</th>
                        <th>상태</th>
                        <th>작업</th>
                    </tr>
                </thead>
                <tbody id="restaurantList">
                    <tr>
                        <td colspan="7" class="loading">로딩 중...</td>
                    </tr>
                </tbody>
            </table>
        </div>

        <div class="pagination" id="pagination"></div>
    `;
}

async function loadRestaurants(page = 0, state = {}) {
    try {
		const filterState = getRestaurantFilterState({ ...state, page });
        const params = {
            page,
            size: pageSize,
            sort: 'createdAt,desc'
        };

        if (filterState.searchName) {
            params.name = filterState.searchName;
        }

        if (filterState.searchAddress) {
            params.address = filterState.searchAddress;
        }

        const result = await window.getRestaurants(params);
        displayRestaurants(result.data);
        displayPagination(result.data);
    } catch (error) {
        alert(`음식점 목록을 불러오는데 실패했습니다: ${error.message}`);
    }
}

function displayRestaurants(data) {
    const tbody = document.getElementById('restaurantList');
    if (!tbody) {
        return;
    }

    if (!data.content || data.content.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="loading">등록된 음식점이 없습니다.</td></tr>';
        return;
    }

    tbody.innerHTML = data.content.map((restaurant) => `
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
                <button class="btn btn-secondary" data-action="edit-restaurant" data-restaurant-id="${restaurant.id}">수정</button>
                <button class="btn btn-primary" data-action="menu-restaurant" data-restaurant-id="${restaurant.id}">메뉴</button>
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
        html += `<button data-page="${currentPage - 1}">이전</button>`;
    }

    for (let i = Math.max(0, currentPage - 2); i < Math.min(totalPages, currentPage + 3); i++) {
        html += `<button class="${i === currentPage ? 'active' : ''}" data-page="${i}">${i + 1}</button>`;
    }

    if (currentPage < totalPages - 1) {
        html += `<button data-page="${currentPage + 1}">다음</button>`;
    }

    if (pagination) {
        pagination.innerHTML = html;
    }
}

function mount(state) {
    const cleanups = [];
	const initialState = getRestaurantFilterState(state);
	applyRestaurantFiltersFromState(initialState);

    const searchBtn = document.getElementById('searchRestaurants');
    if (searchBtn) {
        const clickHandler = () => {
			const filters = getRestaurantFiltersFromInputs();
			if (window.AdminUtils?.navigateTo) {
				window.AdminUtils.navigateTo(getRestaurantListPath(0, { ...filters, page: 0 }));
				return;
			}
			loadRestaurants(0, { ...filters, page: 0 });
		};
        searchBtn.addEventListener('click', clickHandler);
        cleanups.push(() => searchBtn.removeEventListener('click', clickHandler));
    }

    const table = document.getElementById('restaurantList');
    if (table) {
        const rowHandler = (event) => {
            const actionButton = event.target.closest('[data-action]');
            if (!actionButton) {
                return;
            }

            const restaurantId = actionButton.dataset.restaurantId;
            if (!restaurantId) {
                return;
            }

            if (actionButton.dataset.action === 'edit-restaurant') {
                window.AdminUtils?.navigateTo(`/admin/pages/restaurant-edit.html?id=${restaurantId}`);
                return;
            }

            if (actionButton.dataset.action === 'menu-restaurant') {
                window.AdminUtils?.navigateTo(`/admin/pages/restaurant-menu.html?id=${restaurantId}`);
            }
        };

        table.closest('table')?.addEventListener('click', rowHandler);
        cleanups.push(() => {
            table.closest('table')?.removeEventListener('click', rowHandler);
        });
    }

    const pagination = document.getElementById('pagination');
    if (pagination) {
        const paginateHandler = (event) => {
            const pageButton = event.target.closest('button[data-page]');
            if (!pageButton) {
                return;
            }

            const page = Number(pageButton.dataset.page);
            if (!Number.isNaN(page)) {
				const filters = getRestaurantFiltersFromInputs();
				if (window.AdminUtils?.navigateTo) {
					window.AdminUtils.navigateTo(getRestaurantListPath(page, filters));
					return;
				}
                loadRestaurants(page, filters);
            }
        };

        pagination.addEventListener('click', paginateHandler);
        cleanups.push(() => pagination.removeEventListener('click', paginateHandler));
    }

    cleanup = cleanups;
    loadRestaurants(initialState.page, initialState);
}

function unmount() {
    cleanup.forEach((remove) => remove());
    cleanup = [];
}

window.restaurantsView = {
    render,
    mount,
    unmount
};
