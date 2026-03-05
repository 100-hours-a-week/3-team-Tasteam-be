const SEED_PRESETS = {
	low: {
		label: 'Low',
		description: '빠른 기능 검증용',
		memberCount: 500,
		restaurantCount: 200,
		groupCount: 20,
		subgroupPerGroup: 5,
		memberPerGroup: 30,
		reviewCount: 1000,
		chatMessagePerRoom: 50,
		notificationCount: 5000,
		favoriteCount: 1000
	},
	medium: {
		label: 'Medium',
		description: '통합 기능 테스트용',
		memberCount: 5000,
		restaurantCount: 10000,
		groupCount: 50,
		subgroupPerGroup: 5,
		memberPerGroup: 30,
		reviewCount: 50000,
		chatMessagePerRoom: 200,
		notificationCount: 100000,
		favoriteCount: 20000
	},
	high: {
		label: 'High',
		description: '부하 테스트 설계 목표 (수십 분 소요)',
		memberCount: 100000,
		restaurantCount: 500000,
		groupCount: 200,
		subgroupPerGroup: 10,
		memberPerGroup: 100,
		reviewCount: 1000000,
		chatMessagePerRoom: 1000,
		notificationCount: 1500000,
		favoriteCount: 400000
	}
};

let dummyCleanup = [];
let seedPollInterval = null;

function renderDummy(container) {
	container.innerHTML = `
        <div class="content-header">
            <h1>더미 데이터 관리</h1>
            <p class="content-subtitle">부하테스트용 더미 데이터를 빠르게 삽입·조회·삭제합니다.</p>
        </div>

        <div class="dummy-cards">
            <div class="card">
                <h3>현재 데이터 현황</h3>
                <button class="btn btn-secondary count-button" id="countBtn">현황 조회</button>
                <div id="countResult" class="is-hidden">
                    <table class="count-table">
                        <thead>
                            <tr><th>테이블</th><th>레코드 수</th></tr>
                        </thead>
                        <tbody id="countTableBody"></tbody>
                    </table>
                </div>
                <div id="countError" class="status-msg error is-hidden"></div>
            </div>

            <div class="card">
                <h3>더미 데이터 삽입</h3>

                <div class="preset-section">
                    <p class="preset-label">빠른 프리셋</p>
                    <div class="preset-buttons">
                        <button class="btn btn-preset btn-preset-low" data-preset="low">Low<span>빠른 검증</span></button>
                        <button class="btn btn-preset btn-preset-medium" data-preset="medium">Medium<span>기능 테스트</span></button>
                        <button class="btn btn-preset btn-preset-high" data-preset="high">High<span>부하 테스트 목표</span></button>
                    </div>
                </div>

                <div class="seed-form">
                    <div class="form-field">
                        <label>멤버 수 (기본 500, 최대 100,000)</label>
                        <input type="number" id="memberCount" placeholder="500" min="0" max="100000">
                    </div>
                    <div class="form-field">
                        <label>음식점 수 (기본 200, 최대 50,000,000)</label>
                        <input type="number" id="restaurantCount" placeholder="200" min="0" max="50000000">
                    </div>
                    <div class="form-field">
                        <label>그룹 수 (기본 20, 최대 1,000)</label>
                        <input type="number" id="groupCount" placeholder="20" min="0" max="1000">
                    </div>
                    <div class="form-field">
                        <label>그룹당 하위그룹 수 (기본 5, 최대 1,000)</label>
                        <input type="number" id="subgroupPerGroup" placeholder="5" min="0" max="1000">
                    </div>
                    <div class="form-field">
                        <label>그룹당 멤버 수 (기본 30, 최대 1,000)</label>
                        <input type="number" id="memberPerGroup" placeholder="30" min="0" max="1000">
                    </div>
                    <div class="form-field">
                        <label>리뷰 수 (기본 1,000, 최대 100,000,000)</label>
                        <input type="number" id="reviewCount" placeholder="1000" min="0" max="100000000">
                    </div>
                    <div class="form-field">
                        <label>채팅방당 메시지 수 (기본 50, 최대 1,000,000,000)</label>
                        <input type="number" id="chatMessagePerRoom" placeholder="50" min="0" max="1000000000">
                    </div>
                    <div class="form-field">
                        <label>알림 수 (기본 5,000, 최대 2,000,000)</label>
                        <input type="number" id="notificationCount" placeholder="5000" min="0" max="2000000">
                    </div>
                    <div class="form-field form-field-full">
                        <label>즐겨찾기 수 (기본 1,000, 최대 500,000) — 조건: 멤버 수 × 음식점 수 ≥ 즐겨찾기 수</label>
                        <input type="number" id="favoriteCount" placeholder="1000" min="0" max="500000">
                    </div>
                </div>
                <button class="btn btn-primary seed-button" id="seedBtn">삽입 실행</button>
                <div id="seedProgress" class="seed-progress is-hidden">
                    <p id="seedProgressText" class="seed-progress-text"></p>
                    <div class="progress-bar">
                        <div class="progress-fill" id="seedProgressFill"></div>
                    </div>
                    <button class="btn btn-danger btn-cancel-seed" id="cancelSeedBtn">강제 종료</button>
                </div>
                <div id="seedResult" class="seed-result is-hidden">
                    <table>
                        <tr><td>멤버 삽입</td><td id="r-members">-</td></tr>
                        <tr><td>음식점 삽입</td><td id="r-restaurants">-</td></tr>
                        <tr><td>그룹 삽입</td><td id="r-groups">-</td></tr>
                        <tr><td>하위그룹 삽입</td><td id="r-subgroups">-</td></tr>
                        <tr><td>리뷰 삽입</td><td id="r-reviews">-</td></tr>
                        <tr><td>채팅 메시지 삽입</td><td id="r-chat">-</td></tr>
                        <tr><td>알림 삽입</td><td id="r-notifications">-</td></tr>
                        <tr><td>즐겨찾기 삽입</td><td id="r-favorites">-</td></tr>
                    </table>
                    <p class="elapsed" id="r-elapsed"></p>
                </div>
                <div id="seedError" class="status-msg error is-hidden"></div>
            </div>

            <div class="card">
                <h3>더미 데이터 삭제</h3>
                <div class="delete-zone">
                    <p class="delete-zone-text">
                        <strong>주의:</strong> email LIKE <code>%@dummy.tasteam.kr</code> 등 패턴 데이터가 삭제됩니다.
                    </p>
                    <button class="btn btn-danger" id="deleteBtn">더미 데이터 전체 삭제</button>
                    <div id="deleteMsg" class="status-msg is-hidden"></div>
                </div>
            </div>
        </div>
    `;
}

function toIntOrDefault(id, fallback) {
	const node = document.getElementById(id);
	if (!node) {
		return fallback;
	}
	const value = parseInt(node.value, 10);
	return Number.isNaN(value) || value < 0 ? fallback : value;
}

async function refreshCountResult() {
	const errorEl = document.getElementById('countError');
	const resultEl = document.getElementById('countResult');
	const tableBody = document.getElementById('countTableBody');
	if (!errorEl || !resultEl || !tableBody) {
		return;
	}

	try {
		const response = await getDataCounts();
		const data = response?.data || response;
		const rows = [
			['member', data.memberCount],
			['restaurant', data.restaurantCount],
			['group', data.groupCount],
			['subgroup', data.subgroupCount],
			['review', data.reviewCount],
			['chat_message', data.chatMessageCount],
			['notification', data.notificationCount],
			['favorite', data.favoriteCount]
		];
		tableBody.innerHTML = rows
			.map(([name, count]) => `<tr><td>${name}</td><td class="count-value">${(count ?? 0).toLocaleString()}</td></tr>`)
			.join('');
		resultEl.classList.remove('is-hidden');
		errorEl.classList.add('is-hidden');
	} catch (error) {
		errorEl.textContent = `현황 조회 실패: ${error.message}`;
		errorEl.classList.remove('is-hidden');
	}
}

function stopSeedPolling() {
	if (seedPollInterval !== null) {
		clearInterval(seedPollInterval);
		seedPollInterval = null;
	}
}

function updateProgressUI(status) {
	const progressEl = document.getElementById('seedProgress');
	const progressText = document.getElementById('seedProgressText');
	const progressFill = document.getElementById('seedProgressFill');
	const resultEl = document.getElementById('seedResult');
	const errorEl = document.getElementById('seedError');
	const seedBtn = document.getElementById('seedBtn');

	if (!progressEl) return;

	if (status === 'IDLE') {
		return;
	}

	if (status.status === 'RUNNING') {
		progressEl.classList.remove('is-hidden');
		if (resultEl) resultEl.classList.add('is-hidden');
		if (errorEl) errorEl.classList.add('is-hidden');

		const pct = Math.round((status.completedSteps / status.totalSteps) * 100);
		const stepLabel = status.currentStep || '준비 중...';
		if (progressText) {
			progressText.textContent = `step ${status.completedSteps}/${status.totalSteps} — ${stepLabel}`;
		}
		if (progressFill) {
			progressFill.style.width = `${pct}%`;
		}
	} else if (status.status === 'COMPLETED') {
		stopSeedPolling();
		progressEl.classList.add('is-hidden');
		if (seedBtn) seedBtn.disabled = false;

		const result = status.result;
		if (result && resultEl) {
			const set = (id, val) => {
				const el = document.getElementById(id);
				if (el) el.textContent = (val ?? 0).toLocaleString();
			};
			set('r-members', result.membersInserted);
			set('r-restaurants', result.restaurantsInserted);
			set('r-groups', result.groupsInserted);
			set('r-subgroups', result.subgroupsInserted);
			set('r-reviews', result.reviewsInserted);
			set('r-chat', result.chatMessagesInserted);
			set('r-notifications', result.notificationsInserted);
			set('r-favorites', result.favoritesInserted);
			const elapsedEl = document.getElementById('r-elapsed');
			if (elapsedEl) {
				elapsedEl.textContent = `소요 시간: ${(result.elapsedMs ?? 0).toLocaleString()} ms`;
			}
			resultEl.classList.remove('is-hidden');
		}
		refreshCountResult();
	} else if (status.status === 'FAILED') {
		stopSeedPolling();
		progressEl.classList.add('is-hidden');
		if (seedBtn) seedBtn.disabled = false;

		if (errorEl) {
			errorEl.textContent = `삽입 실패: ${status.errorMessage || '알 수 없는 오류'}`;
			errorEl.classList.remove('is-hidden');
		}
		refreshCountResult();
	} else if (status.status === 'CANCELLED') {
		stopSeedPolling();
		progressEl.classList.add('is-hidden');
		if (seedBtn) seedBtn.disabled = false;

		if (errorEl) {
			errorEl.textContent = `시딩이 중단되었습니다 (완료된 스텝: ${status.completedSteps}/${status.totalSteps})`;
			errorEl.classList.remove('is-hidden');
		}
		refreshCountResult();
	}
}

function mountDummy() {
	dummyCleanup = [];

	const countBtn = document.getElementById('countBtn');
	if (countBtn) {
		const countHandler = async () => {
			const errorEl = document.getElementById('countError');
			const resultEl = document.getElementById('countResult');
			if (errorEl) {
				errorEl.classList.add('is-hidden');
			}
			if (resultEl) {
				resultEl.classList.add('is-hidden');
			}
			await refreshCountResult();
		};
		countBtn.addEventListener('click', countHandler);
		dummyCleanup.push(() => countBtn.removeEventListener('click', countHandler));
	}

	const presetContainer = document.querySelector('.preset-buttons');
	if (presetContainer) {
		const presetHandler = (event) => {
			const btn = event.target.closest('[data-preset]');
			if (!btn) {
				return;
			}
			const preset = SEED_PRESETS[btn.dataset.preset];
			if (!preset) {
				return;
			}
			const fields = [
				'memberCount', 'restaurantCount', 'groupCount', 'subgroupPerGroup',
				'memberPerGroup', 'reviewCount', 'chatMessagePerRoom', 'notificationCount', 'favoriteCount'
			];
			fields.forEach((id) => {
				const el = document.getElementById(id);
				if (el) {
					el.value = preset[id];
				}
			});
		};
		presetContainer.addEventListener('click', presetHandler);
		dummyCleanup.push(() => presetContainer.removeEventListener('click', presetHandler));
	}

	const seedBtn = document.getElementById('seedBtn');
	if (seedBtn) {
		const seedHandler = async () => {
			const errorEl = document.getElementById('seedError');
			const resultEl = document.getElementById('seedResult');
			const progressEl = document.getElementById('seedProgress');
			const progressFill = document.getElementById('seedProgressFill');
			const progressText = document.getElementById('seedProgressText');

			if (errorEl) {
				errorEl.classList.add('is-hidden');
			}
			if (resultEl) {
				resultEl.classList.add('is-hidden');
			}
			if (progressEl) {
				progressEl.classList.remove('is-hidden');
			}
			if (progressFill) {
				progressFill.style.width = '0%';
			}
			if (progressText) {
				progressText.textContent = '시작 중...';
			}
			seedBtn.disabled = true;

			try {
				const payload = {
					memberCount: toIntOrDefault('memberCount', 500),
					restaurantCount: toIntOrDefault('restaurantCount', 200),
					groupCount: toIntOrDefault('groupCount', 20),
					subgroupPerGroup: toIntOrDefault('subgroupPerGroup', 5),
					memberPerGroup: toIntOrDefault('memberPerGroup', 30),
					reviewCount: toIntOrDefault('reviewCount', 1000),
					chatMessagePerRoom: toIntOrDefault('chatMessagePerRoom', 50),
					notificationCount: toIntOrDefault('notificationCount', 5000),
					favoriteCount: toIntOrDefault('favoriteCount', 1000)
				};
				await seedDummyData(payload);

				stopSeedPolling();
				seedPollInterval = setInterval(async () => {
					try {
						const resp = await getSeedStatus();
						const status = resp?.data || resp;
						updateProgressUI(status);
					} catch (err) {
						stopSeedPolling();
						if (seedBtn) seedBtn.disabled = false;
						if (progressEl) progressEl.classList.add('is-hidden');
						if (errorEl) {
							errorEl.textContent = `상태 조회 실패: ${err.message}`;
							errorEl.classList.remove('is-hidden');
						}
					}
				}, 1000);
			} catch (error) {
				if (progressEl) progressEl.classList.add('is-hidden');
				seedBtn.disabled = false;
				if (errorEl) {
					errorEl.textContent = `삽입 요청 실패: ${error.message}`;
					errorEl.classList.remove('is-hidden');
				}
			}
		};
		seedBtn.addEventListener('click', seedHandler);
		dummyCleanup.push(() => seedBtn.removeEventListener('click', seedHandler));
	}

	const cancelSeedBtn = document.getElementById('cancelSeedBtn');
	if (cancelSeedBtn) {
		const cancelHandler = async () => {
			cancelSeedBtn.disabled = true;
			try {
				await cancelSeed();
			} catch (err) {
				cancelSeedBtn.disabled = false;
			}
		};
		cancelSeedBtn.addEventListener('click', cancelHandler);
		dummyCleanup.push(() => cancelSeedBtn.removeEventListener('click', cancelHandler));
	}

	const deleteBtn = document.getElementById('deleteBtn');
	if (deleteBtn) {
		const deleteHandler = async () => {
			if (!confirm('더미 데이터를 전부 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) {
				return;
			}
			const msg = document.getElementById('deleteMsg');
			try {
				await deleteDummyData();
				if (msg) {
					msg.textContent = '더미 데이터가 모두 삭제되었습니다.';
					msg.className = 'status-msg success';
					msg.classList.remove('is-hidden');
				}
			} catch (error) {
				if (msg) {
					msg.textContent = `삭제 중 오류: ${error.message}`;
					msg.className = 'status-msg error';
					msg.classList.remove('is-hidden');
				}
			}
			// 삭제 후 현황 자동 갱신
			await refreshCountResult();
		};
		deleteBtn.addEventListener('click', deleteHandler);
		dummyCleanup.push(() => deleteBtn.removeEventListener('click', deleteHandler));
	}

	return () => {
		stopSeedPolling();
		dummyCleanup.forEach((remove) => remove());
		dummyCleanup = [];
	};
}

window.dummyView = {
	render: renderDummy,
	mount: mountDummy,
	unmount: () => {
		stopSeedPolling();
		dummyCleanup.forEach((remove) => remove());
		dummyCleanup = [];
	}
};
