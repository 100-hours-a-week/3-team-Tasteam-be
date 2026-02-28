checkAuth();

let currentPage = 0;
const PAGE_SIZE = 20;

function getRestaurantIdFilter() {
    const val = document.getElementById('restaurantIdFilter').value.trim();
    return val ? parseInt(val) : null;
}

function buildParams(page) {
    const params = { page, size: PAGE_SIZE, sort: 'createdAt,desc' };
    const restaurantId = getRestaurantIdFilter();
    if (restaurantId) {
        params.restaurantId = restaurantId;
    }
    return params;
}

function formatDate(isoString) {
    if (!isoString) return '-';
    const d = new Date(isoString);
    return d.toLocaleDateString('ko-KR') + ' ' + d.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
}

function truncate(text, maxLen = 60) {
    if (!text) return '-';
    return text.length > maxLen ? text.slice(0, maxLen) + '...' : text;
}

function renderTable(reviews) {
    const tbody = document.getElementById('reviewsTableBody');
    if (!reviews || reviews.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7">리뷰가 없습니다.</td></tr>';
        return;
    }

    tbody.innerHTML = reviews.map(r => `
        <tr>
            <td>${r.id}</td>
            <td><a href="/admin/pages/restaurant-edit.html?id=${r.restaurantId}">${r.restaurantName}</a></td>
            <td>${r.memberNickname} (${r.memberId})</td>
            <td title="${r.content || ''}">${truncate(r.content)}</td>
            <td>${r.isRecommended ? '👍 추천' : '👎 비추천'}</td>
            <td>${formatDate(r.createdAt)}</td>
            <td>
                <button class="btn btn-danger btn-sm" onclick="handleDelete(${r.id})">삭제</button>
            </td>
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
        html += `<button class="btn btn-secondary btn-sm" onclick="loadReviews(${current - 1})">이전</button>`;
    }

    for (let i = start; i <= end; i++) {
        const active = i === current ? 'btn-primary' : 'btn-secondary';
        html += `<button class="btn ${active} btn-sm" onclick="loadReviews(${i})">${i + 1}</button>`;
    }

    if (current < totalPages - 1) {
        html += `<button class="btn btn-secondary btn-sm" onclick="loadReviews(${current + 1})">다음</button>`;
    }

    container.innerHTML = html;
}

async function loadReviews(page = 0) {
    currentPage = page;
    const tbody = document.getElementById('reviewsTableBody');
    tbody.innerHTML = '<tr><td colspan="7">로딩 중...</td></tr>';

    try {
        const result = await getReviews(buildParams(page));
        const pageData = result.data;
        renderTable(pageData.content || []);
        renderPagination(pageData);
    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="7">오류: ${error.message}</td></tr>`;
    }
}

async function handleDelete(reviewId) {
    if (!confirm(`리뷰 #${reviewId}를 삭제하시겠습니까?`)) {
        return;
    }

    try {
        await adminDeleteReview(reviewId);
        await loadReviews(currentPage);
    } catch (error) {
        alert('삭제 실패: ' + error.message);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    loadReviews(0);

    document.getElementById('searchBtn').addEventListener('click', () => {
        loadReviews(0);
    });

    document.getElementById('resetBtn').addEventListener('click', () => {
        document.getElementById('restaurantIdFilter').value = '';
        loadReviews(0);
    });

    document.getElementById('restaurantIdFilter').addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            loadReviews(0);
        }
    });
});
