checkAuth();

let foodCategories = [];
let restaurantId = null;
let geocodeTimer = null;
let isImageSaving = false;

async function applyGeocodeResult(query) {
    const latitudeInput = document.getElementById('latitude');
    const longitudeInput = document.getElementById('longitude');
    if (!query || query.trim().length < 4) {
        latitudeInput.value = '';
        longitudeInput.value = '';
        return;
    }
    try {
        const result = await geocodeAddress(query.trim());
        latitudeInput.value = result.data?.latitude ?? '';
        longitudeInput.value = result.data?.longitude ?? '';
    } catch (error) {
        latitudeInput.value = '';
        longitudeInput.value = '';
    }
}

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
            <span>${category.name}</span>
        </label>
    `).join('');

    container.querySelectorAll('input[type="checkbox"]').forEach(input => {
        const label = input.closest('label');
        if (input.checked) {
            label.classList.add('selected');
        }
        input.addEventListener('change', () => {
            label.classList.toggle('selected', input.checked);
        });
    });
}

function getSelectedCategoryIds() {
    return Array.from(document.querySelectorAll('input[name="foodCategory"]:checked'))
        .map(cb => parseInt(cb.value));
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

        const currentImages = document.getElementById('restaurantImageCurrent');
        const images = restaurant.images || [];
        currentImages.innerHTML = images.length > 0
            ? images.map(image => `<img class="image-preview" src="${image.url}" alt="restaurant image">`).join('')
            : '<p class="help-text">등록된 이미지가 없습니다.</p>';

        if (restaurant.latitude && restaurant.longitude) {
            document.getElementById('latitude').value = restaurant.latitude;
            document.getElementById('longitude').value = restaurant.longitude;
        } else if (restaurant.address) {
            await applyGeocodeResult(restaurant.address);
        }
    } catch (error) {
        alert('음식점 정보를 불러오는데 실패했습니다: ' + error.message);
    }
}

async function handleDeleteRestaurant() {
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

function renderImagePreviews(files, container) {
    container.innerHTML = '';
    if (!files || files.length === 0) {
        return;
    }
    files.forEach(file => {
        const img = document.createElement('img');
        img.className = 'image-preview';
        img.alt = file.name;
        img.src = URL.createObjectURL(file);
        container.appendChild(img);
    });
}

document.addEventListener('DOMContentLoaded', async () => {
    await loadFoodCategories();
    await loadRestaurant();

    const addCategoryBtn = document.getElementById('addFoodCategoryBtn');
    const newCategoryInput = document.getElementById('newFoodCategoryName');
    addCategoryBtn.addEventListener('click', async () => {
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
            alert('카테고리 추가에 실패했습니다: ' + error.message);
        }
    });

    const addressInput = document.getElementById('address');
    const latitudeInput = document.getElementById('latitude');
    const longitudeInput = document.getElementById('longitude');

    const triggerGeocode = (overrideValue) => {
        const query = (overrideValue ?? addressInput.value).trim();
        clearTimeout(geocodeTimer);
        geocodeTimer = setTimeout(async () => {
            await applyGeocodeResult(query);
        }, 600);
    };

    addressInput.addEventListener('input', () => triggerGeocode());
    addressInput.addEventListener('blur', () => triggerGeocode());

    const imageInput = document.getElementById('restaurantImages');
    const previewContainer = document.getElementById('restaurantImagePreview');
    const imageStatus = document.getElementById('imageStatus');
    const saveImagesBtn = document.getElementById('saveImagesBtn');
    const resetImageSelectionBtn = document.getElementById('resetImageSelection');
    let selectedFiles = [];

    const setImageStatus = (message, type = 'idle') => {
        imageStatus.textContent = message;
        imageStatus.classList.toggle('image-status--saving', type === 'saving');
        imageStatus.classList.toggle('image-status--success', type === 'success');
        imageStatus.classList.toggle('image-status--error', type === 'error');
    };

    const resetSelection = () => {
        selectedFiles = [];
        imageInput.value = '';
        previewContainer.innerHTML = '';
        setImageStatus('저장된 이미지', 'idle');
    };

    imageInput.addEventListener('change', () => {
        selectedFiles = Array.from(imageInput.files || []).slice(0, 5);
        if (imageInput.files.length > 5) {
            alert('이미지는 최대 5장까지 업로드할 수 있습니다.');
            imageInput.value = '';
            selectedFiles = [];
        }
        renderImagePreviews(selectedFiles, previewContainer);
        if (selectedFiles.length > 0) {
            setImageStatus(`새 이미지 ${selectedFiles.length}장 선택됨`, 'idle');
        } else {
            setImageStatus('저장된 이미지', 'idle');
        }
    });

    resetImageSelectionBtn.addEventListener('click', () => {
        resetSelection();
    });

    saveImagesBtn.addEventListener('click', async () => {
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
            const presigned = await createPresignedUploads('RESTAURANT_IMAGE', selectedFiles);
            const uploads = presigned.data?.uploads || [];
            await Promise.all(uploads.map((item, index) => uploadToPresigned(item, selectedFiles[index])));
            const imageIds = uploads.map(item => item.fileUuid);
            await updateRestaurant(restaurantId, { imageIds });
            await loadRestaurant();
            resetSelection();
            setImageStatus('이미지 저장 완료', 'success');
        } catch (error) {
            setImageStatus('이미지 저장 실패', 'error');
            alert('이미지 저장에 실패했습니다: ' + error.message);
        } finally {
            isImageSaving = false;
            saveImagesBtn.disabled = false;
        }
    });

    document.getElementById('restaurantEditForm').addEventListener('submit', async (e) => {
        e.preventDefault();

        const selectedCategories = getSelectedCategoryIds();

        let imageIds = null;
        if (selectedFiles.length > 0) {
            try {
                const presigned = await createPresignedUploads('RESTAURANT_IMAGE', selectedFiles);
                const uploads = presigned.data?.uploads || [];
                await Promise.all(uploads.map((item, index) => uploadToPresigned(item, selectedFiles[index])));
                imageIds = uploads.map(item => item.fileUuid);
            } catch (error) {
                alert('이미지 업로드에 실패했습니다: ' + error.message);
                return;
            }
        }

        const data = {
            name: document.getElementById('name').value,
            address: document.getElementById('address').value,
            detailAddress: document.getElementById('detailAddress').value || null,
            description: document.getElementById('description').value || null,
            foodCategoryIds: selectedCategories
        };

        if (imageIds) {
            data.imageIds = imageIds;
        }

        try {
            await updateRestaurant(restaurantId, data);
            alert('음식점이 수정되었습니다.');
            location.href = '/admin/pages/restaurants.html';
        } catch (error) {
            alert('음식점 수정에 실패했습니다: ' + error.message);
        }
    });
});
