checkAuth();

let currentPage = 0;
const pageSize = 20;

async function loadGroups(page = 0) {
    try {
        const params = {
            page,
            size: pageSize,
            sort: 'createdAt,desc'
        };

        const result = await getGroups(params);
        displayGroups(result.data);
        displayPagination(result.data);
    } catch (error) {
        alert('그룹 목록을 불러오는데 실패했습니다: ' + error.message);
    }
}

function displayGroups(data) {
    const tbody = document.getElementById('groupList');

    if (!data.content || data.content.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="loading">등록된 그룹이 없습니다.</td></tr>';
        return;
    }

    tbody.innerHTML = data.content.map(group => `
        <tr>
            <td>${group.id}</td>
            <td>${group.name}</td>
            <td>${getGroupTypeLabel(group.type)}</td>
            <td>${group.address}</td>
            <td>${getJoinTypeLabel(group.joinType)}</td>
            <td>${getStatusLabel(group.status)}</td>
            <td>${new Date(group.createdAt).toLocaleDateString()}</td>
        </tr>
    `).join('');
}

function displayPagination(data) {
    const pagination = document.getElementById('pagination');
    const totalPages = data.totalPages;
    currentPage = data.number;

    let html = '';

    if (currentPage > 0) {
        html += `<button onclick="loadGroups(${currentPage - 1})">이전</button>`;
    }

    for (let i = Math.max(0, currentPage - 2); i < Math.min(totalPages, currentPage + 3); i++) {
        html += `<button class="${i === currentPage ? 'active' : ''}" onclick="loadGroups(${i})">${i + 1}</button>`;
    }

    if (currentPage < totalPages - 1) {
        html += `<button onclick="loadGroups(${currentPage + 1})">다음</button>`;
    }

    pagination.innerHTML = html;
}

function getGroupTypeLabel(type) {
    const labels = {
        'COMPANY': '회사',
        'SCHOOL': '학교',
        'CLUB': '동아리',
        'OTHER': '기타'
    };
    return labels[type] || type;
}

function getJoinTypeLabel(type) {
    const labels = {
        'ANYONE': '누구나',
        'APPROVAL': '승인 필요',
        'EMAIL_DOMAIN': '이메일 도메인'
    };
    return labels[type] || type;
}

function getStatusLabel(status) {
    const labels = {
        'ACTIVE': '활성',
        'INACTIVE': '비활성',
        'DELETED': '삭제됨'
    };
    return labels[status] || status;
}

function openCreateModal() {
    document.getElementById('createModal').style.display = 'block';
}

function closeCreateModal() {
    document.getElementById('createModal').style.display = 'none';
    document.getElementById('groupForm').reset();
    document.getElementById('emailDomainGroup').style.display = 'none';
}

document.addEventListener('DOMContentLoaded', () => {
    loadGroups();

    document.getElementById('groupJoinType').addEventListener('change', (e) => {
        const emailDomainGroup = document.getElementById('emailDomainGroup');
        if (e.target.value === 'EMAIL_DOMAIN') {
            emailDomainGroup.style.display = 'block';
        } else {
            emailDomainGroup.style.display = 'none';
        }
    });

    document.getElementById('groupForm').addEventListener('submit', async (e) => {
        e.preventDefault();

        const joinType = document.getElementById('groupJoinType').value;
        const emailDomain = document.getElementById('groupEmailDomain').value;

        if (joinType === 'EMAIL_DOMAIN' && !emailDomain) {
            alert('이메일 도메인을 입력해주세요.');
            return;
        }

        const data = {
            name: document.getElementById('groupName').value,
            type: document.getElementById('groupType').value,
            address: document.getElementById('groupAddress').value,
            detailAddress: document.getElementById('groupDetailAddress').value || null,
            joinType: joinType,
            emailDomain: joinType === 'EMAIL_DOMAIN' ? emailDomain : null
        };

        try {
            await createGroup(data);
            alert('그룹이 생성되었습니다.');
            closeCreateModal();
            loadGroups();
        } catch (error) {
            alert('그룹 생성에 실패했습니다: ' + error.message);
        }
    });

    window.onclick = function(event) {
        const modal = document.getElementById('createModal');
        if (event.target === modal) {
            closeCreateModal();
        }
    }
});
