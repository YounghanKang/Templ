# 🌳 Orchestree

> **AI 기반 협업 조율 서비스**

Slack, GitHub 등 여러 협업 툴에 흩어진 **대화와 작업물을 AI가 분석하여 하나의 Directory 형태로 구조화**하고, 작업 간 충돌이나 역할 중복을 감지해 **사람의 승인 하에 조율**하는 협업 서비스입니다.

## ✨ 핵심 기능

### 1. Specification → Virtual Directory

자연어로 프로젝트 명세서를 입력하면 AI가 이를 실행 가능한 작업 단위로 분해하여 계층적인 디렉토리로 구성합니다.

```text
Project
├── Backend
│   ├── Login API
│   └── User API
├── Frontend
│   └── Login Page
└── Database
```

각 작업에는 담당자, 목표, 기한, 선행 작업, 결과물 등을 관리할 수 있습니다.

### 2. AI Context Analysis

Slack 메시지와 GitHub Issue / Commit / PR을 분석하여 **어떤 작업(Directory)과 관련된 변화인지** 판단합니다.

```text
Slack / GitHub
      ↓
1차 필터링
      ↓
LLM 맥락 분석
      ↓
관련 Directory 매칭
      ↓
경고 / 제안
```

불필요한 이벤트는 백엔드에서 먼저 필터링하여 LLM 사용을 최소화합니다.

### 3. Directory-scoped Warning

프로젝트 전체에 알림을 뿌리지 않고 **관련된 작업에만 경고**합니다.

> `Login API`의 변경 → `Login API`에만 경고

이를 통해 불필요한 알림을 줄이고 담당자가 자신의 작업에 집중할 수 있습니다.

### 4. Human-in-the-loop

AI가 직접 작업을 변경하지 않습니다.

**AI 분석 → 제안 → 사람 승인 → 실제 반영**

역할 충돌이나 작업 중복이 발견되면 담당자에게 제안을 보여주고, 승인된 경우에만 조율합니다.

## 🛠️ Tech Stack

| 영역            | 기술       |
| ------------- | -------- |
| Frontend      | React    |
| Backend       | Spring Boot |
| Database      |          |
| AI            |   |

## 🎯 Core Value

> **팀은 기존 협업 툴을 그대로 사용하고,
> Orchestree는 그 사이의 맥락을 연결합니다.**

새로운 대시보드를 계속 확인해야 하는 것이 아니라, **중요한 변화가 발생했을 때만 AI가 개입**합니다.

---

