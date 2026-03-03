let menuCleanup = [];
let restaurantId = null;
let menuCategories = [];

function renderRestaurantMenu(container) {
    if (!container) {
        return;
    }

    container.innerHTML = `
        <div class="content-header">
            <h1>메뉴 관리</h1>
            <h3 id="restaurantName"></h3>
            <button class="btn btn-secondary" id="goBackToRestaurantBtn">음식점으로 돌아가기</button>
        </div>

        <div class="menu-management">
            <div class="menu-category-section">
                <h3>메뉴 카테고리</h3>
                <form id="categoryForm" class="inline-form">
                    <input type="text" id="categoryName" placeholder="카테고리명" maxlength="50" required>
                    <input type="number" id="categoryDisplayOrder" placeholder="표시순서" min="0" value="0">
                    <button type="submit" class="btn btn-primary">카테고리 추가</button>
                </form>
                <div id="categoryList" class="category-list">
                    <p>로딩 중...</p>
                </div>
            </div>

            <div class="menu-item-section">
                <h3>메뉴 아이템</h3>
                <form id="menuForm" class="form-container">
                    <div class="form-group">
                        <label for="menuCategory">카테고리 *</label>
                        <select id="menuCategory" required>
                            <option value="">카테고리를 선택하세요</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label for="menuName">메뉴명 *</label>
                        <input type="text" id="menuName" required maxlength="100">
                    </div>
                    <div class="form-group">
                        <label for="menuPrice">가격 *</label>
                        <input type="number" id="menuPrice" required min="0">
                    </div>
                    <div class="form-group">
                        <label for="menuDescription">설명</label>
                        <textarea id="menuDescription" rows="3" maxlength="500"></textarea>
                    </div>
                    <div class="form-group">
                        <label for="menuImage">이미지</label>
                        <div class="image-upload-row">
                            <button type="button" class="btn btn-secondary" id="menuImageAddBtn">이미지 추가</button>
                            <span id="menuImageFileName" class="file-name">선택된 파일 없음</span>
                        </div>
                        <input type="file" id="menuImage" accept="image/*" class="visually-hidden">
                        <p class="help-text">이미지 등록은 선택 사항입니다.</p>
                    </div>
                    <div class="form-group">
                        <label for="menuImageUrl">이미지 URL</label>
                        <input type="url" id="menuImageUrl" placeholder="https://...">
                        <p class="help-text">파일 업로드 대신 이미지 URL을 사용할 수 있습니다.</p>
                    </div>
                    <div id="menuImagePreview" class="image-preview-grid"></div>
                    <div class="form-group">
                        <label>
                            <input type="checkbox" id="menuIsRecommended">
                            추천 메뉴
                        </label>
                    </div>
                    <button type="submit" class="btn btn-primary">메뉴 추가</button>
                </form>

                <div id="menuList" class="menu-list">
                    <p>로딩 중...</p>
                </div>
            </div>
        </div>
    `;
}

function cleanupMenuView() {
    menuCleanup.forEach((remove) => remove());
    menuCleanup = [];
}

function getRestaurantId() {
    const params = new URLSearchParams(window.location.search);
    return params.get('id');
}

async function loadRestaurantInfo() {
    restaurantId = getRestaurantId();
    if (!restaurantId) {
        alert('음식점 ID가 없습니다.');
        AdminUtils?.navigateTo('/admin/pages/restaurants.html');
        return;
    }

    try {
        const result = await getRestaurant(restaurantId);
        const restaurantName = document.getElementById('restaurantName');
        if (restaurantName) {
            restaurantName.textContent = result.data.name;
        }
    } catch (error) {
        alert(`음식점 정보를 불러오는데 실패했습니다: ${error.message}`);
    }
}

async function loadMenus() {
    if (!restaurantId) {
        return;
    }

    try {
        const result = await getRestaurantMenus(restaurantId);
        menuCategories = result.data.categories || [];
        displayCategories();
        displayMenus();
        updateCategorySelect();
    } catch (error) {
        alert(`메뉴를 불러오는데 실패했습니다: ${error.message}`);
    }
}

function displayCategories() {
    const container = document.getElementById('categoryList');
    if (!container) {
        return;
    }

    if (menuCategories.length === 0) {
        container.innerHTML = '<p>등록된 카테고리가 없습니다.</p>';
        return;
    }

    container.innerHTML = menuCategories.map((category) => `
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
    if (!container) {
        return;
    }

    const allMenus = menuCategories.flatMap((cat) => (cat.menus || []).map((menu) => ({
        ...menu,
        categoryName: cat.name
    })));

    if (allMenus.length === 0) {
        container.innerHTML = '<p>등록된 메뉴가 없습니다.</p>';
        return;
    }

    container.innerHTML = allMenus.map((menu) => `
        <div class="menu-item">
            <div class="menu-item-body">
                <div class="menu-item-media">
                    ${menu.imageUrl
                        ? `<img class="menu-item-image" src="${menu.imageUrl}" alt="${menu.name}">`
                        : `<div class="menu-item-placeholder">이미지 없음</div>`
                    }
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
    if (!select) {
        return;
    }

    select.innerHTML = '<option value="">카테고리를 선택하세요</option>' +
        menuCategories.map((cat) => `<option value="${cat.id}">${cat.name}</option>`).join('');
}

function mountRestaurantMenu() {
    cleanupMenuView();
    const cleanupFns = [];

    getRestaurantId();

    const onMount = async () => {
        await loadRestaurantInfo();
        await loadMenus();
    };
    onMount();

    const menuImageInput = document.getElementById('menuImage');
    const menuImagePreview = document.getElementById('menuImagePreview');
    const menuImageAddBtn = document.getElementById('menuImageAddBtn');
    const menuImageFileName = document.getElementById('menuImageFileName');
    const menuImageUrlInput = document.getElementById('menuImageUrl');
    let menuImageFile = null;

    if (menuImageAddBtn) {
        const openFileDialog = () => {
            menuImageInput?.click();
        };
        menuImageAddBtn.addEventListener('click', openFileDialog);
        cleanupFns.push(() => menuImageAddBtn.removeEventListener('click', openFileDialog));
    }

    if (menuImageInput) {
        const imageChange = async () => {
            const files = Array.from(menuImageInput.files || []);
            const file = files.length > 0 ? files[0] : null;
            menuImagePreview.innerHTML = '';

            if (!file) {
                menuImageFile = null;
                if (menuImageFileName) {
                    menuImageFileName.textContent = '선택된 파일 없음';
                }
                return;
            }

            try {
                menuImageFile = await ImageOptimizer.optimizeRestaurantImage(file);
                if (menuImageFileName) {
                    menuImageFileName.textContent = menuImageFile.name;
                }
                const img = document.createElement('img');
                img.className = 'image-preview';
                img.alt = menuImageFile.name;
                img.src = URL.createObjectURL(menuImageFile);
                menuImagePreview.appendChild(img);
            } catch (error) {
                alert(`메뉴 이미지 최적화 중 오류가 발생했습니다: ${error.message}`);
                menuImageFile = null;
                if (menuImageFileName) {
                    menuImageFileName.textContent = '선택된 파일 없음';
                }
            }
        };
        menuImageInput.addEventListener('change', imageChange);
        cleanupFns.push(() => menuImageInput.removeEventListener('change', imageChange));
    }

    const categoryForm = document.getElementById('categoryForm');
    if (categoryForm) {
        const submitCategory = async (event) => {
            event.preventDefault();

            const data = {
                name: document.getElementById('categoryName').value,
                displayOrder: parseInt(document.getElementById('categoryDisplayOrder').value)
            };

            try {
                await createMenuCategory(restaurantId, data);
                alert('카테고리가 추가되었습니다.');
                categoryForm.reset();
                await loadMenus();
            } catch (error) {
                alert(`카테고리 추가에 실패했습니다: ${error.message}`);
            }
        };

        categoryForm.addEventListener('submit', submitCategory);
        cleanupFns.push(() => categoryForm.removeEventListener('submit', submitCategory));
    }

    const menuForm = document.getElementById('menuForm');
    if (menuForm) {
        const submitMenu = async (event) => {
            event.preventDefault();

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
                    alert(`이미지 업로드에 실패했습니다: ${error.message}`);
                    return;
                }
            }

            const data = {
                categoryId,
                name: document.getElementById('menuName').value,
                price: parseInt(document.getElementById('menuPrice').value),
                description: document.getElementById('menuDescription').value || null,
                isRecommended: document.getElementById('menuIsRecommended').checked,
                imageUrl: menuImageUrlInput ? menuImageUrlInput.value.trim() || null : null
            };

            if (imageFileUuid) {
                data.imageFileUuid = imageFileUuid;
            }

            try {
                await createMenu(restaurantId, data);
                alert('메뉴가 추가되었습니다.');
                menuForm.reset();
                menuImageFile = null;
                if (menuImagePreview) {
                    menuImagePreview.innerHTML = '';
                }
                if (menuImageFileName) {
                    menuImageFileName.textContent = '선택된 파일 없음';
                }
                await loadMenus();
            } catch (error) {
                alert(`메뉴 추가에 실패했습니다: ${error.message}`);
            }
        };

        menuForm.addEventListener('submit', submitMenu);
        cleanupFns.push(() => menuForm.removeEventListener('submit', submitMenu));
    }

    const backBtn = document.getElementById('goBackToRestaurantBtn');
    if (backBtn) {
        const goBack = () => {
            if (restaurantId) {
                AdminUtils?.navigateTo(`/admin/pages/restaurant-edit.html?id=${restaurantId}`);
            } else {
                AdminUtils?.navigateTo('/admin/pages/restaurants.html');
            }
        };
        backBtn.addEventListener('click', goBack);
        cleanupFns.push(() => backBtn.removeEventListener('click', goBack));
    }

    menuCleanup = cleanupFns;
}

function unmountRestaurantMenu() {
    cleanupMenuView();
}

window.restaurantMenuView = {
    render: renderRestaurantMenu,
    mount: mountRestaurantMenu,
    unmount: unmountRestaurantMenu
};
