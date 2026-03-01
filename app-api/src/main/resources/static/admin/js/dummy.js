document.addEventListener('DOMContentLoaded', () => {

    // ── 현황 조회 ────────────────────────────────────────────────────────────
    document.getElementById('countBtn').addEventListener('click', async () => {
        const errorEl = document.getElementById('countError');
        const resultEl = document.getElementById('countResult');
        errorEl.style.display = 'none';
        resultEl.style.display = 'none';

        try {
            const res = await getDataCounts();
            const data = res.data;
            const rows = [
                ['member', data.memberCount],
                ['restaurant', data.restaurantCount],
                ['"group"', data.groupCount],
                ['subgroup', data.subgroupCount],
                ['review', data.reviewCount],
                ['chat_message', data.chatMessageCount],
            ];
            const tbody = document.getElementById('countTableBody');
            tbody.innerHTML = rows.map(([name, count]) =>
                `<tr><td>${name}</td><td class="count-value">${count.toLocaleString()}</td></tr>`
            ).join('');
            resultEl.style.display = 'block';
        } catch (e) {
            errorEl.textContent = e.message;
            errorEl.style.display = 'block';
        }
    });

    // ── 더미 데이터 삽입 ─────────────────────────────────────────────────────
    document.getElementById('seedBtn').addEventListener('click', async () => {
        const seedBtn = document.getElementById('seedBtn');
        const loadingEl = document.getElementById('seedLoading');
        const resultEl = document.getElementById('seedResult');
        const errorEl = document.getElementById('seedError');

        errorEl.style.display = 'none';
        resultEl.style.display = 'none';
        loadingEl.style.display = 'block';
        seedBtn.disabled = true;

        const intVal = (id, def) => {
            const v = parseInt(document.getElementById(id).value, 10);
            return isNaN(v) || v <= 0 ? def : v;
        };

        const payload = {
            memberCount: intVal('memberCount', 500),
            restaurantCount: intVal('restaurantCount', 200),
            groupCount: intVal('groupCount', 20),
            subgroupPerGroup: intVal('subgroupPerGroup', 5),
            memberPerGroup: intVal('memberPerGroup', 30),
            reviewCount: intVal('reviewCount', 1000),
            chatMessagePerRoom: intVal('chatMessagePerRoom', 50),
        };

        try {
            const res = await seedDummyData(payload);
            const d = res.data;
            document.getElementById('r-members').textContent = d.membersInserted.toLocaleString();
            document.getElementById('r-restaurants').textContent = d.restaurantsInserted.toLocaleString();
            document.getElementById('r-groups').textContent = d.groupsInserted.toLocaleString();
            document.getElementById('r-subgroups').textContent = d.subgroupsInserted.toLocaleString();
            document.getElementById('r-reviews').textContent = d.reviewsInserted.toLocaleString();
            document.getElementById('r-chat').textContent = d.chatMessagesInserted.toLocaleString();
            document.getElementById('r-elapsed').textContent = `소요 시간: ${d.elapsedMs.toLocaleString()} ms`;
            resultEl.style.display = 'block';
        } catch (e) {
            errorEl.textContent = e.message;
            errorEl.style.display = 'block';
        } finally {
            loadingEl.style.display = 'none';
            seedBtn.disabled = false;
        }
    });

    // ── 더미 데이터 삭제 ─────────────────────────────────────────────────────
    document.getElementById('deleteBtn').addEventListener('click', async () => {
        if (!confirm('더미 데이터를 전부 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.')) {
            return;
        }

        const msgEl = document.getElementById('deleteMsg');
        msgEl.style.display = 'none';
        msgEl.className = 'status-msg';

        try {
            await deleteDummyData();
            msgEl.textContent = '더미 데이터가 모두 삭제되었습니다.';
            msgEl.classList.add('success');
        } catch (e) {
            msgEl.textContent = '삭제 중 오류가 발생했습니다: ' + e.message;
            msgEl.classList.add('error');
        }
        msgEl.style.display = 'block';
    });
});
