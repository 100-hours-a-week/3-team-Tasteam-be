checkAuth();

let restaurantId = null;
let menuCategories = [];

function getRestaurantId() {
    const params = new URLSearchParams(window.location.search);
    return params.get('id');
}

function goBackToRestaurant() {
    location.href = `/admin/pages/restaurant-edit.html?id=${restaurantId}`;
}

async function loadRestaurantInfo() {
    restaurantId = getRestaurantId();
    if (!restaurantId) {
        alert('음식점 ID가 없습니다.');
        location.href = '/admin/pages/restaurants.html';
        return;
    }

    try {
        const result = await getRestaurant(restaurantId);
        document.getElementById('restaurantName').textContent = result.data.name;
    } catch (error) {
        alert('음식점 정보를 불러오는데 실패했습니다: ' + error.message);
    }
}

async function loadMenus() {
    try {
        const result = await getRestaurantMenus(restaurantId);
        menuCategories = result.data.categories || [];
        displayCategories();
        displayMenus();
        updateCategorySelect();
    } catch (error) {
        alert('메뉴를 불러오는데 실패했습니다: ' + error.message);
    }
}

function displayCategories() {
    const container = document.getElementById('categoryList');
    if (menuCategories.length === 0) {
        container.innerHTML = '<p>등록된 카테고리가 없습니다.</p>';
        return;
    }

    container.innerHTML = menuCategories.map(category => `
        <div class="category-item">
            <div>
                <strong>${category.name}</strong>
                <span>(순서: ${category.displayOrder})</span>
            </div>
        </div>
    `).join('');
}

function displayMenus() {
    const container = document.getElementById('menuList');
    const allMenus = menuCategories.flatMap(cat =>
        (cat.menus || []).map(menu => ({...menu, categoryName: cat.name}))
    );

    if (allMenus.length === 0) {
        container.innerHTML = '<p>등록된 메뉴가 없습니다.</p>';
        return;
    }

    container.innerHTML = allMenus.map(menu => `
        <div class="menu-item">
            <div class="menu-item-body">
                <div class="menu-item-media">
                    ${menu.imageUrl
                        ? `<img class="menu-item-image" src="${menu.imageUrl}" alt="${menu.name}">`
                        : `<div class="menu-item-placeholder">이미지 없음</div>`}
                </div>
                <div class="menu-item-info">
                    <div class="menu-item-title">
                        <strong>${menu.name}</strong>
                        ${menu.isRecommended ? '<span class="badge badge-active">추천</span>' : ''}
                    </div>
                    <div class="menu-item-meta">
                        ${menu.categoryName} | ${menu.price.toLocaleString()}원
                    </div>
                    ${menu.description ? `<div class="menu-item-description">${menu.description}</div>` : ''}
                </div>
            </div>
        </div>
    `).join('');
}

function updateCategorySelect() {
    const select = document.getElementById('menuCategory');
    select.innerHTML = '<option value="">카테고리를 선택하세요</option>' +
        menuCategories.map(cat => `<option value="${cat.id}">${cat.name}</option>`).join('');
}

document.addEventListener('DOMContentLoaded', async () => {
    await loadRestaurantInfo();
    await loadMenus();

    const menuImageInput = document.getElementById('menuImage');
    const menuImagePreview = document.getElementById('menuImagePreview');
    const menuImageAddBtn = document.getElementById('menuImageAddBtn');
    const menuImageFileName = document.getElementById('menuImageFileName');
    const menuImageUrlInput = document.getElementById('menuImageUrl');
    let menuImageFile = null;

    menuImageAddBtn.addEventListener('click', () => {
        menuImageInput.click();
    });

    menuImageInput.addEventListener('change', async () => {
        const files = Array.from(menuImageInput.files || []);
        const file = files.length > 0 ? files[0] : null;
        menuImagePreview.innerHTML = '';

        if (!file) {
            menuImageFile = null;
            menuImageFileName.textContent = '선택된 파일 없음';
            return;
        }

        try {
            menuImageFile = await ImageOptimizer.optimizeRestaurantImage(file);
            menuImageFileName.textContent = menuImageFile.name;
            const img = document.createElement('img');
            img.className = 'image-preview';
            img.alt = menuImageFile.name;
            img.src = URL.createObjectURL(menuImageFile);
            menuImagePreview.appendChild(img);
        } catch (error) {
            console.error('메뉴 이미지 최적화 오류:', error);
            alert('메뉴 이미지 최적화 중 오류가 발생했습니다: ' + error.message);
            menuImageFile = null;
            menuImageFileName.textContent = '선택된 파일 없음';
        }
    });

    document.getElementById('categoryForm').addEventListener('submit', async (e) => {
        e.preventDefault();

        const data = {
            name: document.getElementById('categoryName').value,
            displayOrder: parseInt(document.getElementById('categoryDisplayOrder').value)
        };

        try {
            await createMenuCategory(restaurantId, data);
            alert('카테고리가 추가되었습니다.');
            document.getElementById('categoryForm').reset();
            await loadMenus();
        } catch (error) {
            alert('카테고리 추가에 실패했습니다: ' + error.message);
        }
    });

    document.getElementById('menuForm').addEventListener('submit', async (e) => {
        e.preventDefault();

        const categoryId = parseInt(document.getElementById('menuCategory').value);

        if (!categoryId) {
            alert('카테고리를 선택해주세요.');
            return;
        }

        let imageFileUuid = null;
        if (menuImageFile) {
            try {
                const presigned = await createPresignedUploads('MENU_IMAGE', [menuImageFile]);
                const upload = presigned.data?.uploads?.[0];
                if (upload) {
                    await uploadToPresigned(upload, menuImageFile);
                    imageFileUuid = upload.fileUuid;
                }
            } catch (error) {
                alert('이미지 업로드에 실패했습니다: ' + error.message);
                return;
            }
        }

        const data = {
            categoryId: categoryId,
            name: document.getElementById('menuName').value,
            price: parseInt(document.getElementById('menuPrice').value),
            description: document.getElementById('menuDescription').value || null,
            isRecommended: document.getElementById('menuIsRecommended').checked,
            imageUrl: menuImageUrlInput.value.trim() || null
        };

        try {
            await createMenu(restaurantId, data);
            alert('메뉴가 추가되었습니다.');
            document.getElementById('menuForm').reset();
            menuImagePreview.innerHTML = '';
            menuImageFileName.textContent = '선택된 파일 없음';
            menuImageUrlInput.value = '';
            menuImageFile = null;
            await loadMenus();
        } catch (error) {
            alert('메뉴 추가에 실패했습니다: ' + error.message);
        }
    });
});
