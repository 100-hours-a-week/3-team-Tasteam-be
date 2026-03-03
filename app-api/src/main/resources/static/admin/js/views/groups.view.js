let groupCleanup = [];
let groupCurrentPage = 0;
const PAGE_SIZE = 20;
let groupLogoFile = null;
let groupGeocodeTimer = null;
let isLocationManual = false;

function renderGroups(container) {
	container.innerHTML = `
        <div class="content-header">
            <h1>그룹 관리</h1>
            <button class="btn btn-primary" id="openCreateModalBtn">그룹 생성</button>
        </div>

        <div class="table-container">
            <table class="data-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>로고</th>
                        <th>그룹명</th>
                        <th>타입</th>
                        <th>주소</th>
                        <th>가입 타입</th>
                        <th>상태</th>
                        <th>생성일</th>
                    </tr>
                </thead>
                <tbody id="groupList">
                    <tr>
                        <td colspan="8" class="loading">로딩 중...</td>
                    </tr>
                </tbody>
            </table>
        </div>

        <div class="pagination" id="groupPagination"></div>

        <div id="createModal" class="modal">
            <div class="modal-content">
                <span class="close" id="closeCreateModalBtn">&times;</span>
                <h2>그룹 생성</h2>
                <form id="groupForm">
                    <div class="form-group">
                        <label for="groupName">그룹명 *</label>
                        <input type="text" id="groupName" required maxlength="100">
                    </div>
                    <div class="form-group">
                        <label for="groupLogoImage">로고 이미지</label>
                        <input type="file" id="groupLogoImage" accept="image/*">
                        <p class="help-text">로고 이미지는 선택 사항입니다.</p>
                    </div>
                    <div id="groupLogoPreview" class="image-preview-grid"></div>
                    <div class="form-group">
                        <label for="groupType">그룹 타입 *</label>
                        <select id="groupType" required>
                            <option value="">선택하세요</option>
                            <option value="OFFICIAL">공식</option>
                            <option value="UNOFFICIAL">비공식</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label for="groupAddress">주소 *</label>
                        <input type="text" id="groupAddress" required maxlength="255">
                    </div>
                    <div class="form-group">
                        <label for="groupDetailAddress">상세 주소</label>
                        <input type="text" id="groupDetailAddress" maxlength="255">
                    </div>
                    <div class="form-row">
                        <div class="form-group">
                            <label for="groupLatitude">위도</label>
                            <input type="text" id="groupLatitude">
                        </div>
                        <div class="form-group">
                            <label for="groupLongitude">경도</label>
                            <input type="text" id="groupLongitude">
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="groupJoinType">가입 타입 *</label>
                        <select id="groupJoinType" required>
                            <option value="">선택하세요</option>
                            <option value="EMAIL">이메일 인증</option>
                            <option value="PASSWORD">비밀번호</option>
                        </select>
                    </div>
                    <div class="form-group is-hidden" id="emailDomainGroup">
                        <label for="groupEmailDomain">이메일 도메인</label>
                        <input type="text" id="groupEmailDomain" maxlength="100" placeholder="example.com">
                    </div>
                    <div class="form-actions">
                        <button type="submit" class="btn btn-primary">생성</button>
                        <button type="button" class="btn btn-secondary" id="cancelCreateModalBtn">취소</button>
                    </div>
                </form>
            </div>
        </div>
    `;
}

function getGroupTypeLabel(type) {
	const labels = {
		OFFICIAL: '공식',
		UNOFFICIAL: '비공식'
	};
	return labels[type] || type;
}

function getJoinTypeLabel(type) {
	const labels = {
		EMAIL: '이메일 인증',
		PASSWORD: '비밀번호'
	};
	return labels[type] || type;
}

function getStatusLabel(status) {
	const labels = {
		ACTIVE: '활성',
		INACTIVE: '비활성',
		DELETED: '삭제됨'
	};
	return labels[status] || status;
}

function displayGroups(data) {
	const tbody = document.getElementById('groupList');
	if (!tbody) {
		return;
	}

	if (!data.content || data.content.length === 0) {
		tbody.innerHTML = '<tr><td colspan="8" class="loading">등록된 그룹이 없습니다.</td></tr>';
		return;
	}

	tbody.innerHTML = data.content.map((group) => `
        <tr>
            <td>${group.id}</td>
            <td>${group.logoImageUrl ? `<img class="table-thumbnail" src="${group.logoImageUrl}" alt="${group.name}">` : '-'}</td>
            <td>${window.AdminUtils.escapeHtml(group.name)}</td>
            <td>${getGroupTypeLabel(group.type)}</td>
            <td>${group.address}</td>
            <td>${getJoinTypeLabel(group.joinType)}</td>
            <td>${getStatusLabel(group.status)}</td>
            <td>${new Date(group.createdAt).toLocaleDateString()}</td>
        </tr>
    `).join('');
}

function displayGroupPagination(data) {
	const pagination = document.getElementById('groupPagination');
	if (!pagination) {
		return;
	}
	const totalPages = data.totalPages || 0;
	groupCurrentPage = data.number || 0;

	if (totalPages <= 1) {
		pagination.innerHTML = '';
		return;
	}

	let html = '';
	if (groupCurrentPage > 0) {
		html += `<button data-page="${groupCurrentPage - 1}">이전</button>`;
	}
	for (let i = Math.max(0, groupCurrentPage - 2); i < Math.min(totalPages, groupCurrentPage + 3); i++) {
		html += `<button class="${i === groupCurrentPage ? 'active' : ''}" data-page="${i}">${i + 1}</button>`;
	}
	if (groupCurrentPage < totalPages - 1) {
		html += `<button data-page="${groupCurrentPage + 1}">다음</button>`;
	}
	pagination.innerHTML = html;
}

function openCreateModal() {
	const modal = document.getElementById('createModal');
	if (modal) {
		modal.style.display = 'block';
	}
}

function resetCreateForm() {
	groupLogoFile = null;
	isLocationManual = false;
	const form = document.getElementById('groupForm');
	if (form) {
		form.reset();
	}
	const logoPreview = document.getElementById('groupLogoPreview');
	if (logoPreview) {
		logoPreview.innerHTML = '';
	}
	const logoInput = document.getElementById('groupLogoImage');
	if (logoInput) {
		logoInput.value = '';
	}
	const emailDomainGroup = document.getElementById('emailDomainGroup');
	if (emailDomainGroup) {
		emailDomainGroup.style.display = 'none';
	}
	const lat = document.getElementById('groupLatitude');
	const lng = document.getElementById('groupLongitude');
	if (lat) {
		lat.value = '';
	}
	if (lng) {
		lng.value = '';
	}
}

function closeCreateModal() {
	const modal = document.getElementById('createModal');
	if (modal) {
		modal.style.display = 'none';
	}
	resetCreateForm();
}

function getGroupPageQuery(state = {}) {
	return Number.parseInt(state.page || '0', 10) || 0;
}

function getPagePath(page) {
	return `/admin/pages/groups.html?page=${page}`;
}

async function loadGroups(page = 0) {
	try {
		const params = new URLSearchParams({
			page,
			size: PAGE_SIZE,
			sort: 'createdAt,desc'
		});
		const response = await getGroups(Object.fromEntries(params));
		const pageData = response?.data || response || {};
		displayGroups(pageData);
		displayGroupPagination(pageData);
	} catch (error) {
		alert(`그룹 목록을 불러오지 못했습니다: ${error.message}`);
	}
}

function mountGroups(state = {}) {
	groupCleanup = [];
	loadGroups(getGroupPageQuery(state));

	const openBtn = document.getElementById('openCreateModalBtn');
	if (openBtn) {
		const openHandler = () => {
			openCreateModal();
		};
		openBtn.addEventListener('click', openHandler);
		groupCleanup.push(() => openBtn.removeEventListener('click', openHandler));
	}

	const closeBtn = document.getElementById('closeCreateModalBtn');
	if (closeBtn) {
		const closeHandler = () => closeCreateModal();
		closeBtn.addEventListener('click', closeHandler);
		groupCleanup.push(() => closeBtn.removeEventListener('click', closeHandler));
	}

	const cancelBtn = document.getElementById('cancelCreateModalBtn');
	if (cancelBtn) {
		const cancelHandler = () => closeCreateModal();
		cancelBtn.addEventListener('click', cancelHandler);
		groupCleanup.push(() => cancelBtn.removeEventListener('click', cancelHandler));
	}

	const modal = document.getElementById('createModal');
	if (modal) {
		const outsideClose = (event) => {
			if (event.target === modal) {
				closeCreateModal();
			}
		};
		window.addEventListener('click', outsideClose);
		groupCleanup.push(() => window.removeEventListener('click', outsideClose));
	}

	const joinType = document.getElementById('groupJoinType');
	if (joinType) {
		const changeJoinType = (event) => {
			const emailDomainGroup = document.getElementById('emailDomainGroup');
			if (!emailDomainGroup) {
				return;
			}
			if (event.target.value === 'EMAIL') {
				emailDomainGroup.style.display = 'block';
			} else {
				emailDomainGroup.style.display = 'none';
			}
		};
		joinType.addEventListener('change', changeJoinType);
		groupCleanup.push(() => joinType.removeEventListener('change', changeJoinType));
	}

	const addressInput = document.getElementById('groupAddress');
	if (addressInput) {
		const triggerGeocode = () => {
			const query = addressInput.value.trim();
			clearTimeout(groupGeocodeTimer);
			groupGeocodeTimer = setTimeout(async () => {
				if (isLocationManual || query.length < 4) {
					return;
				}
				try {
					const result = await geocodeAddress(query);
					const latInput = document.getElementById('groupLatitude');
					const lngInput = document.getElementById('groupLongitude');
					if (latInput) {
						latInput.value = result.data?.latitude ?? '';
					}
					if (lngInput) {
						lngInput.value = result.data?.longitude ?? '';
					}
				} catch (error) {
					const latInput = document.getElementById('groupLatitude');
					const lngInput = document.getElementById('groupLongitude');
					if (latInput) {
						latInput.value = '';
					}
					if (lngInput) {
						lngInput.value = '';
					}
				}
			}, 600);
		};
		addressInput.addEventListener('input', triggerGeocode);
		groupCleanup.push(() => addressInput.removeEventListener('input', triggerGeocode));
	}

	const latInput = document.getElementById('groupLatitude');
	const lngInput = document.getElementById('groupLongitude');
	if (latInput && lngInput) {
		const manualHandler = () => {
			isLocationManual = Boolean(latInput.value.trim() || lngInput.value.trim());
		};
		latInput.addEventListener('input', manualHandler);
		lngInput.addEventListener('input', manualHandler);
		groupCleanup.push(() => latInput.removeEventListener('input', manualHandler));
		groupCleanup.push(() => lngInput.removeEventListener('input', manualHandler));
	}

	const logoInput = document.getElementById('groupLogoImage');
	if (logoInput) {
		const preview = document.getElementById('groupLogoPreview');
		const logoHandler = async () => {
			const files = Array.from(logoInput.files || []);
			const file = files.length ? files[0] : null;
			if (!preview) {
				return;
			}
			preview.innerHTML = '';
			if (!file) {
				groupLogoFile = null;
				return;
			}

			try {
				groupLogoFile = await ImageOptimizer.optimizeGroupLogo(file);
				const img = document.createElement('img');
				img.className = 'image-preview';
				img.alt = groupLogoFile.name;
				img.src = URL.createObjectURL(groupLogoFile);
				preview.appendChild(img);
			} catch (error) {
				alert(`로고 최적화 실패: ${error.message}`);
				groupLogoFile = null;
			}
		};
		logoInput.addEventListener('change', logoHandler);
		groupCleanup.push(() => logoInput.removeEventListener('change', logoHandler));
	}

	const groupForm = document.getElementById('groupForm');
	if (groupForm) {
		const submitHandler = async (event) => {
			event.preventDefault();

			const joinTypeValue = document.getElementById('groupJoinType')?.value;
			const emailDomain = document.getElementById('groupEmailDomain')?.value?.trim() || null;
			if (joinTypeValue === 'EMAIL' && !emailDomain) {
				alert('이메일 도메인을 입력해주세요.');
				return;
			}

			try {
				let logoImageFileUuid = null;
				if (groupLogoFile) {
					const presigned = await createPresignedUploads('GROUP_IMAGE', [groupLogoFile]);
					const upload = presigned.data?.uploads?.[0];
					if (upload) {
						await uploadToPresigned(upload, groupLogoFile);
						logoImageFileUuid = upload.fileUuid;
					}
				}

				const data = {
					name: document.getElementById('groupName')?.value?.trim(),
					type: document.getElementById('groupType')?.value,
					address: document.getElementById('groupAddress')?.value?.trim(),
					detailAddress: document.getElementById('groupDetailAddress')?.value?.trim() || null,
					joinType: joinTypeValue,
					emailDomain: joinTypeValue === 'EMAIL' ? emailDomain : null,
					logoImageFileUuid
				};

				await createGroup(data);
				alert('그룹이 생성되었습니다.');
				closeCreateModal();
				loadGroups(groupCurrentPage);
			} catch (error) {
				alert(`그룹 생성 실패: ${error.message}`);
			}
		};
		groupForm.addEventListener('submit', submitHandler);
		groupCleanup.push(() => groupForm.removeEventListener('submit', submitHandler));
	}

	const pagination = document.getElementById('groupPagination');
	if (pagination) {
		const paginationHandler = (event) => {
			const target = event.target.closest('button[data-page]');
			if (!target) {
				return;
			}
			const nextPage = Number.parseInt(target.dataset.page, 10);
			if (Number.isNaN(nextPage)) {
				return;
			}
			AdminUtils?.navigateTo(getPagePath(nextPage));
		};
		pagination.addEventListener('click', paginationHandler);
		groupCleanup.push(() => pagination.removeEventListener('click', paginationHandler));
	}

	return () => {
		groupCleanup.forEach((remove) => remove());
		groupCleanup = [];
		groupLogoFile = null;
		if (groupGeocodeTimer) {
			clearTimeout(groupGeocodeTimer);
			groupGeocodeTimer = null;
		}
		closeCreateModal();
	};
}

window.groupsView = {
	render: renderGroups,
	mount: mountGroups,
	unmount: () => {
		groupCleanup.forEach((remove) => remove());
		groupCleanup = [];
	}
};
