let selectedPromotionBanner = null;
let selectedPromotionSplash = null;
let detailImageUrls = [];

function renderPromotionForm(container, state = {}) {
	container.innerHTML = `
        <div class="content-header">
            <h1>${state.mode === 'edit' ? '프로모션 수정' : '프로모션 등록'}</h1>
            <button class="btn btn-secondary" id="promotion-form-back-btn">목록으로</button>
        </div>

        <form id="promotionForm" class="form-container">
            <section class="form-section">
                <h2>기본 정보</h2>
                <div class="form-group">
                    <label for="title">제목 *</label>
                    <input type="text" id="title" name="title" required maxlength="200" placeholder="프로모션 제목을 입력하세요">
                </div>
                <div class="form-group">
                    <label for="content">내용 *</label>
                    <textarea id="content" name="content" required rows="5" placeholder="프로모션 상세 내용을 입력하세요"></textarea>
                </div>
                <div class="form-group">
                    <label for="landingUrl">랜딩 URL</label>
                    <input type="url" id="landingUrl" name="landingUrl" maxlength="500" placeholder="https://example.com">
                </div>
            </section>

            <section class="form-section">
                <h2>프로모션 기간</h2>
                <div class="form-group">
                    <label for="promotionStartAt">시작일시 *</label>
                    <input type="datetime-local" id="promotionStartAt" name="promotionStartAt" required>
                </div>
                <div class="form-group">
                    <label for="promotionEndAt">종료일시 *</label>
                    <input type="datetime-local" id="promotionEndAt" name="promotionEndAt" required>
                </div>
                <div class="form-group">
                    <label for="publishStatus">발행 상태 *</label>
                    <select id="publishStatus" name="publishStatus" required>
                        <option value="DRAFT">초안</option>
                        <option value="PUBLISHED" selected>발행</option>
                        <option value="ARCHIVED">보관</option>
                    </select>
                </div>
            </section>

            <section class="form-section">
                <h2>노출 설정</h2>
                <div class="form-group">
                    <label>
                        <input type="checkbox" id="displayEnabled" name="displayEnabled" checked>
                        노출 활성화
                    </label>
                </div>
                <div class="form-group">
                    <label for="displayStartAt">노출 시작일시 *</label>
                    <input type="datetime-local" id="displayStartAt" name="displayStartAt" required>
                </div>
                <div class="form-group">
                    <label for="displayEndAt">노출 종료일시 *</label>
                    <input type="datetime-local" id="displayEndAt" name="displayEndAt" required>
                </div>
                <div class="form-group">
                    <label for="displayChannel">노출 채널 *</label>
                    <select id="displayChannel" name="displayChannel" required>
                        <option value="MAIN_BANNER">메인 배너</option>
                        <option value="PROMOTION_LIST">프로모션 목록</option>
                        <option value="BOTH">둘 다</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="displayPriority">우선순위 *</label>
                    <input type="number" id="displayPriority" name="displayPriority" required min="1" value="1">
                </div>
            </section>

            <section class="form-section">
                <h2>배너 이미지</h2>
                <div class="form-group">
                    <label for="bannerImageUrl">이미지 URL *</label>
                    <input type="url" id="bannerImageUrl" name="bannerImageUrl" required maxlength="500">
                </div>
                <div class="form-group">
                    <label for="bannerImageFile">이미지 파일 업로드</label>
                    <input type="file" id="bannerImageFile" accept="image/*">
                    <p class="help-text">업로드 시 홈 배너 노출 비율(약 3.55:1)로 편집 후 저장됩니다.</p>
                </div>
                <div id="bannerPreview" class="promotion-image-preview-box"></div>
            </section>

            <section class="form-section">
                <h2>스플래시 이미지</h2>
                <div class="form-group">
                    <label for="splashImageUrl">이미지 URL *</label>
                    <input type="url" id="splashImageUrl" name="splashImageUrl" required maxlength="500">
                </div>
                <div class="form-group">
                    <label for="splashImageFile">이미지 파일 업로드</label>
                    <input type="file" id="splashImageFile" accept="image/*">
                    <p class="help-text">업로드 시 스플래시 노출 비율(4:3)로 편집 후 저장됩니다.</p>
                </div>
                <div id="splashPreview" class="promotion-image-preview-box promotion-image-preview-box--splash"></div>
            </section>

            <section class="form-section">
                <h2>배너 대체 텍스트</h2>
                <div class="form-group">
                    <label for="bannerImageAltText">대체 텍스트</label>
                    <input type="text" id="bannerImageAltText" name="bannerImageAltText" maxlength="200">
                </div>
            </section>

            <section class="form-section">
                <h2>상세 이미지</h2>
                <div class="form-group">
                    <label for="detailImagesFile">이미지 업로드 (여러 개 가능)</label>
                    <input type="file" id="detailImagesFile" accept="image/*" multiple>
                    <p class="help-text">상세 이미지는 자유 비율로 편집 후 등록할 수 있습니다.</p>
                </div>
                <div id="detailImagesList" class="detail-images-list"></div>
            </section>

            <div class="form-actions">
                <button type="submit" class="btn btn-primary">${state.mode === 'edit' ? '수정' : '등록'}</button>
                <button type="button" class="btn btn-secondary" id="promotion-form-cancel-btn">취소</button>
            </div>
        </form>
    `;
}

function getPromotionId() {
	const params = new URLSearchParams(window.location.search);
	return params.get('id');
}

function formatDatetimeLocal(value) {
	if (!value) {
		return '';
	}
	const date = new Date(value);
	const year = String(date.getFullYear()).padStart(4, '0');
	const month = String(date.getMonth() + 1).padStart(2, '0');
	const day = String(date.getDate()).padStart(2, '0');
	const hour = String(date.getHours()).padStart(2, '0');
	const minute = String(date.getMinutes()).padStart(2, '0');
	return `${year}-${month}-${day}T${hour}:${minute}`;
}

function parseDatetimeLocal(value) {
	if (!value) {
		return '';
	}
	const date = new Date(value);
	return date.toISOString();
}

async function uploadImageToS3(file) {
	const presignedResponse = await createPresignedUploads('COMMON_ASSET', [file]);
	const upload = presignedResponse?.data?.uploads?.[0];
	if (!upload) {
		throw new Error('업로드 정보가 유효하지 않습니다.');
	}
	await uploadToPresigned(upload, file);
	const urlResponse = await api.get(`/files/${upload.fileUuid}/url`);
	return urlResponse.url;
}

async function editImageIfNeeded(file, type) {
	if (!window.PromotionImageEditor || typeof window.PromotionImageEditor.editImage !== 'function') {
		return file;
	}
	return window.PromotionImageEditor.editImage({ file, type });
}

async function editAndUploadImage(file, type) {
	const editedFile = await editImageIfNeeded(file, type);
	if (!editedFile) {
		return null;
	}
	return uploadImageToS3(editedFile);
}

function renderSingleImagePreview(containerId, url, altText) {
	const preview = document.getElementById(containerId);
	if (!preview) {
		return;
	}
	if (!url) {
		preview.innerHTML = '';
		return;
	}

	preview.innerHTML = `<img src="${url}" alt="${altText}" class="promotion-image-preview-image">`;
}

function renderDetailImages() {
	const list = document.getElementById('detailImagesList');
	if (!list) {
		return;
	}

	list.innerHTML = detailImageUrls.map((url, index) => `
        <div class="detail-image-item">
            <img src="${url}" alt="상세 이미지 ${index + 1}">
            <button type="button" class="btn btn-sm btn-danger" data-action="remove-detail-image" data-index="${index}">삭제</button>
        </div>
    `).join('');
}

function collectPromotionPayload() {
	const bannerImageUrl = document.getElementById('bannerImageUrl')?.value?.trim();
	if (!bannerImageUrl) {
		throw new Error('배너 이미지 URL은 필수입니다.');
	}

	const splashImageUrl = document.getElementById('splashImageUrl')?.value?.trim();
	if (!splashImageUrl) {
		throw new Error('스플래시 이미지 URL은 필수입니다.');
	}

	const startAt = parseDatetimeLocal(document.getElementById('promotionStartAt')?.value);
	const endAt = parseDatetimeLocal(document.getElementById('promotionEndAt')?.value);
	const displayStartAt = parseDatetimeLocal(document.getElementById('displayStartAt')?.value);
	const displayEndAt = parseDatetimeLocal(document.getElementById('displayEndAt')?.value);

	if (!startAt || !endAt || !displayStartAt || !displayEndAt) {
		throw new Error('날짜를 모두 입력해주세요.');
	}

	return {
		title: document.getElementById('title')?.value?.trim() || '',
		content: document.getElementById('content')?.value?.trim() || '',
		landingUrl: document.getElementById('landingUrl')?.value?.trim() || null,
		promotionStartAt: startAt,
		promotionEndAt: endAt,
		publishStatus: document.getElementById('publishStatus')?.value || 'DRAFT',
		displayEnabled: document.getElementById('displayEnabled')?.checked || false,
		displayStartAt,
		displayEndAt,
		displayChannel: document.getElementById('displayChannel')?.value || 'PROMOTION_LIST',
		displayPriority: parseInt(document.getElementById('displayPriority')?.value, 10) || 1,
		bannerImageUrl,
		splashImageUrl,
		bannerImageAltText: document.getElementById('bannerImageAltText')?.value?.trim() || null,
		detailImageUrls: detailImageUrls.length > 0 ? detailImageUrls : null
	};
}

function mountPromotionForm(state = {}) {
	const cleanups = [];
	detailImageUrls = [];

	const mode = state.mode === 'edit' ? 'edit' : 'create';
	const promotionId = getPromotionId();

	const backBtn = document.getElementById('promotion-form-back-btn');
	if (backBtn) {
		const back = () => {
			AdminUtils?.navigateTo('/admin/pages/promotions.html');
		};
		backBtn.addEventListener('click', back);
		cleanups.push(() => backBtn.removeEventListener('click', back));
	}

	const cancelBtn = document.getElementById('promotion-form-cancel-btn');
	if (cancelBtn) {
		const cancel = () => {
			AdminUtils?.navigateTo('/admin/pages/promotions.html');
		};
		cancelBtn.addEventListener('click', cancel);
		cleanups.push(() => cancelBtn.removeEventListener('click', cancel));
	}

	const bannerInput = document.getElementById('bannerImageFile');
	if (bannerInput) {
		const handleBannerUpload = async (event) => {
			const file = event.target.files?.[0];
			if (!file) {
				return;
			}

			try {
				const url = await editAndUploadImage(file, 'banner');
				if (!url) {
					return;
				}
				const bannerUrlInput = document.getElementById('bannerImageUrl');
				selectedPromotionBanner = url;
				if (bannerUrlInput) {
					bannerUrlInput.value = url;
				}
				renderSingleImagePreview('bannerPreview', url, '배너 미리보기');
			} catch (error) {
				alert(`배너 이미지 업로드 실패: ${error.message || '알 수 없는 오류'}`);
			} finally {
				event.target.value = '';
			}
		};
		bannerInput.addEventListener('change', handleBannerUpload);
		cleanups.push(() => bannerInput.removeEventListener('change', handleBannerUpload));
	}

	const splashInput = document.getElementById('splashImageFile');
	if (splashInput) {
		const handleSplashUpload = async (event) => {
			const file = event.target.files?.[0];
			if (!file) {
				return;
			}

			try {
				const url = await editAndUploadImage(file, 'splash');
				if (!url) {
					return;
				}
				const splashUrlInput = document.getElementById('splashImageUrl');
				selectedPromotionSplash = url;
				if (splashUrlInput) {
					splashUrlInput.value = url;
				}
				renderSingleImagePreview('splashPreview', url, '스플래시 미리보기');
			} catch (error) {
				alert(`스플래시 이미지 업로드 실패: ${error.message || '알 수 없는 오류'}`);
			} finally {
				event.target.value = '';
			}
		};
		splashInput.addEventListener('change', handleSplashUpload);
		cleanups.push(() => splashInput.removeEventListener('change', handleSplashUpload));
	}

	const detailInput = document.getElementById('detailImagesFile');
	if (detailInput) {
		const handleDetailUpload = async (event) => {
			const files = Array.from(event.target.files || []);
			if (files.length === 0) {
				return;
			}

			try {
				const uploadedUrls = [];
				for (const file of files) {
					const url = await editAndUploadImage(file, 'detail');
					if (url) {
						uploadedUrls.push(url);
					}
				}
				if (uploadedUrls.length > 0) {
					detailImageUrls = uploadedUrls.concat(detailImageUrls);
					renderDetailImages();
				}
			} catch (error) {
				alert(`상세 이미지 업로드 실패: ${error.message || '알 수 없는 오류'}`);
			} finally {
				event.target.value = '';
			}
		};
		detailInput.addEventListener('change', handleDetailUpload);
		cleanups.push(() => detailInput.removeEventListener('change', handleDetailUpload));
	}

	const detailContainer = document.getElementById('detailImagesList');
	if (detailContainer) {
		const removeImage = (event) => {
			const target = event.target.closest('[data-action="remove-detail-image"]');
			if (!target) {
				return;
			}
			const index = parseInt(target.dataset.index, 10);
			if (Number.isNaN(index)) {
				return;
			}
			detailImageUrls = detailImageUrls.filter((_, i) => i !== index);
			renderDetailImages();
		};
		detailContainer.addEventListener('click', removeImage);
		cleanups.push(() => detailContainer.removeEventListener('click', removeImage));
	}

	const form = document.getElementById('promotionForm');
	if (form) {
		const submitHandler = async (event) => {
			event.preventDefault();
			try {
				const payload = collectPromotionPayload();
				if (mode === 'edit') {
					if (!promotionId) {
						alert('프로모션 ID가 없습니다.');
						return;
					}
					await api.patch(`/admin/promotions/${promotionId}`, payload);
					alert('프로모션이 수정되었습니다.');
				} else {
					await api.post('/admin/promotions', payload);
					alert('프로모션이 등록되었습니다.');
				}
				AdminUtils?.navigateTo('/admin/pages/promotions.html');
			} catch (error) {
				alert(error.message || '요청 처리에 실패했습니다.');
			}
		};
		form.addEventListener('submit', submitHandler);
		cleanups.push(() => form.removeEventListener('submit', submitHandler));
	}

	const loadEditData = async () => {
		if (mode !== 'edit' || !promotionId) {
			return;
		}

		try {
			const promotion = await api.get(`/admin/promotions/${promotionId}`);
			document.getElementById('title').value = promotion.title || '';
			document.getElementById('content').value = promotion.content || '';
			document.getElementById('landingUrl').value = promotion.landingUrl || '';
			document.getElementById('promotionStartAt').value = formatDatetimeLocal(promotion.promotionStartAt);
			document.getElementById('promotionEndAt').value = formatDatetimeLocal(promotion.promotionEndAt);
			document.getElementById('publishStatus').value = promotion.publishStatus || 'DRAFT';
			document.getElementById('displayEnabled').checked = Boolean(promotion.displayEnabled);
			document.getElementById('displayStartAt').value = formatDatetimeLocal(promotion.displayStartAt);
			document.getElementById('displayEndAt').value = formatDatetimeLocal(promotion.displayEndAt);
			document.getElementById('displayChannel').value = promotion.displayChannel || 'PROMOTION_LIST';
			document.getElementById('displayPriority').value = promotion.displayPriority || 1;
			document.getElementById('bannerImageUrl').value = promotion.bannerImageUrl || '';
			document.getElementById('splashImageUrl').value = promotion.splashImageUrl || promotion.bannerImageUrl || '';
			document.getElementById('bannerImageAltText').value = promotion.bannerImageAltText || '';
			selectedPromotionBanner = promotion.bannerImageUrl || null;
			selectedPromotionSplash = promotion.splashImageUrl || promotion.bannerImageUrl || null;

			renderSingleImagePreview('bannerPreview', selectedPromotionBanner, '배너 미리보기');
			renderSingleImagePreview('splashPreview', selectedPromotionSplash, '스플래시 미리보기');

			detailImageUrls = Array.isArray(promotion.detailImageUrls)
				? [...promotion.detailImageUrls]
				: [];
			renderDetailImages();
		} catch (error) {
			alert(`프로모션 로드 실패: ${error.message}`);
			AdminUtils?.navigateTo('/admin/pages/promotions.html');
		}
	};

	loadEditData();

	return () => {
		cleanups.forEach((remove) => remove());
		detailImageUrls = [];
		selectedPromotionBanner = null;
		selectedPromotionSplash = null;
	};
}

window.promotionFormView = {
	render: renderPromotionForm,
	mount: mountPromotionForm,
	unmount: () => {}
};
