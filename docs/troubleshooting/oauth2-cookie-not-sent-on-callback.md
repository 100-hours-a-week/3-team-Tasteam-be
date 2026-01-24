## OAuth2 콜백 시 쿠키 미전송으로 인한 401 오류

- 해결 일자: 26.01.24
- 관련 파일:
  - `app-api/src/main/java/com/tasteam/global/security/oauth/repository/HttpCookieOAuth2AuthorizationRequestRepository.java`
  - `app-api/src/main/resources/application.prod.yml`
  - `app-api/src/main/resources/application.dev.yml`

---

### 증상

Google OAuth2 콜백(`/api/v1/auth/oauth/callback/google`)에서 401 Unauthorized 응답 발생.

콜백 응답 헤더:
```
set-cookie: oauth2_auth_request=; Max-Age=0; ...
set-cookie: redirect_uri=; Max-Age=0; ...
```

콜백 요청 헤더:
```
cookie: JSESSIONID=...
```

`oauth2_auth_request`, `redirect_uri` 쿠키가 콜백 요청에 포함되지 않음.

---

### 원인 분석

#### 1단계: 콜백에서 쿠키가 없는 이유 추적

콜백 요청에 OAuth 쿠키가 없으면 서버는 state 검증 실패로 401을 반환한다. 가능한 원인:

| 원인 | 설명 |
|-----|-----|
| Domain/Path 불일치 | 시작과 콜백의 호스트가 다름 (www vs non-www) |
| Secure 플래그 누락 | HTTPS 환경에서 Secure 없는 쿠키 미전송 |
| SameSite 정책 | cross-site 요청에서 쿠키 차단 |
| 쿠키 자체가 설정 안 됨 | 시작 단계에서 쿠키 미설정 |

#### 2단계: OAuth 시작 요청 분석

시작 요청(`/api/v1/auth/oauth/google`) 응답 헤더 캡처:

```
set-cookie: oauth2_auth_request=ck8w...; Max-Age=180; Secure; HttpOnly; SameSite=Lax
set-cookie: oauth2_auth_request=; Max-Age=0; Secure; HttpOnly; SameSite=Lax
set-cookie: redirect_uri=; Max-Age=0; Secure; HttpOnly; SameSite=Lax
```

**동일한 응답에서 쿠키를 설정한 직후 바로 삭제하고 있었다.**

브라우저는 마지막 `Set-Cookie`를 적용하므로, 결과적으로 쿠키가 저장되지 않음.

#### 3단계: 코드 추적

`HttpCookieOAuth2AuthorizationRequestRepository.saveAuthorizationRequest()`:

```java
public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
    HttpServletRequest request, HttpServletResponse response) {
    if (authorizationRequest == null) {
        removeAuthorizationRequestCookies(response);  // 문제의 원인
        return;
    }
    // 쿠키 설정 로직
}
```

Spring Security OAuth2의 `OAuth2AuthorizationRequestRedirectFilter`가 인증 시작 시:
1. `saveAuthorizationRequest(authorizationRequest, ...)` 호출 → 쿠키 설정
2. 이후 다른 필터 또는 내부 로직에서 `saveAuthorizationRequest(null, ...)` 호출 → 쿠키 삭제

동일한 HTTP 응답에서 두 호출이 발생하면서 쿠키가 설정 후 삭제됨.

#### 4단계: 추가 발견 - Secure 플래그 누락

`SecurityCookieProperties`의 기본값:
```java
private boolean secure = false;
```

운영 환경(HTTPS)에서 `Secure` 플래그가 없으면 브라우저가 쿠키를 전송하지 않을 수 있음.

---

### 적용된 변경

#### 1. 쿠키 삭제 로직 제거

`HttpCookieOAuth2AuthorizationRequestRepository.java`:

**변경 전:**
```java
if (authorizationRequest == null) {
    removeAuthorizationRequestCookies(response);
    return;
}
```

**변경 후:**
```java
if (authorizationRequest == null) {
    return;
}
```

쿠키 삭제는 OAuth 성공/실패 핸들러에서 명시적으로 수행하므로 여기서 삭제할 필요 없음.

#### 2. Secure 플래그 활성화

`application.prod.yml`, `application.dev.yml`:

```yaml
spring:
  security:
    cookie:
      secure: true
```

---

### 흐름 정리

**수정 전:**
```
OAuth 시작 요청
  ↓
saveAuthorizationRequest(request) 호출 → 쿠키 설정
  ↓
saveAuthorizationRequest(null) 호출 → 쿠키 삭제
  ↓
응답: Set-Cookie 설정 + Set-Cookie 삭제 (삭제가 최종 적용)
  ↓
Google 인증 완료 후 콜백
  ↓
쿠키 없음 → state 검증 실패 → 401
```

**수정 후:**
```
OAuth 시작 요청
  ↓
saveAuthorizationRequest(request) 호출 → 쿠키 설정
  ↓
saveAuthorizationRequest(null) 호출 → 아무것도 안 함
  ↓
응답: Set-Cookie 설정만 포함
  ↓
Google 인증 완료 후 콜백
  ↓
쿠키 전송됨 → state 검증 성공 → 인증 완료
  ↓
Success/Failure 핸들러에서 쿠키 삭제
```

---

### 디버깅 팁

OAuth 쿠키 문제 발생 시 확인 순서:

1. **시작 요청 응답 헤더**: `Set-Cookie`가 정상적으로 내려오는지, 삭제 쿠키가 함께 내려오지 않는지
2. **브라우저 쿠키 저장소**: Application → Cookies에서 쿠키가 저장되어 있는지
3. **콜백 요청 헤더**: `Cookie` 헤더에 OAuth 쿠키가 포함되어 있는지
4. **호스트 일치 여부**: 시작과 콜백의 도메인이 동일한지 (www vs non-www)
5. **Secure/SameSite 설정**: HTTPS 환경에서 적절한 값인지

---

### 참고

- `OAuthLoginSuccessHandler.java:56` - 성공 시 쿠키 삭제
- `OAuthLoginFailureHandler.java:38` - 실패 시 쿠키 삭제
- `SecurityCookieProperties.java` - 쿠키 속성 설정
