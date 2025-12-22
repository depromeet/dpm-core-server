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
![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-2088FF?logo=githubactions&logoColor=white)

---

## 🏗️System Architecture
<p align="center">
  <img src="./.github/image/core_architecture.png" alt="core_architecture" width="100%" />
</p>

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

### 2nd Developers
| ![](https://github.com/wjdwnsdnjs13.png?size=100) | ![](https://github.com/BlackBean99.png?size=100) |
|:-------------------------------------------------:|:------------------------------------------------:|
|                      **정준원**                      |                     **이서현**                      |


### 1st Developers
| ![](https://github.com/wjdwnsdnjs13.png?size=100) | ![](https://github.com/LeeHanEum.png?size=100) | ![](https://github.com/its-sky.png?size=100) |
|:-------------------------------------------------:|:--------------------------------------------:|:--------------------------------------------:|
|                      **정준원**                      | **이한음** |                   **신민철**                    |

---
