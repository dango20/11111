# 납작 스캐너 (Android)

종이를 찍으면 반듯하게 펴고 보정해서 PDF로 저장하거나 보내는 안드로이드 앱이에요.
화면과 보정 로직은 `app/src/main/assets/index.html`에 있고, 네이티브 코드(Java)는 카메라 연결, PDF 저장, 공유만 맡아요.

- 최소 안드로이드 8.0 (API 26)
- 권한: 인터넷만 씀 (카메라는 폰의 기본 카메라 앱을 불러서 사용)
- 저장 위치: `다운로드/납작스캐너/`
- 보내기: 안드로이드 공유 창 (카톡, 메일, 드라이브 등)

## APK 만드는 방법 1: GitHub에서 자동으로 (PC 설치 없이)

1. github.com에서 새 저장소를 만들어요 (Private 가능).
2. 저장소 페이지의 **Add file → Upload files**로 이 폴더 안의 파일을 **전부** 끌어다 올려요.
   `.github` 폴더도 꼭 함께 올라가야 해요. 숨김 폴더라서 안 보이면 탐색기/Finder에서 숨김 파일 보기를 켜세요.
3. **Actions** 탭으로 가면 "APK 빌드"가 자동으로 돌아요 (약 3~5분).
4. 끝난 실행을 눌러 맨 아래 **Artifacts**의 `napjak-scanner-apk`를 내려받아요. zip 안에 `napjak-scanner.apk`가 있어요.

## 방법 2: Android Studio

1. Android Studio에서 **Open**으로 이 폴더를 열어요.
2. 동기화가 끝나면 **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
3. 결과: `app/build/outputs/apk/debug/app-debug.apk`

참고: 방법 2에서는 PDF 라이브러리(jsPDF)를 앱이 처음 PDF를 만들 때 인터넷에서 불러와요.
완전 오프라인으로 쓰려면 아래 파일을 받아 `app/src/main/assets/jspdf.umd.min.js`로 넣고 빌드하세요.
https://cdnjs.cloudflare.com/ajax/libs/jspdf/2.5.1/jspdf.umd.min.js
(방법 1은 빌드할 때 자동으로 넣어줘요.)

## 폰에 설치

1. APK 파일을 폰으로 옮기고 (카톡 나에게 보내기, 드라이브 등) 탭해요.
2. "출처를 알 수 없는 앱" 설치 허용을 묻으면 허용해요.
3. Play 프로텍트 경고가 뜨면 **무시하고 설치**를 눌러요. 직접 빌드한 앱이라 서명이 등록되지 않았을 뿐이에요.
