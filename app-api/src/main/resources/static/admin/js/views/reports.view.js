let reportsCleanup = [];
let reportsCurrentPage = 0;
let currentReportId = null;
const REPORT_PAGE_SIZE = 20;

function renderReports(container) {
	container.innerHTML = `
        <div class="content-header">
            <h1>신고 관리</h1>
        </div>

        <div class="filter-section">
            <div class="filter-row">
                <select id="categoryFilter">
                    <option value="">전체 카테고리</option>
                    <option value="BUG">버그/오류</option>
                    <option value="INAPPROPRIATE_REVIEW">부적절한 리뷰</option>
                    <option value="INAPPROPRIATE_CONTENT">부적절한 콘텐츠</option>
                    <option value="RESTAURANT_INFO">음식점 정보 오류</option>
                    <option value="SPAM">스팸/광고</option>
                    <option value="OTHER">기타</option>
                </select>
                <select id="statusFilter">
                    <option value="">전체 상태</option>
                    <option value="PENDING">접수됨</option>
                    <option value="IN_PROGRESS">처리 중</option>
                    <option value="RESOLVED">해결됨</option>
                    <option value="REJECTED">반려</option>
                </select>
                <button class="btn btn-secondary" id="searchReportsBtn">검색</button>
                <button class="btn btn-secondary" id="resetReportsBtn">초기화</button>
            </div>
        </div>

        <div class="table-container">
            <table class="data-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>카테고리</th>
                        <th>신고자</th>
                        <th>내용</th>
                        <th>상태</th>
                        <th>접수일</th>
                        <th>상세보기</th>
                    </tr>
                </thead>
                <tbody id="reportsTableBody">
                    <tr><td colspan="7">로딩 중...</td></tr>
                </tbody>
            </table>
        </div>

        <div class="pagination" id="reportsPagination"></div>

        <div id="reportModal" class="modal is-hidden">
            <div class="modal-content">
                <span class="close" id="modalCloseBtn">&times;</span>
                <h2>신고 상세</h2>
                <table class="data-table" id="modalDetailTable">
                    <tbody></tbody>
                </table>
                <div class="modal-actions">
                    <label for="modalStatus"><strong>처리 상태 변경:</strong></label>
                    <select id="modalStatus" class="modal-status-select">
                        <option value="PENDING">접수됨</option>
                        <option value="IN_PROGRESS">처리 중</option>
                        <option value="RESOLVED">해결됨</option>
                        <option value="REJECTED">반려</option>
                    </select>
                    <button class="btn btn-primary" id="saveStatusBtn">저장</button>
                </div>
            </div>
        </div>
    `;
}

function getReportsState(state = {}) {
	return {
		category: state.category || '',
		status: state.status || '',
		page: Number.parseInt(state.page || '0', 10) || 0
	};
}

function applyReportsFiltersFromState(state = {}) {
	const filter = getReportsState(state);
	const categoryFilter = document.getElementById('categoryFilter');
	const statusFilter = document.getElementById('statusFilter');
	if (categoryFilter) {
		categoryFilter.value = filter.category;
	}
	if (statusFilter) {
		statusFilter.value = filter.status;
	}
}

function getFiltersFromInputs() {
	return {
		category: document.getElementById('categoryFilter')?.value || '',
		status: document.getElementById('statusFilter')?.value || ''
	};
}

function getReportQueryParams(page = 0, state = {}) {
	const filters = getReportsState({ ...state, page });
	const params = {
		page,
		size: REPORT_PAGE_SIZE,
		sort: 'createdAt,desc'
	};
	const category = filters.category;
	const status = filters.status;
	if (category) {
		params.category = category;
	}
	if (status) {
		params.status = status;
	}
	return params;
}

function formatReportDate(value) {
	return value ? window.AdminUtils.toKoreanDateTime(value) : '-';
}

function renderReportRows(items = []) {
	const tbody = document.getElementById('reportsTableBody');
	if (!tbody) {
		return;
	}

	const categoryLabels = {
		BUG: '버그/오류',
		INAPPROPRIATE_REVIEW: '부적절한 리뷰',
		INAPPROPRIATE_CONTENT: '부적절한 콘텐츠',
		RESTAURANT_INFO: '음식점 정보 오류',
		SPAM: '스팸/광고',
		OTHER: '기타'
	};
	const statusLabels = {
		PENDING: '접수됨',
		IN_PROGRESS: '처리 중',
		RESOLVED: '해결됨',
		REJECTED: '반려'
	};
	const statusClass = {
		PENDING: 'status-badge--draft',
		IN_PROGRESS: 'status-badge--ongoing',
		RESOLVED: 'status-badge--published',
		REJECTED: 'badge-deleted'
	};

	if (!items.length) {
		tbody.innerHTML = '<tr><td colspan="7">신고가 없습니다.</td></tr>';
		return;
	}

	tbody.innerHTML = items.map((report) => `
        <tr>
            <td>${report.id}</td>
            <td>${categoryLabels[report.category] || report.category || '-'}</td>
            <td>${report.memberNickname}</td>
            <td title="${window.AdminUtils.escapeHtml(report.contentPreview || '')}">
                ${(report.contentPreview || '').length > 50 ? `${window.AdminUtils.escapeHtml(report.contentPreview.slice(0, 50))}...` : window.AdminUtils.escapeHtml(report.contentPreview || '')}
            </td>
            <td><span class="status-badge ${statusClass[report.status] || 'status-badge'}">${statusLabels[report.status] || report.status}</span></td>
            <td>${formatReportDate(report.createdAt)}</td>
            <td><button class="btn btn-secondary btn-sm" data-action="open-detail" data-id="${report.id}">상세보기</button></td>
        </tr>
    `).join('');
}

function renderReportsPagination(pageData = {}) {
	const pagination = document.getElementById('reportsPagination');
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
	if (current > 0) {
		html += `<button class="btn btn-secondary btn-sm" data-page="${current - 1}">이전</button>`;
	}
	const start = Math.max(0, current - 2);
	const end = Math.min(totalPages - 1, current + 2);
	for (let i = start; i <= end; i++) {
		html += `<button class="btn ${i === current ? 'btn-primary' : 'btn-secondary'} btn-sm" data-page="${i}">${i + 1}</button>`;
	}
	if (current < totalPages - 1) {
		html += `<button class="btn btn-secondary btn-sm" data-page="${current + 1}">다음</button>`;
	}
	pagination.innerHTML = html;
}

function getReportPageUrl(page) {
	const filter = getReportsState({
		...getFiltersFromInputs(),
		page
	});
	const query = new URLSearchParams({
		page: String(filter.page)
	});
	if (filter.category) {
		query.set('category', filter.category);
	}
	if (filter.status) {
		query.set('status', filter.status);
	}
	return `/admin/pages/reports.html?${query.toString()}`;
}

async function loadReports(page = 0, state = {}) {
	try {
		const reportQuery = getReportQueryParams(page, state);
		const response = await getAdminReports(
			new URLSearchParams(reportQuery).toString()
				? {
					...reportQuery
				}
				: {}
		);
		const pageData = response?.data || response || {};
		renderReportRows(pageData.content || []);
		renderReportsPagination(pageData);
		reportsCurrentPage = page;
	} catch (error) {
		const tbody = document.getElementById('reportsTableBody');
		if (tbody) {
			tbody.innerHTML = `<tr><td colspan="7">오류: ${error.message}</td></tr>`;
		}
	}
}

async function openReportDetail(reportId) {
	try {
		const response = await getAdminReport(reportId);
		const report = response?.data || response;
		const categoryLabels = {
			BUG: '버그/오류',
			INAPPROPRIATE_REVIEW: '부적절한 리뷰',
			INAPPROPRIATE_CONTENT: '부적절한 콘텐츠',
			RESTAURANT_INFO: '음식점 정보 오류',
			SPAM: '스팸/광고',
			OTHER: '기타'
		};
		const statusLabels = {
			PENDING: '접수됨',
			IN_PROGRESS: '처리 중',
			RESOLVED: '해결됨',
			REJECTED: '반려'
		};
		const tbody = document.querySelector('#modalDetailTable tbody');
		if (tbody) {
			tbody.innerHTML = `
                <tr><th>ID</th><td>${report.id}</td></tr>
                <tr><th>신고자</th><td>${report.memberNickname} (${report.memberEmail || '-'})</td></tr>
                <tr><th>카테고리</th><td>${categoryLabels[report.category] || report.category}</td></tr>
                <tr><th>상태</th><td>${statusLabels[report.status] || report.status}</td></tr>
                <tr><th>접수일</th><td>${formatReportDate(report.createdAt)}</td></tr>
                <tr><th>수정일</th><td>${formatReportDate(report.updatedAt)}</td></tr>
                <tr><th>상세 내용</th><td class="detail-content-cell">${window.AdminUtils.escapeHtml(report.content || '-')}</td></tr>
            `;
		}

		const statusSelect = document.getElementById('modalStatus');
		if (statusSelect) {
			statusSelect.value = report.status;
		}
		currentReportId = report.id;
		const modal = document.getElementById('reportModal');
		if (modal) {
			modal.style.display = 'block';
		}
	} catch (error) {
		alert(`상세 조회 실패: ${error.message}`);
	}
}

async function saveReportStatus() {
	if (!currentReportId) {
		return;
	}
	const nextStatus = document.getElementById('modalStatus')?.value;
	try {
		await updateReportStatus(currentReportId, nextStatus);
		const modal = document.getElementById('reportModal');
		if (modal) {
			modal.style.display = 'none';
		}
		await loadReports(
			reportsCurrentPage,
			{
				...getFiltersFromInputs(),
				page: reportsCurrentPage
			}
		);
	} catch (error) {
		alert(`상태 변경 실패: ${error.message}`);
	}
}

function mountReports(state = {}) {
	reportsCleanup = [];
	currentReportId = null;
	const initState = getReportsState(state);
	applyReportsFiltersFromState(initState);
	loadReports(initState.page, initState);

	const searchBtn = document.getElementById('searchReportsBtn');
	if (searchBtn) {
		const searchHandler = () => {
			AdminUtils?.navigateTo(getReportPageUrl(0));
		};
		searchBtn.addEventListener('click', searchHandler);
		reportsCleanup.push(() => searchBtn.removeEventListener('click', searchHandler));
	}

	const resetBtn = document.getElementById('resetReportsBtn');
	if (resetBtn) {
		const resetHandler = () => {
			const category = document.getElementById('categoryFilter');
			const status = document.getElementById('statusFilter');
			if (category) {
				category.value = '';
			}
			if (status) {
				status.value = '';
			}
			AdminUtils?.navigateTo(getReportPageUrl(0));
		};
		resetBtn.addEventListener('click', resetHandler);
		reportsCleanup.push(() => resetBtn.removeEventListener('click', resetHandler));
	}

	const tableBody = document.getElementById('reportsTableBody');
	if (tableBody) {
		const openDetail = (event) => {
			const button = event.target.closest('button[data-action="open-detail"]');
			if (!button) {
				return;
			}
			const id = Number.parseInt(button.dataset.id, 10);
			if (id) {
				openReportDetail(id);
			}
		};
		tableBody.addEventListener('click', openDetail);
		reportsCleanup.push(() => tableBody.removeEventListener('click', openDetail));
	}

	const pagination = document.getElementById('reportsPagination');
	if (pagination) {
		const pageHandler = (event) => {
			const button = event.target.closest('button[data-page]');
			if (!button) {
				return;
			}
			const page = Number.parseInt(button.dataset.page, 10);
			if (!Number.isNaN(page)) {
				AdminUtils?.navigateTo(getReportPageUrl(page));
			}
		};
		pagination.addEventListener('click', pageHandler);
		reportsCleanup.push(() => pagination.removeEventListener('click', pageHandler));
	}

	const modalClose = document.getElementById('modalCloseBtn');
	if (modalClose) {
		const close = () => {
			const modal = document.getElementById('reportModal');
			if (modal) {
				modal.style.display = 'none';
			}
		};
		modalClose.addEventListener('click', close);
		reportsCleanup.push(() => modalClose.removeEventListener('click', close));
	}

	const saveBtn = document.getElementById('saveStatusBtn');
	if (saveBtn) {
		const saveHandler = () => {
			saveReportStatus();
		};
		saveBtn.addEventListener('click', saveHandler);
		reportsCleanup.push(() => saveBtn.removeEventListener('click', saveHandler));
	}

	const modal = document.getElementById('reportModal');
	if (modal) {
		const outsideClick = (event) => {
			if (event.target === modal) {
				modal.style.display = 'none';
			}
		};
		window.addEventListener('click', outsideClick);
		reportsCleanup.push(() => window.removeEventListener('click', outsideClick));
	}

	return () => {
		reportsCleanup.forEach((remove) => remove());
		reportsCleanup = [];
		currentReportId = null;
	};
}

window.reportsView = {
	render: renderReports,
	mount: mountReports,
	unmount: () => {
		reportsCleanup.forEach((remove) => remove());
		reportsCleanup = [];
	}
};
