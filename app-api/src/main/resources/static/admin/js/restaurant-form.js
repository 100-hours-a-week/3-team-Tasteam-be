checkAuth();

let foodCategories = [];
let geocodeTimer = null;

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

function applyScheduleToDays(days, config) {
    days.forEach(day => {
        const closedCheckbox = document.querySelector(`.closed-checkbox[data-day="${day}"]`);
        const openTime = document.querySelector(`.open-time[data-day="${day}"]`);
        const closeTime = document.querySelector(`.close-time[data-day="${day}"]`);

        closedCheckbox.checked = config.isClosed;
        openTime.disabled = config.isClosed;
        closeTime.disabled = config.isClosed;
        openTime.value = config.isClosed ? '' : (config.openTime || '');
        closeTime.value = config.isClosed ? '' : (config.closeTime || '');
    });
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

document.addEventListener('DOMContentLoaded', () => {
    loadFoodCategories();

    const addressInput = document.getElementById('address');
    const latitudeInput = document.getElementById('latitude');
    const longitudeInput = document.getElementById('longitude');

    const triggerGeocode = () => {
        const query = addressInput.value.trim();
        if (query.length < 4) {
            latitudeInput.value = '';
            longitudeInput.value = '';
            return;
        }

        clearTimeout(geocodeTimer);
        geocodeTimer = setTimeout(async () => {
            try {
                const result = await geocodeAddress(query);
                latitudeInput.value = result.data?.latitude ?? '';
                longitudeInput.value = result.data?.longitude ?? '';
            } catch (error) {
                latitudeInput.value = '';
                longitudeInput.value = '';
            }
        }, 600);
    };

    addressInput.addEventListener('input', triggerGeocode);
    addressInput.addEventListener('blur', triggerGeocode);

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

    const weekdayClosed = document.getElementById('weekdayClosed');
    const weekdayOpenTime = document.getElementById('weekdayOpenTime');
    const weekdayCloseTime = document.getElementById('weekdayCloseTime');
    const allDaysClosed = document.getElementById('allDaysClosed');
    const allDaysOpenTime = document.getElementById('allDaysOpenTime');
    const allDaysCloseTime = document.getElementById('allDaysCloseTime');

    weekdayClosed.addEventListener('change', () => {
        const disabled = weekdayClosed.checked;
        weekdayOpenTime.disabled = disabled;
        weekdayCloseTime.disabled = disabled;
        if (disabled) {
            weekdayOpenTime.value = '';
            weekdayCloseTime.value = '';
        }
    });

    allDaysClosed.addEventListener('change', () => {
        const disabled = allDaysClosed.checked;
        allDaysOpenTime.disabled = disabled;
        allDaysCloseTime.disabled = disabled;
        if (disabled) {
            allDaysOpenTime.value = '';
            allDaysCloseTime.value = '';
        }
    });

    document.getElementById('applyWeekdaySchedule').addEventListener('click', () => {
        applyScheduleToDays(
            ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'],
            {
                isClosed: weekdayClosed.checked,
                openTime: weekdayOpenTime.value,
                closeTime: weekdayCloseTime.value
            }
        );
    });

    document.getElementById('applyAllSchedule').addEventListener('click', () => {
        applyScheduleToDays(
            ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'],
            {
                isClosed: allDaysClosed.checked,
                openTime: allDaysOpenTime.value,
                closeTime: allDaysCloseTime.value
            }
        );
    });

    const imageInput = document.getElementById('restaurantImages');
    const previewContainer = document.getElementById('restaurantImagePreview');
    let selectedFiles = [];

    imageInput.addEventListener('change', () => {
        selectedFiles = Array.from(imageInput.files || []).slice(0, 5);
        if (imageInput.files.length > 5) {
            alert('이미지는 최대 5장까지 업로드할 수 있습니다.');
            imageInput.value = '';
            selectedFiles = [];
        }
        renderImagePreviews(selectedFiles, previewContainer);
    });

    document.getElementById('restaurantForm').addEventListener('submit', async (e) => {
        e.preventDefault();

        const selectedCategories = Array.from(document.querySelectorAll('input[name="foodCategory"]:checked'))
            .map(cb => parseInt(cb.value));

        let imageIds = [];
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
            foodCategoryIds: selectedCategories,
            imageIds: imageIds,
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
