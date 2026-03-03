# 어드민 SPA - ES 모듈 격리로 인한 크로스 파일 참조 오류

## 에러

```
ReferenceError: renderAdminLayout is not defined
ReferenceError: ImageOptimizer is not defined
ReferenceError: getFoodCategories is not defined
```

어드민 페이지 진입 시 콘텐츠가 렌더링되지 않거나, 이미지 업로드 시 위 에러 발생.

## 원인

### ES 모듈의 스코프 격리

`index.html`에서 스크립트를 `type="module"`로 로드하면 각 파일은 **독립된 모듈 스코프**를 가진다.

```html
<script src="/admin/js/api.js" type="module"></script>
<script src="/admin/js/image-optimizer.js" type="module"></script>
<script src="/admin/js/views/restaurant-edit.view.js" type="module"></script>
```

모듈 내의 `const`, `function` 선언은 해당 모듈 스코프에만 존재하며, `window` 객체에 자동으로 등록되지 않는다. 따라서 다른 파일에서 이름만으로 참조하면 `ReferenceError`가 발생한다.

일반 스크립트(`type` 없음)에서는 top-level `function` 선언이 `window` 프로퍼티가 되지만, `const`/`let`은 모듈 여부와 무관하게 `window`에 등록되지 않는다.

### 어드민 SPA의 참조 패턴

어드민 코드는 `window.xxx = { ... }` 패턴으로 뷰/유틸을 전역에 노출한다.

```js
// api.js
window.getFoodCategories = async () => { ... };

// image-optimizer.js
const ImageOptimizer = { ... };
// window.ImageOptimizer 미설정 → 다른 파일에서 참조 불가
```

그러나 각 뷰 파일에서 다른 파일의 함수를 `window.` 없이 직접 호출하는 코드가 남아 있었다.

```js
// restaurant-edit.view.js (수정 전)
const result = await getFoodCategories();               // window. 누락
const optimized = await ImageOptimizer.optimizeImages(); // window. 누락

// admin-router.js (수정 전)
contentRoot = renderAdminLayout(appRoot, ...);          // window. 누락
unbindLayout = bindAdminLayout(appRoot, ...);           // window. 누락
```

## 해결

### 1. `window.ImageOptimizer` 노출 누락 수정

`image-optimizer.js`에 `window.ImageOptimizer` 할당을 추가한다.

```js
// image-optimizer.js
const ImageOptimizer = {
    optimizeImages: ...,
    optimizeRestaurantImage: ...,
    optimizeGroupLogo: ...,
};

window.ImageOptimizer = ImageOptimizer; // 추가
```

### 2. 크로스 파일 호출에 `window.` 접두사 추가

| 파일 | 수정 내용 |
|------|----------|
| `admin-router.js` | `renderAdminLayout` → `window.renderAdminLayout`, `bindAdminLayout` → `window.bindAdminLayout` |
| `restaurant-edit.view.js` | `getFoodCategories`, `getRestaurant`, `ImageOptimizer`, `createPresignedUploads`, `uploadToPresigned`, `updateRestaurant` → 모두 `window.` 접두사 |
| `restaurant-menu.view.js` | `getRestaurant`, `getRestaurantMenus`, `ImageOptimizer`, `createMenuCategory`, `createPresignedUploads`, `uploadToPresigned`, `createMenu` → 모두 `window.` 접두사 |
| `groups.view.js` | `ImageOptimizer.optimizeGroupLogo` → `window.ImageOptimizer.optimizeGroupLogo` |

## 진단 방법

브라우저 개발자 도구 콘솔에서 `window.renderAdminLayout`, `window.ImageOptimizer` 등을 직접 입력하면 `undefined`인지 확인할 수 있다.

## 예방 원칙

어드민 SPA처럼 `type="module"` 스크립트 간 전역 참조를 사용하는 구조에서는:

1. 다른 파일의 함수/객체를 호출할 때는 반드시 `window.` 접두사를 사용한다.
2. 새 유틸/서비스 파일을 추가할 때는 파일 마지막에 `window.xxx = ...` 할당을 포함한다.
3. 장기적으로는 진짜 ES 모듈(`import`/`export`)로 전환하여 암묵적 전역 의존을 제거하는 것이 권장된다.
