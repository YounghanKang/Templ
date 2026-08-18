#!/bin/bash

# 프로젝트 최상단 디렉토리에서 실행해야 합니다.

echo "🚀 배포 스크립트를 시작합니다..."

# 1. 프론트엔드 빌드
echo "📦 1. 프론트엔드(React) 빌드 중..."
cd frontend
npm install
npm run build

echo "📂 빌드된 프론트엔드 파일을 Nginx 폴더로 복사합니다..."
sudo rm -rf /var/www/html/*
sudo cp -r dist/* /var/www/html/
cd ..

# 2. 백엔드 빌드
echo "☕ 2. 백엔드(Spring Boot) 빌드 중..."
cd backend
chmod +x gradlew
./gradlew build -x test
cd ..

# 3. 백엔드 서비스 재시작
echo "🔄 3. 백엔드 서비스(systemd)를 재시작합니다..."
# 이 스크립트는 deploy/templ.service 가 /etc/systemd/system/ 에 등록되어 있다고 가정합니다.
sudo systemctl restart templ
sudo systemctl restart nginx

echo "✅ 배포가 완료되었습니다!"
