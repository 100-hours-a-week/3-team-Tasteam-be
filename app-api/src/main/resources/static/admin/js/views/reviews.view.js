let reviewsCleanup = [];
let reviewsCurrentPage = 0;
const REVIEW_PAGE_SIZE = 20;

function renderReviews(container) {
	container.innerHTML = `
        <div class="content-header">
            <h1>리뷰 관리</h1>
        </div>

        <div class="filter-section">
            <div class="filter-row">
                <input type="number" id="restaurantIdFilter" placeholder="음식점 ID로 필터" min="1">
                <button class="btn btn-secondary" id="searchReviewsBtn">검색</button>
                <button class="btn btn-secondary" id="resetReviewsBtn">초기화</button>
            </div>
        </div>

        <div id="reviewsTableContainer">
            <table class="data-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>음식점</th>
                        <th>작성자</th>
                        <th>내용</th>
                        <th>추천</th>
                        <th>작성일</th>
                        <th>관리</th>
                    </tr>
                </thead>
                <tbody id="reviewsTableBody">
                    <tr><td colspan="7">로딩 중...</td></tr>
                </tbody>
            </table>
        </div>

        <div class="pagination" id="reviewsPagination"></div>
    `;
}

function getReviewFilterState(state = {}) {
	const page = Number.parseInt(state.page || '0', 10) || 0;
	const rawRestaurantId = state.restaurantId || '';
	const restaurantId = rawRestaurantId ? parseInt(rawRestaurantId, 10) : null;
	return {
		page,
		restaurantId: Number.isNaN(restaurantId) ? null : restaurantId
	};
}

function getRestaurantIdFilterFromInputs() {
	const input = document.getElementById('restaurantIdFilter');
	const value = input?.value?.trim();
	const parsed = value ? parseInt(value, 10) : null;
	return parsed && Number.isNaN(parsed) ? null : parsed;
}

function applyReviewFiltersFromState(state = {}) {
	const filter = getReviewFilterState(state);
	const input = document.getElementById('restaurantIdFilter');
	if (input) {
		input.value = filter.restaurantId || '';
	}
}

function renderReviewsRows(items = []) {
	const tbody = document.getElementById('reviewsTableBody');
	if (!tbody) {
		return;
	}

	if (!items || items.length === 0) {
		tbody.innerHTML = '<tr><td colspan="7">리뷰가 없습니다.</td></tr>';
		return;
	}

	tbody.innerHTML = items.map((review) => `
        <tr>
            <td>${review.id}</td>
            <td>
                <a href="/admin/pages/restaurant-edit.html?id=${review.restaurantId}">
                    ${review.restaurantName || '-'}
                </a>
            </td>
            <td>${review.memberNickname} (${review.memberId})</td>
            <td title="${review.content || ''}">${review.content ? review.content.slice(0, 80) : '-'}</td>
            <td>${review.isRecommended ? '👍 추천' : '👎 비추천'}</td>
            <td>${window.AdminUtils.toKoreanDateTime(review.createdAt)}</td>
            <td><button class="btn btn-danger btn-sm" data-action="delete" data-id="${review.id}">삭제</button></td>
        </tr>
    `).join('');
}

function renderReviewsPagination(pageData = {}) {
	const pagination = document.getElementById('reviewsPagination');
	if (!pagination) {
		return;
	}
	const totalPages = pageData.totalPages || 0;
	const current = pageData.number || 0;
	if (totalPages <= 1) {
		pagination.innerHTML = '';
		return;
	}

	let html = '';
	const start = Math.max(0, current - 2);
	const end = Math.min(totalPages - 1, current + 2);
	if (current > 0) {
		html += `<button class="btn btn-secondary btn-sm" data-page="${current - 1}">이전</button>`;
	}
	for (let i = start; i <= end; i++) {
		html += `<button class="btn ${i === current ? 'btn-primary' : 'btn-secondary'} btn-sm" data-page="${i}">${i + 1}</button>`;
	}
	if (current < totalPages - 1) {
		html += `<button class="btn btn-secondary btn-sm" data-page="${current + 1}">다음</button>`;
	}
	pagination.innerHTML = html;
}

function getReviewsPagePath(page, state = {}) {
	const filter = getReviewFilterState({ ...state, page });
	const query = new URLSearchParams({
		page: String(filter.page)
	});
	if (filter.restaurantId) {
		query.set('restaurantId', filter.restaurantId);
	}
	return `/admin/pages/reviews.html?${query.toString()}`;
}

function buildReviewParams(page = 0, state = {}) {
	const filter = getReviewFilterState({ ...state, page });
	const params = {
		page,
		size: REVIEW_PAGE_SIZE,
		sort: 'createdAt,desc'
	};
	if (filter.restaurantId) {
		params.restaurantId = filter.restaurantId;
	}
	return params;
}

async function loadReviews(page = 0, state = {}) {
	const tbody = document.getElementById('reviewsTableBody');
	if (tbody) {
		tbody.innerHTML = '<tr><td colspan="7">로딩 중...</td></tr>';
	}
	try {
		const data = await getReviews(buildReviewParams(page, state));
		const pageData = data?.data || data || {};
		renderReviewsRows(pageData.content || []);
		renderReviewsPagination(pageData);
		reviewsCurrentPage = page;
	} catch (error) {
		const target = document.getElementById('reviewsTableBody');
		if (target) {
			target.innerHTML = `<tr><td colspan="7">오류: ${error.message}</td></tr>`;
		}
	}
}

async function deleteReview(reviewId) {
	if (!window.confirm(`리뷰 #${reviewId}를 삭제하시겠습니까?`)) {
		return;
	}
	try {
		await adminDeleteReview(reviewId);
		await loadReviews(
			reviewsCurrentPage,
			getReviewFilterState({
				page: reviewsCurrentPage,
				restaurantId: getRestaurantIdFilterFromInputs()
			})
		);
	} catch (error) {
		alert(`삭제 실패: ${error.message}`);
	}
}

function mountReviews(state = {}) {
	reviewsCleanup = [];
	const initState = getReviewFilterState(state);
	applyReviewFiltersFromState(initState);
	loadReviews(initState.page, initState);

	const searchBtn = document.getElementById('searchReviewsBtn');
	if (searchBtn) {
		const searchHandler = () => {
			AdminUtils?.navigateTo(
				getReviewsPagePath(0, {
					restaurantId: getRestaurantIdFilterFromInputs()
				})
			);
		};
		searchBtn.addEventListener('click', searchHandler);
		reviewsCleanup.push(() => searchBtn.removeEventListener('click', searchHandler));
	}

	const resetBtn = document.getElementById('resetReviewsBtn');
	if (resetBtn) {
		const resetHandler = () => {
			const input = document.getElementById('restaurantIdFilter');
			if (input) {
				input.value = '';
			}
			AdminUtils?.navigateTo(getReviewsPagePath(0));
		};
		resetBtn.addEventListener('click', resetHandler);
		reviewsCleanup.push(() => resetBtn.removeEventListener('click', resetHandler));
	}

	const restaurantInput = document.getElementById('restaurantIdFilter');
	if (restaurantInput) {
		const enterHandler = (event) => {
			if (event.key === 'Enter') {
				AdminUtils?.navigateTo(
					getReviewsPagePath(0, {
						restaurantId: getRestaurantIdFilterFromInputs()
					})
				);
			}
		};
		restaurantInput.addEventListener('keydown', enterHandler);
		reviewsCleanup.push(() => restaurantInput.removeEventListener('keydown', enterHandler));
	}

	const pagination = document.getElementById('reviewsPagination');
	if (pagination) {
		const paginationHandler = (event) => {
			const button = event.target.closest('button[data-page]');
			if (!button) {
				return;
			}
			const next = Number.parseInt(button.dataset.page, 10);
			if (!Number.isNaN(next)) {
				AdminUtils?.navigateTo(
					getReviewsPagePath(
						next,
						{
							restaurantId: getRestaurantIdFilterFromInputs()
						}
					)
				);
			}
		};
		pagination.addEventListener('click', paginationHandler);
		reviewsCleanup.push(() => pagination.removeEventListener('click', paginationHandler));
	}

	const tableBody = document.getElementById('reviewsTableBody');
	if (tableBody) {
		const deleteHandler = (event) => {
			const button = event.target.closest('button[data-action="delete"]');
			if (!button) {
				return;
			}
			const id = Number.parseInt(button.dataset.id, 10);
			if (id) {
				deleteReview(id);
			}
		};
		tableBody.addEventListener('click', deleteHandler);
		reviewsCleanup.push(() => tableBody.removeEventListener('click', deleteHandler));
	}

	return () => {
		reviewsCleanup.forEach((remove) => remove());
		reviewsCleanup = [];
	};
}

window.reviewsView = {
	render: renderReviews,
	mount: mountReviews,
	unmount: () => {
		reviewsCleanup.forEach((remove) => remove());
		reviewsCleanup = [];
	}
};
