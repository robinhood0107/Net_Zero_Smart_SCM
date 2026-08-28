# Net_Zero_Smart_SCM

## 포트폴리오 요약

| 항목 | 내용 |
| --- | --- |
| 기간 | 2025. 12 ~ 2025. 12 (1개월) |
| 구분 | 개인 데이터베이스 설계·구축 과제 |
| 결과 | 발주서·발주항목·납품·재고 반영을 하나의 트랜잭션으로 묶어 중간 실패 시 전체 롤백되도록 구성 |
| 개인 역할 | 공급망 도메인·스키마 설계, CHECK/FK·UPSERT, SQLSTATE 기반 오류 처리와 deadlock 재시도 구현 |
| 기술 | Java 17, Spring Boot, JDBC, PostgreSQL, HikariCP, Maven |

이 저장소는 두 개의 애플리케이션을 포함합니다.
- `Console_code`: 콘솔 기반 SCM/탄소배출 관리 도구 (Java 17, Maven)
- `Web_code`: Spring Boot 웹 애플리케이션 + 정적 프런트엔드

아래 절차는 공통 DB 설정 → 개별 앱 실행 순서로 안내합니다.

## 1. 필수 도구
- Java 17 이상
- Maven 3.9+
- PostgreSQL 14+

## 2. 데이터베이스 준비(공통)
1) DB 생성
```sql
CREATE DATABASE scm_db;
```
2) 스키마/샘플 데이터 적용 (루트에서 실행)
```bash
psql -U <사용자명> -d scm_db -f Console_code/schema.sql
psql -U <사용자명> -d scm_db -f Console_code/seed.sql
# 또는 Web_code/schema.sql, Web_code/seed.sql을 사용해도 동일
```

## 3. 민감 정보 관리
- 예시 파일을 복사해 개인 설정을 채운 뒤 사용하세요.
  - 콘솔: `Console_code/config.example.properties` → `Console_code/config.properties`
  - 웹   : `Web_code/src/main/resources/application.example.properties` → `Web_code/src/main/resources/application.properties`

## 4. 콘솔 앱 실행 (`Console_code`)
```bash
cd Console_code
copy config.example.properties config.properties   # 최초 1회, 내용 수정
mvn exec:java                                      # 또는 mvn clean package && java -jar target/carbon-neutral-scm-1.0.0.jar
```

환경변수로 대체하려면 `DB_URL`, `DB_USER`, `DB_PASSWORD`를 설정합니다(README 참고).

## 5. 웹 앱 실행 (`Web_code`)
```bash
cd Web_code
copy src\main\resources\application.example.properties src\main\resources\application.properties   # 최초 1회, 내용 수정
mvn spring-boot:run
```
서버 기동 후 접속:
- 대시보드: http://localhost:8080/dashboard.html
- 발주 관리: http://localhost:8080/order.html
- 공급업체 리포트: http://localhost:8080/supplier.html
- 설정: http://localhost:8080/setting.html
