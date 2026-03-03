let jobsCleanup = [];

function renderJobs(container) {
	container.innerHTML = `
        <div class="content-header">
            <h1>배치 작업 관리</h1>
        </div>

        <div class="job-cards">
            <div class="card job-card">
                <h3>이미지 최적화</h3>
                <p>S3에 저장된 이미지를 WebP 포맷으로 변환하고 정책에 맞게 리사이즈합니다.</p>
                <div class="job-config">
                    <label for="batchSize">배치 크기:</label>
                    <input type="number" id="batchSize" value="100" min="1" max="500">
                </div>
                <div class="button-group">
                    <button id="discoverOptimization" class="btn btn-secondary">탐색 실행</button>
                    <button id="loadPendingImages" class="btn btn-secondary">대기 잡 조회</button>
                    <button id="runImageOptimization" class="btn btn-primary">최적화 실행</button>
                    <button id="resetOptimizationJobs" class="btn btn-danger">잡 초기화</button>
                </div>

                <div id="discoverResult" class="job-result is-hidden">
                    <h4>탐색 결과</h4>
                    <p>등록된 PENDING 잡: <span id="discoveredCount" class="pending-count">0</span>개</p>
                </div>
                <div id="jobResult" class="job-result is-hidden">
                    <h4>실행 결과</h4>
                    <ul>
                        <li>성공: <span id="successCount">0</span></li>
                        <li>실패: <span id="failedCount">0</span></li>
                        <li>건너뜀: <span id="skippedCount">0</span></li>
                    </ul>
                </div>
                <div id="jobError" class="error-message is-hidden"></div>

                <div id="pendingImages" class="pending-images is-hidden">
                    <h4>최적화 대기 잡 (<span id="pendingCount" class="pending-count">0</span>개)</h4>
                    <table>
                        <thead>
                            <tr>
                                <th>잡 ID</th>
                                <th>이미지 ID</th>
                                <th>파일명</th>
                                <th>크기</th>
                                <th>타입</th>
                                <th>용도</th>
                                <th>잡 등록일</th>
                            </tr>
                        </thead>
                        <tbody id="pendingImagesBody"></tbody>
                    </table>
                </div>
            </div>

            <div class="card job-card">
                <h3>이미지 정리</h3>
                <p>PENDING 상태로 만료된 이미지와 삭제 예정 이미지를 S3에서 삭제합니다.</p>
                <div class="button-group">
                    <button id="loadCleanupPending" class="btn btn-secondary">정리 대기 조회</button>
                    <button id="runImageCleanup" class="btn btn-primary">정리 실행</button>
                </div>
                <div id="cleanupResult" class="job-result is-hidden">
                    <h4>실행 결과</h4>
                    <p>정리된 이미지: <span id="cleanedCount" class="pending-count">0</span>개</p>
                </div>
                <div id="cleanupError" class="error-message is-hidden"></div>
                <div id="cleanupPendingImages" class="pending-images is-hidden">
                    <h4>정리 대기 이미지 (<span id="cleanupPendingCount" class="pending-count">0</span>개)</h4>
                    <table>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>파일명</th>
                                <th>크기</th>
                                <th>타입</th>
                                <th>용도</th>
                                <th>상태</th>
                                <th>생성일</th>
                                <th>삭제예정일</th>
                            </tr>
                        </thead>
                        <tbody id="cleanupPendingBody"></tbody>
                    </table>
                </div>
            </div>
        </div>
    `;
}

function formatFileSize(bytes) {
	if (bytes === null || bytes === undefined || Number.isNaN(bytes)) {
		return '-';
	}
	if (bytes < 1024) {
		return `${bytes} B`;
	}
	if (bytes < 1024 * 1024) {
		return `${(bytes / 1024).toFixed(1)} KB`;
	}
	return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

function getPurposeLabel(purpose) {
	const labels = {
		PROFILE_IMAGE: '프로필',
		GROUP_IMAGE: '그룹',
		RESTAURANT_IMAGE: '음식점',
		REVIEW_IMAGE: '리뷰',
		MENU_IMAGE: '메뉴',
		COMMON_ASSET: '공통'
	};
	return labels[purpose] || purpose;
}

function getDomainLabel(domainType) {
	const labels = {
		RESTAURANT: '음식점',
		REVIEW: '리뷰',
		MEMBER: '회원',
		GROUP: '그룹',
		MENU: '메뉴'
	};
	return labels[domainType] || domainType;
}

function setLoading(button, text) {
	if (!button) {
		return;
	}
	button.disabled = true;
	button.dataset.originalText = button.textContent;
	button.textContent = text;
}

function clearLoading(button) {
	if (!button) {
		return;
	}
	button.disabled = false;
	if (button.dataset.originalText) {
		button.textContent = button.dataset.originalText;
	}
}

function mountJobs() {
	jobsCleanup = [];

	const discoverBtn = document.getElementById('discoverOptimization');
	if (discoverBtn) {
		const discoverHandler = async () => {
			const batchResult = document.getElementById('discoveredCount');
			const errorBox = document.getElementById('jobError');
			const listBox = document.getElementById('discoverResult');
			setLoading(discoverBtn, '탐색 중...');
			try {
				const response = await window.apiRequest('/admin/jobs/image-optimization/discover', { method: 'POST' });
				if (batchResult) {
					batchResult.textContent = response?.data?.successCount || 0;
				}
				if (listBox) {
					listBox.style.display = 'block';
				}
				if (errorBox) {
					errorBox.style.display = 'none';
				}
				document.getElementById('loadPendingImages')?.click();
			} catch (error) {
				if (errorBox) {
					errorBox.textContent = error.message;
					errorBox.style.display = 'block';
				}
			} finally {
				clearLoading(discoverBtn);
			}
		};
		discoverBtn.addEventListener('click', discoverHandler);
		jobsCleanup.push(() => discoverBtn.removeEventListener('click', discoverHandler));
	}

	const pendingBtn = document.getElementById('loadPendingImages');
	if (pendingBtn) {
		const loadPending = async () => {
			const list = document.getElementById('pendingImages');
			const errorBox = document.getElementById('jobError');
			const pendingCount = document.getElementById('pendingCount');
			const tbody = document.getElementById('pendingImagesBody');
			const limit = parseInt(document.getElementById('batchSize')?.value || '100', 10);

			setLoading(pendingBtn, '조회 중...');
			if (errorBox) {
				errorBox.style.display = 'none';
			}
			try {
				const response = await window.apiRequest(`/admin/jobs/image-optimization/pending?limit=${limit}`);
				const jobs = response?.data || [];
				if (pendingCount) {
					pendingCount.textContent = jobs.length;
				}
				if (tbody) {
					tbody.innerHTML = jobs.map((job) => `
                        <tr>
                            <td>${job.jobId}</td>
                            <td>${job.imageId}</td>
                            <td title="${job.fileName}">${job.fileName ? (job.fileName.length > 30 ? `${job.fileName.substring(0, 30)}...` : job.fileName) : '-'}</td>
                            <td class="file-size">${formatFileSize(job.fileSize)}</td>
                            <td>${job.fileType || '-'}</td>
                            <td>${getPurposeLabel(job.purpose)} / ${getDomainLabel(job.domainType || job.domain)}</td>
                            <td>${window.AdminUtils.toKoreanDateTime(job.jobCreatedAt)}</td>
                        </tr>
                    `).join('');
				}
				if (list) {
					list.style.display = 'block';
				}
			} catch (error) {
				if (errorBox) {
					errorBox.textContent = error.message;
					errorBox.style.display = 'block';
				}
			} finally {
				clearLoading(pendingBtn);
			}
		};
		pendingBtn.addEventListener('click', loadPending);
		jobsCleanup.push(() => pendingBtn.removeEventListener('click', loadPending));
	}

	const runOptBtn = document.getElementById('runImageOptimization');
	if (runOptBtn) {
		const runOptimize = async () => {
			const errorBox = document.getElementById('jobError');
			const resultBox = document.getElementById('jobResult');
			const success = document.getElementById('successCount');
			const failed = document.getElementById('failedCount');
			const skipped = document.getElementById('skippedCount');
			const batchSize = parseInt(document.getElementById('batchSize')?.value || '100', 10);
			setLoading(runOptBtn, '실행 중...');
			if (resultBox) {
				resultBox.style.display = 'none';
			}
			try {
				const response = await window.apiRequest(`/admin/jobs/image-optimization?batchSize=${batchSize}`, { method: 'POST' });
				if (success) {
					success.textContent = response?.data?.successCount || 0;
				}
				if (failed) {
					failed.textContent = response?.data?.failedCount || 0;
				}
				if (skipped) {
					skipped.textContent = response?.data?.skippedCount || 0;
				}
				if (resultBox) {
					resultBox.style.display = 'block';
				}
				document.getElementById('loadPendingImages')?.click();
			} catch (error) {
				if (errorBox) {
					errorBox.textContent = error.message;
					errorBox.style.display = 'block';
				}
			} finally {
				clearLoading(runOptBtn);
			}
		};
		runOptBtn.addEventListener('click', runOptimize);
		jobsCleanup.push(() => runOptBtn.removeEventListener('click', runOptimize));
	}

	const resetBtn = document.getElementById('resetOptimizationJobs');
	if (resetBtn) {
		const resetJobs = async () => {
			const errorBox = document.getElementById('jobError');
			const resultBox = document.getElementById('jobResult');
			setLoading(resetBtn, '초기화 중...');
			try {
				await window.apiRequest('/admin/jobs/image-optimization', { method: 'DELETE' });
				const pending = document.getElementById('pendingImages');
				const pendingBody = document.getElementById('pendingImagesBody');
				const count = document.getElementById('pendingCount');
				if (pending) {
					pending.style.display = 'none';
				}
				if (pendingBody) {
					pendingBody.innerHTML = '';
				}
				if (count) {
					count.textContent = '0';
				}
				if (resultBox) {
					resultBox.style.display = 'none';
				}
				if (errorBox) {
					errorBox.style.display = 'none';
				}
			} catch (error) {
				if (errorBox) {
					errorBox.textContent = error.message;
					errorBox.style.display = 'block';
				}
			} finally {
				clearLoading(resetBtn);
			}
		};
		resetBtn.addEventListener('click', resetJobs);
		jobsCleanup.push(() => resetBtn.removeEventListener('click', resetJobs));
	}

	const loadCleanupBtn = document.getElementById('loadCleanupPending');
	if (loadCleanupBtn) {
		const loadCleanup = async () => {
			const errorBox = document.getElementById('cleanupError');
			const list = document.getElementById('cleanupPendingImages');
			const count = document.getElementById('cleanupPendingCount');
			const tbody = document.getElementById('cleanupPendingBody');
			setLoading(loadCleanupBtn, '조회 중...');
			try {
				const response = await window.apiRequest('/admin/jobs/image-cleanup/pending');
				const images = response?.data || [];
				if (count) {
					count.textContent = images.length;
				}
				if (tbody) {
					tbody.innerHTML = images.map((img) => `
                        <tr>
                            <td>${img.imageId}</td>
                            <td title="${img.fileName}">${img.fileName ? (img.fileName.length > 30 ? `${img.fileName.substring(0, 30)}...` : img.fileName) : '-'}</td>
                            <td class="file-size">${formatFileSize(img.fileSize)}</td>
                            <td>${img.fileType || '-'}</td>
                            <td>${getPurposeLabel(img.purpose)}</td>
                            <td>${img.status}</td>
                            <td>${window.AdminUtils.toKoreanDateTime(img.createdAt)}</td>
                            <td>${window.AdminUtils.toKoreanDateTime(img.deletedAt)}</td>
                        </tr>
                    `).join('');
				}
				if (list) {
					list.style.display = 'block';
				}
				if (errorBox) {
					errorBox.style.display = 'none';
				}
			} catch (error) {
				if (errorBox) {
					errorBox.textContent = error.message;
					errorBox.style.display = 'block';
				}
			} finally {
				clearLoading(loadCleanupBtn);
			}
		};
		loadCleanupBtn.addEventListener('click', loadCleanup);
		jobsCleanup.push(() => loadCleanupBtn.removeEventListener('click', loadCleanup));
	}

	const runCleanupBtn = document.getElementById('runImageCleanup');
	if (runCleanupBtn) {
		const runCleanup = async () => {
			const errorBox = document.getElementById('cleanupError');
			const result = document.getElementById('cleanupResult');
			const count = document.getElementById('cleanedCount');
			setLoading(runCleanupBtn, '실행 중...');
			if (result) {
				result.style.display = 'none';
			}
			try {
				const response = await window.apiRequest('/admin/jobs/image-cleanup', { method: 'POST' });
				if (count) {
					count.textContent = response?.data?.successCount || 0;
				}
				if (result) {
					result.style.display = 'block';
				}
				document.getElementById('loadCleanupPending')?.click();
			} catch (error) {
				if (errorBox) {
					errorBox.textContent = error.message;
					errorBox.style.display = 'block';
				}
			} finally {
				clearLoading(runCleanupBtn);
			}
		};
		runCleanupBtn.addEventListener('click', runCleanup);
		jobsCleanup.push(() => runCleanupBtn.removeEventListener('click', runCleanup));
	}

	return () => {
		jobsCleanup.forEach((remove) => remove());
		jobsCleanup = [];
	};
}

window.jobsView = {
	render: renderJobs,
	mount: mountJobs,
	unmount: () => {
		jobsCleanup.forEach((remove) => remove());
		jobsCleanup = [];
	}
};
