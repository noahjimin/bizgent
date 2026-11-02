---
target: 로그인·회원가입, 이용약관 동의
total_score: 24
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 4
target_identity: "file:D:\\bizgent\\bizgent\\src\\main\\resources\\templates\\auth\\login.html"
target_fingerprint: "sha256:2abd9e95efaa953dec142eb366bb38f62c3e5bf6fcbd1e72d5e5a5f001129237"
target_path: "D:\\bizgent\\bizgent\\src\\main\\resources\\templates\\auth\\login.html"
timestamp: 2026-11-02T16-15-10Z
slug: src-main-resources-templates-auth-login-html
---
# 로그인·회원가입·이용약관 동의 크리틱
Method: dual-agent (A: 디자인 리뷰 · B: 검출기/헤드리스 렌더)
점수 24/40 (Acceptable)

| # | 휴리스틱 | 점수 | 핵심 |
|---|---|---|---|
| 1 | 상태 가시성 | 2 | 제출 중 표시 없음, 비활성 동의 버튼 이유 없음 |
| 2 | 현실 일치 | 3 | 무료로 시작하기 → 동의 후 '로그인' 카드 |
| 3 | 사용자 통제 | 2 | 로그인 Esc가 입력 중에도 이탈, 회원가입 ✕ 없음 |
| 4 | 일관성 | 2 | 로그인/회원가입 배경 다름, 링크만 초록 |
| 5 | 오류 예방 | 2 | 비밀번호 규칙·autocomplete 없음 |
| 6 | 인식 | 3 | 라벨 노출, 비밀번호 보기 |
| 7 | 효율 | 2 | autocomplete 없어 저장된 비밀번호 자동입력 약함 |
| 8 | 미니멀 | 3 | 로그인 배경(랜딩 사본)이 산만 |
| 9 | 오류 복구 | 2 | 중복 이메일 오류에 다음 행동 없음, aria-describedby 없음, 합니다체 |
| 10 | 도움말 | 3 | 약관·개인정보 표 정직 |

우선순위
- [P1] 동의 후 무조건 /login (AuthController:36). 명세 04/02와 일치 → 팀 보고 대상
- [P1] 모바일 깨짐: 로그인 390px에서 790px 넘침, w-[480px] 카드, body min-width 1180
- [P1] 대비: 약관 부제·더보기 #94a3b8 2.6:1, 약관 오른쪽 패널 2.5/2.2:1, 회원가입 링크 #059669 3.7~3.8:1, placeholder 2.6:1, 비활성 버튼 ~1.5:1
- [P1] 키보드/낭독기: 약관 체크박스 sr-only라 포커스 안 보임, 오류 aria-describedby 없음, 회원가입 첫 진입 모달은 openModal을 안 거쳐 포커스가 배경에
- [P2] 비활성 동의 버튼 이유 없음 + 약관 패널 문구 '알아서 관리해 드립니다' 제품 원칙과 어긋남
- [P2] 로그인·회원가입 불일치(배경, ✕, 가짜 체크 행, 오류 말투 합니다체)

검출기: 실제 렌더 login 1280 13건, 390 8건, signup(모달 열림) 1280 6건, 390 7건. 로그인 배경 텍스트 가림·대비는 aria-hidden 장식이라 의도. 정적 broken-image/flat-type-hierarchy 10건 오탐.
