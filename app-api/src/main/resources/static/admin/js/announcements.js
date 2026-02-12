let currentPage = 0;

async function loadAnnouncements(page = 0) {
    const params = new URLSearchParams({
        page: page,
        size: 20,
        sort: 'createdAt,desc'
    });

    try {
        const data = await api.get(`/admin/announcements?${params.toString()}`);
        renderAnnouncements(data.content);
        renderPagination(data);
        currentPage = page;
    } catch (error) {
        console.error('공지사항 목록 로딩 실패:', error);
        alert('공지사항 목록을 불러오는데 실패했습니다.');
    }
}

function renderAnnouncements(announcements) {
    const tbody = document.getElementById('announcementList');

    if (announcements.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="empty">등록된 공지사항이 없습니다.</td></tr>';
        return;
    }

    tbody.innerHTML = announcements.map(announcement => `
        <tr>
            <td>${announcement.id}</td>
            <td>${escapeHtml(announcement.title)}</td>
            <td>${formatDateTime(announcement.createdAt)}</td>
            <td>${formatDateTime(announcement.updatedAt)}</td>
            <td class="actions">
                <button class="btn btn-sm" onclick="editAnnouncement(${announcement.id})">편집</button>
                <button class="btn btn-sm btn-danger" onclick="deleteAnnouncement(${announcement.id})">삭제</button>
            </td>
        </tr>
    `).join('');
}

function renderPagination(pageData) {
    const pagination = document.getElementById('pagination');
    const totalPages = pageData.totalPages;
    const currentPage = pageData.number;

    if (totalPages <= 1) {
        pagination.innerHTML = '';
        return;
    }

    let html = '';
    if (currentPage > 0) {
        html += `<button onclick="loadAnnouncements(${currentPage - 1})">이전</button>`;
    }

    for (let i = 0; i < totalPages; i++) {
        if (i === currentPage) {
            html += `<button class="active">${i + 1}</button>`;
        } else {
            html += `<button onclick="loadAnnouncements(${i})">${i + 1}</button>`;
        }
    }

    if (currentPage < totalPages - 1) {
        html += `<button onclick="loadAnnouncements(${currentPage + 1})">다음</button>`;
    }

    pagination.innerHTML = html;
}

function editAnnouncement(id) {
    location.href = `/admin/pages/announcement-edit.html?id=${id}`;
}

async function deleteAnnouncement(id) {
    if (!confirm('정말 삭제하시겠습니까?')) {
        return;
    }

    try {
        await api.delete(`/admin/announcements/${id}`);
        alert('삭제되었습니다.');
        loadAnnouncements(currentPage);
    } catch (error) {
        console.error('공지사항 삭제 실패:', error);
        alert('삭제에 실패했습니다.');
    }
}

function formatDateTime(isoString) {
    if (!isoString) return '-';
    const date = new Date(isoString);
    return date.toLocaleString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

document.addEventListener('DOMContentLoaded', () => {
    loadAnnouncements(0);
});
