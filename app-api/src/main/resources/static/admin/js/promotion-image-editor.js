const PROMOTION_IMAGE_EDITOR_PRESETS = {
	banner: {
		label: '배너 이미지',
		aspectRatio: 3.55,
		aspectRatioLabel: '약 3.55:1',
		maxWidth: 1600,
		maxHeight: 500,
		quality: 0.9
	},
	splash: {
		label: '스플래시 이미지',
		aspectRatio: 4 / 3,
		aspectRatioLabel: '4:3',
		maxWidth: 1200,
		maxHeight: 900,
		quality: 0.9
	},
	detail: {
		label: '상세 이미지',
		aspectRatio: null,
		maxWidth: 1600,
		maxHeight: 1600,
		quality: 0.92
	}
};

const MIN_CROP_SCALE = 0.2;

let editorElements = null;
let editorState = null;
let editorResolve = null;
let editorKeydownHandlerBound = false;

function ensureEditorElements() {
	if (editorElements) {
		return editorElements;
	}

	const wrapper = document.createElement('div');
	wrapper.id = 'promotion-image-editor';
	wrapper.className = 'promotion-image-editor';
	wrapper.innerHTML = `
		<div class="promotion-image-editor__backdrop" data-role="backdrop"></div>
		<div class="promotion-image-editor__dialog" role="dialog" aria-modal="true">
			<div class="promotion-image-editor__header">
				<h3 id="promotion-image-editor-title">이미지 편집</h3>
				<p id="promotion-image-editor-subtitle" class="promotion-image-editor__subtitle"></p>
			</div>
			<div class="promotion-image-editor__body">
				<div class="promotion-image-editor__preview-wrap">
					<canvas id="promotion-image-editor-canvas" class="promotion-image-editor__canvas"></canvas>
				</div>
				<div class="promotion-image-editor__controls">
					<label class="promotion-image-editor__control">
						<span>확대</span>
						<input id="promotion-image-editor-zoom" type="range" min="1" max="3" step="0.01" value="1">
					</label>
					<label class="promotion-image-editor__control">
						<span>가로 위치</span>
						<input id="promotion-image-editor-center-x" type="range" min="0" max="1" step="0.01" value="0.5">
					</label>
					<label class="promotion-image-editor__control">
						<span>세로 위치</span>
						<input id="promotion-image-editor-center-y" type="range" min="0" max="1" step="0.01" value="0.5">
					</label>
					<div id="promotion-image-editor-free-controls" class="promotion-image-editor__free-controls">
						<label class="promotion-image-editor__control">
							<span>자르기 너비</span>
							<input id="promotion-image-editor-crop-width" type="range" min="${MIN_CROP_SCALE}" max="1" step="0.01" value="1">
						</label>
						<label class="promotion-image-editor__control">
							<span>자르기 높이</span>
							<input id="promotion-image-editor-crop-height" type="range" min="${MIN_CROP_SCALE}" max="1" step="0.01" value="1">
						</label>
					</div>
				</div>
			</div>
			<div class="promotion-image-editor__footer">
				<button type="button" class="btn btn-secondary" id="promotion-image-editor-cancel">취소</button>
				<button type="button" class="btn btn-primary" id="promotion-image-editor-apply">적용</button>
			</div>
		</div>
	`;
	document.body.appendChild(wrapper);

	editorElements = {
		wrapper,
		backdrop: wrapper.querySelector('[data-role="backdrop"]'),
		title: wrapper.querySelector('#promotion-image-editor-title'),
		subtitle: wrapper.querySelector('#promotion-image-editor-subtitle'),
		canvas: wrapper.querySelector('#promotion-image-editor-canvas'),
		zoom: wrapper.querySelector('#promotion-image-editor-zoom'),
		centerX: wrapper.querySelector('#promotion-image-editor-center-x'),
		centerY: wrapper.querySelector('#promotion-image-editor-center-y'),
		freeControls: wrapper.querySelector('#promotion-image-editor-free-controls'),
		cropWidth: wrapper.querySelector('#promotion-image-editor-crop-width'),
		cropHeight: wrapper.querySelector('#promotion-image-editor-crop-height'),
		cancel: wrapper.querySelector('#promotion-image-editor-cancel'),
		apply: wrapper.querySelector('#promotion-image-editor-apply')
	};

	bindEditorEvents();
	return editorElements;
}

function bindEditorEvents() {
	if (!editorElements) {
		return;
	}

	const rerender = () => {
		if (!editorState) {
			return;
		}
		editorState.zoom = Number(editorElements.zoom.value);
		editorState.centerX = Number(editorElements.centerX.value);
		editorState.centerY = Number(editorElements.centerY.value);
		editorState.cropWidthScale = Number(editorElements.cropWidth.value);
		editorState.cropHeightScale = Number(editorElements.cropHeight.value);
		renderEditorPreview();
	};

	editorElements.zoom.addEventListener('input', rerender);
	editorElements.centerX.addEventListener('input', rerender);
	editorElements.centerY.addEventListener('input', rerender);
	editorElements.cropWidth.addEventListener('input', rerender);
	editorElements.cropHeight.addEventListener('input', rerender);

	editorElements.backdrop.addEventListener('click', () => {
		closeEditor(null);
	});

	editorElements.cancel.addEventListener('click', () => {
		closeEditor(null);
	});

	editorElements.apply.addEventListener('click', async () => {
		if (!editorState) {
			closeEditor(null);
			return;
		}

		try {
			const editedFile = await exportEditedImage();
			closeEditor(editedFile);
		} catch (error) {
			alert(`이미지 편집에 실패했습니다: ${error.message || '알 수 없는 오류'}`);
		}
	});

	if (!editorKeydownHandlerBound) {
		document.addEventListener('keydown', (event) => {
			if (event.key === 'Escape' && editorState) {
				closeEditor(null);
			}
		});
		editorKeydownHandlerBound = true;
	}
}

function openEditorModal() {
	if (!editorElements) {
		return;
	}
	editorElements.wrapper.classList.add('is-open');
}

function hideEditorModal() {
	if (!editorElements) {
		return;
	}
	editorElements.wrapper.classList.remove('is-open');
}

function closeEditor(resultFile) {
	hideEditorModal();

	if (editorResolve) {
		const resolve = editorResolve;
		editorResolve = null;
		resolve(resultFile);
	}

	editorState = null;
}

async function loadImageFromFile(file) {
	return new Promise((resolve, reject) => {
		const objectUrl = URL.createObjectURL(file);
		const image = new Image();

		image.onload = () => {
			URL.revokeObjectURL(objectUrl);
			resolve(image);
		};

		image.onerror = () => {
			URL.revokeObjectURL(objectUrl);
			reject(new Error('이미지를 불러오지 못했습니다.'));
		};

		image.src = objectUrl;
	});
}

function getPreset(type) {
	return PROMOTION_IMAGE_EDITOR_PRESETS[type] || PROMOTION_IMAGE_EDITOR_PRESETS.detail;
}

function calculateCropRect() {
	const preset = getPreset(editorState.type);
	const { image, zoom, centerX, centerY, cropWidthScale, cropHeightScale } = editorState;

	let cropWidth = image.naturalWidth;
	let cropHeight = image.naturalHeight;

	if (preset.aspectRatio) {
		const imageRatio = image.naturalWidth / image.naturalHeight;
		if (imageRatio >= preset.aspectRatio) {
			cropHeight = image.naturalHeight;
			cropWidth = cropHeight * preset.aspectRatio;
		} else {
			cropWidth = image.naturalWidth;
			cropHeight = cropWidth / preset.aspectRatio;
		}
	} else {
		cropWidth = image.naturalWidth * Math.max(MIN_CROP_SCALE, Math.min(1, cropWidthScale));
		cropHeight = image.naturalHeight * Math.max(MIN_CROP_SCALE, Math.min(1, cropHeightScale));
	}

	cropWidth = Math.max(1, cropWidth / zoom);
	cropHeight = Math.max(1, cropHeight / zoom);

	const maxX = Math.max(0, image.naturalWidth - cropWidth);
	const maxY = Math.max(0, image.naturalHeight - cropHeight);

	const x = maxX * Math.max(0, Math.min(1, centerX));
	const y = maxY * Math.max(0, Math.min(1, centerY));

	return {
		x,
		y,
		width: cropWidth,
		height: cropHeight
	};
}

function renderEditorPreview() {
	if (!editorElements || !editorState) {
		return;
	}

	const ctx = editorElements.canvas.getContext('2d');
	if (!ctx) {
		return;
	}

	const cropRect = calculateCropRect();
	const previewAspect = cropRect.width / cropRect.height;
	const previewWidth = 960;
	const previewHeight = Math.max(240, Math.round(previewWidth / previewAspect));
	editorElements.canvas.width = previewWidth;
	editorElements.canvas.height = previewHeight;

	ctx.clearRect(0, 0, previewWidth, previewHeight);
	ctx.drawImage(
		editorState.image,
		cropRect.x,
		cropRect.y,
		cropRect.width,
		cropRect.height,
		0,
		0,
		previewWidth,
		previewHeight
	);
}

async function exportEditedImage() {
	const preset = getPreset(editorState.type);
	const cropRect = calculateCropRect();
	const scale = Math.min(
		preset.maxWidth / cropRect.width,
		preset.maxHeight / cropRect.height,
		1
	);

	const exportWidth = Math.max(1, Math.round(cropRect.width * scale));
	const exportHeight = Math.max(1, Math.round(cropRect.height * scale));
	const canvas = document.createElement('canvas');
	canvas.width = exportWidth;
	canvas.height = exportHeight;

	const ctx = canvas.getContext('2d');
	if (!ctx) {
		throw new Error('이미지 편집 컨텍스트를 생성하지 못했습니다.');
	}

	ctx.drawImage(
		editorState.image,
		cropRect.x,
		cropRect.y,
		cropRect.width,
		cropRect.height,
		0,
		0,
		exportWidth,
		exportHeight
	);

	const blob = await new Promise((resolve, reject) => {
		canvas.toBlob(
			(createdBlob) => {
				if (!createdBlob) {
					reject(new Error('편집 결과를 생성하지 못했습니다.'));
					return;
				}
				resolve(createdBlob);
			},
			'image/webp',
			preset.quality
		);
	});

	const originalName = editorState.file.name || 'promotion-image';
	const fileName = originalName.replace(/\.[^.]+$/u, '') + '.webp';
	return new File([blob], fileName, { type: 'image/webp' });
}

function setEditorPresetUI(type) {
	const preset = getPreset(type);
	editorElements.title.textContent = `${preset.label} 편집`;
	editorElements.subtitle.textContent = preset.aspectRatio
		? `고정 비율 ${preset.aspectRatioLabel || `${preset.aspectRatio}:1`}로 업로드됩니다.`
		: '상세 이미지는 자유 비율로 자를 수 있습니다.';
	editorElements.freeControls.style.display = preset.aspectRatio ? 'none' : 'block';
}

async function editImage({ file, type }) {
	if (!(file instanceof File)) {
		throw new Error('유효한 파일이 아닙니다.');
	}

	const elements = ensureEditorElements();
	const image = await loadImageFromFile(file);

	if (editorResolve) {
		closeEditor(null);
	}

	editorState = {
		file,
		type: type || 'detail',
		image,
		zoom: 1,
		centerX: 0.5,
		centerY: 0.5,
		cropWidthScale: 1,
		cropHeightScale: 1
	};

	elements.zoom.value = '1';
	elements.centerX.value = '0.5';
	elements.centerY.value = '0.5';
	elements.cropWidth.value = '1';
	elements.cropHeight.value = '1';
	setEditorPresetUI(editorState.type);
	renderEditorPreview();
	openEditorModal();

	return new Promise((resolve) => {
		editorResolve = resolve;
	});
}

window.PromotionImageEditor = {
	editImage
};
