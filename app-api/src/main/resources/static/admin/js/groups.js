checkAuth();

let currentPage = 0;
const pageSize = 20;
let logoFile = null;
let geocodeTimer = null;
let locationManual = false;

async function applyGeocodeResult(query) {
    const latInput = document.getElementById('groupLatitude');
    const lngInput = document.getElementById('groupLongitude');
    if (locationManual && latInput.value.trim() && lngInput.value.trim()) {
        return;
    }
    if (!query || query.trim().length < 4) {
        latInput.value = '';
        lngInput.value = '';
        return;
    }
    try {
        const result = await geocodeAddress(query.trim());
        latInput.value = result.data?.latitude ?? '';
        lngInput.value = result.data?.longitude ?? '';
    } catch (error) {
        latInput.value = '';
        lngInput.value = '';
    }
}

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
        tbody.innerHTML = '<tr><td colspan="8" class="loading">등록된 그룹이 없습니다.</td></tr>';
        return;
    }

    tbody.innerHTML = data.content.map(group => `
        <tr>
            <td>${group.id}</td>
            <td>${group.logoImageUrl ? `<img class="table-thumbnail" src="${group.logoImageUrl}" alt="${group.name}">` : '-'}</td>
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
        'OFFICIAL': '공식',
        'UNOFFICIAL': '비공식'
    };
    return labels[type] || type;
}

function getJoinTypeLabel(type) {
    const labels = {
        'EMAIL': '이메일 인증',
        'PASSWORD': '비밀번호'
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
    const logoPreview = document.getElementById('groupLogoPreview');
    const logoInput = document.getElementById('groupLogoImage');
    if (logoPreview) {
        logoPreview.innerHTML = '';
    }
    if (logoInput) {
        logoInput.value = '';
    }
    logoFile = null;
    const latInput = document.getElementById('groupLatitude');
    const lngInput = document.getElementById('groupLongitude');
    if (latInput) {
        latInput.value = '';
    }
    if (lngInput) {
        lngInput.value = '';
    }
}

document.addEventListener('DOMContentLoaded', () => {
    loadGroups();

    document.getElementById('groupJoinType').addEventListener('change', (e) => {
        const emailDomainGroup = document.getElementById('emailDomainGroup');
        if (e.target.value === 'EMAIL') {
            emailDomainGroup.style.display = 'block';
        } else {
            emailDomainGroup.style.display = 'none';
        }
    });

    const addressInput = document.getElementById('groupAddress');
    const triggerGeocode = () => {
        const query = addressInput.value.trim();
        clearTimeout(geocodeTimer);
        geocodeTimer = setTimeout(async () => {
            await applyGeocodeResult(query);
        }, 600);
    };
    addressInput.addEventListener('input', triggerGeocode);
    addressInput.addEventListener('blur', triggerGeocode);

    const latInput = document.getElementById('groupLatitude');
    const lngInput = document.getElementById('groupLongitude');
    const handleManualLocation = () => {
        const latValue = latInput.value.trim();
        const lngValue = lngInput.value.trim();
        if (latValue || lngValue) {
            locationManual = true;
        } else {
            locationManual = false;
        }
    };
    latInput.addEventListener('input', handleManualLocation);
    lngInput.addEventListener('input', handleManualLocation);

    const logoInput = document.getElementById('groupLogoImage');
    const logoPreview = document.getElementById('groupLogoPreview');
    logoInput.addEventListener('change', () => {
        const files = Array.from(logoInput.files || []);
        logoFile = files.length > 0 ? files[0] : null;
        logoPreview.innerHTML = '';
        if (logoFile) {
            const img = document.createElement('img');
            img.className = 'image-preview';
            img.alt = logoFile.name;
            img.src = URL.createObjectURL(logoFile);
            logoPreview.appendChild(img);
        }
    });

    document.getElementById('groupForm').addEventListener('submit', async (e) => {
        e.preventDefault();

        const joinType = document.getElementById('groupJoinType').value;
        const emailDomain = document.getElementById('groupEmailDomain').value;

        if (joinType === 'EMAIL' && !emailDomain) {
            alert('이메일 도메인을 입력해주세요.');
            return;
        }

        let logoImageFileUuid = null;
        if (logoFile) {
            try {
                const presigned = await createPresignedUploads('GROUP_IMAGE', [logoFile]);
                const upload = presigned.data?.uploads?.[0];
                if (upload) {
                    await uploadToPresigned(upload, logoFile);
                    logoImageFileUuid = upload.fileUuid;
                }
            } catch (error) {
                alert('로고 이미지 업로드에 실패했습니다: ' + error.message);
                return;
            }
        }

        const data = {
            name: document.getElementById('groupName').value,
            type: document.getElementById('groupType').value,
            address: document.getElementById('groupAddress').value,
            detailAddress: document.getElementById('groupDetailAddress').value || null,
            joinType: joinType,
            emailDomain: joinType === 'EMAIL' ? emailDomain : null,
            logoImageFileUuid: logoImageFileUuid
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
