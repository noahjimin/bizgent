# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트

BizGent — 소상공인용 AI 디지털 마케팅 에이전트 (정보시스템 프로젝트 캡스톤 디자인, 팀 사계절). 분석·설계 산출물은 `01_기획/`(PROJECT.md, 기능 ID F01~F19), `02_요구사항분석/`(Use Case), `03_화면설계/`(화면설계서 PDF·PPTX), `04_DB설계/`(ERD)에 있다. 화면·기능을 바꿀 때는 화면설계서의 Screen ID(SCR-U-xxx)와 Description 번호, Use Case 명칭에 맞춘다.

현재 단계: 목업 단계에서 남은 화면·버튼 기능을 구현 중이다. 이후 Oracle DB 연동(교수님 제공 서버), 마지막으로 실제 AI API 연결 순서로 진행한다.

## 팀 작업 규칙 (반드시 지킬 것)

- **기준 문서 우선**: 기능·화면·명칭은 `01_기획/사계절_PROJECT.md`와 `03_화면설계/사계절_화면설계서.pdf`를 기준으로 한다. 문서와 코드가 다르면 임의로 정하지 말고 차이를 보고한 뒤 지시를 기다린다.
- **확정되지 않은 사항을 확정된 것처럼 처리하지 않는다**: PROJECT.md에서 "검토 중"·"확인 필요"로 표시된 항목(예: 구독 시 채널 한도)은 임의로 값을 정하지 말고 먼저 물어본다.
- **요청하지 않은 파일은 수정하지 않는다**: 특히 `README.md`, `01_기획/`~`04_DB설계/`의 문서, `.gitignore`는 명시적으로 요청받았을 때만 수정한다.
- **목업 단계 원칙**: AI 기능은 실제 API를 호출하지 않고 `MockAiService`로 처리한다. 실제 API 연동은 요청이 있을 때만 한다.
- **비밀 정보 금지**: DB 비밀번호, API 키 등은 코드나 `application.yml`에 직접 쓰지 않는다. gitignore된 `application-local.yml` 또는 환경변수를 쓴다.
- **UI 텍스트는 모두 한글**로 작성한다.
- **작업 단위**: 한 번에 화면 하나 또는 기능 하나만 수정한다. 여러 파일을 크게 바꾸는 작업은 수정 전에 계획을 먼저 보여 준다.
- **완료 보고**: 작업이 끝나면 수정한 파일 목록, 바뀐 내용, 브라우저에서 확인하는 방법을 알려 준다.

## 명령어

JDK 17이 필요하다. 이 PC의 기본 PATH는 Java 8이므로 먼저 JAVA_HOME을 지정한다.

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-17"   # PowerShell: $env:JAVA_HOME="C:\Program Files\Java\jdk-17"
./gradlew bootRun                     # http://localhost:8080 (아무 이메일·비밀번호로 로그인)
./gradlew build                       # 컴파일 + 테스트
./gradlew test --tests com.bizgent.BizgentApplicationTests   # 단일 테스트
```

- 테스트는 현재 `contextLoads` 하나뿐이다. 화면 검증은 서버를 띄우고 HTTP 요청(로그인 쿠키 유지)으로 확인한다.
- `bootRun`은 `sourceResources`로 설정되어 있어 템플릿·정적 파일은 저장 후 새로고침만 하면 반영된다. Java 변경은 재시작해야 한다.
- curl로 한글 JSON을 보낼 때는 UTF-8 파일을 `--data-binary @file`로 보낸다 (셸 인코딩 때문에 400이 난다).

## 구조

Spring Boot 4.0 + Thymeleaf. Figma Make의 React 프로토타입을 Thymeleaf로 옮긴 서버 렌더링 앱이다. DB는 Oracle(교수님 제공 서버)이며, 데이터 접근 방식(MyBatis 또는 JPA)은 팀에서 확정 전이다. 현재 코드의 MyBatis·ojdbc8 설정은 연동 준비용이다.

- **프로필**: 기본 `mock` 프로필은 DataSource·MyBatis 자동 설정을 끄고(`application-mock.yml`) DB 없이 동작한다. DB 연동 시 `db` 프로필 + gitignore된 `application-local.yml`을 쓴다. Boot 4에서 자동 설정 패키지는 `org.springframework.boot.jdbc.autoconfigure`다.
- **데이터 계층**: 아직 DB가 없으므로 `demo/DemoSession`(@SessionScope)이 회원·매장·캠페인·매출·알림을 들고 있고 `DemoSeed`가 시연 데이터(모먼트 카페)를 채운다. 프로토타입의 localStorage 역할이다. 컨트롤러는 DemoSession을 직접 쓰지 말고 `service/`를 거친다 — 나중에 서비스 내부만 DB 접근 코드로 바꾸기 위해서다.
- **도메인**: `domain/`은 ERD 테이블 기준이다. `Campaign`이 기획(AiPlan)·콘텐츠/이미지 설정·시안(Draft)·게시(Post)를 한 객체로 묶는다.
- **AI**: `service/ai/AiService` 인터페이스와 `MockAiService` 구현. 실제 LLM·이미지 API는 이 인터페이스의 새 구현으로 붙인다.
- **플랜 규칙**: 무료는 월 3회 캠페인(캠페인 시작 시 차감), 채널 최대 2개, 시안은 무료·구독 모두 A/B 2종. 구독은 재분석·문구 대안·이미지 수정·참고 이미지를 쓸 수 있다. 구독 시 채널 한도는 PROJECT.md상 검토 중이다. 구독 전용 API는 서버에서도 검사하고 `ForbiddenException`(403)을 던진다. 화면의 잠금 버튼은 `data-upgrade-feature-name`으로 구독 안내를 띄운다.
- **로그인**: `config/LoginCheckInterceptor`가 로그인 전 화면(/landing, /login, /signup, /terms/agree)과 나머지를 나눈다. 모의 인증이라 비밀번호를 검사하지 않는다.

### 캠페인 생성 위자드 (`WizardController`, `WizardService`)

- 단계: `1 → 2 → 3-1 → 3-2 → 4 → 5` (`Campaign.STEPS`). 3-1에서 광고 이미지를 끄면 3-2를 건너뛴다.
- 이전 단계로 돌아갈 수 없다. 각 단계 POST가 `lockedStep`을 기록하고, 잠긴 단계 GET은 다음 단계로, `getResumeStep()`보다 앞선 단계는 그 단계로 리다이렉트한다.
- 각 단계의 다음 버튼은 `data-confirm-next`를 달면 공통 확인 팝업(`wizard-parts :: confirmNext`)을 거쳐 `#step-form`을 제출한다.
- 2단계 기획·4단계 시안은 GET 화면에서 비어 있으면 JS가 `/api/wizard/plan`·`/api/wizard/drafts`를 호출한 뒤 새로고침한다.
- 새 캠페인은 반드시 `POST /campaigns/new`로 시작한다 (`[data-new-campaign]` 버튼이 숨은 폼을 제출, data-goal/product-id/channels/source로 미리 채움). 무료 한도를 넘으면 upgradeNotice 플래시로 돌아간다.

### 화면 (templates, static)

- 레이아웃: `layout/app.html`(사이드바, `layout(title, active, content, scripts)`), `layout/wizard.html`(단계 표시줄 + 2~4단계 요약 패널). 공통 조각은 `fragments/`.
- 스타일은 Tailwind v4 브라우저 빌드(`static/js/vendor`, `<style type="text/tailwindcss">@theme`)로, 프로토타입의 className 문자열을 그대로 옮겼다. 차트는 Chart.js.
- `static/js/app.js`의 `window.BG`: openModal/closeModal, toast, confirm, upgradeHint, requestSubscribe(모든 구독 버튼의 단일 진입점), postJson, downloadImage, setChip. `<form data-confirm="...">`는 제출 전 확인 팝업을 띄운다. 서버 플래시 `toast`는 자동 표시된다.
- 업로드 파일은 `uploads/`(gitignore)에 저장되고 `/uploads/**`로 서빙된다 (`FileStorage`).
- 화면용 헬퍼 빈: `@fmt`(날짜·금액·채널 라벨), `@icons`(SVG path). `GlobalModelAdvice`가 `me`, `store`, `insightsView`를 모든 화면에 넣는다.

### Thymeleaf 주의점 (실제로 겪은 오류)

- `th:src`/`th:value`/`th:attr`/`th:style` 같은 속성 처리기 안에서는 `@bean`, `T()`, `param` 사용이 막혀 있다. `th:with`로 먼저 계산하거나 Java 쪽(getter·모델 속성)에서 만든다.
- `th:replace`는 같은 태그의 `th:each`/`th:if`보다 먼저 실행된다. 반복·조건은 바깥 `th:block`에 둔다.
- 다른 템플릿에 포함되는 조각 안에서는 `~{:: x}` 대신 `~{fragments/파일 :: x}`처럼 경로를 명시한다.

## 개발용 기능

화면 오른쪽 아래 "개발 바로가기"(`app-parts :: devShortcuts`, `HomeController`의 `/dev/*`)로 무료 횟수 소진/초기화, 무료↔구독 전환(모의 결제), AI 개선 방향 데이터 양(early/enough), 시연 데이터 초기화를 할 수 있다. 시연·검증용이며 화면설계서 범위 밖이다.