# HW10 - 탄소중립 스마트 SCM 플랫폼

Java + PostgreSQL 기반의 조선·해양 산업 부품 공급망(SCM) 및 탄소배출 통합 관리 시스템

---

## 콘솔 버전 실행 환경

| 항목 | 버전 |
|------|------|
| Java (JDK) | 17 이상 |
| PostgreSQL | 14 이상 |
| Maven | 3.9 이상 |

---

## 빠른 시작 가이드

### 1단계: PostgreSQL DB 생성

```sql
-- postgres 계정으로 접속 후 실행
CREATE DATABASE scm_db OWNER parkjongjin;
```

### 2단계: 스키마 및 샘플 데이터 적용

### 3단계: DB 접속 설정

프로젝트 루트에 `config.properties` 파일 생성:

```properties
db.url=jdbc:postgresql://localhost:5432/scm_db
db.user=내 이름
db.password=내 비밀번호
```

> ⚠️ 본인의 DB 비밀번호로 수정하세요!

### 4단계: 빌드 및 실행

```powershell
# 방법 1: Maven으로 바로 실행 (추천)
mvn exec:java

# 방법 2: JAR 패키징 후 실행
mvn clean package
java -jar target/carbon-neutral-scm-1.0.0.jar
```

---

## 주요 기능

### 기능 1: 프로젝트 대시보드
- 프로젝트 ID 또는 선박명으로 검색
- 총 발주 금액, 공급업체별 TOP 3
- 탄소배출 합계 (운송/보관/전체)
- 탄소 집약도 (kg CO₂e / 백만 원) 지표

### 기능 2: 발주 등록 (트랜잭션)
- 발주서 + 발주항목 + 납품 + 재고 반영을 단일 트랜잭션으로 처리
- 오류 발생 시 전체 롤백
- 교착상태(Deadlock) 발생 시 자동 재시도

### 기능 3: 공급업체 ESG/지연 리포트
- ESG 등급 필터 (A~D 다중 선택)
- 지연 납품 비율 필터
- 공급업체 상세: 최근 5건 발주/납품 이력

---

## 파일 구조

```
hw10/
├── config.properties      # DB 접속 설정 (본인이 생성)
├── schema.sql             # 테이블 생성 SQL
├── seed.sql               # 샘플 데이터
├── pom.xml                # Maven 설정
└── src/main/java/hw10/
    ├── App.java           # 메인 진입점
    ├── config/            # 설정 로드
    ├── db/                # DB 연결
    ├── dao/               # SQL 쿼리 (SELECT/INSERT)     ├── service/           # 트랜잭션 처리
    ├── ui/                # 콘솔 UI (기능 1~3)
    └── util/              # 로그, 입력, 에러처리
```

---

## 환경변수로 설정하기 (선택)

`config.properties` 대신 환경변수 사용 가능:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/scm_db"
$env:DB_USER = "내 USER 이름"
$env:DB_PASSWORD = "내 비밀번호"
```

---

## 문제 해결

| 문제 | 해결 |
|------|------|
| `mvn` 명령어 인식 안 됨 | Maven 설치 후 PATH에 `C:\apache-maven-3.9.x\bin` 추가 |
| `java` 명령어 인식 안 됨 | JDK 17 설치 후 PATH에 `C:\Program Files\Java\jdk-17\bin` 추가 |
| `No suitable driver` 오류 | `java -jar` 대신 `mvn exec:java`로 실행 |
| DB 접속 실패 | `config.properties` 파일 확인 및 PostgreSQL 서비스 실행 확인 |
