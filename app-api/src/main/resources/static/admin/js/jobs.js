checkAuth();

document.getElementById('logoutBtn').addEventListener('click', (e) => {
    e.preventDefault();
    logout();
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
    } catch (error) {
        errorDiv.textContent = error.message;
        errorDiv.style.display = 'block';
    } finally {
        button.disabled = false;
        button.textContent = '실행';
    }
});
