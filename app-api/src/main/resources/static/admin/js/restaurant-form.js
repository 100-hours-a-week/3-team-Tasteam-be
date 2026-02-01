checkAuth();

let foodCategories = [];

async function loadFoodCategories() {
    try {
        const result = await getFoodCategories();
        foodCategories = result.data || [];
        displayFoodCategories();
    } catch (error) {
        alert('음식 카테고리를 불러오는데 실패했습니다: ' + error.message);
    }
}

function displayFoodCategories() {
    const container = document.getElementById('foodCategoryList');
    container.innerHTML = foodCategories.map(category => `
        <label>
            <input type="checkbox" name="foodCategory" value="${category.id}">
            ${category.name}
        </label>
    `).join('');
}

function getScheduleData() {
    const schedules = [];
    const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

    days.forEach(day => {
        const isClosed = document.querySelector(`.closed-checkbox[data-day="${day}"]`).checked;
        const openTime = document.querySelector(`.open-time[data-day="${day}"]`).value;
        const closeTime = document.querySelector(`.close-time[data-day="${day}"]`).value;

        if (isClosed || (openTime && closeTime)) {
            schedules.push({
                dayOfWeek: day,
                openTime: isClosed ? null : openTime,
                closeTime: isClosed ? null : closeTime,
                isClosed: isClosed,
                effectiveFrom: null,
                effectiveTo: null
            });
        }
    });

    return schedules;
}

document.addEventListener('DOMContentLoaded', () => {
    loadFoodCategories();

    document.querySelectorAll('.closed-checkbox').forEach(checkbox => {
        checkbox.addEventListener('change', (e) => {
            const day = e.target.dataset.day;
            const openTime = document.querySelector(`.open-time[data-day="${day}"]`);
            const closeTime = document.querySelector(`.close-time[data-day="${day}"]`);
            openTime.disabled = e.target.checked;
            closeTime.disabled = e.target.checked;
            if (e.target.checked) {
                openTime.value = '';
                closeTime.value = '';
            }
        });
    });

    document.getElementById('restaurantForm').addEventListener('submit', async (e) => {
        e.preventDefault();

        const selectedCategories = Array.from(document.querySelectorAll('input[name="foodCategory"]:checked'))
            .map(cb => parseInt(cb.value));

        if (selectedCategories.length === 0) {
            alert('최소 1개 이상의 음식 카테고리를 선택해주세요.');
            return;
        }

        const data = {
            name: document.getElementById('name').value,
            address: document.getElementById('address').value,
            detailAddress: document.getElementById('detailAddress').value || null,
            description: document.getElementById('description').value || null,
            foodCategoryIds: selectedCategories,
            imageIds: [],
            weeklySchedules: getScheduleData()
        };

        try {
            await createRestaurant(data);
            alert('음식점이 등록되었습니다.');
            location.href = '/admin/pages/restaurants.html';
        } catch (error) {
            alert('음식점 등록에 실패했습니다: ' + error.message);
        }
    });
});
