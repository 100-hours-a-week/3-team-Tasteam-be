checkAuth();

let currentPage = 0;
const PAGE_SIZE = 20;
let currentReportId = null;

const CATEGORY_LABELS = {
    BUG: '버그/오류',
    INAPPROPRIATE_REVIEW: '부적절한 리뷰',
    INAPPROPRIATE_CONTENT: '부적절한 콘텐츠',
    RESTAURANT_INFO: '음식점 정보 오류',
    SPAM: '스팸/광고',
    OTHER: '기타'
};

const STATUS_LABELS = {
    PENDING: '접수됨',
    IN_PROGRESS: '처리 중',
    RESOLVED: '해결됨',
    REJECTED: '반려'
};

const STATUS_BADGE_CLASS = {
    PENDING: 'status-badge--draft',
    IN_PROGRESS: 'status-badge--ongoing',
    RESOLVED: 'status-badge--published',
    REJECTED: 'badge-deleted'
};

function formatDate(isoString) {
    if (!isoString) return '-';
    const d = new Date(isoString);
    return d.toLocaleDateString('ko-KR') + ' ' + d.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
}

function truncate(text, maxLen = 60) {
    if (!text) return '-';
    return text.length > maxLen ? text.slice(0, maxLen) + '...' : text;
}

function buildParams(page) {
    const params = { page, size: PAGE_SIZE, sort: 'createdAt,desc' };
    const category = document.getElementById('categoryFilter').value;
    const status = document.getElementById('statusFilter').value;
    if (category) params.category = category;
    if (status) params.status = status;
    return params;
}

function renderStatusBadge(status) {
    const cls = STATUS_BADGE_CLASS[status] || 'status-badge';
    return `<span class="status-badge ${cls}">${STATUS_LABELS[status] || status}</span>`;
}

function renderTable(reports) {
    const tbody = document.getElementById('reportsTableBody');
    if (!reports || reports.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7">신고가 없습니다.</td></tr>';
        return;
    }

    tbody.innerHTML = reports.map(r => `
        <tr>
            <td>${r.id}</td>
            <td>${CATEGORY_LABELS[r.category] || r.category}</td>
            <td>${r.memberNickname}</td>
            <td title="${r.contentPreview || ''}">${truncate(r.contentPreview)}</td>
            <td>${renderStatusBadge(r.status)}</td>
            <td>${formatDate(r.createdAt)}</td>
            <td><button class="btn btn-secondary btn-sm" onclick="openDetail(${r.id})">상세보기</button></td>
        </tr>
    `).join('');
}

function renderPagination(pageData) {
    const container = document.getElementById('pagination');
    const totalPages = pageData.totalPages || 0;
    const current = pageData.number || 0;

    if (totalPages <= 1) {
        container.innerHTML = '';
        return;
    }

    const start = Math.max(0, current - 2);
    const end = Math.min(totalPages - 1, current + 2);
    let html = '';

    if (current > 0) {
        html += `<button class="btn btn-secondary btn-sm" onclick="loadReports(${current - 1})">이전</button>`;
    }

    for (let i = start; i <= end; i++) {
        const active = i === current ? 'btn-primary' : 'btn-secondary';
        html += `<button class="btn ${active} btn-sm" onclick="loadReports(${i})">${i + 1}</button>`;
    }

    if (current < totalPages - 1) {
        html += `<button class="btn btn-secondary btn-sm" onclick="loadReports(${current + 1})">다음</button>`;
    }

    container.innerHTML = html;
}

async function loadReports(page = 0) {
    currentPage = page;
    const tbody = document.getElementById('reportsTableBody');
    tbody.innerHTML = '<tr><td colspan="7">로딩 중...</td></tr>';

    try {
        const result = await getAdminReports(buildParams(page));
        const pageData = result.data;
        renderTable(pageData.content || []);
        renderPagination(pageData);
    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="7">오류: ${error.message}</td></tr>`;
    }
}

async function openDetail(reportId) {
    currentReportId = reportId;
    try {
        const result = await getAdminReport(reportId);
        const r = result.data;

        const tbody = document.querySelector('#modalDetailTable tbody');
        tbody.innerHTML = `
            <tr><th>ID</th><td>${r.id}</td></tr>
            <tr><th>신고자</th><td>${r.memberNickname} (${r.memberEmail || '-'})</td></tr>
            <tr><th>카테고리</th><td>${CATEGORY_LABELS[r.category] || r.category}</td></tr>
            <tr><th>상태</th><td>${renderStatusBadge(r.status)}</td></tr>
            <tr><th>접수일</th><td>${formatDate(r.createdAt)}</td></tr>
            <tr><th>수정일</th><td>${formatDate(r.updatedAt)}</td></tr>
            <tr><th>상세 내용</th><td style="white-space:pre-wrap;">${r.content || '-'}</td></tr>
        `;

        document.getElementById('modalStatus').value = r.status;
        document.getElementById('reportModal').style.display = 'block';
    } catch (error) {
        alert('상세 조회 실패: ' + error.message);
    }
}

async function saveStatus() {
    if (!currentReportId) return;
    const status = document.getElementById('modalStatus').value;

    try {
        await updateReportStatus(currentReportId, status);
        document.getElementById('reportModal').style.display = 'none';
        await loadReports(currentPage);
    } catch (error) {
        alert('상태 변경 실패: ' + error.message);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    loadReports(0);

    document.getElementById('searchBtn').addEventListener('click', () => loadReports(0));
    document.getElementById('resetBtn').addEventListener('click', () => {
        document.getElementById('categoryFilter').value = '';
        document.getElementById('statusFilter').value = '';
        loadReports(0);
    });

    document.getElementById('modalClose').addEventListener('click', () => {
        document.getElementById('reportModal').style.display = 'none';
    });

    document.getElementById('saveStatusBtn').addEventListener('click', saveStatus);

    window.addEventListener('click', (e) => {
        const modal = document.getElementById('reportModal');
        if (e.target === modal) {
            modal.style.display = 'none';
        }
    });
});
