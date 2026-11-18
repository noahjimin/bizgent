# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트

BizGent — 소상공인용 AI 디지털 마케팅 에이전트 (정보시스템 프로젝트 캡스톤 디자인, 팀 사계절). 분석·설계 산출물은 `01_기획/`(PROJECT.md, 기능 ID F01~F19), `02_요구사항분석/`(Use Case), `03_화면설계/`(화면설계서 PDF·PPTX), `04_DB설계/`(ERD)에 있다. 화면·기능을 바꿀 때는 화면설계서의 Screen ID(SCR-U-xxx)와 Description 번호, Use Case 명칭에 맞춘다.

현재 단계: Oracle DB(교수님 제공 서버) 연동을 마쳤다. 화면 데이터는 모두 DB(MyBatis)에서 읽고 쓴다. 남은 단계는 실제 AI API 연결이다.

## 팀 작업 규칙 (반드시 지킬 것)

- **기준 문서 우선**: 기능·화면·명칭은 `01_기획/사계절_PROJECT.md`와 `03_화면설계/사계절_화면설계서.pdf`를 기준으로 한다. 문서와 코드가 다르면 임의로 정하지 말고 차이를 보고한 뒤 지시를 기다린다.
- **확정되지 않은 사항을 확정된 것처럼 처리하지 않는다**: PROJECT.md에서 "검토 중"·"확인 필요"로 표시된 항목(예: 구독 시 채널 한도)은 임의로 값을 정하지 말고 먼저 물어본다.
- **요청하지 않은 파일은 수정하지 않는다**: 특히 `README.md`, `01_기획/`~`04_DB설계/`의 문서, `.gitignore`는 명시적으로 요청받았을 때만 수정한다.
- **목업 단계 원칙**: AI 기능은 실제 API를 호출하지 않고 `MockAiService`로 처리한다. 실제 API 연동은 요청이 있을 때만 한다.
- **비밀 정보 금지**: DB 비밀번호, API 키 등은 코드나 `application.yml`에 직접 쓰지 않는다. gitignore된 `application-local.yml` 또는 환경변수를 쓴다.
- **UI 텍스트는 모두 한글**로 작성한다.
- **작업 단위**: 한 번에 화면 하나 또는 기능 하나만 수정한다. 여러 파일을 크게 바꾸는 작업은 수정 전에 계획을 먼저 보여 준다.
- **완료 보고**: 작업이 끝나면 수정한 파일 목록, 바뀐 내용, 브라우저에서 확인하는 방법을 알려 준다.
- **승인 전 한국어 설명**: 파일 수정이나 명령 실행처럼 사용자 승인이 필요한 작업은, 승인 요청 전에 무엇을 왜 하는지 한국어로 한두 문장 먼저 설명한다. 사용자에게 묻는 질문도 모두 한국어로 한다.

## 명령어

JDK 17이 필요하다. 이 PC의 기본 PATH는 Java 8이므로 먼저 JAVA_HOME을 지정한다.

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-17"   # PowerShell: $env:JAVA_HOME="C:\Program Files\Java\jdk-17"
./gradlew bootRun                     # http://localhost:8080 (시연 계정 demo@bizgent.kr / BizGentDemo26!)
./gradlew build                       # 컴파일 + 테스트
./gradlew test --tests com.bizgent.BizgentApplicationTests   # 단일 테스트
```

- 서비스 단위 테스트는 매퍼를 Mockito로 대신한다. `contextLoads`와 `CampaignDeleteCascadeTest`는 실제 DB에 접속하므로 `application-local.yml`이 있어야 한다(삭제 확인 테스트는 롤백된다). 화면 검증은 서버를 띄우고 HTTP 요청(로그인 쿠키 유지)으로 확인한다.
- `bootRun`은 `sourceResources`로 설정되어 있어 템플릿·정적 파일은 저장 후 새로고침만 하면 반영된다. Java 변경은 재시작해야 한다.
- curl로 한글 JSON을 보낼 때는 UTF-8 파일을 `--data-binary @file`로 보낸다 (셸 인코딩 때문에 400이 난다).

## 구조

Spring Boot 4.0 + Thymeleaf. Figma Make의 React 프로토타입을 Thymeleaf로 옮긴 서버 렌더링 앱이다. DB는 Oracle 11g XE(교수님 제공 서버)이고, 데이터 접근은 MyBatis로 확정했다(PROJECT.md v0.07). 테이블·컬럼 이름은 `04_DB설계/sql/01~03`(테이블 25·시퀀스 15)을 그대로 따른다(접두어 없이 `MEMBERS`, `CAMPAIGN` 등). 11g라 IDENTITY 컬럼과 FETCH FIRST를 쓸 수 없으므로, PK는 `<selectKey order="BEFORE">SELECT SEQ_xxx.NEXTVAL FROM DUAL`로 만들고 행 수 제한은 ROWNUM으로 작성한다. Oracle은 빈 문자열을 NULL로 저장하므로 NOT NULL 컬럼에 빈 값이 들어가지 않게 서비스에서 막는다.

- **프로필**: 기본은 `db` 프로필(`application-db.yml`: 접속 주소)이고, 계정·비밀번호는 gitignore된 `application-local.yml`에만 쓴다(`application-local.yml.example` 참고). `application.yml`의 `jdbc-type-for-null`은 `'NULL'`처럼 따옴표가 필요하다(따옴표 없으면 YAML null).
- **데이터 계층**: 매퍼 인터페이스는 `mapper/`(행 클래스는 `mapper/row/`), SQL은 `resources/mapper/*.xml`. 컨트롤러는 `service/`만 쓴다. `service/RequestData`(@RequestScope)가 로그인 회원의 회원·매장·캠페인·매출·알림을 요청 하나 동안만 기억하고, 저장 후에는 `clear()`한다. 캠페인은 `CampaignRepository`가 여러 테이블(CAMPAIGN_PRODUCT·CHANNEL, AI_PLAN 최신 버전, IMAGE_SETTING, DRAFT·HASHTAG, POST·METRIC)을 묶어 읽고 `save()`로 다시 쓴다. 목표·채널은 화면에서 한글 이름, DB에서는 코드라 `CodeService`가 바꾼다(목록에 없는 목표 문구는 `ETC` + CUSTOM_GOAL). `service/UserSession`(@SessionScope)은 로그인 회원 ID·작성 중 캠페인 ID 같은 세션 상태만 들고 있다.
- **도메인**: `domain/`은 ERD 테이블 기준이다. `Campaign`이 기획(AiPlan)·콘텐츠/이미지 설정·시안(Draft)·게시(Post)를 한 객체로 묶는다.
- **AI**: `service/ai/AiService` 인터페이스와 `MockAiService` 구현. 실제 LLM·이미지 API는 이 인터페이스의 새 구현으로 붙인다.
- **AI 캠페인 개선 방향** (`InsightService`): 최근 게시한 캠페인 5개 중 게시 전후 매출 데이터가 있는 캠페인이 4개 이상이면 Good/Bad 비교를, 4개 미만이면 초기 분석을 보여 준다. Good/Bad 판정 기준은 대상 메뉴 매출 증가율(게시 전 7일 대비 게시 후 7일)이다. 분석 결과는 INSIGHT·INSIGHT_ITEM·INSIGHT_DIFF·INSIGHT_CAMPAIGN에 저장하고(Good/Bad 반응 합계·선정 이유 포함), 마지막 분석 이후 새 매출 업로드가 있을 때만(BASE_UPLOAD_ID 비교) 새로 분석한다.
- **게시물 반응**(좋아요·댓글·공유·저장·도달): 게시할 때 POST_METRIC에 `COLLECT_METHOD='SAMPLE'`로 저장하는 예시 데이터이고, 화면에 "예시 데이터"라고 표시한다. 인스타그램 연동(F12)은 추후 계획이다. 성과 입력 알림은 NOTIFICATION(유형 `CAMPAIGN_RESULT`, DUE_AT = 게시 7일 뒤).
- **플랜 규칙**: 한도는 PRICE_PLAN 테이블 한 곳에서 읽는다(`domain/PricePlan`, PROJECT.md v0.08, 크레딧 없음). 현재 요금제는 SUBSCRIPTION의 ACTIVE 행(없으면 FREE). 무료는 월 4회 캠페인·0원, 구독은 월 8회 캠페인·월 9,900원이고, 무료·구독 모두 캠페인 시작 시 1회 차감하고 한도를 검사한다. 채널은 무료·구독 모두 최대 2개, 시안은 모두 A/B 2종. 매장 대표 이미지 AI 생성은 무료 월 1회·구독 월 2회(한 번 만들기 = 3장 = 1회, "다시 만들기"도 1회)이고 `StoreService.generateHeroImages`가 검사한다(넘으면 403과 안내 문구). 3장을 같은 CREATED_AT으로 STORE_IMAGE에 남기고, 월 횟수는 생성 시각 묶음 수로 센다. 구독은 재분석·문구 대안·이미지 수정·참고 이미지를 쓸 수 있고 따로 차감하지 않는다. 횟수를 다 쓰고 새 캠페인을 누르면 `upgradeNotice` 팝업이 뜬다. 무료는 구독 안내, 구독은 소진 안내와 "구독 관리로 이동" 버튼을 보여 준다. 화면 문구의 한도 숫자는 `me`(로그인 후) 또는 `freePlan`/`proPlan`(랜딩·약관) 모델 속성에서 읽는다. 구독 전용 API는 서버에서도 검사하고 `ForbiddenException`(403)을 던진다. 화면의 잠금 버튼은 `data-upgrade-feature-name`으로 구독 안내를 띄운다. 캠페인 이용 횟수는 MONTHLY_USAGE(회원+YYYYMM)에 +1하고 CAMPAIGN.USAGE_MONTH에 차감한 달을 남긴다. 달이 바뀌면 새 행이라 매월 1일 초기화와 같다.
- **로그인**: 비밀번호는 BCrypt(`spring-security-crypto`만 사용)로 저장·검사하고, 로그인 회원은 세션에 memberId로 둔다. 탈퇴(MEMBER_STATUS='WITHDRAWN', WITHDRAWN_AT)한 회원은 로그인할 수 없다. `config/LoginCheckInterceptor`가 로그인 전 화면(/landing, /login, /signup)과 나머지를 나누고, `/dev/*`는 시연 계정이 아니면 403으로 막는다. 가입 시 STORE도 함께 만든다(회원당 1개).
- **회원가입**: 랜딩의 시작 버튼(`data-signup`)은 모두 `/login`으로 가고(대표 이미지 버튼은 `?intent=heroImage`, 세션에 기억해 가입 후에도 적용), 계정이 없으면 로그인 화면의 회원가입 링크로 `/signup`에 간다. 별도 약관 동의 카드는 없고, 가입 화면의 동의 체크란 하나(`agreeTerms`)로 받아 `termsAgreedAt`에 기록한다. 이메일 칸 blur 시 `GET /api/auth/email-check`(로그인 검사 제외), 비밀번호 규칙(8~20자, 영문·숫자 포함)은 `MemberService.passwordProblem`과 화면 JS가 같게 검사한다.

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
- 화면용 헬퍼 빈: `@fmt`(날짜·금액·채널 라벨), `@icons`(SVG path). `GlobalModelAdvice`가 `freePlan`/`proPlan`/`demoEmail`/`demoPassword`(로그인 전에도)와 `me`, `store`, `isDemo`, `insightsView`(로그인 후)를 모든 화면에 넣는다.

### Thymeleaf 주의점 (실제로 겪은 오류)

- `th:src`/`th:value`/`th:attr`/`th:style` 같은 속성 처리기 안에서는 `@bean`, `T()`, `param` 사용이 막혀 있다. `th:with`로 먼저 계산하거나 Java 쪽(getter·모델 속성)에서 만든다.
- `th:replace`는 같은 태그의 `th:each`/`th:if`보다 먼저 실행된다. 반복·조건은 바깥 `th:block`에 둔다.
- 다른 템플릿에 포함되는 조각 안에서는 `~{:: x}` 대신 `~{fragments/파일 :: x}`처럼 경로를 명시한다.

## 개발용 기능

시연 계정(`bizgent.demo.email` = demo@bizgent.kr / BizGentDemo26!, 구독 상태)은 서버를 켤 때 없으면 `demo/DemoSeed`가 만들고 모먼트 카페 데이터를 DB에 넣는다. 로그인 화면의 "임시 계정" 카드가 이 계정 정보를 입력칸에 채운다(자동 로그인은 하지 않음). 화면 오른쪽 아래 "개발 바로가기"(`app-parts :: devShortcuts`, `HomeController`의 `/dev/*`)는 시연 계정에서만 보이고, 이용 횟수 소진/초기화(캠페인·매장 이미지), 무료↔구독 전환(SUBSCRIPTION 추가·해지), AI 개선 방향 데이터 양(early/enough, 바꾸면 바로 새로 분석), 시연 데이터 초기화(시연 계정 데이터만 지우고 처음 상태로)를 할 수 있다. 시연·검증용이며 화면설계서 범위 밖이다.