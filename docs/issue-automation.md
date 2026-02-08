# GitHub 이슈 자동 생성 스크립트 사용법

## 개요
`scripts/create-issues.sh`는 템플릿(md) 파일을 읽어 에픽/서브이슈를 대량 생성하고, 라벨/이슈 타입/마일스톤/담당자/Projects v2까지 한 번에 설정하는 도구입니다. 템플릿은 `.github/issue/` 아래에 두며, 실행 후 템플릿은 `.github/issue/done/`으로 이동합니다.

## 템플릿 작성 규칙
- 위치
  - 에픽: `.github/issue/epic/*.md`
  - 서브이슈: `.github/issue/<에픽파일이름>/ *.md`
- 파일 구조 예시
  ```md
  # [BE] 어드민 페이지 로그/모니터링 개선      # 첫 줄: 제목
  IssueType: TASK                                 # 옵션: BUG | FEATURE | TASK (대소문자 무관)
  Labels: BE, 가시성, 안정성                      # 옵션, 쉼표 구분
 
  ## 배경
  ...
  ```
  서브이슈도 동일하게 `IssueType: Task`, `Labels: ...` 식으로 메타를 적습니다.
  - 참고용 기본 양식: `.github/issue/_templates/BUG.md`, `.github/issue/_templates/FEATURE.md`, `.github/issue/_templates/TASK.md`
- 본문에서 `IssueType:`/`Labels:` 줄은 스크립트가 메타로만 쓰고 본문에는 넣지 않습니다.

## 실행 전에 준비
- GitHub CLI `gh` 설치 및 로그인
  - `brew install gh`
  - `gh auth login --scopes "repo,read:org,project" --web`
- jq 설치: `brew install jq`
- 원격이 실제 이슈를 만들 GitHub 저장소를 가리키는지 확인: `git remote -v`

## 주요 옵션
```bash
scripts/create-issues.sh [options]
```

### 옵션(필드) 상세 설명
- `--issue-root PATH`
  - 템플릿을 읽을 루트 디렉터리입니다. 기본값은 `.github/issue` 입니다.
  - 이 값 아래에 `epic/`과 `<epic-name>/` 구조를 기대합니다.
- `--update-existing`
  - 동일한 제목의 이슈가 이미 존재해도 “스킵”하지 않고 업데이트합니다.
  - 업데이트 대상: 본문(`body`), 라벨(추가), 마일스톤, 담당자(추가), Issue Type(가능할 때), Projects v2 필드(가능할 때).
  - 주의: 라벨은 기본적으로 “추가”만 합니다(기존 라벨을 제거하지 않음).
- `--assignee LOGIN`
  - 이슈 담당자를 지정합니다. 예: `--assignee ImGdevel`
  - 미지정이면 할당 동작을 하지 않습니다(기본값 강제 없음).

#### 라벨 관련
- `--epic-label LABEL`
  - 에픽 이슈에 공통으로 추가할 라벨 1개입니다. 예: `EPIC`
  - 템플릿의 `Labels:`와 함께 합쳐져 적용됩니다.
- `--task-label LABEL`
  - 서브이슈(태스크)에 공통으로 추가할 라벨 1개입니다. 예: `TASK`
  - 템플릿의 `Labels:`와 함께 합쳐져 적용됩니다.
- `--area-label LABEL`
  - 모든 이슈(에픽/태스크)에 공통으로 추가할 “영역(Area) 라벨” 1개입니다. 예: `BE`, `FE`
  - 템플릿에 매번 `Labels: BE`를 적지 않고도 일괄 적용하고 싶을 때 사용합니다.

#### 마일스톤 관련
- `--milestone TITLE`
  - 모든 이슈에 적용할 마일스톤 제목입니다. 예: `V2`
  - 스크립트는 해당 제목의 마일스톤이 없으면 생성한 뒤 적용합니다.

#### Issue Type 관련
- `--task-type NAME`
  - 템플릿에 `IssueType:`가 없는 경우, 서브이슈의 타입을 이 값으로 설정하려고 시도합니다.
  - 예: `--task-type Task`
  - 템플릿에 `IssueType:`가 있으면 그 값이 우선입니다.
  - 템플릿에서 사용할 수 있는 키워드(대소문자 무관): `BUG`, `FEATURE`, `TASK` (내부적으로 `Bug`, `Feature`, `Task`로 매핑)
  - 레포에 Issue Types 기능이 꺼져 있거나 해당 타입이 없으면 “타입 설정”은 자동으로 스킵됩니다.

#### Projects(v2) 관련
- `--project-url URL`
  - 이슈를 추가할 Projects v2 URL입니다.
  - 예: `https://github.com/orgs/100-hours-a-week/projects/304`
  - 이 기능을 쓰려면 `gh` 토큰에 `project` 스코프가 필요합니다.
    - 예: `gh auth login --scopes "repo,read:org,project" --web`
- `--status-field NAME`
  - 프로젝트의 단일 선택(single select) 필드 이름입니다. 보통 `Status` 입니다.
- `--epic-status VALUE`
  - 에픽 이슈에 설정할 `Status` 옵션 이름입니다. 예: `Epic-Backlog-Todo`
- `--task-status VALUE`
  - 태스크 이슈에 설정할 `Status` 옵션 이름입니다. 예: `Sprint-Backlog-Todo`
- `--start-field NAME`
  - “시작일/시작시간”에 해당하는 프로젝트 필드 이름입니다.
  - 예: `Start date` 또는 프로젝트에서 사용하는 한글 필드명 `시작시간`
  - 지정한 필드가 프로젝트에 없으면 시작일 설정은 스킵됩니다.

## 동작 흐름
1) 템플릿 스캔
   - 에픽 템플릿(`epic/*.md`)을 읽고 제목/본문/라벨/이슈타입 추출 후 이슈 생성(또는 업데이트).
   - 대응하는 서브폴더(`<에픽파일이름>/`)의 md들을 읽어 태스크 생성 후 에픽과 sub-issue 링크.
2) 라벨·마일스톤·담당자 적용
   - 옵션 값 + 템플릿 메타 라벨을 모두 적용합니다.
   - 마일스톤은 `--milestone` 값 사용.
3) Projects v2
   - `--project-url` 지정 시 에픽/태스크를 프로젝트에 추가하고 Status 필드와 시작일 필드를 설정합니다.
4) 템플릿 이동
   - 처리 완료된 템플릿은 `.github/issue/done/` 아래 동일 구조로 이동합니다.

## 예시 실행
```bash
scripts/create-issues.sh \
  --area-label BE \
  --task-label TASK \
  --epic-label EPIC \
  --milestone V2 \
  --project-url https://github.com/orgs/100-hours-a-week/projects/304 \
  --update-existing
```

## 자주 묻는 문제
- **프로젝트 할당 실패**: 토큰에 `project` 스코프가 없으면 프로젝트 필드를 읽지 못해 스킵됩니다. `gh auth refresh --hostname github.com --scopes project` 로 확장 후 재실행.
- **시작일 필드 이름 다름**: 프로젝트에 "시작시간"처럼 다른 이름이면 `--start-field "시작시간"`으로 지정.
- **이미 존재하는 이슈가 업데이트 안 됨**: `--update-existing` 옵션을 추가하세요.

## 기본값 정책
- 스크립트는 기본값을 강제하지 않습니다. (프로젝트/마일스톤/라벨/담당자/필드는 템플릿 메타 또는 CLI 옵션으로 지정)

