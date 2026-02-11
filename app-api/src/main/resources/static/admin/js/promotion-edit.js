let promotionId;
let detailImageUrls = [];
let originalBannerUrl;

async function loadPromotion() {
    const urlParams = new URLSearchParams(window.location.search);
    promotionId = urlParams.get('id');

    if (!promotionId) {
        alert('프로모션 ID가 없습니다.');
        location.href = '/admin/pages/promotions.html';
        return;
    }

    try {
        const promotion = await api.get(`/admin/promotions/${promotionId}`);

        document.getElementById('title').value = promotion.title;
        document.getElementById('content').value = promotion.content;
        document.getElementById('landingUrl').value = promotion.landingUrl || '';
        document.getElementById('promotionStartAt').value = formatDateTimeForInput(promotion.promotionStartAt);
        document.getElementById('promotionEndAt').value = formatDateTimeForInput(promotion.promotionEndAt);
        document.getElementById('publishStatus').value = promotion.publishStatus;
        document.getElementById('displayEnabled').checked = promotion.displayEnabled;
        document.getElementById('displayStartAt').value = formatDateTimeForInput(promotion.displayStartAt);
        document.getElementById('displayEndAt').value = formatDateTimeForInput(promotion.displayEndAt);
        document.getElementById('displayChannel').value = promotion.displayChannel;
        document.getElementById('displayPriority').value = promotion.displayPriority;

        if (promotion.bannerImageUrl) {
            originalBannerUrl = promotion.bannerImageUrl;
            document.getElementById('bannerImageUrl').value = promotion.bannerImageUrl;
            document.getElementById('currentBannerPreview').innerHTML =
                `<img src="${promotion.bannerImageUrl}" alt="배너" style="max-width: 300px;">`;
        }

        if (promotion.bannerImageAltText) {
            document.getElementById('bannerImageAltText').value = promotion.bannerImageAltText;
        }

        if (promotion.detailImageUrls && promotion.detailImageUrls.length > 0) {
            detailImageUrls = [...promotion.detailImageUrls];
            document.getElementById('currentDetailImages').innerHTML =
                promotion.detailImageUrls.map((url, index) =>
                    `<img src="${url}" alt="상세 ${index + 1}" style="max-width: 200px; margin: 5px;">`
                ).join('');
        }
    } catch (error) {
        console.error('프로모션 로딩 실패:', error);
        alert('프로모션을 불러오는데 실패했습니다.');
        location.href = '/admin/pages/promotions.html';
    }
}

async function handleBannerImageUpload(event) {
    const file = event.target.files[0];
    if (!file) return;

    try {
        const url = await uploadImageToS3(file);
        document.getElementById('bannerImageUrl').value = url;
        document.getElementById('currentBannerPreview').innerHTML =
            `<img src="${url}" alt="새 배너" style="max-width: 300px;">`;
    } catch (error) {
        console.error('배너 이미지 업로드 실패:', error);
        alert('이미지 업로드에 실패했습니다.');
    }
}

async function handleDetailImagesUpload(event) {
    const files = Array.from(event.target.files);
    if (files.length === 0) return;

    try {
        for (const file of files) {
            const url = await uploadImageToS3(file);
            detailImageUrls.push(url);
        }
        renderDetailImagesList();
    } catch (error) {
        console.error('상세 이미지 업로드 실패:', error);
        alert('이미지 업로드에 실패했습니다.');
    }
}

async function uploadImageToS3(file) {
    const presignedResponse = await api.post('/files/uploads/presigned', {
        purpose: 'COMMON_ASSET',
        files: [{
            fileName: file.name,
            contentType: file.type,
            size: file.size
        }]
    });

    const upload = presignedResponse.uploads[0];

    const formData = new FormData();
    Object.entries(upload.fields).forEach(([key, value]) => {
        formData.append(key, value);
    });
    formData.append('file', file);

    await fetch(upload.url, {
        method: 'POST',
        body: formData
    });

    const urlResponse = await api.get(`/files/${upload.fileUuid}/url`);
    return urlResponse.url;
}

function renderDetailImagesList() {
    const list = document.getElementById('detailImagesList');
    list.innerHTML = detailImageUrls.map((url, index) => `
        <div class="detail-image-item">
            <img src="${url}" alt="상세 이미지 ${index + 1}" style="max-width: 200px;">
            <button type="button" class="btn btn-sm btn-danger" onclick="removeDetailImage(${index})">삭제</button>
        </div>
    `).join('');
}

function removeDetailImage(index) {
    detailImageUrls.splice(index, 1);
    renderDetailImagesList();
}

function formatDateTimeForInput(isoString) {
    if (!isoString) return '';
    const date = new Date(isoString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
}

document.getElementById('promotionForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const formData = new FormData(e.target);
    const bannerUrl = formData.get('bannerImageUrl');

    const data = {
        title: formData.get('title'),
        content: formData.get('content'),
        landingUrl: formData.get('landingUrl') || null,
        promotionStartAt: new Date(formData.get('promotionStartAt')).toISOString(),
        promotionEndAt: new Date(formData.get('promotionEndAt')).toISOString(),
        publishStatus: formData.get('publishStatus'),
        displayEnabled: document.getElementById('displayEnabled').checked,
        displayStartAt: new Date(formData.get('displayStartAt')).toISOString(),
        displayEndAt: new Date(formData.get('displayEndAt')).toISOString(),
        displayChannel: formData.get('displayChannel'),
        displayPriority: parseInt(formData.get('displayPriority')),
        bannerImageUrl: bannerUrl !== originalBannerUrl ? bannerUrl : null,
        bannerImageAltText: formData.get('bannerImageAltText') || null,
        detailImageUrls: detailImageUrls.length > 0 ? detailImageUrls : null
    };

    try {
        await api.patch(`/admin/promotions/${promotionId}`, data);
        alert('프로모션이 수정되었습니다.');
        location.href = '/admin/pages/promotions.html';
    } catch (error) {
        console.error('프로모션 수정 실패:', error);
        alert('프로모션 수정에 실패했습니다: ' + (error.message || '알 수 없는 오류'));
    }
});

document.addEventListener('DOMContentLoaded', () => {
    loadPromotion();
});
