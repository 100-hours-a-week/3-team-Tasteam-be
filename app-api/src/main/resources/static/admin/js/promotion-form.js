let detailImageUrls = [];

async function handleBannerImageUpload(event) {
    const file = event.target.files[0];
    if (!file) return;

    try {
        const url = await uploadImageToS3(file);
        document.getElementById('bannerImageUrl').value = url;
        showBannerPreview(url);
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

function showBannerPreview(url) {
    const preview = document.getElementById('bannerPreview');
    preview.innerHTML = `<img src="${url}" alt="배너 미리보기" style="max-width: 300px;">`;
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

document.getElementById('promotionForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const formData = new FormData(e.target);
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
        bannerImageUrl: formData.get('bannerImageUrl'),
        bannerImageAltText: formData.get('bannerImageAltText') || null,
        detailImageUrls: detailImageUrls.length > 0 ? detailImageUrls : null
    };

    try {
        await api.post('/admin/promotions', data);
        alert('프로모션이 등록되었습니다.');
        location.href = '/admin/pages/promotions.html';
    } catch (error) {
        console.error('프로모션 등록 실패:', error);
        alert('프로모션 등록에 실패했습니다: ' + (error.message || '알 수 없는 오류'));
    }
});
