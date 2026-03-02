let promotionsCleanup = [];
let currentPage = 0;

function renderPromotions(container) {
	if (!container) {
		return;
	}

	container.innerHTML = `
        <div class="content-header">
            <h1>프로모션 관리</h1>
            <a class="btn btn-primary" href="/admin/pages/promotion-create.html">프로모션 등록</a>
        </div>

        <div class="filter-bar">
            <select id="filterPromotionStatus">
                <option value="">전체 프로모션 상태</option>
                <option value="UPCOMING">예정</option>
                <option value="ONGOING">진행중</option>
                <option value="ENDED">종료</option>
            </select>
            <select id="filterPublishStatus">
                <option value="">전체 발행 상태</option>
                <option value="DRAFT">초안</option>
                <option value="PUBLISHED">발행</option>
                <option value="ARCHIVED">보관</option>
            </select>
            <select id="filterDisplayStatus">
                <option value="">전체 노출 상태</option>
                <option value="HIDDEN">숨김</option>
                <option value="SCHEDULED">예약</option>
                <option value="DISPLAYING">노출중</option>
                <option value="DISPLAY_ENDED">노출종료</option>
            </select>
            <button class="btn btn-secondary" id="applyPromotionFilterBtn">필터 적용</button>
        </div>

        <div class="table-container">
            <table class="data-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>제목</th>
                        <th>프로모션 상태</th>
                        <th>노출 상태</th>
                        <th>발행 상태</th>
                        <th>시작일</th>
                        <th>종료일</th>
                        <th>채널</th>
                        <th>작업</th>
                    </tr>
                </thead>
                <tbody id="promotionList">
                    <tr>
                        <td colspan="9" class="loading">로딩 중...</td>
                    </tr>
                </tbody>
            </table>
        </div>

        <div class="pagination" id="promotionPagination"></div>
    `;
}

function getPromotionFilterState(state = {}) {
	return {
		promotionStatus: state.promotionStatus || '',
		publishStatus: state.publishStatus || '',
		displayStatus: state.displayStatus || '',
		page: Number.parseInt(state.page || '0', 10) || 0
	};
}

function getFiltersFromInputs() {
	const promotionStatus = document.getElementById('filterPromotionStatus')?.value || '';
	const publishStatus = document.getElementById('filterPublishStatus')?.value || '';
	const displayStatus = document.getElementById('filterDisplayStatus')?.value || '';
	return { promotionStatus, publishStatus, displayStatus };
}

function getPagePath(page, state = {}) {
	const filterState = getPromotionFilterState({ ...state, page });
	const query = new URLSearchParams({
		page: String(filterState.page)
	});

	if (filterState.promotionStatus) {
		query.set('promotionStatus', filterState.promotionStatus);
	}
	if (filterState.publishStatus) {
		query.set('publishStatus', filterState.publishStatus);
	}
	if (filterState.displayStatus) {
		query.set('displayStatus', filterState.displayStatus);
	}

	return `/admin/pages/promotions.html?${query.toString()}`;
}

function renderPromotionRows(promotions) {
	const tbody = document.getElementById('promotionList');
	if (!tbody) {
		return;
	}
	if (!promotions || promotions.length === 0) {
		tbody.innerHTML = '<tr><td colspan="9" class="empty">등록된 프로모션이 없습니다.</td></tr>';
		return;
	}

	tbody.innerHTML = promotions.map((promotion) => `
        <tr>
            <td>${promotion.id}</td>
            <td>${window.AdminUtils.escapeHtml(promotion.title)}</td>
            <td><span class="badge badge-${getPromotionStatusClass(promotion.promotionStatus)}">${getPromotionStatusText(promotion.promotionStatus)}</span></td>
            <td><span class="badge badge-${getDisplayStatusClass(promotion.displayStatus)}">${getDisplayStatusText(promotion.displayStatus)}</span></td>
            <td><span class="badge badge-${getPublishStatusClass(promotion.publishStatus)}">${getPublishStatusText(promotion.publishStatus)}</span></td>
            <td>${formatDateTime(promotion.promotionStartAt)}</td>
            <td>${formatDateTime(promotion.promotionEndAt)}</td>
            <td>${getDisplayChannelText(promotion.displayChannel)}</td>
            <td class="actions">
                <button class="btn btn-sm" data-action="edit" data-id="${promotion.id}">편집</button>
                <button class="btn btn-sm btn-danger" data-action="delete" data-id="${promotion.id}">삭제</button>
            </td>
        </tr>
    `).join('');
}

function renderPromotionPagination(pageData) {
	const pagination = document.getElementById('promotionPagination');
	if (!pagination) {
		return;
	}

	const totalPages = pageData.totalPages || 0;
	const page = pageData.number || 0;

	if (totalPages <= 1) {
		pagination.innerHTML = '';
		return;
	}

	let html = '';
	if (page > 0) {
		html += `<button data-page="${page - 1}">이전</button>`;
	}
	for (let i = 0; i < totalPages; i++) {
		html += `<button class="${i === page ? 'active' : ''}" data-page="${i}">${i + 1}</button>`;
	}
	if (page < totalPages - 1) {
		html += `<button data-page="${page + 1}">다음</button>`;
	}

	pagination.innerHTML = html;
}

async function loadPromotions(page = 0, state = {}) {
	try {
		const params = new URLSearchParams({
			page: page,
			size: 20,
			sort: 'createdAt,desc'
		});
		const filters = getPromotionFilterState({ ...state, page });
		if (filters.promotionStatus) {
			params.append('promotionStatus', filters.promotionStatus);
		}
		if (filters.publishStatus) {
			params.append('publishStatus', filters.publishStatus);
		}
		if (filters.displayStatus) {
			params.append('displayStatus', filters.displayStatus);
		}

		const data = await window.api.get(`/admin/promotions?${params.toString()}`);
		renderPromotionRows(data?.content || []);
		renderPromotionPagination(data || { totalPages: 1, number: 0 });
		currentPage = page;
	} catch (error) {
		alert(`프로모션 목록을 불러오는데 실패했습니다: ${error.message}`);
	}
}

function applyFiltersFromState(state = {}) {
	const filterElements = {
		promotionStatus: document.getElementById('filterPromotionStatus'),
		publishStatus: document.getElementById('filterPublishStatus'),
		displayStatus: document.getElementById('filterDisplayStatus')
	};

	if (filterElements.promotionStatus) {
		filterElements.promotionStatus.value = state.promotionStatus || '';
	}
	if (filterElements.publishStatus) {
		filterElements.publishStatus.value = state.publishStatus || '';
	}
	if (filterElements.displayStatus) {
		filterElements.displayStatus.value = state.displayStatus || '';
	}
}

async function editPromotion(id) {
	window.AdminUtils.navigateTo(`/admin/pages/promotion-edit.html?id=${id}`);
}

async function deletePromotion(id) {
	if (!window.confirm('정말 삭제하시겠습니까?')) {
		return;
	}

	try {
		await window.api.delete(`/admin/promotions/${id}`);
		alert('삭제되었습니다.');
		loadPromotions(
			currentPage,
			getPromotionFilterState({
				...(getFiltersFromInputs()),
				page: currentPage
			})
		);
	} catch (error) {
		alert(`삭제 실패: ${error.message}`);
	}
}

function getPromotionStatusText(status) {
	const texts = {
		UPCOMING: '예정',
		ONGOING: '진행중',
		ENDED: '종료'
	};
	return texts[status] || status;
}

function getPromotionStatusClass(status) {
	const classes = {
		UPCOMING: 'info',
		ONGOING: 'success',
		ENDED: 'secondary'
	};
	return classes[status] || '';
}

function getDisplayStatusText(status) {
	const texts = {
		HIDDEN: '숨김',
		SCHEDULED: '예약',
		DISPLAYING: '노출중',
		DISPLAY_ENDED: '노출종료'
	};
	return texts[status] || status;
}

function getDisplayStatusClass(status) {
	const classes = {
		HIDDEN: 'secondary',
		SCHEDULED: 'info',
		DISPLAYING: 'success',
		DISPLAY_ENDED: 'warning'
	};
	return classes[status] || '';
}

function getPublishStatusText(status) {
	const texts = {
		DRAFT: '초안',
		PUBLISHED: '발행',
		ARCHIVED: '보관'
	};
	return texts[status] || status;
}

function getPublishStatusClass(status) {
	const classes = {
		DRAFT: 'warning',
		PUBLISHED: 'success',
		ARCHIVED: 'secondary'
	};
	return classes[status] || '';
}

function getDisplayChannelText(channel) {
	const texts = {
		MAIN_BANNER: '메인 배너',
		PROMOTION_LIST: '프로모션 목록',
		BOTH: '둘 다'
	};
	return texts[channel] || channel;
}

function formatDateTime(isoString) {
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

function mountPromotions(state = {}) {
	promotionsCleanup = [];

	const initialFilters = getPromotionFilterState(state);
	applyFiltersFromState(initialFilters);
	loadPromotions(initialFilters.page, initialFilters);

	const applyButton = document.getElementById('applyPromotionFilterBtn');
	if (applyButton) {
		const applyHandler = () => {
			window.AdminUtils.navigateTo(getPagePath(0, getFiltersFromInputs()));
		};
		applyButton.addEventListener('click', applyHandler);
		promotionsCleanup.push(() => applyButton.removeEventListener('click', applyHandler));
	}

	const pagination = document.getElementById('promotionPagination');
	if (pagination) {
		const paginationHandler = (event) => {
			const button = event.target.closest('button[data-page]');
			if (!button) {
				return;
			}
			const nextPage = Number.parseInt(button.dataset.page, 10);
			if (Number.isNaN(nextPage)) {
				return;
			}
			window.AdminUtils.navigateTo(getPagePath(
				nextPage,
				getFiltersFromInputs()
			));
		};
		pagination.addEventListener('click', paginationHandler);
		promotionsCleanup.push(() => pagination.removeEventListener('click', paginationHandler));
	}

	const table = document.getElementById('promotionList');
	if (table) {
		const tableHandler = async (event) => {
			const button = event.target.closest('button[data-action]');
			if (!button) {
				return;
			}
			const id = Number.parseInt(button.dataset.id, 10);
			if (!id) {
				return;
			}

			const action = button.dataset.action;
			if (action === 'edit') {
				await editPromotion(id);
				return;
			}
			if (action === 'delete') {
				await deletePromotion(id);
			}
		};
		table.addEventListener('click', tableHandler);
		promotionsCleanup.push(() => table.removeEventListener('click', tableHandler));
	}

	return () => {
		promotionsCleanup.forEach((remove) => remove());
		promotionsCleanup = [];
	};
}

window.promotionsView = {
	render: renderPromotions,
	mount: mountPromotions,
	unmount: () => {
		promotionsCleanup.forEach((remove) => remove());
		promotionsCleanup = [];
	}
};
