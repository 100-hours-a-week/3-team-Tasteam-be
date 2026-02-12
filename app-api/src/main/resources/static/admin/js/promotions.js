let currentPage = 0;
let currentFilters = {};

async function loadPromotions(page = 0) {
    const params = new URLSearchParams({
        page: page,
        size: 20,
        sort: 'createdAt,desc'
    });

    if (currentFilters.promotionStatus) params.append('promotionStatus', currentFilters.promotionStatus);
    if (currentFilters.publishStatus) params.append('publishStatus', currentFilters.publishStatus);
    if (currentFilters.displayStatus) params.append('displayStatus', currentFilters.displayStatus);

    try {
        const data = await api.get(`/admin/promotions?${params.toString()}`);
        renderPromotions(data.content);
        renderPagination(data);
        currentPage = page;
    } catch (error) {
        console.error('프로모션 목록 로딩 실패:', error);
        alert('프로모션 목록을 불러오는데 실패했습니다.');
    }
}

function renderPromotions(promotions) {
    const tbody = document.getElementById('promotionList');

    if (promotions.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" class="empty">등록된 프로모션이 없습니다.</td></tr>';
        return;
    }

    tbody.innerHTML = promotions.map(promotion => `
        <tr>
            <td>${promotion.id}</td>
            <td>${escapeHtml(promotion.title)}</td>
            <td><span class="badge badge-${getPromotionStatusClass(promotion.promotionStatus)}">${getPromotionStatusText(promotion.promotionStatus)}</span></td>
            <td><span class="badge badge-${getDisplayStatusClass(promotion.displayStatus)}">${getDisplayStatusText(promotion.displayStatus)}</span></td>
            <td><span class="badge badge-${getPublishStatusClass(promotion.publishStatus)}">${getPublishStatusText(promotion.publishStatus)}</span></td>
            <td>${formatDateTime(promotion.promotionStartAt)}</td>
            <td>${formatDateTime(promotion.promotionEndAt)}</td>
            <td>${getDisplayChannelText(promotion.displayChannel)}</td>
            <td class="actions">
                <button class="btn btn-sm" onclick="editPromotion(${promotion.id})">편집</button>
                <button class="btn btn-sm btn-danger" onclick="deletePromotion(${promotion.id})">삭제</button>
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
        html += `<button onclick="loadPromotions(${currentPage - 1})">이전</button>`;
    }

    for (let i = 0; i < totalPages; i++) {
        if (i === currentPage) {
            html += `<button class="active">${i + 1}</button>`;
        } else {
            html += `<button onclick="loadPromotions(${i})">${i + 1}</button>`;
        }
    }

    if (currentPage < totalPages - 1) {
        html += `<button onclick="loadPromotions(${currentPage + 1})">다음</button>`;
    }

    pagination.innerHTML = html;
}

function applyFilters() {
    currentFilters = {
        promotionStatus: document.getElementById('filterPromotionStatus').value,
        publishStatus: document.getElementById('filterPublishStatus').value,
        displayStatus: document.getElementById('filterDisplayStatus').value
    };
    loadPromotions(0);
}

function editPromotion(id) {
    location.href = `/admin/pages/promotion-edit.html?id=${id}`;
}

async function deletePromotion(id) {
    if (!confirm('정말 삭제하시겠습니까?')) {
        return;
    }

    try {
        await api.delete(`/admin/promotions/${id}`);
        alert('삭제되었습니다.');
        loadPromotions(currentPage);
    } catch (error) {
        console.error('프로모션 삭제 실패:', error);
        alert('삭제에 실패했습니다.');
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
    loadPromotions(0);
});
