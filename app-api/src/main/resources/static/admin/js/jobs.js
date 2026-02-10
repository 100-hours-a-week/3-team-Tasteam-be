checkAuth();

document.getElementById('logoutBtn').addEventListener('click', (e) => {
    e.preventDefault();
    logout();
});

function formatFileSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
}

function formatDate(isoString) {
    if (!isoString) return '-';
    const date = new Date(isoString);
    return date.toLocaleDateString('ko-KR') + ' ' + date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
}

function getPurposeLabel(purpose) {
    const labels = {
        'PROFILE_IMAGE': '프로필',
        'GROUP_IMAGE': '그룹',
        'RESTAURANT_IMAGE': '음식점',
        'REVIEW_IMAGE': '리뷰',
        'MENU_IMAGE': '메뉴',
        'COMMON_ASSET': '공통'
    };
    return labels[purpose] || purpose;
}

function getDomainLabel(domainType) {
    const labels = {
        'RESTAURANT': '음식점',
        'REVIEW': '리뷰',
        'MEMBER': '회원',
        'GROUP': '그룹',
        'MENU': '메뉴'
    };
    return labels[domainType] || domainType;
}

document.getElementById('loadPendingImages').addEventListener('click', async () => {
    const button = document.getElementById('loadPendingImages');
    const pendingDiv = document.getElementById('pendingImages');
    const errorDiv = document.getElementById('jobError');
    const limit = parseInt(document.getElementById('batchSize').value) || 100;

    button.disabled = true;
    button.textContent = '조회 중...';
    errorDiv.style.display = 'none';

    try {
        const response = await apiRequest(`/admin/jobs/image-optimization/pending?limit=${limit}`);
        const images = response.data;

        document.getElementById('pendingCount').textContent = images.length;

        const tbody = document.getElementById('pendingImagesBody');
        tbody.innerHTML = images.map(img => `
            <tr>
                <td>${img.imageId}</td>
                <td title="${img.fileName}">${img.fileName.length > 30 ? img.fileName.substring(0, 30) + '...' : img.fileName}</td>
                <td class="file-size">${formatFileSize(img.fileSize)}</td>
                <td>${img.fileType}</td>
                <td>${getPurposeLabel(img.purpose)}</td>
                <td>${getDomainLabel(img.domainType)} #${img.domainId}</td>
                <td>${formatDate(img.createdAt)}</td>
            </tr>
        `).join('');

        pendingDiv.style.display = 'block';
    } catch (error) {
        errorDiv.textContent = error.message;
        errorDiv.style.display = 'block';
    } finally {
        button.disabled = false;
        button.textContent = '대기 이미지 조회';
    }
});

document.getElementById('runImageOptimization').addEventListener('click', async () => {
    const button = document.getElementById('runImageOptimization');
    const resultDiv = document.getElementById('jobResult');
    const errorDiv = document.getElementById('jobError');
    const batchSize = parseInt(document.getElementById('batchSize').value) || 100;

    button.disabled = true;
    button.textContent = '실행 중...';
    resultDiv.style.display = 'none';
    errorDiv.style.display = 'none';

    try {
        const response = await apiRequest(`/admin/jobs/image-optimization?batchSize=${batchSize}`, {
            method: 'POST'
        });

        document.getElementById('successCount').textContent = response.data.successCount;
        document.getElementById('failedCount').textContent = response.data.failedCount;
        document.getElementById('skippedCount').textContent = response.data.skippedCount;
        resultDiv.style.display = 'block';

        document.getElementById('loadPendingImages').click();
    } catch (error) {
        errorDiv.textContent = error.message;
        errorDiv.style.display = 'block';
    } finally {
        button.disabled = false;
        button.textContent = '최적화 실행';
    }
});
