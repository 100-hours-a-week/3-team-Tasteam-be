
항목 | 내용
-- | --
문서 제목 | 테이블 정의서
문서 목적 | 본 문서는 Tasteam DB 테이블 구조 및 책임을 정의하여, <br> 데이터 일관성 확보와 백엔드 개발 및 운영 시 기준 자료로 활용하는 것을 목적으로 한다.
작성 및 관리 | Backend Team
최초 작성일 | 2026.01.06
최종 수정일 | 2026.01.08
문서 버전 | v1.2



## member

- 테이블명 : member
- 테이블 설명 : 회원의 계정 및 정보를 저장하는 테이블
- 책임 : 회원 식별, 상태, 권한, 약관 동의 관리
- 생명주기 : 가입 → ACTIVE → BLOCKED/WITHDRAWN
- 삭제 정책 : 물리 삭제 없음
- 주요 조회 패턴 : id 기반 전체 조회
- 제약조건 : nickname 빈 문자열 불가
- 인덱스 : -
- 설계 근거 및 향후 확장성 :
    - 유저에 있어 필수적인 데이터만 포함하며, 부수적인 데이터, 통계 데이터, 유저 세팅 같은 경우 별도의 테이블로 분리 및 확장하여 설계한다.
    - 해당 테이블에서는 변경이 자주 일어나지 않는 데이터만 포함한다.
    - 테이블 존속 여부는 status로 표기한다
    - nickname 기본 값은 애플리케이션에서 임의의 닉네임 자동 생성 방식을 사용한다.
    - 소셜 로그인이므로 반드시 email이 필요하지 않지만 추후 이메일 알림 기능 확장될 수 있으므로 추가한다. 기본 값 초기화는 최초로 회원가입한 OAuth2 이메일로 설정한다.

### [테이블 정의]

| 컬럼명 | 데이터 타입 | NULL 허용 | Key (PK/FK/-) | UNIQUE | 기본값 / IDENTITY | ENUM / 제약 / 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | N | PK | Y | IDENTITY |  |
| email | VARCHAR(255) | Y | - | Y |  | 최초 oauth 로그인 이메일로 설정 |
| nickname | VARCHAR(50) | N | - | N |  | 빈 문자열 불가 |
| status | VARCHAR(20) | N | - | N |  | ENUM('ACTIVE', 'BLOCKED', 'WITHDRAWN') |
| role | VARCHAR(20) | N | - | N |  | ENUM('USER', 'ADMIN') |
| profile_image_url | VARCHAR(500) | Y | - | N |  |  |
| last_login_at | TIMESTAMP | Y | - | N |  |  |
| agreed_terms_at | TIMESTAMP | Y | - | N |  |  |
| agreed_privacy_at | TIMESTAMP | Y | - | N |  |  |
| created_at | TIMESTAMP | N | - | N | CURRENT_TIMESTAMP |  |
| updated_at | TIMESTAMP | N | - | N | CURRENT_TIMESTAMP |  |
| deleted_at | TIMESTAMP | Y | - | N |  |  |

---

## member_oauth_account

- 테이블명 : member_oauth_account
- 테이블 설명 : 회원의 외부 OAuth 계정 매핑
- 책임 : OAuth 제공자 계정과 회원 계정 연결 관리
- 생명주기 : 연결 생성 → 필요 시 연결 해제 (정책에 따라 소프트 삭제)
- 삭제 정책 : member 물리 삭제 시 CASCADE로 물리 삭제
- 주요 조회 패턴 : provider/provider_id로 회원 조회
- 제약조건 :
    - 복합 UNIQUE(provider, provider_id)
    - provider_user_id 빈 문자열 불가
- 인덱스 : INDEX(provider, provider_id)
- 설계 근거 및 향후 확장성 :
    - MVP 단계에서는 kakao로그인만 지원하지만, 다양한 OAuth Provider 확장을 고려하여 member : member_oauth_account는 1:N 관계를 유지한다.
    - member가 존재하지 않으면 해당 데이터는 필요 없으므로 member와 생명 주기를 같이한다

### [테이블 정의]

| 컬럼명 | 데이터 타입 | NULL 허용 | Key (PK/FK/-) | UNIQUE | 기본값 / IDENTITY | ENUM / 제약 / 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | N | PK | Y | IDENTITY |  |
| member_id | BIGINT | N | FK | N |  | [member.id](http://member.id/) 참조 (ON DELETE CASCADE) |
| provider | VARCHAR(30) | N | - | N |  | 빈 문자열 불가 |
| provider_user_id | VARCHAR(255) | N | - | N |  | 빈 문자열 불가 |
| provider_user_email | VARCHAR(255) | Y | - | N |  | 빈 문자열 불가 |
| created_at | TIMESTAMP | N | - | N | CURRENT_TIMESTAMP |  |

---

## member_favorite_restaurant

- 테이블명 : member_favorite_restaurant
- 테이블 설명 : 회원 즐겨찾기 음식점 정보를 저장하는 테이블
- 책임 : 회원 단위 즐겨찾기 관리
- 생명주기 : 생성 → 유지 → 소프트 삭제
- 삭제 정책 : 정책에 따라 물리 삭제 가능
- 주요 조회 패턴 : member_id 기반 즐겨찾기 목록 조회
- 제약조건 : 복합 UNIQUE(member_id, restaurant_id)
- 인덱스 : UNIQUE INDEX(member_id, restaurant_id)
- 설계 근거 및 향후 확장성 :
    - 개인화 추천/검색 품질 개선을 위해 사용한다.
    - 찜 엔티티는 단순 매핑관계가 아닌 독립적인 객체이므로 복합 PK가 아닌 단일 PK + Unique 제약 조건으로 구현한다.

### [테이블 정의]

| 컬럼명 | 데이터 타입 | NULL 허용 | Key (PK/FK/-) | UNIQUE | 기본값 / IDENTITY | ENUM / 제약 / 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | N | PK | Y | IDENTITY |  |
| member_id | BIGINT | N | FK | N |  | [member.id](http://member.id/) 참조 |
| restaurant_id | BIGINT | N | FK | N |  | [restaurant.id](http://restaurant.id/) 참조 |
| created_at | TIMESTAMP | N | - | N | CURRENT_TIMESTAMP |  |
| deleted_at | TIMESTAMP | Y | - | N |  |  |

---

## member_recent_search

- 테이블명 : member_recent_search
- 테이블 설명 : 회원의 최근 검색어 기록을 저장하는 테이블
- 책임 : 검색 편의 기능(최근 검색어) 제공
- 생명주기 : 추가 → 소프트 삭제
- 삭제 정책 : 오래된 기록은 배치로 삭제 가능
- 주요 조회 패턴 : member_id 기반 최근 검색어 조회 (created_at desc)
- 제약조건 : keyword 빈 문자열 불가
- 인덱스 : PARTIAL INDEX (member_id, created_at DESC, deleted_at = NULL)
- 설계 근거 및 향후 확장성 :
    - 사용자가 검색 결과를 삭제하더라도 논리적 삭제한다. (데이터 활용 목적)
    - 검색 UX 개선 및 개인화 추천에 활용 가능하다.
    - 쓰기 작업이 많고 강한 정합성이 요구되지 않는 데이터이므로 데이터 분산 저장에 용이한 NoSQL 사용을 고려한다.

### [테이블 정의]

| 컬럼명 | 데이터 타입 | NULL 허용 | Key (PK/FK/-) | UNIQUE | 기본값 / IDENTITY | ENUM / 제약 / 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | N | PK | Y | IDENTITY |  |
| member_id | BIGINT | N | FK | N |  | [member.id](http://member.id/) 참조 |
| keyword | VARCHAR(100) | N | - | N |  | 빈 문자열 불가 |
| count | BIGINT | N | - | N |  |  |
| created_at | TIMESTAMP | N | - | N | CURRENT_TIMESTAMP |  |
| deleted_at | TIMESTAMP | Y | - | N |  |  |

---

## group

- 테이블명 : group
- 테이블 설명 : 그룹(모임)의 기본 정보를 저장하는 테이블
- 책임 : 그룹 기본 프로필 관리 및 위치기반 정보제공
- 생명주기 : 생성 →updated_at으로 수정사항 관리-> ACTIVE/INACTIVE
- 삭제 정책 : 물리 삭제 없음(INACTIVE를 사용한 소프트 딜리트)
- 주요 조회 패턴 : id 기반 조회, 위치 정보를 기반으로 한 조회, 그룹명 기반 조회
- 제약조건 :
    - join_type이 'EMAIL' 일 경우 email_domin은 NOT NULL이어야함
    - latitude, longitude는 부동소수점 오차 방지를 위해 위도, 경도를 충분히 커버하면서 약 10cm단위로 정밀도를 확실히 보장하는 DEMICAL(9,6)으로 저장
- 인덱스 :
    - GIST(location)
- 설계 근거 및 향후 확장성 :
    - 하위 그룹(subgroup), 그룹 멤버, 권한 코드 등을 분리하여 확장
    - geometry(Point,4326) 타입을 사용하여 정밀한 위치 서비스를 지원
    - 활성 데이터만을 대상으로 Partial GIST Index를 적용, 모든 위치 기반 조회는 해당 인덱스 사용을 전제로 한다

### [테이블 정의]

| **컬럼명** | **데이터 타입** | **NULL 허용** | **Key (PK/FK/-)** | **UNIQUE** | **기본값 / IDENTITY** | **ENUM / 제약 / 비고** |
| --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | N | PK | Y | IDENTITY |  |
| name | VARCHAR(100) | N | - | N |  | 빈 문자열 불가 |
| type | VARCHAR(20) | N | - | N |  | ENUM(’OFFICIAL’, ‘UNOFFICIAL’) |
| logo_image_url | VARCHAR(500) | Y | - | N |  |  |
| address | VARCHAR(225) | N | - | N |  |  |
| detail_address | VARCHAR(255) | N | - | N |  |  |
| location | geometry(Point,4326) | Y | - | N |  |  |
| join_type | VARCHAR(20) | N | - | N |  | ENUM(’EMAIL’, ‘PASSWORD’) |
| email_domain | VARCHAR(100) | Y | - | N |  | join_type=EMAIL 필수 |
| status | VARCHAR(20) | N | - | N | DEFAULT ACTIVE | ENUM(’ACTIVE’, ‘INACTIVE’) |
| created_at | TIMESTAMP | N | - | N |  |  |
| updated_at | TIMESTAMP | N | - | N |  |  |
| deleted_at | TIMESTAMP | Y | - | N |  |  |

---

## group_member

- 테이블명 : group_member
- 테이블 설명 : 특정 그룹에 가입한 회원들의 가입, 탈퇴 상태를 관리하는 매핑 테이블 입니다.
- 책임 : 그룹과 회원간의 다대다 관계해소 및 회원별 그룹 가입 이력을 관리
- 생명주기 : 생성 → 그룹 삭제시 함께 소프트딜리트
- 삭제 정책 :
    - 물리 삭제 지양(이력 보존)
    - 그룹 소프트 삭제시 소프트 삭제
    - 그룹 하드 삭제시 CASCADE로 물리 삭제
- 주요 조회 패턴 : group_id 기반 멤버 목록 조회, member_id 기반 소속 그룹 조회
- 제약조건 :
    - 복합 UNIQUE(group_id, member_id)
    - 재가입시 재가입 일시는 저장하지 않으며 deleted_at은 null로 변경한다.
- 인덱스 :
    - PARTIAL INDEX (group_id, deleted_at = NULL)
    - PARTIAL INDEX (member_id, deleted_at = NULL)
- 설계 근거 및 향후 확장성 :
    - deleted_at 을 활용해 데이터 분석 및 재가입 제한 로직 등에 활용
    - 역할/권한/상태(승인 등) 확장을 고려

### [테이블 정의]

| **컬럼명** | **데이터 타입** | **NULL 허용** | **Key (PK/FK/-)** | **UNIQUE** | **기본값 / IDENTITY** | **ENUM / 제약 / 비고** |
| --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | N | PK | Y | IDENTITY |  |
| group_id | BIGINT | N | FK | N | - | [group.id](http://group.id) 참조 |
| member_id | BIGINT | N | FK | N | - | [member.id](http://member.id) 참조 |
| created_at | TIMESTAMP | N | - | N | - |  |
| deleted_at | TIMESTAMP | Y | - | N | - |  |

---

## group_request

- 테이블명 : group_request
- 테이블 설명 : 사용자의 새로운 그룹 생성 신청 정보를 관리하는 테이블
- 책임 : 그룹 생성 신청 정보 및 처리 상태 관리
- 생명주기 : PENDING → APPROVED / REJECTED
- 삭제 정책 : 물리 삭제 지양(이력 보존)
- 주요 조회 패턴 :
    - 관리자가 승인 대기중인 신청 목록 조회(status)
    - 특정 사용자(member_id)가 본인의 신청 내역을 조회
- 제약조건 : APPROVED / REJECTED 상태에서 PENDING 상태로 변경할 수 없다
- 인덱스 :
    - PARTICAL INDEX(status = 'PENDING', created_at ASC)
    - INDEX (member_id, created_at DESC)
- 설계 근거 및 향후 확장성 :
    - status를 ENUM 타입으로 정의하여 승인 프로세스의 정합성을 보장하고 향후 새로운 상태가 추가될 경우 유연하게 확장 가능
    - created_at을 복합 인덱스에 포함하여 사용자의 최신 신청 내역을 효율적으로 조회할 수 있도록 설계

### [테이블 정의]

| **컬럼명** | **데이터 타입** | **NULL 허용** | **Key (PK/FK/-)** | **UNIQUE** | **기본값 / IDENTITY** | **ENUM / 제약 / 비고** |
| --- | --- | --- | --- | --- | --- | --- |
| **id** | BIGINT | N | PK | Y | IDENTITY |  |
| **member_id** | BIGINT | N | FK | N | - | [member.id](http://member.id) 참조 |
| **status** | VARCHAR(20) | N | - | N | - | ENUM('PENDING', 'APPROVED', 'REJECTED') |
| **email** | VARCHAR(255) | N | - | N | - |  |
| **request_type** | VARCHAR(20) | N | - | N | - | ENUM('OFFICIAL', 'UNOFFICIAL') |
| **company_name** | VARCHAR(30) | N | - | N | - |  |
| **address** | VARCHAR(255) | N | - | N | - |  |
| **detail_address** | VARCHAR(255) | Y | - | N | - |  |
| **postal_code** | VARCHAR(16) | Y | - | N | - |  |
| **agreed_terms_at** | TIMESTAMP | Y | - | N | - |  |
| **agreed_privacy_at** | TIMESTAMP | Y | - | N | - |  |
| **created_at** | TIMESTAMP | N | - | N | - |  |

---

## group_request_status_history

- **테이블명** : group_request_status_history
- **테이블 설명** : 그룹 생성 요청의 상태 변경 이력을 기록하는 테이블
- **책임** :
    - 그룹 생성 요청 처리 과정의 추적
    - 승인/반려 이력에 대한 감사(Audit) 근거 제공
- **생명주기** : 생성 → 삭제
- **삭제 정책** :
    - 감사 및 운영 기록으로서 물리 삭제 지양
- **주요 조회 패턴** :
    - group_request_id 기준 상태 변경 이력 조회
- **제약조건** :
    - 하나의 로그는 하나의 상태 변경 이벤트를 의미하며 수정 불가능하다
- **설계 근거 및 향후 확장성** :
    - 요청 테이블과 로그 테이블을 분리하여 현재 상태와 변경 이력을 명확히 구분한다
    - 추후 처리자 정보, 사유 코드, 자동/수동 처리 여부 등을 확장 가능하다

### **[테이블 정의]**

| **컬럼명** | **데이터 타입** | **NULL 허용** | **Key (PK/FK/-)** | **UNIQUE** | **기본값 / IDENTITY** | **ENUM / 제약 / 비고** |
| --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | N | PK | Y | IDENTITY | SEQUENCE |
| group_request_id | BIGINT | N | FK | N |  | group_request.id 참조 |
| previous_status | VARCHAR(20) | N | - | N |  | 이전 상태 |
| current_status | VARCHAR(20) | N | - | N |  | 변경 후 상태 |
| reason | VARCHAR(1000) | Y | - | N |  | 신청 반려 사유 |
| group_id | BIGINT | Y | FK | N |  | group.id 참조, 신청 결과로 만들어진 그룹 id |
| created_at | TIMESTAMP | N | - | N |  | 상태 변경 시각 |


---

## group_auth_code

- 테이블명 : group_auth_code
- 테이블 설명 : 그룹(공식/비공식) 가입을 위한 인증 코드를 관리하는 테이블
- 책임 : 가입 인증 코드 생성 및 만료 시각 관리
- 생명주기 : 생성 → expires_at 경과 후 무효
- 삭제 정책 : 만료 데이터는 배치로 정리 가능
- 주요 조회 패턴 :
    - group_id 기준 최신 인증 코드 조회
    - expires_at 기준 만료 정리
- 제약조건 : UNIQUE(code)
- 인덱스 :
    - INDEX(expires_at)
    - INDEX(group_id, created_at DESC)
- 설계 근거 및 향후 확장성 : -

### [테이블 정의]

| **컬럼명** | **데이터 타입** | **NULL 허용** | **Key (PK/FK/-)** | **UNIQUE** | **기본값 / IDENTITY** | **ENUM / 제약 / 비고** |
| --- | --- | --- | --- | --- | --- | --- |
| **id** | BIGINT | N | PK | Y | IDENTITY |  |
| **group_id** | BIGINT | N | FK | N | - | [group.id](http://group.id/) 참조 |
| **code** | VARCHAR(20) | N | - | N | - |  |
| **expires_at** | TIMESTAMP | N | - | N | - |  |
| **created_at** | TIMESTAMP | N | - | N | - |  |

---

## subgroup

- 테이블명 : subgroup
- 테이블 설명 : 그룹 내에서 세부적으로 운영되는 하위 그룹의 정보를 관리하는 테이블
- 책임 : 서브그룹 기본 정보 및 가입 방식(OPEN/PASSWORD) 관리
- 생명주기 : 생성 → updated_at으로 수정사항 관리 -> ACTIVE/INACTIVE
- 삭제 정책 : 물리 삭제 없음(상태로 표현)
- 주요 조회 패턴 :
    - 특정 그룹(group_id)에 속한 하위 그룹 목록 조회
    - 하위 그룹 이름(name) 검색
    - 상태(status)에 따른 필터링 조회
- 제약조건 :
    - name 빈 문자열 불가
    - join_type=PASSWORD이면 join_password 필수
- 인덱스 : 
    - PARTICAL INDEX (group_id, created_at DESC, status = 'ACTIVE')
    - PARTICAL INDEX (name, created_at DESC, status = 'ACTIVE')
- 설계 근거 및 향후 확장성 : - 



### [테이블 정의]

| **컬럼명** | **데이터 타입** | **NULL 허용** | **Key (PK/FK)** | **UNIQUE** | **기본값 / IDENTITY** | **ENUM / 제약 / 비고** |
| --- | --- | --- | --- | --- | --- | --- |
| **id** | BIGINT | N | PK | Y | IDENTITY |  |
| **group_id** | BIGINT | N | FK | N | - |  |
| **name** | VARCHAR(100) | N | - | N | - |  |
| **description** | VARCHAR(500) | Y | - | N | - |  |
| **profile_image_url** | VARCHAR(500) | Y | - | N | - |  |
| **join_type** | VARCHAR(20) | N | - | N | - | ENUM('OPEN', 'PASSWORD') |
| **join_password** | VARCHAR(255) | Y | - | N | - |  |
| **status** | VARCHAR(20) | N | - | N | - | ENUM('ACTIVE', 'INACTIVE') |
| **created_at** | TIMESTAMP | N | - | N | - |  |
| **updated_at** | TIMESTAMP | N | - | N | - |  |

---

## subgroup_member

- 테이블명 : subgroup_member
- 테이블 설명 : 서브그룹 멤버 정보를 저장하는 테이블
- 책임 : 하위 그룹과 회원 간의 관계를 정의
- 생명주기 : 생성 → 유지 → 삭제
- 삭제 정책 :
    - 물리 삭제 지양(이력 보존)
    - 그룹 소프트 삭제시 소프트 삭제
    - 그룹 하드 삭제시 CASCADE로 물리 삭제
- 주요 조회 패턴 :
    - 특정 하위그룹(subgroup_id)의 활성 멤버 목록 조회
    - 특정 회원(member_id)이 참여 중인  하위그룹 목록 조회
    - 회원의 가입 여부 및 탈퇴 상태(deleted_at 유무) 검증
- 제약조건 :
    - 복합 UNIQUE(subgroup_id, member_id)
    - 재가입시 재가입 일시는 저장하지 않으며 deleted_at은 null로 변경한다.
- 인덱스 :
    - PARTICAL INDEX(subgroup_id, deleted_at = NULL)
    - PARTICAL INDEX (member_id, deleted_at = NULL)
- 설계 근거 및 향후 확장성 : 서브그룹 단위의 권한/상태 확장을 고려한다.

### [테이블 정의]

| **컬럼명** | **데이터 타입** | **NULL 허용** | **Key (PK/FK)** | **UNIQUE** | **기본값 / ID** | **ENUM / 제약 / 비고** |
| --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | N | PK | Y | IDENTITY |  |
| member_id | BIGINT | N | FK | N | - | [member.id](http://member.id) 참조 |
| subgroup_id | BIGINT | N | FK | N | - | [subgroup.id](http://subgroup.id) 참조 |
| created_at | TIMESTAMP | N | - | N | - |  |
| deleted_at | TIMESTAMP | Y | - | N | - |  |

---

## subgroup_favorite_restaurant

- 테이블명 : subgroup_favorite_restaurant
- 테이블 설명 : 하위 그룹의 찜 목록을 관리하는 매핑 테이블
- 책임 : 하위 그룹과 음식점 간의 관계 데이터를 저장, 관리
- 생명주기 : 생성 → 하드 딜리트
- 삭제 정책 : 정책에 따른 물리삭제 가능
- 주요 조회 패턴 :
    - 특정 하위그룹(subgroup_id) 기준 찜 목록 최신순 정렬
    - 최근에 등록된 선호 음식점 순으로 정렬 조회
- 제약조건 :
    - 복합 UNIQUE(subgroup_id, restaurant_id)
    - 복합 UNIQUE(restaurant_id, member_id)
- 인덱스 : INDEX (subgroup_id, created_at DESC)
- 설계 근거 및 향후 확장성 :
    - 하위그룹과 음식점 간의 다대다 관계를 효율적으로 관리하기 위해 매핑 테이블을 구성
    - 특정 하위그룹의 찜 목록을 쉽게 조회하기 위해 subgroup_member_id를 참조하는 대신 member_id와 subgroup_id 각각 직접 추가하여 조인 복잡도를 줄이고 조회 성능을 높임

### [테이블 정의]

| **컬럼명** | **데이터 타입** | **NULL 허용** | **Key (PK/FK)** | **UNIQUE** | **기본값 /** IDENTITY | **ENUM / 제약 / 비고** |
| --- | --- | --- | --- | --- | --- | --- |
| **id** | BIGINT | N | PK | Y | IDENTITY |  |
| member_id | BIGINT | N | FK | N |  | [member.id](http://member.id) 참조 |
| **subgroup_id** | BIGINT | N | FK | N | - | [subgroup.id](http://subgroup.id) 참조 |
| **restaurant_id** | BIGINT | N | FK | N | - | [restaurant.id](http://restaurant.id) 참조 |
| **created_at** | TIMESTAMP | N | - | N | - |  |

---

## restaurant

- 테이블명 : restaurant
- 테이블 설명 : 음식점의 기본 정보를 저장하는 마스터 테이블
- 책임 : 음식점 식별, 위치 정보 관리, 소프트 삭제 상태 관리
- 생명주기 : 생성 → 유지 → 소프트 삭제
- 삭제 정책 : `deleted_at` 기반 소프트 삭제
- 주요 조회 패턴 :
    - id 기반 상세 조회
    - 위치 기반 조회 (PostGIS)
- 제약조건 :
    - name 빈 문자열 불가
- 인덱스 :
    - PARTICAL INDEX (GIST(location), deleted_at = NULL)
- 설계 근거 및 향후 확장성 :
    - 위치 정보는 PostGIS `Point(4326)`로 관리하여 거리/반경 검색 확장 가능
    - 활성 데이터만을 대상으로 Partial GIST Index를 적용, 모든 위치 기반 조회는 해당 인덱스 사용을 전제로 한다
    - 주소 세부 정보는 `restaurant_address` 테이블로 분리

### [테이블 정의]

| 컬럼명 | 데이터 타입 | NULL 허용 | Key (PK/FK/-) | UNIQUE | 기본값 / IDENTITY | ENUM / 제약 / 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | N | PK | Y | IDENTITY |  |
| name | VARCHAR(100) | N | - | N |  | 빈 문자열 불가 |
| full_address | VARCHAR(255) | N | - | N |  |  |
| location | geometry(Point,4326) | N | - | N |  | WGS84 |
| created_at | TIMESTAMP | N | - | N |  |  |
| updated_at | TIMESTAMP | N | - | N |  |  |
| deleted_at | TIMESTAMP | Y | - | N |  | 소프트 삭제 |

---

## restaurant_image

- 테이블명 : restaurant_image
- 테이블 설명 : 음식점 대표 이미지 정보를 저장하는 테이블
- 책임 : 음식점 대표 이미지 URL 및 정렬 순서 관리
- 생명주기 : 생성 → 유지 → 소프트 삭제
- 삭제 정책 : `deleted_at` 기반 소프트 삭제
- 주요 조회 패턴 : restaurant_id 기반 이미지 정렬순 목록 조회
- 제약조건 : -
- 인덱스 : PARTICAL INDEX (restaurant_id, sort_order ASC, deleted_at = NULL)
- 설계 근거 및 향후 확장성 :
    - 대표 이미지 개수 추가 등 확장을 고려하여 별도의 테이블로 분리한다.
    - 대표 이미지 정렬을 위해 `sort_order` 필드를 사용한다.
    - `sort_order` 값이 작을 수록 정렬 우선 순위가 높다.

### [테이블 정의]

| 컬럼명 | 데이터 타입 | NULL 허용 | Key (PK/FK/-) | UNIQUE | 기본값 / IDENTITY | ENUM / 제약 / 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | N | PK | Y | IDENTITY |  |
| restaurant_id | BIGINT | N | FK | N |  | [restaurant.id](http://restaurant.id/) |
| image_url | VARCHAR(500) | N | - | N |  | 빈 문자열 불가 |
| sort_order | INT | N | - | N |  | 양의 정수 |
| created_at | TIMESTAMP | N | - | N |  |  |
| deleted_at | TIMESTAMP | Y | - | N |  |  |

---

## restaurant_food_category

- 테이블명 : restaurant_food_category
- 테이블 설명 : 음식점과 음식 카테고리 간의 매핑 테이블
- 책임 : 음식점의 음식 카테고리 목록 관리
- 생명주기 : 생성 → 삭제
- 삭제 정책 : 음식점(restaurant), 음식 카테고리(food_category) 물리 삭제 시 연쇄 물리 삭제
- 주요 조회 패턴 :
    - restaurant_id 기준 음식 카테고리 조회
    - food_category_id 기준 음식점 조회
- 제약조건 : -
- 인덱스 :
    - INDEX (restaurant_id)
    - INDEX (food_category_id)
- 설계 근거 및 향후 확장성 :
    - 음식점과 음식 카테고리의 다대다 관계를 표현하기 위해 매핑 테이블을 사용한다.

### [테이블 정의]

| 컬럼명 | 데이터 타입 | NULL 허용 | Key (PK/FK/-) | UNIQUE | 기본값 / IDENTITY | ENUM / 제약 / 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | N | PK | Y | IDENTITY |  |
| restaurant_id | BIGINT | N | FK | N |  | [restaurant.id](http://restaurant.id/) |
| food_category_id | BIGINT | N | FK | N |  | food_category.id |

---

## food_category

- 테이블명 : food_category
- 테이블 설명 : 음식점 분류에 사용되는 음식 카테고리의 정보를 관리하는 테이블
- 책임 : 음식 카테고리명 관리
- 생명주기 : 생성 → 삭제
- 삭제 정책 : 물리 삭제 가능
- 주요 조회 패턴 : 전체 음식 카테고리 목록 조회
- 제약조건 : 카테고리 명은 한글로 작성한다.
- 인덱스 : -
- 설계 근거 및 향후 확장성 :
    - 음식 분류를 독립 테이블로 관리하여 분류 기준 변경 및 확장에 유리하다.

### [테이블 정의]

| 컬럼명 | 데이터 타입 | NULL 허용 | Key (PK/FK/-) | UNIQUE | 기본값 / IDENTITY | ENUM / 제약 / 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | N | PK | Y | IDENTITY | SEQUENCE |
| name | VARCHAR(20) | N | - | N |  | 빈 문자열 불가 |

---

## restaurant_address

- **테이블명** : restaurant_address
- **테이블 설명** : 음식점의 주소 정보를 관리하는 테이블
- **책임** : 음식점의 행정구역 기반 주소 정보 관리
- **생명주기** : 생성 → 수정
- **삭제 정책** : 음식점(restaurant) 물리 삭제 시 연쇄 물리 삭제
- **주요 조회 패턴** :
    - restaurant_id 기준 주소 조회
    - 지역 기반(시도/시군구) 음식점 필터링
- **제약조건** : -
- **인덱스** :
    - INDEX (restaurant_id)
- **설계 근거 및 향후 확장성** :
    - 주소 정보를 음식점 기본 정보와 분리하여 **주소 체계 변경 및 확장에 유연**하도록 설계
    - 행정구역 단위를 컬럼으로 분리하여 **지역 기반 검색/집계에 최적화**
    - 향후 도로명 주소, 상세 주소, 좌표(lat/lng) 컬럼 추가 가능
    - 음식점당 주소는 1개를 전제로 하나, 필요 시 이력 관리 테이블로 확장 가능

---

### [테이블 정의]

| 컬럼명 | 데이터 타입 | NULL 허용 | Key (PK/FK/-) | UNIQUE | 기본값 / IDENTITY | ENUM / 제약 / 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | N | PK | Y | IDENTITY |  |
| restaurant_id | BIGINT | N | FK | N |  | restaurant.id |
| sido | VARCHAR(20) | Y | - | N |  | 시/도 |
| sigungu | VARCHAR(30) | Y | - | N |  | 시/군/구 |
| eupmyeondong | VARCHAR(30) | Y | - | N |  | 읍/면/동 |
| postal_code | VARCHAR(16) | Y | - | N |  | 우편번호 |
| created_at | TIMESTAMP | N | - | N |  |  |
| updated_at | TIMESTAMP | N | - | N |  |  |

---

## review

- 테이블명 : review
- 테이블 설명 : 그룹/하위 그룹 단위로 작성되는 음식점 리뷰
- 책임 : 리뷰 내용 및 음식점 추천 여부 관리
- 생명주기 : 생성 → 수정 → 삭제
- 삭제 정책 : `deleted_at` 기반 소프트 삭제
- 주요 조회 패턴 :
    - restaurant_id/group_id/subgroup_id 기준 리뷰 조회
- 제약 조건: -
- 인덱스 :
    - PARTICAL INDEX(restaurant_id, created_at DESC, deleted_at = NULL)
    - PARTICAL INDEX(group_id, created_at DESC, deleted_at = NULL)
    - PARTICAL INDEX(subgroup_id, created_at DESC, deleted_at = NULL)
- 설계 근거 :
    - 내용 없이 키워드만 있는 리뷰를 허용하므로 내용(`content`) 필드 NULL 허용
    - 리뷰는 항상 그룹에 속하지만 하위 그룹에는 속하지 않는 경우 있으므로 하위 그룹(subgroup_id) 필드 NULL 허용

### [테이블 정의]

| 컬럼명 | 데이터 타입 | NULL 허용 | Key (PK/FK/-) | UNIQUE | 기본값 / IDENTITY | ENUM / 제약 / 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | N | PK | Y | IDENTITY |  |
| restaurant_id | BIGINT | N | FK | N |  | [restaurant.id](http://restaurant.id/) |
| member_id | BIGINT | N | FK | N |  | [member.id](http://member.id/) |
| group_id | BIGINT | N | FK | N |  | [group.id](http://group.id/) |
| subgroup_id | BIGINT | Y | FK | N |  | [subgroup.id](http://subgroup.id/) |
| content | VARCHAR(1000) | Y | - | N |  |  |
| is_recommended | BOOLEAN | N | - | N |  |  |
| created_at | TIMESTAMP | N | - | N |  |  |
| updated_at | TIMESTAMP | N | - | N |  |  |
| deleted_at | TIMESTAMP | Y | - | N |  |  |

---

## review_keyword

- 테이블명 : review_keyword
- 테이블 설명 : 리뷰와 키워드 간의 매핑 정보를 저장하는 테이블
- 책임 : 리뷰에 선택된 키워드를 관리하며, 리뷰–키워드 간 Many-to-Many 관계를 표현
- 생명주기 : 생성 → 유지 → 삭제
- 삭제 정책 :
    - 리뷰 삭제 시 연관된 매핑 데이터는 연쇄 물리 삭제
    - (ON DELETE CASCADE 적용 대상)
- 주요 조회 패턴 :
    - review_id 기준 키워드 목록 조회
- 제약조건 :
    - 하나의 리뷰에는 동일 키워드를 중복 매핑할 수 없다.
    - 복합 UNIQUE (review_id, keyword_id)
- 인덱스 :
    - UNIQUE INDEX (review_id, keyword_id)
- 설계 근거 및 향후 확장성 :
    - 키워드를 독립 엔티티로 관리하고 리뷰와는 매핑 테이블로 연결하여
    키워드 기반 검색, 필터링, 통계 및 추천 기능 확장을 고려한다.

### [테이블 정의]

| 컬럼명 | 데이터 타입 | NULL 허용 | Key (PK/FK/-) | UNIQUE | 기본값 / IDENTITY | ENUM / 제약 / 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | N | PK | Y | IDENTITY |  |
| review_id | BIGINT | N | FK | N |  | [review.id](http://review.id/) 참조 |
| keyword_id | BIGINT | N | FK | N |  | [keyword.id](http://keyword.id/) 참조 |

---

## keyword

- 테이블명 : keyword
- 테이블 설명 : 리뷰에 사용되는 유형별 키워드를 관리하는 테이블
- 책임 : 리뷰에서 선택 가능한 키워드의 사전 데이터 관리 (키워드 분류(type)와 키워드 명 관리)
- 생명주기 : 생성 → 영구 존속
- 삭제 정책 : 물리 삭제 없음
- 주요 조회 패턴 :
    - 전체 키워드 리스트 조회
    - type 기준 키워드 목록 조회 (UI 선택형 키워드 구성)
- 추가 제약 :
    - 동일한 type 내에서 동일한 키워드 명은 중복될 수 없다.
    - 키워드 명은 빈 문자열을 허용하지 않는다.
    - 복합 UNIQUE (type, name)
- 인덱스 :
    - UNIQUE INDEX (type, name)
- 설계 근거 및 향후 확장성 :
    - 같은 키워드가 여러 리뷰에서 반복 사용되며, 키워드를 독립 엔티티로 관리함으로써
    검색, 필터링, 통계 및 추천 기능 확장을 고려한다.

### [테이블 정의]

| 컬럼명 | 데이터 타입 | NULL 허용 | Key (PK/FK/-) | UNIQUE | 기본값 / IDENTITY | ENUM / 제약 / 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | N | PK | Y | IDENTITY |  |
| type | VARCHAR(50) | N | - | N |  | ENUM('VISIT_PURPOSE', 'COMPANION_TYPE', 'WAITING_EXPERIENCE', 'POSITIVE_ASPECT') |
| name | VARCHAR(200) | N | - | N |  | 키워드 명 (빈 문자열 불가) |

---

## review_image

- 테이블명 : review_image
- 테이블 설명 : 리뷰 이미지 정보를 저장하는 테이블
- 책임 : 리뷰 이미지 URL 관리
- 생명주기 : 생성 → 유지 → 삭제
- 삭제 정책 : 리뷰 삭제 정책에 따라 연쇄 물리 삭제
- 주요 조회 패턴 : review_id 기반 이미지 목록 조회
- 제약조건 : -
- 인덱스 : INDEX (review_id)
- 설계 근거 및 향후 확장성 : 이미지 정렬/대표 여부 등 확장을 위해 별도 테이블로 분리한다.

### [테이블 정의]

| 컬럼명 | 데이터 타입 | NULL 허용 | Key (PK/FK/-) | UNIQUE | 기본값 / IDENTITY | ENUM / 제약 / 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | N | PK | Y | IDENTITY |  |
| review_id | BIGINT | N | FK | N |  | [review.id](http://review.id/) 참조 |
| image_url | VARCHAR(500) | N | - | N |  | 빈 문자열 불가 |
| created_at | TIMESTAMP | N | - | N | CURRENT_TIMESTAMP |  |

---

## chat_room

- **테이블명** : chat_room
- **테이블 설명** : 하위 그룹(subgroup) 단위로 생성되는 채팅방 정보를 관리하는 테이블
- **책임** : 채팅방의 식별자 및 소속 하위 그룹 관리
- **생명주기** : 생성 → 유지 → 소프트 삭제
- **삭제 정책** : `deleted_at` 기반 소프트 삭제
- **주요 조회 패턴** :
    - subgroup_id 기준 채팅방 조회
    - member 기준 참여 중인 채팅방 조회 (chat_room_member 조인)
- **제약조건** :
    - UNIQUE (subgroup_id)
    - 하나의 하위 그룹(subgroup)에 하나의 채팅방만 존재
- **인덱스** :
    - PARTICAL INDEX(subgroup_id, deleted_at = NULL)
- **설계 근거 및 향후 확장성** :
    - 채팅방을 하위 그룹 단위로 1:1 매핑하여 도메인 구조를 단순화
    - 채팅방 자체에는 최소한의 메타 정보만 유지하여 메시지 및 참여자 관리 책임을 분리
    - 소프트 삭제를 통해 채팅방 비활성화 및 이력 유지 가능
    - 향후 채팅방 이름, 유형, 설정 정보 등을 확장 컬럼으로 추가 가능

---

### [테이블 정의]

| 컬럼명 | 데이터 타입 | NULL 허용 | Key (PK/FK/-) | UNIQUE | 기본값 / IDENTITY | ENUM / 제약 / 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | N | PK | Y | IDENTITY | SEQUENCE |
| subgroup_id | BIGINT | N | FK | Y |  | subgroup.id |
| created_at | TIMESTAMP | N | - | N |  |  |
| deleted_at | TIMESTAMP | Y | - | N |  | 채팅방 비활성화 |

---

## chat_message

- **테이블명** : chat_message
- **테이블 설명** : 채팅방 내에서 전송되는 메시지 정보를 관리하는 테이블
- **책임** : 채팅 메시지의 내용, 유형, 작성자 및 삭제 상태 관리
- **생명주기** : 생성 → 유지 → 소프트 삭제
- **삭제 정책** : 소프트 삭제
- **주요 조회 패턴** :
    - chat_room_id 기준 메시지 목록 조회 (생성일 기준 정렬)
    - chat_room_id + deleted_at 조건 기반 활성 메시지 조회
- **제약조건** : -
- **인덱스** : PARTICAL INDEX(chat_room_id, created_at DESC, deleted_at = NULL)
- **설계 근거 및 향후 확장성** :
    - 메시지 유형 type을 분리하여 텍스트, 파일, 시스템 메시지를 유연하게 확장 가능
    - 메시지 리액션, 채팅방 추가기능(투표) 등 부가 기능 확장을 고려한 기본 구조
    - 시스템 메시지의 경우 작성자(member_id)가 없을 수 있으므로 NULL 허용
    - 소프트 삭제를 통해 메시지 복구 및 이력 관리 가능
    - type = FILE 인 경우 content는 사용하지 않는다
    - FILE 메시지의 실제 데이터는 chat_message_file 테이블을 통해 관리한다
    - 쓰기 작업이 많고 강한 정합성이 요구되지 않는 데이터이므로 데이터 분산 저장에 용이한 NoSQL 사용을 고려한다.
    

---

### [테이블 정의]

| 컬럼명 | 데이터 타입 | NULL 허용 | Key (PK/FK/-) | UNIQUE | 기본값 / IDENTITY | ENUM / 제약 / 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | N | PK | Y | IDENTITY | SEQUENCE |
| chat_room_id | BIGINT | N | FK | N |  | chat_room.id |
| member_id | BIGINT | Y | FK | N |  | SYSTEM 메시지의 경우 NULL |
| type | VARCHAR(20) | N | - | N |  | ENUM(’TEXT’, ‘FILE’, ‘SYSTEM’) |
| content | VARCHAR(500) | Y | - | N |  |  |
| created_at | TIMESTAMP | N | - | N |  |  |
| deleted_at | TIMESTAMP | Y | - | N |  |  |

---

## chat_room_member

- **테이블명** : chat_room_member
- **테이블 설명** : 채팅방과 회원 간의 참여 관계 및 읽음 상태를 관리하는 테이블
- **책임** :
    - 채팅방 참여자 관리
    - 참여자의 마지막 읽은 메시지 위치 관리
    - 채팅방 탈퇴(소프트 삭제) 상태 관리
- **생명주기** : 생성 → 유지/수정 → 소프트 삭제
- **삭제 정책** : 소프트 삭제
- **주요 조회 패턴** :
    - chat_room_id 기준 참여자 목록 조회
    - member_id 기준 참여 중인 채팅방 목록 조회
    - 채팅방 입장 시 마지막 읽은 메시지 조회
- **제약조건 :**
    - UNIQUE (chat_room_id, member_id)
    - 하나의 채팅방에 동일 회원은 1회만 참여 가능 (재참여시 기존 데이터 사용)
- **인덱스** :
    - PARTICAL INDEX(chat_room_id, deleted_at = NULL)
    - PARTICAL INDEX(member_id, deleted_at = NULL)
- **설계 근거 및 향후 확장성** :
    - 채팅방과 회원의 다대다 관계를 표현하기 위한 매핑 테이블
    - 마지막 읽은 메시지 last_read_message_id를 저장하여 안 읽은 메시지 계산을 효율화
    - 소프트 삭제를 통해 채팅방 나가기/복귀 시 이력 유지 가능
    - last_read_message_id는 논리적 참조를 의미하며, 메시지 물리 삭제 정책 도입 시 FK 유지 여부를 재검토한다

---

### [테이블 정의]

| 컬럼명 | 데이터 타입 | NULL 허용 | Key (PK/FK/-) | UNIQUE | 기본값 / IDENTITY | ENUM / 제약 / 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | N | PK | Y | IDENTITY | SEQUENCE |
| member_id | BIGINT | N | FK | N |  | member.id |
| chat_room_id | BIGINT | N | FK | N |  | chat_room.id |
| last_read_message_id | BIGINT | Y | - | N |  |  |
| created_at | TIMESTAMP | N | - | N |  | 참여 시점 |
| updated_at | TIMESTAMP | N | - | N |  | 읽음 위치 변경 시 갱신 |
| deleted_at | TIMESTAMP | Y | - | N |  | 채팅방 나가기 |

---

## chat_message_file

- **테이블명** : chat_message_file
- **테이블 설명** : 채팅 메시지에 첨부된 파일 정보를 관리하는 테이블
- **책임** :
    - 파일 메시지에 대한 메타데이터 관리
    - 파일 유형 및 접근 URL 관리
- **생명주기** : 생성 → 유지 → 소프트 삭제
- **삭제 정책** : 소프트 삭제
- **주요 조회 패턴** :
    - chat_message_id 기준 파일 정보 조회
- **제약조건** :
    - UNIQUE (chat_message_id)
    - 파일 유형은 사전에 정의된 ENUM 값만 허용
- **인덱스** :
    - INDEX (chat_message_id, deleted_at)
- **설계 근거 및 향후 확장성** :
    - 파일 메시지를 일반 메시지 chat_messaage와 분리하여 도메인 책임을 명확히 함
    - 메시지 유형(FILE)별 상세 정보를 별도 테이블로 관리하여 구조 단순화
    - 파일 유형 file_type 확장을 통해 이미지, 동영상, 문서 등 다양한 첨부 파일 지원 가능
    - 파일 삭제 시 메시지 본문과 독립적으로 이력 관리 가능
    - 쓰기 작업이 많고 강한 정합성이 요구되지 않는 데이터이므로 데이터 분산 저장에 용이한 NoSQL 사용을 고려한다.

---

### [테이블 정의]

| 컬럼명 | 데이터 타입 | NULL 허용 | Key (PK/FK/-) | UNIQUE | 기본값 / IDENTITY | ENUM / 제약 / 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | N | PK | Y | IDENTITY | SEQUENCE |
| chat_message_id | BIGINT | N | FK | Y |  | chat_message.id |
| file_type | VARCHAR(20) | N | - | N |  | ENUM(’IMAGE’) |
| file_url | VARCHAR(500) | N | - | N |  | 빈 문자열 불가 |
| created_at | TIMESTAMP | N | - | N |  |  |
| deleted_at | TIMESTAMP | Y | - | N |  |  |

---

## member_notification_preference

- 테이블명 : member_notification_preference
- 테이블 설명 : 회원의 알림 수신 설정을 저장하는 테이블
- 책임 : 알림 채널/유형별 수신 설정 관리
- 생명주기 : 생성 → 갱신
- 삭제 정책 : 기본은 물리 삭제 없음, member 물리 삭제 시 CASCADE로 물리 삭제
- 주요 조회 패턴 : member_id 기반 설정 조회
- 제약조건 : 복합 UNIQUE(member_id, channel, category), category 빈 문자열 불가
- 인덱스 : INDEX (member_id)
- 설계 근거 및 향후 확장성 :
    - 알림 유형 추가 시 컬럼 확장 또는 상세 테이블 분리 가능하다.
    - member와 생명주기를 같이한다.

### [테이블 정의]

| 컬럼명 | 데이터 타입 | NULL 허용 | Key (PK/FK/-) | UNIQUE | 기본값 / IDENTITY | ENUM / 제약 / 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | N | PK | Y | IDENTITY |  |
| member_id | BIGINT | N | FK | Y |  | [member.id](http://member.id/) 참조 |
| channel | VARCHAR(20) | N | - | N |  | ENUM('PUSH', 'EMAIL', 'SMS', 'WEB') |
| service_enabled | VARCHAR(20) | N | - | N |  | ENUM('CHAT', 'SYSTEM', 'NOTICE') |
| is_enabled | BOOLEAN | N | - | FALSE |  |  |
| created_at | TIMESTAMP | N | - | N | CURRENT_TIMESTAMP |  |
| updated_at | TIMESTAMP | N | - | N | CURRENT_TIMESTAMP |  |

---

## push_notification_target

- 테이블명 : push_notification_target
- 테이블 설명 : 푸시 알림 발송을 위해 사용자 단말의 FCM 토큰 정보를 저장하는 테이블
- 책임 :
    - 사용자(`member`)와 푸시 알림 수신 단말 간의 연결 정보 관리
    - 푸시 알림 발송 대상 식별
- 생명주기 : 생성 → 삭제
- 삭제 정책 :
    - 로그아웃, 토큰 만료, 푸시 전송 실패 등의 사유로 물리 삭제
    - member 물리 삭제 시 CASCADE로 물리 삭제
- 주요 조회 패턴 :
    - 특정 사용자(`member_id`)의 푸시 알림 발송 대상 조회
- 제약조건 :
    - FCM 토큰은 중복 저장되지 않아야 한다
- 인덱스
    - INDEX(member_id)
- 설계 근거 및 향후 확장성 :
    - 푸시 알림은 단말 단위로 발송되므로 사용자–단말 관계를 별도 테이블로 관리한다
    - 향후 단말 유형(Android/iOS/Web), 앱 버전, 마지막 사용 시각 등의 메타 정보를 추가할 수 있다

### [테이블 정의]

| **컬럼명** | **데이터 타입** | **NULL 허용** | **Key (PK/FK/-)** | **UNIQUE** | **기본값 / IDENTITY** | **ENUM / 제약 / 비고** |
| --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | N | PK | Y | IDENTITY | SEQUENCE |
| member_id | BIGINT | N | FK | N |  | member.id 참조 |
| fcm_token | VARCHAR(255) | N | - | Y |  | 푸시 알림 수신 토큰 |
| created_at | TIMESTAMP | N | - | N |  | 최초 등록 시각 |

---

## notification

- 테이블명 : notification
- 테이블 설명 : 사용자에게 전달되는 인앱 알림
- 책임 : 알림 생성 및 읽음 처리
- 생명주기 : 생성 → 유지
- 삭제 정책 : 물리 삭제 가능, member 물리 삭제 시 CASCADE로 물리 삭제
- 주요 조회 패턴 : member_id 기준 최신 읽지 않은 알림 조회
- 제약조건 : -
- 인덱스 : PARTICAL INDEX(member_id, created_at DESC, read_at = NULL) INCLUDE(title)
- 설계 근거 및 향후 확장성 :
    - 쓰기 작업이 많고 강한 정합성이 요구되지 않는 데이터이므로 데이터 분산 저장에 용이한 NoSQL 사용을 고려한다.

### [테이블 정의]

| 컬럼명 | 데이터 타입 | NULL | Key | UNIQUE | 기본값 | 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | N | PK | Y | IDENTITY |  |
| member_id | BIGINT | N | FK | N |  | member.id |
| type | VARCHAR(20) | N | - | N |  | ENUM(’CHAT’, ‘SYSTEM’, ‘NOTICE’) |
| title | VARCHAR(120) | N | - | N |  | 빈 문자열 불가 |
| body | VARCHAR(500) | N | - | N |  | 빈 문자열 불가 |
| deep_link | VARCHAR(500) | Y | - | N |  | 연관 페이지 링크 |
| created_at | TIMESTAMP | N | - | N |  |  |
| read_at | TIMESTAMP | Y | - | N |  | 읽음 시각 |

---

## ai_restaurant_recommendation

- 테이블명 : ai_restaurant_recommendation
- 테이블 설명 : AI 분석을 통해 생성된 음식점 추천 결과를 배치 단위로 누적 저장하는 테이블
- 책임 : 메인 화면 또는 추천 영역에 노출되는 음식점 추천 결과와 추천 사유 제공
- 생명주기 : 생성 → 삭제
- 삭제 정책 :
    - 음식점(restaurant) 물리 삭제 시 연쇄 물리 삭제
    - 생성 일시(created_at)을 기준으로 보존 기간(1주) 초과 시 배치 작업을 통해 물리 삭제
- 주요 조회 패턴 : 최신 추천 목록 조회
- 제약조건 : -
- 인덱스 : INDEX (created_at)
- 설계 근거 및 향후 확장성 :
    - 최신성이 중요한 데이터이므로 오래된 데이터는 배치 작업을 통해 순차적으로 삭제한다.

### [테이블 정의]

| 컬럼명 | 데이터 타입 | NULL 허용 | Key (PK/FK/-) | UNIQUE | 기본값 / IDENTITY | ENUM / 제약 / 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | N | PK | Y | IDENTITY |  |
| restaurant_id | BIGINT | N | FK | N |  | [restaurant.id](http://restaurant.id/) 참조 |
| reason | VARCHAR(1000) | N | - | N |  | 빈 문자열 불가 |
| created_at | TIMESTAMP | N | - | N | CURRENT_TIMESTAMP |  |

---

## ai_restaurant_review_analysis

- 테이블명 : ai_restaurant_review_analysis
- 테이블 설명 : AI가 음식점의 리뷰 데이터를 분석하여 생성한 요약 및 긍정 비율 정보를 저장하는 테이블
- 책임 : 음식점 단위의 리뷰 분석 결과 제공
- 생명주기 : 생성 → 갱신 → 삭제
- 삭제 정책 : 음식점 물리 삭제 시 연쇄 물리 삭제
- 주요 조회 패턴 : 음식점(restaurant_id) 기준 최신 리뷰 분석 결과 및 긍정 비율 조회
- 제약조건 : 하나의 음식점에 대해 하나의 리뷰 분석 레코드가 존재할 수 있다
- 인덱스: -
- 설계 근거 및 향후 확장성 :
    - 향후 키워드 통계 등 추가 분석 결과 컬럼 확장이 가능하다

### [테이블 정의]

| **컬럼명** | **데이터 타입** | **NULL 허용** | **Key (PK/FK/-)** | **UNIQUE** | **기본값 / IDENTITY** | **ENUM / 제약 / 비고** |
| --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | N | PK | Y | IDENTITY | SEQUENCE |
| restaurant_id | BIGINT | N | FK | Y |  | restaurant.id 참조 |
| summary | VARCHAR(500) | N | - | N |  | 빈 문자열 불가 |
| positive_review_ratio | DECIMAL(3,2) | N | - | N |  | 긍정 리뷰 비율 (0.00 ~ 1.00) |
| created_at | TIMESTAMP | N | - | N |  | 최초 생성 시각 |
| updated_at | TIMESTAMP | N | - | N |  | 마지막 분석 갱신 시각 |

---

## ai_restaurant_feature

- 테이블명 : ai_restaurant_feature
- 테이블 설명 : AI 분석을 통해 도출된 음식점의 특징을 설명하는 글을 저장하는 테이블
- 책임 : 음식점의 주요 특징 정보를 1-2 문장의 글로 제공
- 생명주기 : 생성 → 갱신 → 삭제
- 삭제 정책 : 음식점 물리 삭제 시 연쇄 물리 삭제
- 주요 조회 패턴 : 음식점(restaurant_id) 기준 음식점의 최신 특징 정보 조회
- 제약조건 : 하나의 음식점에 대해 하나의 특징 레코드가 존재할 수 있다
- 설계 근거 및 향후 확장성 :
    - 향후 특징 유형(type), 중요도(score), 노출 우선순위 등의 컬럼을 추가하여 확장할 수 있다

### [테이블 정의]

| **컬럼명** | **데이터 타입** | **NULL 허용** | **Key (PK/FK/-)** | **UNIQUE** | **기본값 / IDENTITY** | **ENUM / 제약 / 비고** |
| --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | N | PK | Y | IDENTITY | SEQUENCE |
| restaurant_id | BIGINT | N | FK | Y |  | restaurant.id 참조 |
| content | VARCHAR(500) | N | - | N |  | 빈 문자열 불가 |
| created_at | TIMESTAMP | N | - | N |  | 최초 생성 시각 |
| updated_at | TIMESTAMP | N | - | N |  | 마지막 갱신 시각 |

---

## batch_execution

- 테이블명 : batch_execution
- 테이블 설명 : 배치 작업의 실행 이력(성공/실패, 실행 시간, 오류 정보)을 저장하는 테이블
- 책임 :
    - 배치 작업 실행 결과 추적
    - 장애 분석 및 운영 모니터링을 위한 근거 데이터 제공
- 생명주기 : 생성 → 수정 → 삭제
- 삭제 정책 : 보관 기간 정책에 따라 물리 삭제 가능
- 주요 조회 패턴 :
    - 특정 배치 작업(job_name)의 최신 실행 이력 조회
    - 배치 실패 발생 시 최근 실패 이력 조회
- 제약조건 :
    - 동일한 실행 단위를 식별하기 위해 `job_name + execution_key` 조합은 중복되지 않아야 한다
- 인덱스 : -
- 설계 근거 및 향후 확장성 :
    - execution_key는 배치 작업 식별자로, 동일한 배치가 중복 실행되는 것을 방지하는 데 사용된다
    - 배치 실행 결과를 상태(state)로 명확히 구분하여 운영자가 즉시 성공/실패 여부를 판단할 수 있다
    - 오류 메시지를 별도 컬럼으로 저장하여 로그 접근 없이도 1차 원인 파악이 가능하다
    - 향후 실행 노드, 재시도 횟수, 트리거 유형(수동/스케줄) 등의 메타 정보 확장이 가능하다
    - 관리자를 위한 운영용 이력 테이블이므로 조회 빈도가 낮아 인덱스를 추가하지 않는다

### [테이블 정의]

| **컬럼명** | **데이터 타입** | **NULL 허용** | **Key (PK/FK/-)** | **UNIQUE** | **기본값 / IDENTITY** | **ENUM / 제약 / 비고** |
| --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | N | PK | Y | IDENTITY | SEQUENCE |
| job_name | VARCHAR(100) | N | - | N |  | 배치 작업 식별자 |
| execution_key | VARCHAR(100) | N | - | N |  | 실행 단위 키 (예: yyyy-mm-dd-hh) |
| status | VARCHAR(20) | N | - | N |  | ENUM: RUNNING, SUCCESS, FAILED |
| error_message | VARCHAR(1000) | Y | - | N |  | 실패 사유 |
| started_at | TIMESTAMP | N | - | N |  | 배치 실행 시작 시각 |
| finished_at | TIMESTAMP | Y | - | N |  | 배치 실행 종료 시각 |

---

## refresh_token_blacklist

- 테이블명 : refresh_token_blacklist
- 테이블 설명 : 로그아웃, 토큰 재발급, 보안 정책 등에 의해 더 이상 사용할 수 없는 Refresh Token을 관리하는 테이블
- 책임 :
    - 무효화된 Refresh Token의 재사용 방지
    - 서버 측에서 Refresh Token의 강제 차단 근거 제공
- 생명주기 : 생성 → 만료 → 삭제
- 삭제 정책 : 토큰의 만료 시점(`expires_at`) 이후에는 배치 작업을 통해 물리 삭제
- 주요 조회 패턴 :
    - Refresh Token으로 재발급 요청 시, 해당 토큰이 블랙리스트에 존재하는지 여부 확인
    - expires_at 기준으로 삭제할 데이터 조회
- 제약조건 : 동일한 Refresh Token은 중복 등록되지 않아야 한다
- 인덱스: PARTICAL INDEX(expires_at = NULL)
- 설계 근거 및 향후 확장성 :
    - 블랙리스트 테이블을 통해 서버 주도의 보안 통제를 가능하게 한다
    - 만료 시점을 명시적으로 관리하여 불필요한 데이터 축적을 방지한다

### [테이블 정의]

| **컬럼명** | **데이터 타입** | **NULL 허용** | **Key (PK/FK/-)** | **UNIQUE** | **기본값 / IDENTITY** | **ENUM / 제약 / 비고** |
| --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | N | PK | Y | IDENTITY | SEQUENCE |
| refresh_token | VARCHAR(255) | N | - | N |  | 무효화된 Refresh Token |
| expires_at | TIMESTAMP | N | - | N |  | 토큰 만료 시각 |
| created_at | TIMESTAMP | N | - | N |  | 블랙리스트 등록 시각 |

---

## 변경 이력

| 버전 | 일자 | 작성자 | 비고 |
| --- | --- | --- | --- |
| v1.0 | 2026.01.06 | devon(우승화) - Backend | 문서 초안 작성 |
| v1.1 | 2026.01.06 | devon(우승화) - Backend | 리뷰 및 리뷰 키워드 N:M 관계로 수정 |
| v1.2 | 2026.01.08 | Backend Team | 수정된 ERD에 맞춰서 동기화 |

