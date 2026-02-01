checkAuth();

let foodCategories = [];
let restaurantId = null;

function getRestaurantId() {
    const params = new URLSearchParams(window.location.search);
    return params.get('id');
}

async function loadFoodCategories() {
    try {
        const result = await getFoodCategories();
        foodCategories = result.data || [];
        displayFoodCategories();
    } catch (error) {
        alert('음식 카테고리를 불러오는데 실패했습니다: ' + error.message);
    }
}

function displayFoodCategories(selectedIds = []) {
    const container = document.getElementById('foodCategoryList');
    container.innerHTML = foodCategories.map(category => `
        <label>
            <input type="checkbox" name="foodCategory" value="${category.id}"
                ${selectedIds.includes(category.id) ? 'checked' : ''}>
            ${category.name}
        </label>
    `).join('');
}

async function loadRestaurant() {
    restaurantId = getRestaurantId();
    if (!restaurantId) {
        alert('음식점 ID가 없습니다.');
        location.href = '/admin/pages/restaurants.html';
        return;
    }

    try {
        const result = await getRestaurant(restaurantId);
        const restaurant = result.data;

        document.getElementById('restaurantId').value = restaurant.id;
        document.getElementById('name').value = restaurant.name;
        document.getElementById('address').value = restaurant.address;
        document.getElementById('detailAddress').value = restaurant.detailAddress || '';
        document.getElementById('description').value = restaurant.description || '';

        const selectedCategoryIds = (restaurant.foodCategories || []).map(fc => fc.id);
        displayFoodCategories(selectedCategoryIds);
    } catch (error) {
        alert('음식점 정보를 불러오는데 실패했습니다: ' + error.message);
    }
}

async function deleteRestaurant() {
    if (!confirm('정말로 이 음식점을 삭제하시겠습니까?')) {
        return;
    }

    try {
        await deleteRestaurant(restaurantId);
        alert('음식점이 삭제되었습니다.');
        location.href = '/admin/pages/restaurants.html';
    } catch (error) {
        alert('음식점 삭제에 실패했습니다: ' + error.message);
    }
}

document.addEventListener('DOMContentLoaded', async () => {
    await loadFoodCategories();
    await loadRestaurant();

    document.getElementById('restaurantEditForm').addEventListener('submit', async (e) => {
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
            imageIds: []
        };

        try {
            await updateRestaurant(restaurantId, data);
            alert('음식점이 수정되었습니다.');
            location.href = '/admin/pages/restaurants.html';
        } catch (error) {
            alert('음식점 수정에 실패했습니다: ' + error.message);
        }
    });
});
