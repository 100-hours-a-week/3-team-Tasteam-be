let notificationsCleanup = [];

const NOTIFICATION_TYPES = ['NOTICE', 'SYSTEM', 'CHAT'];

function renderNotifications(container) {
	container.innerHTML = `
        <div class="content-header">
            <h1>알림 발송</h1>
            <p class="content-subtitle">회원에게 푸시·웹·이메일 알림을 발송합니다.</p>
        </div>

        <div class="job-cards" style="grid-template-columns: repeat(2, minmax(0, 1fr));">

            <!-- 채널 선택 일괄 발송 (비동기) -->
            <div class="card job-card" style="grid-column: 1 / -1;">
                <h3>채널 선택 일괄 발송 <span class="badge badge-info" style="font-size:12px;margin-left:8px;">비동기</span></h3>
                <p>WEB·PUSH·EMAIL 채널을 복수 선택하여 모든 활성 회원에게 일괄 발송합니다. 202 즉시 반환 후 백그라운드에서 처리됩니다.</p>

                <div class="form-group">
                    <label>알림 타입</label>
                    <select id="broadcastAllType">
                        ${NOTIFICATION_TYPES.map((t) => `<option value="${t}">${t}</option>`).join('')}
                    </select>
                </div>
                <div class="form-group">
                    <label>제목 <span style="color:#e74c3c;">*</span></label>
                    <input type="text" id="broadcastAllTitle" maxlength="100" placeholder="알림 제목 (최대 100자)">
                </div>
                <div class="form-group">
                    <label>내용 <span style="color:#e74c3c;">*</span></label>
                    <textarea id="broadcastAllBody" rows="3" maxlength="500" placeholder="알림 내용 (최대 500자)"></textarea>
                </div>
                <div class="form-group">
                    <label>딥링크</label>
                    <input type="text" id="broadcastAllDeepLink" maxlength="500" placeholder="/notices (선택사항)">
                </div>
                <div class="form-group">
                    <label>발송 채널 <span style="color:#e74c3c;">*</span></label>
                    <div class="button-group" style="gap:12px;">
                        <label style="display:flex;align-items:center;gap:6px;cursor:pointer;">
                            <input type="checkbox" id="channelWeb" value="WEB"> WEB (인앱)
                        </label>
                        <label style="display:flex;align-items:center;gap:6px;cursor:pointer;">
                            <input type="checkbox" id="channelPush" value="PUSH"> PUSH (FCM)
                        </label>
                        <label style="display:flex;align-items:center;gap:6px;cursor:pointer;">
                            <input type="checkbox" id="channelEmail" value="EMAIL"> EMAIL
                        </label>
                    </div>
                </div>
                <div id="emailFieldsAll" class="is-hidden">
                    <div class="form-group">
                        <label>템플릿 키 <span style="color:#e74c3c;">*</span></label>
                        <input type="text" id="broadcastAllTemplateKey" placeholder="member-welcome">
                    </div>
                    <div class="form-group">
                        <label>템플릿 변수 (JSON)</label>
                        <textarea id="broadcastAllTemplateVars" rows="3" placeholder='{"name": "홍길동"}'></textarea>
                        <div class="help-text">EMAIL 채널 선택 시 templateVariables를 JSON으로 입력하세요.</div>
                    </div>
                </div>
                <div class="button-group">
                    <button id="runBroadcastAll" class="btn btn-primary">발송 요청</button>
                </div>
                <div id="broadcastAllResult" class="status-msg success is-hidden"></div>
                <div id="broadcastAllError" class="status-msg error is-hidden"></div>
            </div>

            <!-- 푸시 테스트 발송 -->
            <div class="card job-card">
                <h3>푸시 테스트 발송</h3>
                <p>특정 회원에게 FCM 푸시 알림을 테스트 발송합니다.</p>

                <div class="form-group">
                    <label>회원 ID <span style="color:#e74c3c;">*</span></label>
                    <input type="number" id="testMemberId" min="1" placeholder="123">
                </div>
                <div class="form-group">
                    <label>제목 <span style="color:#e74c3c;">*</span></label>
                    <input type="text" id="testPushTitle" maxlength="100" placeholder="테스트 알림 제목">
                </div>
                <div class="form-group">
                    <label>내용 <span style="color:#e74c3c;">*</span></label>
                    <textarea id="testPushBody" rows="3" maxlength="1000" placeholder="테스트 알림 내용"></textarea>
                </div>
                <div class="form-group">
                    <label>딥링크</label>
                    <input type="text" id="testPushDeepLink" maxlength="500" placeholder="/notices (선택사항)">
                </div>
                <div class="button-group">
                    <button id="runTestPush" class="btn btn-primary">발송</button>
                </div>
                <div id="testPushResult" class="status-msg success is-hidden"></div>
                <div id="testPushError" class="status-msg error is-hidden"></div>
            </div>

            <!-- 푸시 일괄 발송 -->
            <div class="card job-card">
                <h3>푸시 일괄 발송</h3>
                <p>지정한 알림 타입의 PUSH 채널을 허용한 모든 회원에게 FCM 알림을 일괄 발송합니다.</p>

                <div class="form-group">
                    <label>알림 타입</label>
                    <select id="broadcastPushType">
                        ${NOTIFICATION_TYPES.map((t) => `<option value="${t}">${t}</option>`).join('')}
                    </select>
                </div>
                <div class="form-group">
                    <label>제목 <span style="color:#e74c3c;">*</span></label>
                    <input type="text" id="broadcastPushTitle" maxlength="100" placeholder="알림 제목">
                </div>
                <div class="form-group">
                    <label>내용 <span style="color:#e74c3c;">*</span></label>
                    <textarea id="broadcastPushBody" rows="3" maxlength="1000" placeholder="알림 내용"></textarea>
                </div>
                <div class="form-group">
                    <label>딥링크</label>
                    <input type="text" id="broadcastPushDeepLink" maxlength="500" placeholder="/notices (선택사항)">
                </div>
                <div class="button-group">
                    <button id="runBroadcastPush" class="btn btn-primary">일괄 발송</button>
                </div>
                <div id="broadcastPushResult" class="job-result is-hidden">
                    <h4>발송 결과</h4>
                    <ul>
                        <li>대상: <strong id="pushTotal">0</strong></li>
                        <li>성공: <strong id="pushSuccess" style="color:#2ecc71;">0</strong></li>
                        <li>실패: <strong id="pushFailed" style="color:#e74c3c;">0</strong></li>
                        <li>건너뜀: <strong id="pushSkipped">0</strong></li>
                    </ul>
                </div>
                <div id="broadcastPushError" class="status-msg error is-hidden"></div>
            </div>

            <!-- 이메일 일괄 발송 -->
            <div class="card job-card" style="grid-column: 1 / -1;">
                <h3>이메일 일괄 발송</h3>
                <p>지정한 알림 타입의 EMAIL 채널을 허용한 모든 회원에게 템플릿 이메일을 일괄 발송합니다.</p>

                <div class="form-row">
                    <div class="form-group">
                        <label>알림 타입</label>
                        <select id="broadcastEmailType">
                            ${NOTIFICATION_TYPES.map((t) => `<option value="${t}">${t}</option>`).join('')}
                        </select>
                    </div>
                    <div class="form-group">
                        <label>템플릿 키 <span style="color:#e74c3c;">*</span></label>
                        <input type="text" id="broadcastEmailTemplateKey" placeholder="member-welcome">
                    </div>
                </div>
                <div class="form-group">
                    <label>템플릿 변수 (JSON)</label>
                    <textarea id="broadcastEmailVars" rows="3" placeholder='{"name": "홍길동", "groupName": "팀A"}'></textarea>
                    <div class="help-text">템플릿에서 사용할 변수를 JSON 형식으로 입력하세요. 비워두면 빈 객체로 전송됩니다.</div>
                </div>
                <div class="button-group">
                    <button id="runBroadcastEmail" class="btn btn-primary">이메일 발송</button>
                </div>
                <div id="broadcastEmailResult" class="job-result is-hidden">
                    <h4>발송 결과</h4>
                    <ul>
                        <li>대상: <strong id="emailTotal">0</strong></li>
                        <li>성공: <strong id="emailSuccess" style="color:#2ecc71;">0</strong></li>
                        <li>실패: <strong id="emailFailed" style="color:#e74c3c;">0</strong></li>
                        <li>건너뜀: <strong id="emailSkipped">0</strong></li>
                    </ul>
                </div>
                <div id="broadcastEmailError" class="status-msg error is-hidden"></div>
            </div>

        </div>
    `;
}

function setLoading(btn, text) {
	if (!btn) {
		return;
	}
	btn.disabled = true;
	btn.dataset.originalText = btn.textContent;
	btn.textContent = text;
}

function clearLoading(btn) {
	if (!btn) {
		return;
	}
	btn.disabled = false;
	if (btn.dataset.originalText) {
		btn.textContent = btn.dataset.originalText;
	}
}

function showSuccess(el, msg) {
	if (!el) {
		return;
	}
	el.textContent = msg;
	el.classList.remove('is-hidden');
}

function showError(el, msg) {
	if (!el) {
		return;
	}
	el.textContent = msg;
	el.classList.remove('is-hidden');
}

function hideEl(el) {
	if (!el) {
		return;
	}
	el.classList.add('is-hidden');
}

function parseJsonOrNull(str) {
	const trimmed = (str || '').trim();
	if (!trimmed) {
		return null;
	}
	try {
		return JSON.parse(trimmed);
	} catch (e) {
		throw new Error('JSON 형식이 올바르지 않습니다.');
	}
}

function mountNotifications() {
	notificationsCleanup = [];

	// EMAIL 채널 체크 시 이메일 필드 토글
	const channelEmailCheckbox = document.getElementById('channelEmail');
	if (channelEmailCheckbox) {
		const toggleEmailFields = () => {
			const emailFields = document.getElementById('emailFieldsAll');
			if (emailFields) {
				if (channelEmailCheckbox.checked) {
					emailFields.classList.remove('is-hidden');
				} else {
					emailFields.classList.add('is-hidden');
				}
			}
		};
		channelEmailCheckbox.addEventListener('change', toggleEmailFields);
		notificationsCleanup.push(() => channelEmailCheckbox.removeEventListener('change', toggleEmailFields));
	}

	// 채널 선택 일괄 발송
	const broadcastAllBtn = document.getElementById('runBroadcastAll');
	if (broadcastAllBtn) {
		const handler = async () => {
			const resultEl = document.getElementById('broadcastAllResult');
			const errorEl = document.getElementById('broadcastAllError');
			hideEl(resultEl);
			hideEl(errorEl);

			const channels = ['channelWeb', 'channelPush', 'channelEmail']
				.filter((id) => document.getElementById(id)?.checked)
				.map((id) => document.getElementById(id).value);

			if (channels.length === 0) {
				showError(errorEl, '발송 채널을 하나 이상 선택하세요.');
				return;
			}

			const title = document.getElementById('broadcastAllTitle')?.value.trim();
			const body = document.getElementById('broadcastAllBody')?.value.trim();
			if (!title || !body) {
				showError(errorEl, '제목과 내용을 입력하세요.');
				return;
			}

			const templateKey = document.getElementById('broadcastAllTemplateKey')?.value.trim() || null;
			if (channels.includes('EMAIL') && !templateKey) {
				showError(errorEl, 'EMAIL 채널 선택 시 템플릿 키가 필요합니다.');
				return;
			}

			let templateVariables = null;
			try {
				templateVariables = parseJsonOrNull(document.getElementById('broadcastAllTemplateVars')?.value);
			} catch (e) {
				showError(errorEl, `템플릿 변수 ${e.message}`);
				return;
			}

			const payload = {
				notificationType: document.getElementById('broadcastAllType')?.value,
				title,
				body,
				deepLink: document.getElementById('broadcastAllDeepLink')?.value.trim() || null,
				channels,
				templateKey,
				templateVariables
			};

			setLoading(broadcastAllBtn, '요청 중...');
			try {
				await window.apiRequest('/admin/notifications/broadcast', {
					method: 'POST',
					body: JSON.stringify(payload)
				});
				showSuccess(resultEl, '발송 요청이 접수되었습니다. 백그라운드에서 처리됩니다.');
			} catch (e) {
				showError(errorEl, e.message);
			} finally {
				clearLoading(broadcastAllBtn);
			}
		};
		broadcastAllBtn.addEventListener('click', handler);
		notificationsCleanup.push(() => broadcastAllBtn.removeEventListener('click', handler));
	}

	// 푸시 테스트 발송
	const testPushBtn = document.getElementById('runTestPush');
	if (testPushBtn) {
		const handler = async () => {
			const resultEl = document.getElementById('testPushResult');
			const errorEl = document.getElementById('testPushError');
			hideEl(resultEl);
			hideEl(errorEl);

			const memberId = parseInt(document.getElementById('testMemberId')?.value, 10);
			const title = document.getElementById('testPushTitle')?.value.trim();
			const body = document.getElementById('testPushBody')?.value.trim();
			if (!memberId || !title || !body) {
				showError(errorEl, '회원 ID, 제목, 내용을 모두 입력하세요.');
				return;
			}

			setLoading(testPushBtn, '발송 중...');
			try {
				const res = await window.apiRequest('/admin/notifications/push/test', {
					method: 'POST',
					body: JSON.stringify({
						memberId,
						title,
						body,
						deepLink: document.getElementById('testPushDeepLink')?.value.trim() || null
					})
				});
				const data = res?.data;
				showSuccess(resultEl, `발송 완료. messageId: ${data?.messageId || '-'}`);
			} catch (e) {
				showError(errorEl, e.message);
			} finally {
				clearLoading(testPushBtn);
			}
		};
		testPushBtn.addEventListener('click', handler);
		notificationsCleanup.push(() => testPushBtn.removeEventListener('click', handler));
	}

	// 푸시 일괄 발송
	const broadcastPushBtn = document.getElementById('runBroadcastPush');
	if (broadcastPushBtn) {
		const handler = async () => {
			const resultEl = document.getElementById('broadcastPushResult');
			const errorEl = document.getElementById('broadcastPushError');
			hideEl(resultEl);
			hideEl(errorEl);

			const title = document.getElementById('broadcastPushTitle')?.value.trim();
			const body = document.getElementById('broadcastPushBody')?.value.trim();
			if (!title || !body) {
				showError(errorEl, '제목과 내용을 입력하세요.');
				return;
			}

			setLoading(broadcastPushBtn, '발송 중...');
			try {
				const res = await window.apiRequest('/admin/notifications/push/broadcast', {
					method: 'POST',
					body: JSON.stringify({
						notificationType: document.getElementById('broadcastPushType')?.value,
						title,
						body,
						deepLink: document.getElementById('broadcastPushDeepLink')?.value.trim() || null
					})
				});
				const d = res?.data;
				document.getElementById('pushTotal').textContent = d?.totalTargets ?? 0;
				document.getElementById('pushSuccess').textContent = d?.successCount ?? 0;
				document.getElementById('pushFailed').textContent = d?.failureCount ?? 0;
				document.getElementById('pushSkipped').textContent = d?.skippedCount ?? 0;
				resultEl.classList.remove('is-hidden');
			} catch (e) {
				showError(errorEl, e.message);
			} finally {
				clearLoading(broadcastPushBtn);
			}
		};
		broadcastPushBtn.addEventListener('click', handler);
		notificationsCleanup.push(() => broadcastPushBtn.removeEventListener('click', handler));
	}

	// 이메일 일괄 발송
	const broadcastEmailBtn = document.getElementById('runBroadcastEmail');
	if (broadcastEmailBtn) {
		const handler = async () => {
			const resultEl = document.getElementById('broadcastEmailResult');
			const errorEl = document.getElementById('broadcastEmailError');
			hideEl(resultEl);
			hideEl(errorEl);

			const templateKey = document.getElementById('broadcastEmailTemplateKey')?.value.trim();
			if (!templateKey) {
				showError(errorEl, '템플릿 키를 입력하세요.');
				return;
			}

			let variables = {};
			try {
				variables = parseJsonOrNull(document.getElementById('broadcastEmailVars')?.value) || {};
			} catch (e) {
				showError(errorEl, `템플릿 변수 ${e.message}`);
				return;
			}

			setLoading(broadcastEmailBtn, '발송 중...');
			try {
				const res = await window.apiRequest('/admin/notifications/email/broadcast', {
					method: 'POST',
					body: JSON.stringify({
						notificationType: document.getElementById('broadcastEmailType')?.value,
						templateKey,
						variables
					})
				});
				const d = res?.data;
				document.getElementById('emailTotal').textContent = d?.totalTargets ?? 0;
				document.getElementById('emailSuccess').textContent = d?.successCount ?? 0;
				document.getElementById('emailFailed').textContent = d?.failureCount ?? 0;
				document.getElementById('emailSkipped').textContent = d?.skippedCount ?? 0;
				resultEl.classList.remove('is-hidden');
			} catch (e) {
				showError(errorEl, e.message);
			} finally {
				clearLoading(broadcastEmailBtn);
			}
		};
		broadcastEmailBtn.addEventListener('click', handler);
		notificationsCleanup.push(() => broadcastEmailBtn.removeEventListener('click', handler));
	}

	return () => {
		notificationsCleanup.forEach((remove) => remove());
		notificationsCleanup = [];
	};
}

window.notificationsView = {
	render: renderNotifications,
	mount: mountNotifications,
	unmount: () => {
		notificationsCleanup.forEach((remove) => remove());
		notificationsCleanup = [];
	}
};
