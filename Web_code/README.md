# 🚢 탄소중립 스마트 SCM 플랫폼 (HW10)

본 프로젝트는 **Java Spring Boot**와 **PostgreSQL**을 기반으로 한 조선·해양 산업의 부품 공급망(SCM) 및 탄소 배출 통합 관리 시스템입니다.

---

## ✅ 필수 요구 사항 (Prerequisites)

이 프로젝트를 실행하기 위해 다음 소프트웨어가 설치되어 있어야 합니다.

- **Java (JDK)**: 17 버전 이상
- **Maven**: 3.9 버전 이상
- **PostgreSQL**: 14 버전 이상

---

## 🚀 빠른 시작 가이드 (Quick Start)

### 1단계: 데이터베이스 설정 (Database Setup)

PostgreSQL을 설치한 후, 아래 명령어를 순서대로 실행하여 데이터베이스와 초기 데이터를 설정합니다.

1.  **데이터베이스 생성**

    ```sql
    -- psql 또는 pgAdmin 쿼리툴에서 실행
    CREATE DATABASE scm_db;
    ```

2.  **테이블 및 데이터 생성**
    프로젝트 루트 폴더(`Web_code/`)에 있는 `schema.sql`과 `seed.sql` 파일을 실행합니다.

    **(방법 A) 커맨드라인(CMD/Terminal) 사용 시:**

    ```bash
    # 프로젝트 루트 폴더에서 실행
    psql -U [사용자명] -d scm_db -f schema.sql
    psql -U [사용자명] -d scm_db -f seed.sql
    ```

    _(윈도우의 경우 psql 환경변수 설정이 필요할 수 있습니다. pgAdmin을 사용하는 것을 권장합니다.)_

    **(방법 B) pgAdmin / DBeaver 사용 시:**

    - `scm_db` 데이터베이스에 접속합니다.
    - Query Tool을 엽니다.
    - `schema.sql` 파일 내용을 복사-붙여넣기 후 실행합니다.
    - `seed.sql` 파일 내용을 복사-붙여넣기 후 실행합니다.

---

### 2단계: 프로젝트 설정 (Configuration)

DB 접속 정보를 본인의 환경에 맞게 수정해야 합니다.

1.  예시 설정 파일을 복사하여 실제 설정 파일을 만듭니다.

    ```bash
    # Windows
    copy src\main\resources\application.example.properties src\main\resources\application.properties
    ```

2.  생성된 `src/main/resources/application.properties`에서 PostgreSQL 계정 정보를 본인 환경으로 수정하세요.

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/scm_db
spring.datasource.username=<본인 사용자>   # 예: postgres
spring.datasource.password=<본인 비밀번호> # 예: myStrongPwd!
```

> ⚠️ `application.properties`는 `.gitignore`에 포함되어 있어 저장소에 올라가지 않습니다. 비밀번호를 직접 채워 넣으세요.

---

### 3단계: 실행 (Run)

Maven을 사용하여 프로젝트를 실행합니다. 프론트엔드 빌드(Node.js, Terraform, TailwindCSS)는 자동으로 수행됩니다.

1.  프로젝트 루트 폴더(`Web_code/`)에서 터미널(CMD/Powershell)을 엽니다.
2.  다음 명령어를 입력합니다.

```bash
mvn spring-boot:run
```

> **참고:** 최초 실행 시 라이브러리 다운로드 및 프론트엔드 환경 구성으로 인해 시간이 다소 소요될 수 있습니다.

---

## 🌐 접속 방법 (Usage)

서버가 정상적으로 시작되면(`Started Application in...` 메시지 표시), 웹 브라우저를 열고 아래 주소로 접속하세요.

- **메인 대시보드**: [http://localhost:8080/dashboard.html](http://localhost:8080/dashboard.html)
- **발주 관리**: [http://localhost:8080/order.html](http://localhost:8080/order.html)
- **공급업체 리포트**: [http://localhost:8080/supplier.html](http://localhost:8080/supplier.html)
- **설정**: [http://localhost:8080/setting.html](http://localhost:8080/setting.html)

---

## ❓ 자주 묻는 질문 (Troubleshooting)

**Q. `mvn` 명령어를 찾을 수 없다고 나와요.**
A. Apache Maven이 설치되어 있지 않거나, 환경 변수(PATH) 설정이 안 되어 있는 경우입니다. Maven을 설치하거나 IDE(IntelliJ, Eclipse)의 내장 기능을 사용하세요.

**Q. 데이터베이스 연결 오류가 발생해요.**
A. `application.properties` 파일의 `username`과 `password`가 PostgreSQL 설치 시 설정한 정보와 일치하는지 확인하세요. 또한 `scm_db` 데이터베이스가 생성되었는지 확인하세요.

**Q. 8080 포트가 이미 사용 중이라고 나와요.**
A. 다른 프로그램이 8080 포트를 사용 중입니다. `application.properties`에서 `server.port=8081`과 같이 포트 번호를 변경하고 다시 실행하세요.

---

## 📁 주요 기능 소개

### 1. 📊 프로젝트 대시보드

- 프로젝트별 탄소 배출량 및 진행 상황 모니터링
- **선박 탄소 집약도(CII)** 및 주요 지표 시각화
- 공급업체별 납품 현황 분석

### 2. 📝 발주 관리 및 트랜잭션

- 부품 발주부터 납품, 재고 반영까지 단일 트랜잭션으로 처리
- 데이터 무결성 보장 및 교착상태(Deadlock) 자동 감지/재시도 로직 적용

### 3. 📉 공급망 리포트

- 공급업체 ESG 등급(A~D) 기반 필터링
- 납품 지연율 분석 및 우수/위험 공급업체 식별
