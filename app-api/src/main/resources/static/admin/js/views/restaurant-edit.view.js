let cleanupTasks = [];
let foodCategories = [];
let restaurantId = null;
let geocodeTimer = null;
let isImageSaving = false;

function renderRestaurantEdit(container) {
    if (!container) {
        return;
    }

    container.innerHTML = `
        <div class="content-header">
            <h1>음식점 수정</h1>
            <div>
                <button class="btn btn-secondary" id="goRestaurantMenuBtn">메뉴 관리</button>
                <a class="btn btn-secondary" href="/admin/pages/restaurants.html">목록으로</a>
            </div>
        </div>

        <form id="restaurantEditForm" class="form-container">
            <input type="hidden" id="restaurantId">

            <div class="form-section">
                <h3>기본 정보</h3>
                <div class="form-group">
                    <label for="name">음식점명 *</label>
                    <input type="text" id="name" name="name" required maxlength="100">
                </div>
                <div class="form-group">
                    <label for="address">주소 *</label>
                    <input type="text" id="address" name="address" required maxlength="255">
                </div>
                <div class="form-group">
                    <label for="detailAddress">상세 주소</label>
                    <input type="text" id="detailAddress" name="detailAddress" maxlength="255">
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label for="latitude">위도</label>
                        <input type="text" id="latitude" readonly>
                    </div>
                    <div class="form-group">
                        <label for="longitude">경도</label>
                        <input type="text" id="longitude" readonly>
                    </div>
                </div>
                <div class="form-group">
                    <label for="description">설명</label>
                    <textarea id="description" name="description" rows="4" maxlength="1000"></textarea>
                </div>
            </div>

            <div class="form-section">
                <h3>음식 카테고리</h3>
                <div class="category-add-row">
                    <input type="text" id="newFoodCategoryName" placeholder="카테고리명 (최대 20자)" maxlength="20">
                    <button type="button" class="btn btn-secondary" id="addFoodCategoryBtn">카테고리 추가</button>
                </div>
                <div id="foodCategoryList" class="toggle-group">
                    <p>로딩 중...</p>
                </div>
            </div>

            <div class="form-section">
                <h3>이미지</h3>
                <div class="image-section-header">
                    <div class="image-section-title">
                        <strong>현재 이미지</strong>
                        <span id="imageStatus" class="image-status">저장된 이미지</span>
                    </div>
                    <div class="image-actions">
                        <button type="button" class="btn btn-secondary" id="resetImageSelection">선택 초기화</button>
                        <button type="button" class="btn btn-primary" id="saveImagesBtn">이미지 저장</button>
                    </div>
                </div>
                <div class="form-group">
                    <div id="restaurantImageCurrent" class="image-preview-grid"></div>
                </div>
                <div class="form-group">
                    <label for="restaurantImages">새 이미지 업로드</label>
                    <input type="file" id="restaurantImages" accept="image/*" multiple>
                    <p class="help-text">새 이미지를 업로드하면 기존 이미지는 교체됩니다.</p>
                </div>
                <div id="restaurantImagePreview" class="image-preview-grid"></div>
            </div>

            <div class="form-actions">
                <button type="submit" class="btn btn-primary">전체 저장</button>
                <button type="button" class="btn btn-danger" id="deleteRestaurantBtn">삭제</button>
                <a href="/admin/pages/restaurants.html" class="btn btn-secondary">취소</a>
            </div>
        </form>
    `;
}

function cleanup() {
    cleanupTasks.forEach((remove) => remove());
    cleanupTasks = [];
    if (geocodeTimer) {
        clearTimeout(geocodeTimer);
        geocodeTimer = null;
    }
}

function getRestaurantIdFromQuery() {
    const params = new URLSearchParams(window.location.search);
    return params.get('id');
}

async function loadFoodCategories() {
    try {
        const result = await window.getFoodCategories();
        foodCategories = result.data || [];
        displayFoodCategories();
    } catch (error) {
        alert(`음식 카테고리를 불러오는데 실패했습니다: ${error.message}`);
    }
}

function displayFoodCategories(selectedIds = []) {
    const container = document.getElementById('foodCategoryList');
    if (!container) {
        return;
    }

    container.innerHTML = foodCategories.map((category) => `
        <label>
            <input type="checkbox" name="foodCategory" value="${category.id}"
                ${selectedIds.includes(category.id) ? 'checked' : ''}>
            <span>${category.name}</span>
        </label>
    `).join('');

    container.querySelectorAll('input[type="checkbox"]').forEach((input) => {
        const label = input.closest('label');
        if (!label) {
            return;
        }

        if (input.checked) {
            label.classList.add('selected');
        }

        const checkboxHandler = () => {
            label.classList.toggle('selected', input.checked);
        };
        input.addEventListener('change', checkboxHandler);
        cleanupTasks.push(() => input.removeEventListener('change', checkboxHandler));
    });
}

function getSelectedCategoryIds() {
    return Array.from(document.querySelectorAll('input[name="foodCategory"]:checked'))
        .map((cb) => parseInt(cb.value));
}

async function loadRestaurant() {
    restaurantId = getRestaurantIdFromQuery();
    if (!restaurantId) {
        alert('음식점 ID가 없습니다.');
        AdminUtils?.navigateTo('/admin/pages/restaurants.html');
        return;
    }

    try {
        const result = await window.getRestaurant(restaurantId);
        const restaurant = result.data;

        const restaurantIdInput = document.getElementById('restaurantId');
        const nameInput = document.getElementById('name');
        const addressInput = document.getElementById('address');
        const detailAddressInput = document.getElementById('detailAddress');
        const descriptionInput = document.getElementById('description');
        const latitudeInput = document.getElementById('latitude');
        const longitudeInput = document.getElementById('longitude');

        if (restaurantIdInput) {
            restaurantIdInput.value = restaurant.id;
        }
        if (nameInput) {
            nameInput.value = restaurant.name;
        }
        if (addressInput) {
            addressInput.value = restaurant.address;
        }
        if (detailAddressInput) {
            detailAddressInput.value = restaurant.detailAddress || '';
        }
        if (descriptionInput) {
            descriptionInput.value = restaurant.description || '';
        }

        const selectedCategoryIds = (restaurant.foodCategories || []).map((fc) => fc.id);
        displayFoodCategories(selectedCategoryIds);

        const currentImages = document.getElementById('restaurantImageCurrent');
        const images = restaurant.images || [];
        if (currentImages) {
            currentImages.innerHTML = images.length > 0
                ? images.map((image) => `<img class="image-preview" src="${image.url}" alt="restaurant image">`).join('')
                : '<p class="help-text">등록된 이미지가 없습니다.</p>';
        }

        if (restaurant.latitude && restaurant.longitude) {
            if (latitudeInput) {
                latitudeInput.value = restaurant.latitude;
            }
            if (longitudeInput) {
                longitudeInput.value = restaurant.longitude;
            }
        } else if (restaurant.address) {
            await applyGeocodeResult(restaurant.address);
        }
    } catch (error) {
        alert(`음식점 정보를 불러오는데 실패했습니다: ${error.message}`);
    }
}

async function applyGeocodeResult(query) {
    const latitudeInput = document.getElementById('latitude');
    const longitudeInput = document.getElementById('longitude');
    if (!query || query.trim().length < 4) {
        if (latitudeInput) {
            latitudeInput.value = '';
        }
        if (longitudeInput) {
            longitudeInput.value = '';
        }
        return;
    }

    try {
        const result = await geocodeAddress(query.trim());
        if (latitudeInput) {
            latitudeInput.value = result.data?.latitude ?? '';
        }
        if (longitudeInput) {
            longitudeInput.value = result.data?.longitude ?? '';
        }
    } catch (error) {
        if (latitudeInput) {
            latitudeInput.value = '';
        }
        if (longitudeInput) {
            longitudeInput.value = '';
        }
    }
}

function renderImagePreviews(files, container) {
    if (!container) {
        return;
    }

    container.innerHTML = '';
    if (!files || files.length === 0) {
        return;
    }

    files.forEach((file) => {
        const img = document.createElement('img');
        img.className = 'image-preview';
        img.alt = file.name;
        img.src = URL.createObjectURL(file);
        container.appendChild(img);
    });
}

async function handleDeleteRestaurant() {
    if (!confirm('정말로 이 음식점을 삭제하시겠습니까?')) {
        return;
    }

    try {
        await deleteRestaurant(restaurantId);
        alert('음식점이 삭제되었습니다.');
        AdminUtils?.navigateTo('/admin/pages/restaurants.html');
    } catch (error) {
        alert(`음식점 삭제에 실패했습니다: ${error.message}`);
    }
}

function mountRestaurantEdit() {
    const cleanups = [];
    cleanup();

    awaitables();

    async function awaitables() {
        await loadFoodCategories();
        await loadRestaurant();
    }

    const addCategoryBtn = document.getElementById('addFoodCategoryBtn');
    const newCategoryInput = document.getElementById('newFoodCategoryName');
    if (addCategoryBtn && newCategoryInput) {
        const addCategory = async () => {
            const name = newCategoryInput.value.trim();
            if (!name) {
                alert('카테고리명을 입력해주세요.');
                return;
            }

            try {
                const selectedIds = getSelectedCategoryIds();
                await createFoodCategory({ name });
                newCategoryInput.value = '';
                await loadFoodCategories(selectedIds);
            } catch (error) {
                alert(`카테고리 추가에 실패했습니다: ${error.message}`);
            }
        };
        addCategoryBtn.addEventListener('click', addCategory);
        cleanups.push(() => addCategoryBtn.removeEventListener('click', addCategory));
    }

    const goMenuButton = document.getElementById('goRestaurantMenuBtn');
    if (goMenuButton) {
        const goMenu = () => {
            if (!restaurantId) {
                alert('음식점 ID가 없습니다.');
                return;
            }
            AdminUtils?.navigateTo(`/admin/pages/restaurant-menu.html?id=${restaurantId}`);
        };
        goMenuButton.addEventListener('click', goMenu);
        cleanups.push(() => goMenuButton.removeEventListener('click', goMenu));
    }

    const addressInput = document.getElementById('address');
    const latitudeInput = document.getElementById('latitude');
    const longitudeInput = document.getElementById('longitude');
    if (addressInput) {
        const triggerGeocode = () => {
            const query = addressInput.value.trim();
            clearTimeout(geocodeTimer);
            geocodeTimer = setTimeout(async () => {
                await applyGeocodeResult(query);
            }, 600);
        };
        addressInput.addEventListener('input', triggerGeocode);
        addressInput.addEventListener('blur', triggerGeocode);
        cleanups.push(() => {
            addressInput.removeEventListener('input', triggerGeocode);
            addressInput.removeEventListener('blur', triggerGeocode);
        });
    }

    const imageInput = document.getElementById('restaurantImages');
    const previewContainer = document.getElementById('restaurantImagePreview');
    const imageStatus = document.getElementById('imageStatus');
    const saveImagesBtn = document.getElementById('saveImagesBtn');
    const resetImageSelectionBtn = document.getElementById('resetImageSelection');
    let selectedFiles = [];

    const setImageStatus = (message, type = 'idle') => {
        if (!imageStatus) {
            return;
        }
        imageStatus.textContent = message;
        imageStatus.classList.toggle('image-status--saving', type === 'saving');
        imageStatus.classList.toggle('image-status--success', type === 'success');
        imageStatus.classList.toggle('image-status--error', type === 'error');
    };

    const resetSelection = () => {
        selectedFiles = [];
        if (imageInput) {
            imageInput.value = '';
        }
        renderImagePreviews(selectedFiles, previewContainer);
        setImageStatus('저장된 이미지', 'idle');
    };

    if (imageInput) {
        const imageChange = async () => {
            const files = Array.from(imageInput.files || []).slice(0, 5);
            if (imageInput.files.length > 5) {
                alert('이미지는 최대 5장까지 업로드할 수 있습니다.');
                resetSelection();
                return;
            }
            if (files.length === 0) {
                resetSelection();
                return;
            }

            setImageStatus('이미지 최적화 중...', 'saving');
            try {
                const optimized = await window.ImageOptimizer.optimizeImages(files, 'restaurant');
                selectedFiles = optimized.optimized;
                if (optimized.errors && optimized.errors.length > 0) {
                    alert(`일부 이미지 최적화 실패:\\n${optimized.errors.map((error) => `- ${error.file}: ${error.error}`).join('\n')}`);
                }
                renderImagePreviews(selectedFiles, previewContainer);
                setImageStatus('새 이미지 선택됨', 'success');
            } catch (error) {
                alert(`이미지 최적화 중 오류가 발생했습니다: ${error.message}`);
                selectedFiles = [];
                renderImagePreviews(selectedFiles, previewContainer);
                setImageStatus('오류 발생', 'error');
            }
        };
        imageInput.addEventListener('change', imageChange);
        cleanups.push(() => imageInput.removeEventListener('change', imageChange));
    }

    if (resetImageSelectionBtn) {
        const resetHandler = () => resetSelection();
        resetImageSelectionBtn.addEventListener('click', resetHandler);
        cleanups.push(() => resetImageSelectionBtn.removeEventListener('click', resetHandler));
    }

    if (saveImagesBtn) {
        const saveImages = async () => {
            if (isImageSaving) {
                return;
            }
            if (selectedFiles.length === 0) {
                alert('저장할 이미지를 선택해주세요.');
                return;
            }

            isImageSaving = true;
            saveImagesBtn.disabled = true;
            setImageStatus('이미지 저장 중...', 'saving');
            try {
                const presigned = await window.createPresignedUploads('RESTAURANT_IMAGE', selectedFiles);
                const uploads = presigned.data?.uploads || [];
                await Promise.all(
                    uploads.map((item, index) => window.uploadToPresigned(item, selectedFiles[index]))
                );
                const imageIds = uploads.map((item) => item.fileUuid);
                await window.updateRestaurant(restaurantId, { imageIds });
                await loadRestaurant();
                setImageStatus('이미지 저장 완료', 'success');
            } catch (error) {
                setImageStatus('이미지 저장 실패', 'error');
                alert(`이미지 저장에 실패했습니다: ${error.message}`);
            } finally {
                isImageSaving = false;
                saveImagesBtn.disabled = false;
            }
        };
        saveImagesBtn.addEventListener('click', saveImages);
        cleanups.push(() => saveImagesBtn.removeEventListener('click', saveImages));
    }

    const deleteBtn = document.getElementById('deleteRestaurantBtn');
    if (deleteBtn) {
        const clickDelete = () => handleDeleteRestaurant();
        deleteBtn.addEventListener('click', clickDelete);
        cleanups.push(() => deleteBtn.removeEventListener('click', clickDelete));
    }

    const form = document.getElementById('restaurantEditForm');
    if (form) {
        const submitHandler = async (event) => {
            event.preventDefault();

            const selectedCategories = getSelectedCategoryIds();
            const data = {
                name: document.getElementById('name')?.value,
                address: document.getElementById('address')?.value,
                detailAddress: document.getElementById('detailAddress')?.value || null,
                description: document.getElementById('description')?.value || null,
                foodCategoryIds: selectedCategories
            };

            if (selectedFiles.length > 0) {
                alert('이미지는 "이미지 저장" 버튼으로 먼저 저장해주세요.');
                return;
            }

            try {
                await window.updateRestaurant(restaurantId, data);
                alert('음식점이 수정되었습니다.');
                AdminUtils?.navigateTo('/admin/pages/restaurants.html');
            } catch (error) {
                alert(`음식점 수정에 실패했습니다: ${error.message}`);
            }
        };
        form.addEventListener('submit', submitHandler);
        cleanups.push(() => form.removeEventListener('submit', submitHandler));
    }

    cleanupTasks = cleanups;
}

function mount(state) {
    mountRestaurantEdit();
}

function unmountRestaurantEdit() {
    cleanup();
}

window.restaurantEditView = {
    render: renderRestaurantEdit,
    mount,
    unmount: unmountRestaurantEdit
};
