let dummyCleanup = [];

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
                <div class="seed-form">
                    <div class="form-field">
                        <label>멤버 수 (기본 500)</label>
                        <input type="number" id="memberCount" placeholder="500" min="0">
                    </div>
                    <div class="form-field">
                        <label>음식점 수 (기본 200)</label>
                        <input type="number" id="restaurantCount" placeholder="200" min="0">
                    </div>
                    <div class="form-field">
                        <label>그룹 수 (기본 20)</label>
                        <input type="number" id="groupCount" placeholder="20" min="0">
                    </div>
                    <div class="form-field">
                        <label>그룹당 하위그룹 수 (기본 5)</label>
                        <input type="number" id="subgroupPerGroup" placeholder="5" min="0">
                    </div>
                    <div class="form-field">
                        <label>그룹당 멤버 수 (기본 30)</label>
                        <input type="number" id="memberPerGroup" placeholder="30" min="0">
                    </div>
                    <div class="form-field">
                        <label>리뷰 수 (기본 1000)</label>
                        <input type="number" id="reviewCount" placeholder="1000" min="0">
                    </div>
                    <div class="form-field form-field-full">
                        <label>채팅방당 메시지 수 (기본 50)</label>
                        <input type="number" id="chatMessagePerRoom" placeholder="50" min="0">
                    </div>
                </div>
                <button class="btn btn-primary seed-button" id="seedBtn">삽입 실행</button>
                <div id="seedResult" class="seed-result is-hidden">
                    <table>
                        <tr><td>멤버 삽입</td><td id="r-members">-</td></tr>
                        <tr><td>음식점 삽입</td><td id="r-restaurants">-</td></tr>
                        <tr><td>그룹 삽입</td><td id="r-groups">-</td></tr>
                        <tr><td>하위그룹 삽입</td><td id="r-subgroups">-</td></tr>
                        <tr><td>리뷰 삽입</td><td id="r-reviews">-</td></tr>
                        <tr><td>채팅 메시지 삽입</td><td id="r-chat">-</td></tr>
                    </table>
                    <p class="elapsed" id="r-elapsed"></p>
                </div>
                <div id="seedError" class="status-msg error is-hidden"></div>
                <div id="seedLoading" class="seed-loading is-hidden">삽입 중...</div>
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

function mountDummy() {
	dummyCleanup = [];

	const countBtn = document.getElementById('countBtn');
	if (countBtn) {
		const countHandler = async () => {
			const errorEl = document.getElementById('countError');
			const resultEl = document.getElementById('countResult');
			const tableBody = document.getElementById('countTableBody');
			if (!errorEl || !resultEl || !tableBody) {
				return;
			}
			errorEl.style.display = 'none';
			resultEl.style.display = 'none';

			try {
				const response = await getDataCounts();
				const data = response?.data || response;
				const rows = [
					['member', data.memberCount],
					['restaurant', data.restaurantCount],
					['group', data.groupCount],
					['subgroup', data.subgroupCount],
					['review', data.reviewCount],
					['chat_message', data.chatMessageCount]
				];
				tableBody.innerHTML = rows.map(([name, count]) => `<tr><td>${name}</td><td class="count-value">${count.toLocaleString()}</td></tr>`).join('');
				resultEl.style.display = 'block';
			} catch (error) {
				errorEl.textContent = error.message;
				errorEl.style.display = 'block';
			}
		};
		countBtn.addEventListener('click', countHandler);
		dummyCleanup.push(() => countBtn.removeEventListener('click', countHandler));
	}

	const seedBtn = document.getElementById('seedBtn');
	if (seedBtn) {
		const seedHandler = async () => {
			const errorEl = document.getElementById('seedError');
			const resultEl = document.getElementById('seedResult');
			const loading = document.getElementById('seedLoading');
			const msgRows = {
				members: document.getElementById('r-members'),
				restaurants: document.getElementById('r-restaurants'),
				groups: document.getElementById('r-groups'),
				subgroups: document.getElementById('r-subgroups'),
				reviews: document.getElementById('r-reviews'),
				chats: document.getElementById('r-chat'),
				elapsed: document.getElementById('r-elapsed')
			};
			errorEl.style.display = 'none';
			if (resultEl) {
				resultEl.style.display = 'none';
			}
			loading.style.display = 'block';
			seedBtn.disabled = true;

			try {
				const payload = {
					memberCount: toIntOrDefault('memberCount', 500),
					restaurantCount: toIntOrDefault('restaurantCount', 200),
					groupCount: toIntOrDefault('groupCount', 20),
					subgroupPerGroup: toIntOrDefault('subgroupPerGroup', 5),
					memberPerGroup: toIntOrDefault('memberPerGroup', 30),
					reviewCount: toIntOrDefault('reviewCount', 1000),
					chatMessagePerRoom: toIntOrDefault('chatMessagePerRoom', 50)
				};
				const response = await seedDummyData(payload);
				const result = response?.data || response;
				if (msgRows.members) {
					msgRows.members.textContent = result.membersInserted.toLocaleString();
				}
				if (msgRows.restaurants) {
					msgRows.restaurants.textContent = result.restaurantsInserted.toLocaleString();
				}
				if (msgRows.groups) {
					msgRows.groups.textContent = result.groupsInserted.toLocaleString();
				}
				if (msgRows.subgroups) {
					msgRows.subgroups.textContent = result.subgroupsInserted.toLocaleString();
				}
				if (msgRows.reviews) {
					msgRows.reviews.textContent = result.reviewsInserted.toLocaleString();
				}
				if (msgRows.chats) {
					msgRows.chats.textContent = result.chatMessagesInserted.toLocaleString();
				}
				if (msgRows.elapsed) {
					msgRows.elapsed.textContent = `소요 시간: ${result.elapsedMs.toLocaleString()} ms`;
				}
				if (resultEl) {
					resultEl.style.display = 'block';
				}
			} catch (error) {
				errorEl.textContent = error.message;
				errorEl.style.display = 'block';
			} finally {
				loading.style.display = 'none';
				seedBtn.disabled = false;
			}
		};
		seedBtn.addEventListener('click', seedHandler);
		dummyCleanup.push(() => seedBtn.removeEventListener('click', seedHandler));
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
				msg.textContent = '더미 데이터가 모두 삭제되었습니다.';
				msg.className = 'status-msg success';
			} catch (error) {
				msg.textContent = `삭제 중 오류: ${error.message}`;
				msg.className = 'status-msg error';
			}
			msg.style.display = 'block';
		};
		deleteBtn.addEventListener('click', deleteHandler);
		dummyCleanup.push(() => deleteBtn.removeEventListener('click', deleteHandler));
	}

	return () => {
		dummyCleanup.forEach((remove) => remove());
		dummyCleanup = [];
	};
}

window.dummyView = {
	render: renderDummy,
	mount: mountDummy,
	unmount: () => {
		dummyCleanup.forEach((remove) => remove());
		dummyCleanup = [];
	}
};
