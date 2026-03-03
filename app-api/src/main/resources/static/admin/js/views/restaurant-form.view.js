let cleanup = [];
let foodCategories = [];
let geocodeTimer = null;

function render(container) {
    if (!container) {
        return;
    }

    container.innerHTML = `
        <div class="content-header">
            <h1>음식점 등록</h1>
            <a href="/admin/pages/restaurants.html" class="btn btn-secondary">목록으로</a>
        </div>

        <form id="restaurantForm" class="form-container">
            <div class="form-section">
                <h3>기본 정보</h3>
                <div class="form-group">
                    <label for="name">음식점명 *</label>
                    <input type="text" id="name" name="name" required maxlength="100">
                </div>
                <div class="form-group">
                    <label for="address">주소 *</label>
                    <input type="text" id="address" name="address" required maxlength="255">
                    <p class="help-text">주소 입력 시 좌표는 서버에서 자동으로 계산됩니다.</p>
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
                <h3>영업시간</h3>
                <div class="schedule-bulk">
                    <div class="schedule-bulk-row">
                        <span>평일 일괄</span>
                        <label class="inline-check">
                            <input type="checkbox" id="weekdayClosed">
                            휴무
                        </label>
                        <input type="time" id="weekdayOpenTime">
                        <span>~</span>
                        <input type="time" id="weekdayCloseTime">
                        <button type="button" class="btn btn-secondary btn-sm" id="applyWeekdaySchedule">적용</button>
                    </div>
                    <div class="schedule-bulk-row">
                        <span>전체 일괄</span>
                        <label class="inline-check">
                            <input type="checkbox" id="allDaysClosed">
                            휴무
                        </label>
                        <input type="time" id="allDaysOpenTime">
                        <span>~</span>
                        <input type="time" id="allDaysCloseTime">
                        <button type="button" class="btn btn-secondary btn-sm" id="applyAllSchedule">적용</button>
                    </div>
                </div>
                <div id="weeklySchedules">
                    <div class="schedule-item" data-day="MONDAY">
                        <label>월요일</label>
                        <input type="checkbox" class="closed-checkbox" data-day="MONDAY">
                        <span>휴무</span>
                        <input type="time" class="open-time" data-day="MONDAY">
                        <span>~</span>
                        <input type="time" class="close-time" data-day="MONDAY">
                    </div>
                    <div class="schedule-item" data-day="TUESDAY">
                        <label>화요일</label>
                        <input type="checkbox" class="closed-checkbox" data-day="TUESDAY">
                        <span>휴무</span>
                        <input type="time" class="open-time" data-day="TUESDAY">
                        <span>~</span>
                        <input type="time" class="close-time" data-day="TUESDAY">
                    </div>
                    <div class="schedule-item" data-day="WEDNESDAY">
                        <label>수요일</label>
                        <input type="checkbox" class="closed-checkbox" data-day="WEDNESDAY">
                        <span>휴무</span>
                        <input type="time" class="open-time" data-day="WEDNESDAY">
                        <span>~</span>
                        <input type="time" class="close-time" data-day="WEDNESDAY">
                    </div>
                    <div class="schedule-item" data-day="THURSDAY">
                        <label>목요일</label>
                        <input type="checkbox" class="closed-checkbox" data-day="THURSDAY">
                        <span>휴무</span>
                        <input type="time" class="open-time" data-day="THURSDAY">
                        <span>~</span>
                        <input type="time" class="close-time" data-day="THURSDAY">
                    </div>
                    <div class="schedule-item" data-day="FRIDAY">
                        <label>금요일</label>
                        <input type="checkbox" class="closed-checkbox" data-day="FRIDAY">
                        <span>휴무</span>
                        <input type="time" class="open-time" data-day="FRIDAY">
                        <span>~</span>
                        <input type="time" class="close-time" data-day="FRIDAY">
                    </div>
                    <div class="schedule-item" data-day="SATURDAY">
                        <label>토요일</label>
                        <input type="checkbox" class="closed-checkbox" data-day="SATURDAY">
                        <span>휴무</span>
                        <input type="time" class="open-time" data-day="SATURDAY">
                        <span>~</span>
                        <input type="time" class="close-time" data-day="SATURDAY">
                    </div>
                    <div class="schedule-item" data-day="SUNDAY">
                        <label>일요일</label>
                        <input type="checkbox" class="closed-checkbox" data-day="SUNDAY">
                        <span>휴무</span>
                        <input type="time" class="open-time" data-day="SUNDAY">
                        <span>~</span>
                        <input type="time" class="close-time" data-day="SUNDAY">
                    </div>
                </div>
            </div>

            <div class="form-section">
                <h3>이미지</h3>
                <div class="form-group">
                    <label for="restaurantImages">이미지 업로드</label>
                    <input type="file" id="restaurantImages" accept="image/*" multiple>
                    <p class="help-text">이미지는 최대 5장까지 업로드할 수 있습니다.</p>
                </div>
                <div id="restaurantImagePreview" class="image-preview-grid"></div>
            </div>

            <div class="form-actions">
                <button type="submit" class="btn btn-primary">등록</button>
                <a href="/admin/pages/restaurants.html" class="btn btn-secondary">취소</a>
            </div>
        </form>
    `;
}

async function loadFoodCategories(selectedIds = []) {
    try {
        const result = await window.getFoodCategories();
        foodCategories = result.data || [];
        displayFoodCategories(selectedIds);
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
            <input type="checkbox" name="foodCategory" value="${category.id}">
            <span>${category.name}</span>
        </label>
    `).join('');

    container.querySelectorAll('input[type="checkbox"]').forEach((input) => {
        const label = input.closest('label');
        if (selectedIds.includes(parseInt(input.value, 10))) {
            input.checked = true;
        }
        if (input.checked) {
            label.classList.add('selected');
        }
        const checkboxChange = () => {
            label.classList.toggle('selected', input.checked);
        };
        input.addEventListener('change', checkboxChange);
        cleanup.push(() => input.removeEventListener('change', checkboxChange));
    });
}

function getSelectedCategoryIds() {
    return Array.from(document.querySelectorAll('input[name="foodCategory"]:checked'))
        .map((cb) => parseInt(cb.value));
}

function getScheduleData() {
    const schedules = [];
    const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

    days.forEach((day) => {
        const isClosed = document.querySelector(`.closed-checkbox[data-day="${day}"]`)?.checked;
        const openTime = document.querySelector(`.open-time[data-day="${day}"]`)?.value;
        const closeTime = document.querySelector(`.close-time[data-day="${day}"]`)?.value;

        if (isClosed || (openTime && closeTime)) {
            schedules.push({
                dayOfWeek: day,
                openTime: isClosed ? null : openTime,
                closeTime: isClosed ? null : closeTime,
                isClosed,
                effectiveFrom: null,
                effectiveTo: null
            });
        }
    });

    return schedules;
}

function applyScheduleToDays(days, config) {
    days.forEach((day) => {
        const closedCheckbox = document.querySelector(`.closed-checkbox[data-day="${day}"]`);
        const openTime = document.querySelector(`.open-time[data-day="${day}"]`);
        const closeTime = document.querySelector(`.close-time[data-day="${day}"]`);

        if (!closedCheckbox || !openTime || !closeTime) {
            return;
        }

        closedCheckbox.checked = config.isClosed;
        openTime.disabled = config.isClosed;
        closeTime.disabled = config.isClosed;
        openTime.value = config.isClosed ? '' : (config.openTime || '');
        closeTime.value = config.isClosed ? '' : (config.closeTime || '');
    });
}

function renderImagePreviews(files, previewContainer) {
    previewContainer.innerHTML = '';
    if (!files || files.length === 0) {
        return;
    }

    files.forEach((file) => {
        const img = document.createElement('img');
        img.className = 'image-preview';
        img.alt = file.name;
        img.src = URL.createObjectURL(file);
        previewContainer.appendChild(img);
    });
}

async function mount() {
    const cleanups = [];

    await loadFoodCategories();

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
                const result = await window.geocodeAddress(query);
                latitudeInput.value = result.data?.latitude ?? '';
                longitudeInput.value = result.data?.longitude ?? '';
            } catch (error) {
                latitudeInput.value = '';
                longitudeInput.value = '';
            }
        }, 600);
    };

    if (addressInput) {
        addressInput.addEventListener('input', triggerGeocode);
        addressInput.addEventListener('blur', triggerGeocode);
        cleanups.push(() => {
            addressInput.removeEventListener('input', triggerGeocode);
            addressInput.removeEventListener('blur', triggerGeocode);
        });
    }

    const checkboxClosedHandler = (event) => {
        const target = event?.target;
        const day = target?.dataset?.day;
        if (!day) {
            return;
        }
        const openTime = document.querySelector(`.open-time[data-day="${day}"]`);
        const closeTime = document.querySelector(`.close-time[data-day="${day}"]`);
        if (!openTime || !closeTime) {
            return;
        }

        openTime.disabled = target.checked;
        closeTime.disabled = target.checked;
        if (target.checked) {
            openTime.value = '';
            closeTime.value = '';
        }
    };

    document.querySelectorAll('.closed-checkbox').forEach((checkbox) => {
        checkbox.addEventListener('change', checkboxClosedHandler);
        cleanups.push(() => checkbox.removeEventListener('change', checkboxClosedHandler));
    });

    const weekdayClosed = document.getElementById('weekdayClosed');
    const weekdayOpenTime = document.getElementById('weekdayOpenTime');
    const weekdayCloseTime = document.getElementById('weekdayCloseTime');
    const allDaysClosed = document.getElementById('allDaysClosed');
    const allDaysOpenTime = document.getElementById('allDaysOpenTime');
    const allDaysCloseTime = document.getElementById('allDaysCloseTime');

    if (weekdayClosed && weekdayOpenTime && weekdayCloseTime) {
        const weekdayReset = () => {
            const disabled = weekdayClosed.checked;
            weekdayOpenTime.disabled = disabled;
            weekdayCloseTime.disabled = disabled;
            if (disabled) {
                weekdayOpenTime.value = '';
                weekdayCloseTime.value = '';
            }
        };
        weekdayClosed.addEventListener('change', weekdayReset);
        cleanups.push(() => weekdayClosed.removeEventListener('change', weekdayReset));
    }

    if (allDaysClosed && allDaysOpenTime && allDaysCloseTime) {
        const allDaysReset = () => {
            const disabled = allDaysClosed.checked;
            allDaysOpenTime.disabled = disabled;
            allDaysCloseTime.disabled = disabled;
            if (disabled) {
                allDaysOpenTime.value = '';
                allDaysCloseTime.value = '';
            }
        };
        allDaysClosed.addEventListener('change', allDaysReset);
        cleanups.push(() => allDaysClosed.removeEventListener('change', allDaysReset));
    }

    if (document.getElementById('applyWeekdaySchedule') && weekdayClosed && weekdayOpenTime && weekdayCloseTime) {
        const applyWeekday = () => {
            applyScheduleToDays(['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'], {
                isClosed: weekdayClosed.checked,
                openTime: weekdayOpenTime.value,
                closeTime: weekdayCloseTime.value
            });
        };
        document.getElementById('applyWeekdaySchedule').addEventListener('click', applyWeekday);
        cleanups.push(() => document.getElementById('applyWeekdaySchedule')?.removeEventListener('click', applyWeekday));
    }

    if (document.getElementById('applyAllSchedule') && allDaysClosed && allDaysOpenTime && allDaysCloseTime) {
        const applyAll = () => {
            applyScheduleToDays(['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'], {
                isClosed: allDaysClosed.checked,
                openTime: allDaysOpenTime.value,
                closeTime: allDaysCloseTime.value
            });
        };
        document.getElementById('applyAllSchedule').addEventListener('click', applyAll);
        cleanups.push(() => document.getElementById('applyAllSchedule')?.removeEventListener('click', applyAll));
    }

    const imageInput = document.getElementById('restaurantImages');
    const previewContainer = document.getElementById('restaurantImagePreview');
    let selectedFiles = [];

    if (imageInput) {
        const imageChange = async () => {
            const files = Array.from(imageInput.files || []).slice(0, 5);
            if (imageInput.files.length > 5) {
                alert('이미지는 최대 5장까지 업로드할 수 있습니다.');
                imageInput.value = '';
                selectedFiles = [];
                renderImagePreviews(selectedFiles, previewContainer);
                return;
            }

            if (files.length === 0) {
                selectedFiles = [];
                renderImagePreviews(selectedFiles, previewContainer);
                return;
            }

            try {
                const { optimized, errors } = await window.ImageOptimizer.optimizeImages(files, 'restaurant');

                if (errors.length > 0) {
                    alert(`일부 이미지 최적화 실패:\n${errors.map((err) => `- ${err.file}: ${err.error}`).join('\n')}`);
                }

                selectedFiles = optimized;
                renderImagePreviews(selectedFiles, previewContainer);
            } catch (error) {
                alert(`이미지 최적화 중 오류가 발생했습니다: ${error.message}`);
                selectedFiles = [];
                renderImagePreviews(selectedFiles, previewContainer);
            }
        };

        imageInput.addEventListener('change', imageChange);
        cleanups.push(() => imageInput.removeEventListener('change', imageChange));
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
                await window.createFoodCategory({ name });
                newCategoryInput.value = '';
                await loadFoodCategories(selectedIds);
            } catch (error) {
                alert(`카테고리 추가에 실패했습니다: ${error.message}`);
            }
        };

        addCategoryBtn.addEventListener('click', addCategory);
        cleanups.push(() => addCategoryBtn.removeEventListener('click', addCategory));
    }

    const form = document.getElementById('restaurantForm');
    if (form) {
        const submitHandler = async (event) => {
            event.preventDefault();

            const selectedCategories = getSelectedCategoryIds();
            let imageIds = [];
            if (selectedFiles.length > 0) {
                try {
                    const presigned = await window.createPresignedUploads('RESTAURANT_IMAGE', selectedFiles);
                    const uploads = presigned.data?.uploads || [];
                    await Promise.all(uploads.map((item, index) =>
                        window.uploadToPresigned(item, selectedFiles[index])
                    ));
                    imageIds = uploads.map((item) => item.fileUuid);
                } catch (error) {
                    alert(`이미지 업로드에 실패했습니다: ${error.message}`);
                    return;
                }
            }

            const data = {
                name: document.getElementById('name')?.value,
                address: document.getElementById('address')?.value,
                detailAddress: document.getElementById('detailAddress')?.value || null,
                description: document.getElementById('description')?.value || null,
                foodCategoryIds: selectedCategories,
                imageIds,
                weeklySchedules: getScheduleData()
            };

            try {
                await window.createRestaurant(data);
                alert('음식점이 등록되었습니다.');
                window.AdminUtils?.navigateTo('/admin/pages/restaurants.html');
            } catch (error) {
                alert(`음식점 등록에 실패했습니다: ${error.message}`);
            }
        };

        form.addEventListener('submit', submitHandler);
        cleanups.push(() => form.removeEventListener('submit', submitHandler));
    }

    cleanup = cleanups;
}

function unmount() {
    cleanup.forEach((remove) => remove());
    cleanup = [];
    if (geocodeTimer) {
        clearTimeout(geocodeTimer);
        geocodeTimer = null;
    }
}

window.restaurantFormView = {
    render,
    mount,
    unmount
};
