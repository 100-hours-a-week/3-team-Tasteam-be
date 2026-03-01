function renderAnnouncementForm(container, state = {}) {
	const isEditMode = state.mode === 'edit';
	container.innerHTML = `
        <div class="content-header">
            <h1>${isEditMode ? '공지사항 수정' : '공지사항 등록'}</h1>
            <button class="btn btn-secondary" id="announcement-form-back-btn">목록으로</button>
        </div>

        <form id="announcementForm" class="form-container">
            <section class="form-section">
                <h2>공지사항 정보</h2>
                <div class="form-group">
                    <label for="title">제목 *</label>
                    <input type="text" id="title" name="title" required maxlength="200" placeholder="공지사항 제목을 입력하세요">
                </div>
                <div class="form-group">
                    <label for="content">내용 *</label>
                    <textarea id="content" name="content" required rows="12" placeholder="공지사항 내용을 입력하세요"></textarea>
                </div>
            </section>

            <div class="form-actions">
                <button type="submit" class="btn btn-primary">${isEditMode ? '수정' : '등록'}</button>
                <button type="button" class="btn btn-secondary" id="announcement-form-cancel-btn">취소</button>
            </div>
        </form>
    `;
}

function getAnnouncementId() {
	const params = new URLSearchParams(window.location.search);
	return params.get('id');
}

function mountAnnouncementForm(state = {}) {
	const cleanups = [];
	const isEditMode = state.mode === 'edit';
	const announcementId = isEditMode ? getAnnouncementId() : null;

	const goList = () => {
		AdminUtils?.navigateTo('/admin/pages/announcements.html');
	};

	const back = document.getElementById('announcement-form-back-btn');
	if (back) {
		back.addEventListener('click', goList);
		cleanups.push(() => back.removeEventListener('click', goList));
	}

	const cancel = document.getElementById('announcement-form-cancel-btn');
	if (cancel) {
		cancel.addEventListener('click', goList);
		cleanups.push(() => cancel.removeEventListener('click', goList));
	}

	if (isEditMode) {
		if (!announcementId) {
			alert('공지사항 ID가 없습니다.');
			goList();
			return () => cleanups.forEach((remove) => remove());
		}

		const loadAnnouncement = async () => {
			try {
				const data = await api.get(`/admin/announcements/${announcementId}`);
				const title = document.getElementById('title');
				const content = document.getElementById('content');
				if (title) {
					title.value = data.title || '';
				}
				if (content) {
					content.value = data.content || '';
				}
			} catch (error) {
				alert('공지사항 정보를 불러오지 못했습니다.');
				goList();
			}
		};
		loadAnnouncement();
	}

	const form = document.getElementById('announcementForm');
	if (form) {
		const submit = async (event) => {
			event.preventDefault();

			try {
				const payload = {
					title: document.getElementById('title')?.value?.trim() || '',
					content: document.getElementById('content')?.value?.trim() || ''
				};

				if (isEditMode) {
					await api.patch(`/admin/announcements/${announcementId}`, payload);
					alert('수정되었습니다.');
				} else {
					await api.post('/admin/announcements', payload);
					alert('등록되었습니다.');
				}
				goList();
			} catch (error) {
				alert(`저장 실패: ${error.message || '요청 처리 중 오류가 발생했습니다.'}`);
			}
		};
		form.addEventListener('submit', submit);
		cleanups.push(() => form.removeEventListener('submit', submit));
	}

	return () => {
		cleanups.forEach((remove) => remove());
	};
}

window.announcementFormView = {
	render: renderAnnouncementForm,
	mount: mountAnnouncementForm,
	unmount: () => {}
};
