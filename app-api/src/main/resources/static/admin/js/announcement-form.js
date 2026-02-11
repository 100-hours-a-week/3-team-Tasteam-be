document.addEventListener('DOMContentLoaded', async () => {
    if (typeof isEditMode !== 'undefined' && isEditMode && announcementId) {
        await loadAnnouncementData();
    }

    document.getElementById('announcementForm').addEventListener('submit', handleSubmit);
});

async function loadAnnouncementData() {
    try {
        const data = await api.get(`/admin/announcements/${announcementId}`);
        document.getElementById('title').value = data.title || '';
        document.getElementById('content').value = data.content || '';
    } catch (error) {
        console.error('공지사항 로딩 실패:', error);
        alert('공지사항 정보를 불러오는데 실패했습니다.');
        location.href = '/admin/pages/announcements.html';
    }
}

async function handleSubmit(event) {
    event.preventDefault();

    const formData = {
        title: document.getElementById('title').value,
        content: document.getElementById('content').value
    };

    try {
        if (typeof isEditMode !== 'undefined' && isEditMode && announcementId) {
            await api.patch(`/admin/announcements/${announcementId}`, formData);
            alert('수정되었습니다.');
        } else {
            await api.post('/admin/announcements', formData);
            alert('등록되었습니다.');
        }
        location.href = '/admin/pages/announcements.html';
    } catch (error) {
        console.error('공지사항 저장 실패:', error);
        alert('저장에 실패했습니다: ' + (error.message || '알 수 없는 오류'));
    }
}
