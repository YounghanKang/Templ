# Templ 배포 가이드

이 폴더에는 가비아 클라우드(Ubuntu/Linux) 서버에 배포하기 위한 스크립트와 설정 파일들이 들어있습니다.

## 1. Nginx 설정 (`nginx.conf`)
이 파일은 웹 서버(Nginx)가 80번 포트로 들어오는 요청을 받아서, 
- 정적 파일(React)은 `/var/www/html`에서 응답하고,
- `/api/...`로 시작하는 요청은 백엔드(8080 포트)로 넘겨주는 역할을 합니다.

**사용 방법 (서버에서):**
```bash
sudo cp deploy/nginx.conf /etc/nginx/sites-available/default
sudo systemctl restart nginx
```

## 2. Spring Boot 백그라운드 서비스 (`templ.service`)
서버의 터미널을 종료해도 백엔드가 계속 켜져 있도록 해주는 시스템 데몬 설정 파일입니다.
`WorkingDirectory` 항목을 실제 서버 내 프로젝트가 있는 경로로 수정해주세요.

**사용 방법 (서버에서):**
```bash
# 1. 파일 복사
sudo cp deploy/templ.service /etc/systemd/system/

# 2. 데몬 리로드 및 자동 실행 등록
sudo systemctl daemon-reload
sudo systemctl enable templ

# 3. 백엔드 실행
sudo systemctl start templ
```

## 3. 자동 배포 스크립트 (`deploy.sh`)
이후 코드가 수정되었을 때 프론트엔드와 백엔드를 자동으로 재빌드하고 서버를 재시작해주는 스크립트입니다.

**사용 방법 (서버 프로젝트 최상위 폴더에서):**
```bash
chmod +x deploy/deploy.sh
./deploy/deploy.sh
```
