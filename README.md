# 디프만의 모든 활동을 지원하는, DPM CORE

<p align="center">
  <img src="./.github/image/core_banner.png" alt="core_banner" width="100%" />
</p>

---
## 🛠 Tech Stack

![Java](https://img.shields.io/badge/kotlin-1.9-7F52FF?logo=kotlin)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6-6DB33F?logo=springsecurity)
<br>
![JPA](https://img.shields.io/badge/Spring%20Data%20JPA-Hibernate-59666C?logo=hibernate)
![jOOQ](https://img.shields.io/badge/jOOQ-9.0-4868AA)
![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql&logoColor=white)
<br>
![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white)
![Traefik](https://img.shields.io/badge/Traefik-2.11-24A1C1?logo=traefikproxy&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-2088FF?logo=githubactions&logoColor=white)
![Oracle Cloud](https://img.shields.io/badge/Oracle%20Cloud-Ampere%20ARM-F80000?logo=oracle&logoColor=white)

---

## 🏗️ System Architecture

> 2026.09 iwinv + AWS(RDS·EC2) → Oracle Cloud 마이그레이션 완료

### 런타임 구조

```mermaid
flowchart TB
    FE["🌐 프론트엔드<br/>core.depromeet.com"]

    subgraph ORACLE["Oracle Cloud (Ampere ARM)"]
        subgraph PROD["core-prod 인스턴스"]
            TRAEFIK["Traefik v2.11<br/>:80/:443 · Let's Encrypt 자동 발급<br/>호스트명 기반 라우팅"]
            APP["spring-app (prod)<br/>Docker Swarm · start-first 무중단 배포"]
            DEVAPP["dev-spring-app (dev)"]
        end
        subgraph DBHOST["core-db 인스턴스"]
            MYSQL[("MySQL 8.0.41 (Docker)<br/>dpm_core · dpm_core_dev<br/>스키마별 최소 권한 계정")]
        end
    end

    FE -->|api.depromeet.com| TRAEFIK
    FE -.->|api.depromeet.shop| TRAEFIK
    TRAEFIK -->|"Host(api.depromeet.com)"| APP
    TRAEFIK -.->|"Host(api.depromeet.shop)"| DEVAPP
    APP -->|"dpm_core (TLS)"| MYSQL
    DEVAPP -.->|"dpm_core_dev (TLS)"| MYSQL
```

- prod/dev 앱이 **한 인스턴스(core-prod)에서 Swarm 서비스로 분리** 운영되고, Traefik이 호스트명으로 라우팅합니다.
- prod/dev DB는 **하나의 MySQL 인스턴스에서 스키마로 분리**되며, 계정도 스키마별 최소 권한(`core_prod`/`core_dev`)으로 격리됩니다.
- core-db는 방화벽(VCN Security List + iptables)으로 **앱 서버에서만 3306 접근**을 허용합니다.
- core-db에서 매일 04:00(KST) `mysqldump` 백업이 요일별 7개 파일로 로테이션됩니다.

### 배포 파이프라인

```mermaid
flowchart LR
    DEVELOP["develop push"] --> DEVCD["dev-cd"]
    MAIN["main push"] --> PRODCD["prod-cd"]
    DEVCD --> HUB[("DockerHub<br/>Jib 멀티아치<br/>amd64 + arm64")]
    PRODCD --> HUB
    HUB -->|"dev-{sha}"| DEVDEPLOY["core-prod<br/>dev 스택 deploy"]
    HUB -->|"prod-{sha}"| PRODDEPLOY["core-prod<br/>prod 스택 deploy"]
```

- 이미지는 Jib으로 **멀티아치(amd64+arm64)** 빌드됩니다.
- prod 환경변수는 **GitHub Actions Secrets가 원본**이며, prod-cd가 배포할 때마다 서버의 `.env`를 재생성해 컨테이너에 주입합니다. (dev는 서버에 상주하는 `.env.dev` 사용)

---

## 🧱 Module Structure

### 프로젝트 모듈 구조

```
root
├── .github
├── application # API/유스케이스
├── codegen # jOOQ DSL 생성 모듈
├── domain # 순수 도메인 모델
├── entity # JPA 엔티티
└── persistence # DB 접근 (jOOQ, Spring Data JPA)
```

* 멀티 모듈 구조를 사용하여 **의존성 방향을 명확히** 관리합니다.
* 도메인 모듈은 순수 POJO로 유지하여 **비즈니스 로직의 독립성**을 보장합니다.
* `./gradlew:codegen jooqGenerate` 명령어로 jOOQ DSL 클래스를 생성합니다.

### 도메인 모듈 구조

```
foo-domain
  └─ aggregate # 도메인 객체
  └─ enums
  └─ port # inbound: usecase / outbound: persistence port
  └─ vo # 값 객체, 식별자 등
  └─ constant
  └─ event # 도메인 이벤트
```

---

## 👨🏻‍💻Contributors

### 3rd Developers
| ![](https://github.com/wjdwnsdnjs13.png?size=100) | ![](https://github.com/uykm.png?size=100) | ![](https://github.com/cowboysj.png?size=100) |
|:-------------------------------------------------:|:-----------------------------------------:|:---------------------------------------------:|
|                      **정준원**                      |                  **신민규**                  |                    **김수진**                    |

### 2nd Developers
| ![](https://github.com/wjdwnsdnjs13.png?size=100) | ![](https://github.com/BlackBean99.png?size=100) |
|:-------------------------------------------------:|:------------------------------------------------:|
|                      **정준원**                      |                     **이서현**                      |


### 1st Developers
| ![](https://github.com/wjdwnsdnjs13.png?size=100) | ![](https://github.com/LeeHanEum.png?size=100) | ![](https://github.com/its-sky.png?size=100) |
|:-------------------------------------------------:|:--------------------------------------------:|:--------------------------------------------:|
|                      **정준원**                      | **이한음** |                   **신민철**                    |

---
## 📐 Team Rules
- 팀 내부 결정 사항은 Issue, Suggestion 등의 형태로 내부 논의 후 결정
  - [특정 정책에 대한 하드 코딩 관리(feat. 세션 시작 시간 14시)](https://github.com/depromeet/dpm-core-server/issues/33)
  - [멀티 모듈 아키텍처에 대한 질문입니다.(각 모듈에 대한 의문 제기)](https://github.com/depromeet/dpm-core-server/issues/172)
- 이유가 있고, 모두가 동의하는 방식의 의사결정
  - [JPA Entity와 schema.sql의 불일치로 인한 잠재적 위험 (jOOQ DSL codegen으로 인한 순환 참조 문제)](https://github.com/depromeet/dpm-core-server/issues/164)
  - [커스텀 에러 코드 관련 논의입니다.](https://github.com/depromeet/dpm-core-server/issues/60)
  - [Response 에서 시각(datetime)데이터 타입 논의](https://github.com/depromeet/dpm-core-server/issues/63)
