let announcementsCleanup = [];
let announcementsPage = 0;

function renderAnnouncements(container) {
	container.innerHTML = `
        <div class="content-header">
            <h1>공지사항 관리</h1>
            <button class="btn btn-primary" id="announcement-create-btn">공지사항 등록</button>
        </div>

        <div class="table-container">
            <table class="data-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>제목</th>
                        <th>등록일</th>
                        <th>수정일</th>
                        <th>작업</th>
                    </tr>
                </thead>
                <tbody id="announcementList">
                    <tr>
                        <td colspan="5" class="loading">로딩 중...</td>
                    </tr>
                </tbody>
            </table>
        </div>
        <div class="pagination" id="announcementPagination"></div>
    `;
}

function getAnnouncementsFilterState(state = {}) {
	return {
		page: Number.parseInt(state.page || '0', 10) || 0
	};
}

function renderAnnouncementsTable(announcements = []) {
	const tbody = document.getElementById('announcementList');
	if (!tbody) {
		return;
	}

	if (!announcements.length) {
		tbody.innerHTML = '<tr><td colspan="5" class="empty">등록된 공지사항이 없습니다.</td></tr>';
		return;
	}

	tbody.innerHTML = announcements.map((announcement) => `
        <tr>
            <td>${announcement.id}</td>
            <td>${window.AdminUtils.escapeHtml(announcement.title || '')}</td>
            <td>${window.AdminUtils.toKoreanDateTime(announcement.createdAt)}</td>
            <td>${window.AdminUtils.toKoreanDateTime(announcement.updatedAt)}</td>
            <td class="actions">
                <button class="btn btn-secondary btn-sm" data-action="edit" data-id="${announcement.id}">편집</button>
                <button class="btn btn-sm btn-danger" data-action="delete" data-id="${announcement.id}">삭제</button>
            </td>
        </tr>
    `).join('');
}

function renderAnnouncementsPagination(pageData = {}) {
	const pagination = document.getElementById('announcementPagination');
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

function getPageUrl(page) {
	return `/admin/pages/announcements.html?page=${page}`;
}

async function loadAnnouncements(page = 0) {
	try {
		const query = new URLSearchParams({
			page: page,
			size: 20,
			sort: 'createdAt,desc'
		});
		const response = await api.get(`/admin/announcements?${query.toString()}`);
		const pageData = response?.data ? response.data : response;
		const data = pageData || {};
		renderAnnouncementsTable(data.content || []);
		renderAnnouncementsPagination(data);
		announcementsPage = page;
	} catch (error) {
		alert(`공지사항 목록을 불러오지 못했습니다: ${error.message}`);
	}
}

async function deleteAnnouncement(id) {
	if (!window.confirm('정말 삭제하시겠습니까?')) {
		return;
	}
	try {
		await api.delete(`/admin/announcements/${id}`);
		alert('삭제되었습니다.');
		await loadAnnouncements(announcementsPage);
	} catch (error) {
		alert(`삭제 실패: ${error.message}`);
	}
}

function mountAnnouncements(state = {}) {
	announcementsCleanup = [];

	const initial = getAnnouncementsFilterState(state);
	loadAnnouncements(initial.page);

	const createBtn = document.getElementById('announcement-create-btn');
	if (createBtn) {
		const createHandler = () => {
			AdminUtils?.navigateTo('/admin/pages/announcement-create.html');
		};
		createBtn.addEventListener('click', createHandler);
		announcementsCleanup.push(() => createBtn.removeEventListener('click', createHandler));
	}

	const pagination = document.getElementById('announcementPagination');
	if (pagination) {
		const paginationHandler = (event) => {
			const target = event.target.closest('button[data-page]');
			if (!target) {
				return;
			}
			const page = Number.parseInt(target.dataset.page, 10);
			if (Number.isNaN(page)) {
				return;
			}
			AdminUtils?.navigateTo(getPageUrl(page));
		};
		pagination.addEventListener('click', paginationHandler);
		announcementsCleanup.push(() => pagination.removeEventListener('click', paginationHandler));
	}

	const table = document.getElementById('announcementList');
	if (table) {
		const tableHandler = (event) => {
			const button = event.target.closest('button[data-action]');
			if (!button) {
				return;
			}

			const id = Number.parseInt(button.dataset.id, 10);
			if (!id) {
				return;
			}

			if (button.dataset.action === 'edit') {
				AdminUtils?.navigateTo(`/admin/pages/announcement-edit.html?id=${id}`);
				return;
			}

			if (button.dataset.action === 'delete') {
				deleteAnnouncement(id);
			}
		};
		table.addEventListener('click', tableHandler);
		announcementsCleanup.push(() => table.removeEventListener('click', tableHandler));
	}

	return () => {
		announcementsCleanup.forEach((remove) => remove());
		announcementsCleanup = [];
	};
}

window.announcementsView = {
	render: renderAnnouncements,
	mount: mountAnnouncements,
	unmount: () => {
		announcementsCleanup.forEach((remove) => remove());
		announcementsCleanup = [];
	}
};
