# 2. 구현한 클래스 설명

본 문서에서는 탄소중립 스마트 SCM 플랫폼 애플리케이션의 클래스 구조와 핵심 함수들에 대해 설명합니다.

---

## 2.1 클래스 다이어그램

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              hw10 패키지 구조                                │
└─────────────────────────────────────────────────────────────────────────────┘

                              ┌─────────────────┐
                              │   Application   │ ◀── 프로그램 진입점
                              │   (메인 클래스)  │
                              └────────┬────────┘
                                       │
                    ┌──────────────────┼──────────────────┐
                    ▼                  ▼                  ▼
           ┌────────────────┐ ┌────────────────┐ ┌────────────────┐
           │ DatabaseConfig │ │DatabaseConnection│ │    Logger     │
           │  (설정 로더)    │ │  (DB 연결)      │ │   (로그)       │
           └────────────────┘ └────────┬────────┘ └────────────────┘
                                       │
                                       ▼
                    (변경 가능) ┌────────────────┐
                              │  ConsoleMenu   │ ◀── UI 레이어
                              │  (메인 메뉴)    │     (웹/GUI로 교체 가능)
                              └────────┬────────┘
                                       │
          ┌────────────────────────────┼────────────────────────────┐
          ▼                            ▼                            ▼
(변경 가능)                    (변경 가능)                   (변경 가능)
┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐
│ ProjectDashboard │    │ OrderRegistration│    │  SupplierReport  │
│   (기능 1 UI)     │    │   (기능 2 UI)     │    │   (기능 3 UI)     │
└────────┬─────────┘    └────────┬─────────┘    └────────┬─────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐
│ProjectRepository │    │OrderTransaction- │    │SupplierRepository│
│  (프로젝트 DAO)   │    │    Service       │    │ (공급업체 DAO)    │
└──────────────────┘    │ (트랜잭션 서비스) │    └──────────────────┘
                        └────────┬─────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         ▼                       ▼                       ▼
┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐
│ OrderRepository  │    │DeliveryRepository│    │InventoryRepository│
│  (발주 DAO)       │    │  (납품 DAO)       │    │   (재고 DAO)      │
└──────────────────┘    └──────────────────┘    └──────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 ▼
                        ┌──────────────────┐
                        │SequenceGenerator │
                        │   (PK 생성기)     │
                        └──────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                            유틸리티 클래스                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│  ErrorHandler (예외→메시지 변환)  │  InputHelper (콘솔 입력 처리, 변경 가능) │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2.2 패키지 구조

본 프로젝트는 다음과 같은 패키지 구조로 구성되어 있습니다.

| 패키지 | 역할 | 변경 가능 여부 |
|--------|------|---------------|
| `hw10` | 메인 클래스 (Application) | 고정 |
| `hw10.config` | DB 환경설정 로더 | 고정 |
| `hw10.db` | DB 연결 관리 | 고정 |
| `hw10.dao` | 데이터 접근 계층 (SQL 실행) | 고정 |
| `hw10.service` | 비즈니스 로직 및 트랜잭션 | 고정 |
| `hw10.ui` | 사용자 인터페이스 | **(변경 가능)** |
| `hw10.util` | 유틸리티 클래스 | 부분 변경 가능 |

---

## 2.3 클래스별 상세 설명

### 2.3.1 Application (메인 클래스)

**파일 위치:** `hw10/Application.java`

Application 클래스는 프로그램의 진입점(Entry Point)으로서 `main()` 메서드를 포함합니다. 이 클래스는 애플리케이션의 생명주기를 관리하며, 로그 초기화, DB 연결, UI 실행을 담당합니다.

#### 메서드 상세 설명

**`public static void main(String[] args)`**

main 메서드는 프로그램이 실행될 때 JVM에 의해 가장 먼저 호출되는 메서드입니다. 이 메서드는 프로그램의 전체 실행 흐름을 제어합니다.

메서드가 시작되면 먼저 `Logger.init()`을 호출하여 로그 시스템을 초기화합니다. 이 과정에서 콘솔과 파일(logs/app.log)에 동시에 로그를 기록할 수 있도록 설정됩니다. 초기화가 완료되면 `Logger.info("애플리케이션 시작")`을 호출하여 프로그램 시작을 기록합니다. 이는 과제 요구사항에서 명시한 "애플리케이션 시작/종료 로그"를 충족합니다.

다음으로 `DatabaseConfig.load()`를 호출하여 DB 접속 정보를 외부 설정에서 로드합니다. 로드된 설정으로 DatabaseConnection 객체를 생성하는데, 이때 try-with-resources 문법을 사용합니다. 이 문법은 Java 7에서 도입된 것으로, 블록이 종료될 때 자동으로 close() 메서드를 호출하여 리소스를 정리합니다.

DB 연결이 성공하면 "DB 접속 성공" 로그를 기록하고, ConsoleMenu 객체를 생성하여 run() 메서드를 호출합니다. 이 시점부터 사용자와의 상호작용이 시작됩니다.

예외가 발생하면 catch 블록에서 처리합니다. Logger.error()로 상세 오류 정보를 로그에 기록하고, 사용자에게는 간단한 오류 메시지만 출력합니다. 이는 과제 요구사항의 "사용자에게 이해 가능한 에러 메시지를 출력하고 개발용 상세 내용은 로그로 남기도록"하는 조건을 충족합니다.

마지막으로 finally 블록에서 "애플리케이션 종료" 로그를 기록합니다. finally 블록은 예외 발생 여부와 관계없이 항상 실행되므로, 정상 종료든 비정상 종료든 로그가 남게 됩니다.

#### 과제 요구사항 충족

- **[기능 4]** 애플리케이션 시작/종료 로그를 기록합니다.
- **[기능 4]** DB 접속 성공/실패 로그를 기록합니다.
- **[기능 4]** 예외 발생 시 프로그램이 중단되지 않고 사용자에게 오류 메시지를 출력합니다.

---

### 2.3.2 DatabaseConfig (환경설정 로더)

**파일 위치:** `hw10/config/DatabaseConfig.java`

DatabaseConfig 클래스는 DB 접속 정보를 외부에서 로드하는 역할을 담당합니다. 과제 요구사항에 따라 DB 접속 정보를 코드에 하드코딩하지 않고, 환경변수 또는 설정 파일에서 읽어옵니다.

#### 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `dbUrl` | String | JDBC 연결 URL (예: jdbc:postgresql://localhost:5432/scm_db) |
| `dbUser` | String | DB 접속 계정명 |
| `dbPassword` | String | DB 접속 비밀번호 |

세 필드 모두 `public final`로 선언되어 있어 외부에서 읽을 수 있지만 변경할 수 없습니다.

#### 메서드 상세 설명

**`private DatabaseConfig(String dbUrl, String dbUser, String dbPassword)`**

생성자는 private으로 선언되어 외부에서 직접 인스턴스를 생성할 수 없습니다. 대신 load() 정적 메서드를 통해서만 객체를 생성할 수 있습니다. 이러한 패턴을 정적 팩토리 메서드 패턴이라고 합니다. 생성자는 전달받은 세 개의 파라미터를 각각 필드에 저장합니다.

**`public static DatabaseConfig load()`**

load 메서드는 설정을 로드하여 DatabaseConfig 객체를 반환하는 정적 메서드입니다. 두 단계의 우선순위로 설정을 로드합니다.

첫 번째 단계에서는 System.getenv()를 사용하여 운영체제의 환경변수를 확인합니다. DB_URL, DB_USER, DB_PASSWORD 세 환경변수가 모두 설정되어 있으면 해당 값으로 DatabaseConfig 객체를 생성하여 반환합니다. 환경변수는 운영 환경에서 민감한 정보를 관리할 때 주로 사용됩니다.

첫 번째 단계에서 설정을 찾지 못하면 두 번째 단계로 config.properties 파일을 읽습니다. Java의 Properties 클래스를 사용하여 key=value 형태의 설정 파일을 파싱합니다. FileInputStream으로 파일을 열고 Properties.load()로 내용을 읽어옵니다. db.url, db.user, db.password 세 키에서 값을 가져와 유효성을 검증한 후 DatabaseConfig 객체를 생성합니다.

파일이 없거나 읽기에 실패하면 Logger.warn()으로 경고 로그를 남깁니다. 두 단계 모두에서 설정을 찾지 못하면 모든 필드가 null인 객체를 반환하며, 이 경우 DatabaseConnection 생성 시점에서 IllegalStateException이 발생합니다.

**`private static boolean notBlank(String s)`**

문자열이 null이 아니고 비어있지 않은지 확인하는 헬퍼 메서드입니다. s가 null이면 false를 반환하고, trim()을 호출하여 앞뒤 공백을 제거한 후 isEmpty()로 빈 문자열인지 확인합니다.

#### 과제 요구사항 충족

- **[기능 4]** DB 접속 정보(host, port, user, password, dbname)를 코드에 하드코딩하지 않습니다.
- **[기능 4]** 설정 파일 및 환경 변수로 분리하여 관리합니다.

---

### 2.3.3 DatabaseConnection (DB 연결 관리)

**파일 위치:** `hw10/db/DatabaseConnection.java`

DatabaseConnection 클래스는 JDBC를 사용하여 PostgreSQL 데이터베이스와의 연결을 관리합니다. AutoCloseable 인터페이스를 구현하여 try-with-resources 문법을 지원합니다.

#### 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `config` | DatabaseConfig | DB 접속 설정 정보를 담고 있는 객체 |

#### 메서드 상세 설명

**`public DatabaseConnection(DatabaseConfig config)`**

생성자는 DatabaseConfig 객체를 받아 저장하고, 설정값의 유효성을 검증합니다. dbUrl, dbUser, dbPassword 중 하나라도 null이면 IllegalStateException을 발생시킵니다. 이 예외는 "DB 설정이 비어 있습니다. 환경변수 또는 config.properties를 설정하세요."라는 메시지를 포함하여, 사용자가 문제의 원인을 파악할 수 있도록 합니다.

**`public Connection openConnection() throws SQLException`**

새로운 JDBC Connection 객체를 생성하여 반환하는 메서드입니다. 내부적으로 DriverManager.getConnection(url, user, password)를 호출하여 PostgreSQL 데이터베이스에 실제로 연결합니다.

이 메서드는 호출될 때마다 새로운 Connection을 생성합니다. Connection은 호출자가 직접 관리해야 하며, 사용이 끝나면 반드시 close()를 호출해야 합니다. 일반적으로 try-with-resources 문법을 사용하여 자동으로 닫히도록 합니다.

연결에 실패하면 SQLException이 발생합니다. 네트워크 문제, 잘못된 인증 정보, DB 서버 다운 등 다양한 원인이 있을 수 있습니다.

**`public void close()`**

AutoCloseable 인터페이스를 구현하기 위한 메서드입니다. DatabaseConnection 자체는 Connection 풀링을 구현하지 않고 단순히 Connection 팩토리 역할만 수행하므로, 이 메서드는 비어있습니다. 실제 Connection의 닫기는 각 호출자가 책임집니다.

---

### 2.3.4 Logger (로그 유틸리티)

**파일 위치:** `hw10/util/Logger.java`

Logger 클래스는 과제 요구사항에 명시된 로깅 기능을 구현합니다. java.util.logging 패키지를 사용하여 콘솔과 파일(logs/app.log)에 동시에 로그를 기록합니다.

#### 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `LOGGER` | java.util.logging.Logger | Java 표준 로거 객체 (이름: "scm") |
| `initialized` | boolean | 초기화 완료 플래그 (중복 초기화 방지) |

#### 메서드 상세 설명

**`public static void init()`**

로그 시스템을 초기화하는 메서드입니다. 프로그램 시작 시 한 번만 호출해야 합니다.

먼저 initialized 플래그를 확인하여 이미 초기화되었으면 바로 리턴합니다. 초기화 과정에서는 setUseParentHandlers(false)를 호출하여 Java 기본 로거의 핸들러를 비활성화합니다. 이렇게 하지 않으면 로그가 중복으로 출력될 수 있습니다.

다음으로 두 개의 핸들러를 추가합니다. ConsoleHandler는 표준 출력(콘솔)에 로그를 출력합니다. SimpleFormatter를 사용하여 간단한 형태로 포맷팅합니다.

FileHandler는 logs/app.log 파일에 로그를 기록합니다. 핸들러 생성 전에 Files.createDirectories()를 호출하여 logs 디렉토리가 없으면 생성합니다. FileHandler의 두 번째 파라미터 true는 append 모드를 의미하며, 기존 로그 파일 끝에 이어서 기록합니다. 파일 핸들러 생성에 실패해도 프로그램은 계속 실행되며, 콘솔 로그만 사용됩니다.

**`public static void info(String msg)`**

INFO 레벨의 일반 정보 로그를 기록합니다. 애플리케이션 시작/종료, DB 접속 성공, 트랜잭션 커밋 등 정상적인 동작을 기록할 때 사용합니다.

**`public static void warn(String msg)`**

WARNING 레벨의 경고 로그를 기록합니다. 설정 파일 로드 실패 등 프로그램 실행에는 지장이 없지만 주의가 필요한 상황에서 사용합니다.

**`public static void error(String msg, Throwable t)`**

SEVERE 레벨의 심각한 오류 로그를 기록합니다. 메시지와 함께 예외 객체(Throwable)를 받아 스택트레이스를 함께 기록합니다. 이를 통해 개발자는 오류 발생 위치와 원인을 상세히 파악할 수 있습니다.

#### 과제 요구사항 충족

- **[기능 4]** 애플리케이션 시작/종료, DB 접속 성공/실패, 트랜잭션 시작/커밋/롤백, 주요 오류를 기록합니다.
- **[기능 4]** 콘솔 로그와 파일 로그를 모두 지원합니다.

---

### 2.3.5 ErrorHandler (예외 처리)

**파일 위치:** `hw10/util/ErrorHandler.java`

ErrorHandler 클래스는 SQLException을 사용자가 이해할 수 있는 메시지로 변환하는 역할을 담당합니다. PostgreSQL의 SQLSTATE 코드를 분석하여 적절한 한국어 메시지를 반환합니다.

#### 메서드 상세 설명

**`public static String toUserMessage(SQLException e)`**

SQLException 객체를 받아 사용자 친화적인 메시지로 변환하는 메서드입니다.

먼저 e.getSQLState()를 호출하여 SQLSTATE 코드를 추출합니다. SQLSTATE는 SQL 표준에서 정의한 5자리 에러 코드로, PostgreSQL은 이를 확장하여 사용합니다. 코드가 null이면 "DB 오류가 발생했습니다."라는 일반적인 메시지를 반환합니다.

SQLSTATE 코드가 있으면 switch expression을 사용하여 코드별로 다른 메시지를 반환합니다.

| SQLSTATE | 의미 | 반환 메시지 |
|----------|------|------------|
| 23503 | foreign_key_violation | "외래키 제약조건 위반입니다. (예: 존재하지 않는 ProjectID/SupplierID/PartID/WarehouseID)" |
| 23505 | unique_violation | "중복 데이터(UNIQUE 제약조건 위반)입니다." |
| 23514 | check_violation | "값 범위/상태 값(CHECK 제약조건)을 위반했습니다." |
| 40P01 | deadlock_detected | "DB 교착상태로 트랜잭션이 실패했습니다. 잠시 후 다시 시도하세요." |
| 40001 | serialization_failure | "트랜잭션 충돌로 실패했습니다. 다시 시도하세요." |
| 기타 | - | "DB 처리 중 오류가 발생했습니다. (SQLSTATE=코드)" |

switch expression은 Java 14에서 도입된 문법으로, 화살표(->)를 사용하면 break 문이 필요 없고, 표현식으로 값을 직접 반환할 수 있습니다.

#### 과제 요구사항 충족

- **[기능 4]** 사용자에게 이해 가능한 에러 메시지를 출력합니다.
- **[기능 4]** 개발용 상세 내용은 로그로 남기도록 합니다.

---

### 2.3.6 ProjectRepository (프로젝트 DAO)

**파일 위치:** `hw10/dao/ProjectRepository.java`

ProjectRepository 클래스는 기능 1(프로젝트 대시보드)에서 필요한 모든 데이터 조회 SQL을 담당합니다. 집계 함수, GROUP BY, JOIN 등 과제에서 요구하는 SQL 기능을 활용합니다.

#### 내부 record 정의

| record | 필드 | 설명 |
|--------|------|------|
| `ProjectBasic` | projectId, shipName, shipType, contractDate, deliveryDueDate, status | 프로젝트 기본 정보 |
| `SupplierAmount` | supplierId, name, amount | 공급업체별 발주 금액 |
| `ProjectSearchItem` | projectId, shipName, shipType, status | 프로젝트 검색 결과 항목 |

record는 Java 16에서 도입된 불변 데이터 클래스로, 필드 선언만으로 생성자, getter, equals(), hashCode(), toString() 메서드가 자동 생성됩니다.

#### 메서드 상세 설명

**`public ProjectBasic findProjectById(Connection conn, int projectId) throws SQLException`**

ProjectID로 프로젝트 기본 정보를 조회하는 메서드입니다. ShipProject 테이블에서 ProjectID가 일치하는 한 건을 조회합니다.

Text Block(""" """)을 사용하여 여러 줄의 SQL 문자열을 가독성 좋게 작성했습니다. PreparedStatement의 ?에 projectId를 바인딩하여 SQL 인젝션을 방지합니다. 결과가 없으면 null을 반환하고, 있으면 ResultSet에서 값을 추출하여 ProjectBasic 객체를 생성하여 반환합니다.

**`public List<ProjectSearchItem> searchProjectsByShipName(Connection conn, String keyword, int limit) throws SQLException`**

선박명으로 프로젝트를 검색하는 메서드입니다. PostgreSQL의 ILIKE 연산자를 사용하여 대소문자 구분 없이 부분 일치 검색을 수행합니다.

검색 키워드 앞뒤에 % 와일드카드를 붙여 "%" + keyword + "%" 형태로 바인딩합니다. 이렇게 하면 키워드가 선박명의 어느 위치에 있든 검색됩니다. LIMIT 절로 결과 수를 제한하고, ORDER BY ProjectID로 정렬합니다. 결과를 List<ProjectSearchItem>으로 반환합니다.

**`public double totalOrderAmount(Connection conn, int projectId) throws SQLException`**

프로젝트의 총 발주 금액을 계산하는 메서드입니다. 과제 요구사항에서 명시한 JOIN과 SUM 집계 함수를 사용합니다.

PurchaseOrder와 PurchaseOrderLine 테이블을 POID로 JOIN하여 해당 프로젝트의 모든 발주 항목을 찾습니다. SUM(pol.Quantity * pol.UnitPriceAtOrder)로 수량과 단가를 곱한 값의 합계를 계산합니다. COALESCE 함수는 발주가 없어서 SUM 결과가 NULL인 경우 0을 반환하도록 합니다.

**`public List<SupplierAmount> topSuppliersByAmount(Connection conn, int projectId, int topN) throws SQLException`**

공급업체별 발주 금액 상위 N개를 조회하는 메서드입니다. 과제 요구사항에서 명시한 GROUP BY, ORDER BY, LIMIT을 모두 사용합니다.

세 테이블(PurchaseOrder, PurchaseOrderLine, Supplier)을 JOIN합니다. GROUP BY로 공급업체별로 그룹화하고, SUM으로 각 그룹의 발주 금액을 집계합니다. ORDER BY amount DESC로 금액 내림차순 정렬하고, LIMIT으로 상위 N개만 반환합니다.

**`public double emissionSumByType(Connection conn, int projectId, String emissionType) throws SQLException`**

특정 배출 유형(운송 또는 보관)의 탄소배출 합계를 계산하는 메서드입니다.

CarbonEmissionRecord 테이블에서 두 가지 조건을 OR로 결합합니다. 첫째, 프로젝트에 직접 연결된 레코드(c.ProjectID = ?). 둘째, 해당 프로젝트의 발주서에 연결된 Delivery를 통해 간접적으로 연결된 레코드. 두 번째 조건을 위해 서브쿼리를 사용하여 해당 프로젝트의 모든 DeliveryID를 찾습니다.

**`public double emissionSumTotal(Connection conn, int projectId) throws SQLException`**

프로젝트 전체 탄소배출 합계를 계산하는 메서드입니다. emissionSumByType()과 동일한 로직이지만 EmissionType 필터가 없어 모든 유형의 배출량을 합산합니다.

#### 과제 요구사항 충족

- **[기능 1]** 총 발주 금액을 발주서+발주항목 기준으로 집계합니다.
- **[기능 1]** 공급업체별 발주 금액 상위 3개를 조회합니다.
- **[기능 1]** 운송/보관/전체 탄소배출 합계를 계산합니다.
- **[기능 1]** 집계 함수, GROUP BY, JOIN을 코드 안에서 활용합니다.

---

### 2.3.7 SupplierRepository (공급업체 DAO)

**파일 위치:** `hw10/dao/SupplierRepository.java`

SupplierRepository 클래스는 기능 3(공급업체 ESG 및 지연 납품 리포트)에서 필요한 데이터 조회를 담당합니다. WITH CTE(Common Table Expression)를 활용한 복잡한 쿼리와 동적 필터링을 구현합니다.

#### 내부 record 정의

| record | 필드 | 설명 |
|--------|------|------|
| `SupplierRow` | supplierId, name, country, esgGrade, totalOrderAmount, delayedDeliveries, totalDeliveries, delayRatio | 공급업체 목록 행 (지연 비율 포함) |
| `SupplierPoRow` | poid, orderDate, status, delayed | 발주서 상세 행 (지연 여부 포함) |

#### 메서드 상세 설명

**`public List<SupplierRow> listSuppliers(Connection conn, List<String> esgGrades, Double minRatio, Double maxRatio) throws SQLException`**

ESG 등급 및 지연 비율 필터를 적용하여 공급업체 목록을 조회하는 메서드입니다. 복잡한 쿼리를 WITH CTE로 구조화했습니다.

SQL은 세 개의 CTE를 정의합니다. 첫 번째 order_totals CTE는 공급업체별 총 발주 금액을 계산합니다. PurchaseOrder와 PurchaseOrderLine을 JOIN하고 SupplierID로 GROUP BY하여 SUM을 구합니다.

두 번째 delivery_stats CTE는 공급업체별 전체 납품 건수와 지연 납품 건수를 계산합니다. CASE WHEN d.Status = '지연' THEN 1 ELSE 0 END 표현식으로 지연 건수를 카운트합니다. 이는 과제 요구사항의 "지연 납품 비율은 Delivery.status = '지연' 기준으로 계산"을 충족합니다.

세 번째 base CTE는 Supplier 테이블에 앞의 두 CTE를 LEFT JOIN하여 모든 정보를 통합합니다. LEFT JOIN을 사용하므로 발주나 납품이 없는 공급업체도 결과에 포함됩니다. COALESCE로 NULL 값을 0으로 대체하고, CASE WHEN으로 지연 비율을 계산합니다(전체 건수가 0이면 비율도 0).

필터링은 동적으로 구성됩니다. StringBuilder를 사용하여 SQL 문자열을 조립하고, 파라미터 리스트를 별도로 관리합니다. WHERE 1=1 트릭을 사용하여 이후 AND 조건을 쉽게 추가할 수 있도록 합니다.

ESG 등급 필터가 제공되면 AND ESGGrade IN (?, ?, ...) 절을 동적으로 생성합니다. 지연 비율 상한/하한이 제공되면 >= ? 또는 <= ? 조건을 추가합니다. 마지막으로 발주금액 내림차순, 지연비율 내림차순, SupplierID 순으로 정렬합니다.

**`public List<SupplierPoRow> recentPurchaseOrders(Connection conn, int supplierId, int limit, int offset) throws SQLException`**

특정 공급업체의 최근 발주서를 페이징하여 조회하는 메서드입니다. 과제 요구사항의 "최근 N개 발주서 목록"과 "페이징" 기능을 구현합니다.

각 발주서가 지연 납품을 가지고 있는지 확인하기 위해 EXISTS 서브쿼리를 사용합니다. EXISTS (SELECT 1 FROM Delivery d WHERE d.POID = po.POID AND d.Status = '지연')은 해당 발주서에 지연 상태의 납품이 하나라도 있으면 TRUE를 반환합니다.

ORDER BY po.OrderDate DESC, po.POID DESC로 최신 발주서가 먼저 나오도록 정렬합니다. LIMIT과 OFFSET으로 페이징을 구현합니다. limit은 한 페이지에 표시할 개수, offset은 건너뛸 개수(페이지 번호 * 페이지 크기)입니다.

#### 과제 요구사항 충족

- **[기능 3]** 공급업체별 지연 납품 비율(지연건수/전체건수)을 계산합니다.
- **[기능 3]** ESG 등급(A~D) 다중 선택 필터를 지원합니다.
- **[기능 3]** 지연 납품 비율 상한/하한 필터를 지원합니다.
- **[기능 3]** 최근 N개 발주서 목록을 페이징으로 조회합니다.

---

### 2.3.8 OrderRepository (발주 DAO)

**파일 위치:** `hw10/dao/OrderRepository.java`

OrderRepository 클래스는 발주서(PurchaseOrder) 및 발주항목(PurchaseOrderLine) 테이블에 대한 INSERT 작업을 담당합니다.

#### 메서드 상세 설명

**`public void insertPurchaseOrder(Connection conn, int poid, Date orderDate, String status, String engineerName, int projectId, int supplierId) throws SQLException`**

PurchaseOrder 테이블에 새 발주서를 삽입하는 메서드입니다.

파라미터로 POID(발주서 ID), 발주일, 상태, 담당 엔지니어명, 프로젝트 ID, 공급업체 ID를 받습니다. POID는 SequenceGenerator를 통해 미리 생성된 값을 받습니다. 상태(status)는 과제 요구사항에 따라 '요청' 또는 '발주완료' 값을 사용합니다.

PreparedStatement의 ?에 각 파라미터를 순서대로 바인딩하고 executeUpdate()를 호출하여 INSERT를 실행합니다. 외래키 제약조건 위반(존재하지 않는 ProjectID, SupplierID 참조) 시 SQLException이 발생합니다.

**`public void insertPurchaseOrderLine(Connection conn, int poid, int lineNo, int partId, int qty, double unitPrice, Date requestedDueDate) throws SQLException`**

PurchaseOrderLine 테이블에 발주 항목을 삽입하는 메서드입니다.

POID와 LineNo가 복합 기본키를 구성합니다. LineNo는 호출자가 1부터 순차적으로 증가시켜 전달합니다. UnitPriceAtOrder는 발주 시점의 단가를 기록하여 나중에 가격이 변경되더라도 발주 당시 금액을 추적할 수 있도록 합니다. requestedDueDate는 null이 허용되며, null을 전달하면 SQL NULL이 삽입됩니다.

#### 과제 요구사항 충족

- **[기능 2]** PurchaseOrder에 발주서 1건을 삽입합니다.
- **[기능 2]** PurchaseOrderLine에 각 발주 항목을 삽입합니다.

---

### 2.3.9 DeliveryRepository (납품 DAO)

**파일 위치:** `hw10/dao/DeliveryRepository.java`

DeliveryRepository 클래스는 납품(Delivery) 및 납품상세(DeliveryLine) 테이블에 대한 INSERT 작업을 담당합니다.

#### 메서드 상세 설명

**`public void insertDelivery(Connection conn, int deliveryId, int poid, Date actualArrivalDate, String transportMode, Double distanceKm, String status) throws SQLException`**

Delivery 테이블에 납품 기록을 삽입하는 메서드입니다.

파라미터로 납품 ID, 발주서 ID, 실제 도착일, 운송 수단, 운송 거리, 상태를 받습니다. distanceKm는 Double 타입(래퍼 클래스)으로 선언되어 null을 허용합니다. null인 경우 ps.setNull(5, java.sql.Types.DOUBLE)을 호출하여 SQL NULL을 삽입합니다.

과제 요구사항에 따라 actualArrivalDate는 오늘 날짜, status는 '정상입고' 또는 '지연' 등의 값을 사용합니다.

**`public void insertDeliveryLine(Connection conn, int deliveryId, int poid, int lineNo, int receivedQty, String inspectionResult) throws SQLException`**

DeliveryLine 테이블에 납품 상세를 삽입하는 메서드입니다.

DeliveryID, POID, LineNo가 복합 기본키를 구성합니다. receivedQty는 실제 입고된 수량이며, 과제 요구사항에 따라 발주 수량의 50%가 자동 계산되어 전달됩니다. inspectionResult는 검수 결과를 기록합니다.

#### 과제 요구사항 충족

- **[기능 2]** Delivery에 레코드 1건을 삽입합니다 (delivery_date = 오늘, status = '정상입고').
- **[기능 2]** DeliveryLine에 각 항목의 delivered_qty 일부(50%)를 삽입합니다.

---

### 2.3.10 InventoryRepository (재고 DAO)

**파일 위치:** `hw10/dao/InventoryRepository.java`

InventoryRepository 클래스는 재고(Inventory) 테이블에 대한 UPSERT(INSERT 또는 UPDATE) 작업을 담당합니다. PostgreSQL의 ON CONFLICT DO UPDATE 문법을 활용합니다.

#### 메서드 상세 설명

**`public void addInventory(Connection conn, int warehouseId, int partId, int deltaQty) throws SQLException`**

해당 창고·부품 조합이 없으면 INSERT, 있으면 수량을 UPDATE하는 메서드입니다.

SQL 문은 다음과 같은 구조를 가집니다:
```sql
INSERT INTO Inventory(WarehouseID, PartID, Quantity)
VALUES (?, ?, ?)
ON CONFLICT (WarehouseID, PartID)
DO UPDATE SET Quantity = Inventory.Quantity + EXCLUDED.Quantity
```

먼저 INSERT를 시도합니다. Inventory 테이블은 (WarehouseID, PartID)에 UNIQUE 제약조건이 있으므로, 같은 창고-부품 조합이 이미 존재하면 CONFLICT가 발생합니다.

ON CONFLICT (WarehouseID, PartID) 절은 이 UNIQUE 제약조건에서 충돌이 발생했을 때의 동작을 정의합니다. DO UPDATE SET 절에서 Inventory.Quantity는 기존 레코드의 수량이고, EXCLUDED.Quantity는 INSERT 하려던 값(deltaQty)입니다. 따라서 기존 수량에 추가 수량을 더한 결과로 UPDATE됩니다.

이 UPSERT 연산은 원자적으로 실행되므로 동시성 문제 없이 안전하게 재고를 증가시킬 수 있습니다.

#### 과제 요구사항 충족

- **[기능 2]** 창고·부품별 Inventory 수량을 증가시킵니다.
- **[기능 2]** 기존 레코드가 없으면 신규 INSERT, 있으면 UPDATE합니다.

---

### 2.3.11 SequenceGenerator (PK 생성기)

**파일 위치:** `hw10/dao/SequenceGenerator.java`

SequenceGenerator 클래스는 스키마에 SEQUENCE/SERIAL이 없는 테이블에 대해 새 PK 값을 생성합니다. MAX(id)+1 방식을 사용합니다.

#### 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `ALLOWED` | Set<String> | 허용된 테이블.컬럼 목록 (SQL 인젝션 방지) |

ALLOWED Set에는 "PurchaseOrder.POID", "Delivery.DeliveryID", "CarbonEmissionRecord.RecordID" 세 가지가 등록되어 있습니다.

#### 메서드 상세 설명

**`public int nextId(Connection conn, String tableDotCol) throws SQLException`**

지정된 테이블.컬럼의 MAX 값 + 1을 반환하는 메서드입니다.

먼저 tableDotCol이 ALLOWED Set에 포함되어 있는지 확인합니다. 포함되어 있지 않으면 IllegalArgumentException을 발생시킵니다. 이는 SQL 인젝션 공격을 방지하기 위한 화이트리스트 방식의 보안 조치입니다.

tableDotCol을 점(.)으로 분리하여 테이블명과 컬럼명을 추출합니다. SQL 문 SELECT COALESCE(MAX(col), 0) + 1 AS next_id FROM table을 실행하여 결과를 반환합니다. COALESCE는 테이블이 비어있어 MAX가 NULL인 경우 0을 반환하도록 하여, 첫 번째 레코드의 ID가 1이 되도록 합니다.

주의: 이 방식은 동시에 여러 트랜잭션이 nextId()를 호출하면 같은 ID를 받을 수 있어 동시성에 취약합니다. 실무에서는 PostgreSQL의 SEQUENCE를 사용하는 것이 권장되지만, 과제 데모 수준에서는 이 방식으로 충분합니다.

---

### 2.3.12 OrderTransactionService (트랜잭션 서비스)

**파일 위치:** `hw10/service/OrderTransactionService.java`

OrderTransactionService 클래스는 기능 2(발주 등록 처리)의 핵심 비즈니스 로직을 담당합니다. 여러 DAO를 조합하여 하나의 트랜잭션으로 묶어 처리하며, 과제에서 요구하는 모든 트랜잭션 관련 요구사항을 구현합니다.

#### 내부 record 정의

| record | 필드 | 설명 |
|--------|------|------|
| `OrderLineInput` | partId, quantity, unitPrice | 발주 항목 입력 데이터 |
| `TransactionResult` | poid, deliveryId | 트랜잭션 결과 (생성된 ID들) |

#### 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `db` | DatabaseConnection | DB 연결 객체 |
| `sequenceGenerator` | SequenceGenerator | PK 생성기 |
| `orderRepository` | OrderRepository | 발주 DAO |
| `deliveryRepository` | DeliveryRepository | 납품 DAO |
| `inventoryRepository` | InventoryRepository | 재고 DAO |

#### 메서드 상세 설명

**`public TransactionResult createOrderWithInitialDelivery(int projectId, int supplierId, String engineerName, List<OrderLineInput> lines, int warehouseId, String transportMode, Double distanceKm) throws Exception`**

발주서 생성, 발주항목 생성, 납품 생성, 납품상세 생성, 재고 반영을 하나의 트랜잭션으로 처리하는 메서드입니다.

메서드는 최대 3회까지 재시도하는 루프로 시작합니다. 이는 과제 요구사항의 "교착상태가 표시되어 트랜잭션이 실패하면 트랜잭션을 다시 시도한다"를 충족합니다.

각 시도에서 먼저 db.openConnection()으로 새 Connection을 열고, setAutoCommit(false)를 호출하여 트랜잭션을 시작합니다. JDBC에서 auto-commit을 끄면 명시적으로 commit() 또는 rollback()을 호출할 때까지 모든 SQL이 하나의 트랜잭션으로 묶입니다.

setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED)를 호출하여 트랜잭션 격리 수준을 READ_COMMITTED로 설정합니다. 이 격리 수준에서는 커밋된 데이터만 읽을 수 있어 dirty read가 방지됩니다. 이는 과제 요구사항의 "트랜잭션 isolation level을 적절하게 설정한다"를 충족합니다.

트랜잭션 내에서는 다음 순서로 작업을 수행합니다:

1. **발주서 생성**: SequenceGenerator로 새 POID를 생성하고 OrderRepository.insertPurchaseOrder()로 발주서를 삽입합니다. 상태는 '요청'으로 설정합니다.

2. **발주 항목 생성**: 각 발주 항목에 대해 OrderRepository.insertPurchaseOrderLine()을 호출합니다. lineNo는 1부터 순차적으로 증가합니다.

3. **초기 납품 생성**: SequenceGenerator로 새 DeliveryID를 생성하고 DeliveryRepository.insertDelivery()로 납품 기록을 삽입합니다. 날짜는 오늘, 상태는 '정상입고'입니다.

4. **납품 상세 + 재고 반영**: 각 발주 항목에 대해 발주 수량의 50%(올림)를 계산하여 DeliveryLine을 삽입하고, InventoryRepository.addInventory()로 재고를 증가시킵니다. 50% 올림 계산은 (quantity + 1) / 2로 구현합니다.

모든 작업이 성공하면 conn.commit()을 호출하여 트랜잭션을 커밋하고 결과를 반환합니다. Logger.info()로 트랜잭션 커밋 로그를 기록합니다.

중간에 SQLException이 발생하면 catch 블록에서 conn.rollback()을 호출하여 모든 변경사항을 취소합니다. Logger.info()로 트랜잭션 롤백 로그를 기록합니다.

교착상태(SQLSTATE 40P01) 발생 시에는 즉시 예외를 던지지 않고 루프를 계속하여 재시도합니다. Thread.sleep()으로 짧은 대기 시간을 두어 교착상태가 해소될 시간을 줍니다. 대기 시간은 시도 횟수에 비례하여 증가합니다(200ms, 400ms, 600ms).

#### 과제 요구사항 충족

- **[기능 2]** 전 과정을 하나의 트랜잭션으로 묶습니다.
- **[기능 2]** 중간 오류 발생 시 전체 작업을 롤백하고 "발주 등록 실패" 메시지를 출력합니다.
- **[기능 2]** 정상 완료 시 커밋하고 "발주 등록 완료" 메시지를 출력합니다.
- **[기능 2]** 명시적으로 setAutoCommit(false)/commit()/rollback()을 사용합니다.
- **[구현 참고사항 2]** 트랜잭션 isolation level을 READ_COMMITTED로 설정합니다.
- **[구현 참고사항 3]** 교착상태 발생 시 트랜잭션을 재시도합니다.
- **[기능 4]** 트랜잭션 시작/커밋/롤백 로그를 기록합니다.

---

### (변경 가능) 2.3.13 ConsoleMenu (메인 메뉴 UI)

**파일 위치:** `hw10/ui/ConsoleMenu.java`

> ⚠️ **변경 가능**: 이 클래스는 콘솔 기반 UI입니다. 웹(Spring/Flask) 또는 GUI로 교체될 수 있습니다.

ConsoleMenu 클래스는 콘솔 기반의 메인 메뉴 UI를 제공합니다. 무한 루프에서 사용자 입력을 받아 각 기능으로 분기합니다.

#### 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `db` | DatabaseConnection | DB 연결 객체 |
| `sc` | Scanner | 콘솔 입력용 Scanner |

#### 메서드 상세 설명

**`public ConsoleMenu(DatabaseConnection db)`**

생성자는 DatabaseConnection 객체를 받아 저장합니다. Scanner는 System.in으로 초기화하여 표준 입력에서 읽습니다.

**`public void run()`**

메뉴를 출력하고 사용자 선택에 따라 기능을 실행하는 메서드입니다.

무한 루프(while(true)) 안에서 동작합니다. 매 반복마다 메뉴 옵션(1: 프로젝트 대시보드, 2: 발주 등록, 3: 공급업체 리포트, 0: 종료)을 출력하고 Scanner로 사용자 입력을 받습니다.

switch expression으로 입력값에 따라 분기합니다. "1"이면 ProjectDashboard.run(), "2"이면 OrderRegistration.run(), "3"이면 SupplierReport.run()을 호출합니다. "0"이면 return 문으로 메서드를 종료합니다. 그 외 입력은 안내 메시지를 출력하고 루프를 계속합니다.

try-catch 블록으로 각 기능 실행을 감싸서 예외가 발생해도 프로그램이 중단되지 않도록 합니다. SQLException 발생 시 ErrorHandler.toUserMessage()를 통해 사용자 친화적 메시지를 출력합니다. 모든 예외에 대해 Logger.error()로 상세 오류 정보를 기록합니다.

---

### (변경 가능) 2.3.14 ProjectDashboard (기능 1 UI)

**파일 위치:** `hw10/ui/ProjectDashboard.java`

> ⚠️ **변경 가능**: 이 클래스는 콘솔 기반 UI입니다. 웹 또는 GUI로 교체될 수 있습니다.

ProjectDashboard 클래스는 기능 1(프로젝트 대시보드)의 사용자 인터페이스를 담당합니다.

#### 메서드 상세 설명

**`public static void run(DatabaseConnection db, Scanner sc) throws Exception`**

프로젝트ID/선박명을 입력받고, ProjectRepository를 통해 데이터를 조회하여 화면에 출력하는 메서드입니다.

먼저 InputHelper.readLine()으로 프로젝트ID 또는 선박명을 입력받습니다. 빈 입력이면 안내 메시지를 출력하고 종료합니다.

입력값이 숫자로만 구성되어 있는지 InputHelper.isAllDigits()로 확인합니다. 숫자면 바로 ProjectID로 사용하고, 아니면 선박명으로 검색합니다.

선박명 검색의 경우 searchProjectsByShipName()으로 최대 10개의 결과를 조회하여 보여주고, 사용자에게 조회할 ProjectID를 다시 입력받습니다. 검색 결과가 없으면 안내 메시지를 출력하고 종료합니다.

ProjectID가 확정되면 순차적으로 데이터를 조회합니다: findProjectById()로 기본 정보, totalOrderAmount()로 총 발주 금액, topSuppliersByAmount()로 상위 3개 공급업체, emissionSumByType()으로 운송/보관 탄소배출, emissionSumTotal()로 전체 탄소배출.

마지막으로 탄소 집약도 지표를 계산합니다. 공식은 totalEmission / (totalCost / 1_000_000.0)입니다. 총 발주 금액이 0인 경우 0으로 나누기 오류를 방지하기 위해 "N/A"를 출력합니다.

**`private static String nvl(Object o)`**

null이면 "-"를, 아니면 String.valueOf(o)를 반환하는 헬퍼 메서드입니다. 출력할 때 null 대신 보기 좋은 문자열을 표시하기 위해 사용합니다.

---

### (변경 가능) 2.3.15 OrderRegistration (기능 2 UI)

**파일 위치:** `hw10/ui/OrderRegistration.java`

> ⚠️ **변경 가능**: 이 클래스는 콘솔 기반 UI입니다. 웹 또는 GUI로 교체될 수 있습니다.

OrderRegistration 클래스는 기능 2(발주 및 납품 관리)의 사용자 인터페이스를 담당합니다.

#### 메서드 상세 설명

**`public static void run(DatabaseConnection db, Scanner sc) throws Exception`**

발주 정보를 입력받고 OrderTransactionService를 호출하여 트랜잭션을 실행하는 메서드입니다.

단계별로 사용자 입력을 받습니다. InputHelper.readInt()로 ProjectID와 SupplierID를 필수로 입력받고, InputHelper.readLine()으로 담당 엔지니어명을 선택적으로 입력받습니다.

발주 항목 개수를 입력받고, 각 항목에 대해 PartID, Quantity, UnitPrice를 반복적으로 입력받습니다. 수량은 1 이상, 단가는 0 이상이어야 하며, 조건을 만족하지 않으면 안내 메시지를 출력하고 종료합니다.

입고할 창고ID와 운송 정보(운송 수단, 거리)를 입력받습니다. 운송 수단은 빈 입력 시 기본값 "트럭"을 사용합니다.

모든 입력이 완료되면 OrderTransactionService.createOrderWithInitialDelivery()를 호출합니다. 성공하면 생성된 POID와 DeliveryID를 출력합니다. SQLException 발생 시 ErrorHandler.toUserMessage()로 사용자 친화적 메시지를 출력하고, 모든 예외에 대해 Logger.error()로 상세 오류를 기록합니다.

---

### (변경 가능) 2.3.16 SupplierReport (기능 3 UI)

**파일 위치:** `hw10/ui/SupplierReport.java`

> ⚠️ **변경 가능**: 이 클래스는 콘솔 기반 UI입니다. 웹 또는 GUI로 교체될 수 있습니다.

SupplierReport 클래스는 기능 3(공급업체 ESG 및 지연 납품 리포트)의 사용자 인터페이스를 담당합니다.

#### 메서드 상세 설명

**`public static void run(DatabaseConnection db, Scanner sc) throws Exception`**

ESG 등급/지연비율 필터를 입력받고 공급업체 목록을 출력하는 메서드입니다.

InputHelper.readCsvTokensUpper()로 ESG 등급 필터를 입력받습니다. 콤마로 구분하여 여러 개를 입력할 수 있습니다(예: "A,B"). InputHelper.readDoubleOptional()로 지연 비율 상한/하한을 퍼센트 단위로 입력받고, 내부적으로 0~1 비율로 변환합니다.

SupplierRepository.listSuppliers()로 필터 조건에 맞는 공급업체 목록을 조회합니다. 결과가 없으면 안내 메시지를 출력하고 종료합니다.

표 형태로 포맷팅하여 출력합니다. 지연 비율은 "33.3% (1/3)" 형태로 비율과 건수를 함께 표시합니다.

목록 출력 후 상세 보기 루프에 진입합니다. SupplierID를 입력하면 showSupplierDetail()을 호출하고, 빈 입력이면 메인 메뉴로 돌아갑니다.

**`private static void showSupplierDetail(Connection conn, SupplierRepository repository, Scanner sc, int supplierId) throws Exception`**

특정 공급업체의 최근 발주서를 페이징으로 보여주는 메서드입니다.

한 페이지에 표시할 개수를 입력받습니다(기본 5개). 페이지 번호를 관리하며 SupplierRepository.recentPurchaseOrders()로 해당 페이지의 발주서를 조회합니다.

'n'(다음), 'p'(이전), 'q'(종료) 키로 페이지를 이동합니다. 첫 페이지에서 'p'를 누르면 "이미 첫 페이지입니다" 메시지를, 더 이상 데이터가 없는 페이지에서 'n'을 누르면 "다음 페이지가 없습니다" 메시지를 출력합니다.

**`private static String nvl(Object o)`**

null이면 "-"를 반환하는 헬퍼 메서드입니다.

**`private static String cut(String s, int max)`**

문자열이 max보다 길면 잘라서 "…"를 붙이는 헬퍼 메서드입니다. 표 출력 시 긴 문자열이 레이아웃을 깨지 않도록 합니다.

---

### (변경 가능) 2.3.17 InputHelper (콘솔 입력 유틸)

**파일 위치:** `hw10/util/InputHelper.java`

> ⚠️ **변경 가능**: 이 클래스는 콘솔 입력용입니다. 웹/GUI에서는 다른 입력 처리 방식을 사용합니다.

InputHelper 클래스는 콘솔에서 사용자 입력을 안전하게 처리하는 유틸리티 함수들을 제공합니다.

#### 메서드 상세 설명

**`public static String readLine(Scanner sc, String prompt)`**

프롬프트를 출력하고 한 줄을 입력받아 앞뒤 공백을 제거하여 반환하는 메서드입니다. System.out.print()로 프롬프트를 출력하고, sc.nextLine().trim()으로 입력을 받습니다.

**`public static int readInt(Scanner sc, String prompt)`**

정수를 필수로 입력받는 메서드입니다. 무한 루프 안에서 readLine()으로 입력을 받고 Integer.parseInt()로 변환을 시도합니다. NumberFormatException이 발생하면 안내 메시지를 출력하고 다시 입력을 받습니다.

**`public static Integer readIntOptional(Scanner sc, String prompt)`**

정수를 선택적으로 입력받는 메서드입니다. 빈 입력이면 null을 반환하고, 그렇지 않으면 정수로 변환합니다. 변환 실패 시 안내 메시지를 출력하고 다시 입력을 받습니다.

**`public static Double readDoubleOptional(Scanner sc, String prompt)`**

실수를 선택적으로 입력받는 메서드입니다. readIntOptional()과 동일한 로직이지만 Double.parseDouble()을 사용합니다.

**`public static List<String> readCsvTokensUpper(Scanner sc, String prompt)`**

콤마로 구분된 문자열을 입력받아 리스트로 반환하는 메서드입니다. 입력을 콤마로 분리하고, 각 토큰에 trim()과 toUpperCase()를 적용하여 리스트에 추가합니다. 빈 입력이면 빈 리스트를 반환합니다.

**`public static boolean isAllDigits(String s)`**

문자열이 숫자로만 구성되어 있는지 확인하는 메서드입니다. null이나 빈 문자열이면 false를 반환합니다. 각 문자에 대해 Character.isDigit()을 호출하여 숫자가 아닌 문자가 있으면 false를 반환합니다. ProjectID(숫자)인지 선박명(문자열)인지 판단하는 데 사용됩니다.

---

## 2.4 레이어별 역할 요약

| 레이어 | 클래스들 | 역할 | 변경 가능 |
|--------|---------|------|----------|
| **Entry Point** | Application | 프로그램 시작/종료, 의존성 연결 | 고정 |
| **Config** | DatabaseConfig | 환경설정 로드 | 고정 |
| **DB** | DatabaseConnection | DB 연결 관리 | 고정 |
| **DAO** | ProjectRepository, SupplierRepository, OrderRepository, DeliveryRepository, InventoryRepository, SequenceGenerator | SQL 실행 및 데이터 접근 | 고정 |
| **Service** | OrderTransactionService | 비즈니스 로직, 트랜잭션 관리 | 고정 |
| **UI** | ConsoleMenu, ProjectDashboard, OrderRegistration, SupplierReport | 사용자 인터페이스 | **(변경 가능)** |
| **Util** | Logger, ErrorHandler, InputHelper | 공통 유틸리티 | InputHelper만 변경 가능 |

---

## 2.5 과제 요구사항 매핑 요약

| 요구사항 | 구현 클래스 | 핵심 구현 내용 |
|----------|------------|---------------|
| 기능 1: 프로젝트 대시보드 | ProjectRepository, ProjectDashboard | JOIN, SUM, GROUP BY, 탄소집약도 계산 |
| 기능 2: 트랜잭션 처리 | OrderTransactionService, 각 DAO | setAutoCommit, commit, rollback, 교착상태 재시도 |
| 기능 3: 공급업체 리포트 | SupplierRepository, SupplierReport | WITH CTE, 동적 필터, 페이징 |
| 기능 4: 예외처리/로그/환경설정 | Logger, ErrorHandler, DatabaseConfig | 파일/콘솔 로그, SQLSTATE 변환, config.properties |









## 2.6 변경 내역

### v1.0.0 - 최초 생성

- 클래스 다이어그램 및 패키지 구조 작성
- 기본 클래스 설명 추가

### v1.0.1 - 메서드 상세 설명 추가

✅ **전체 17개 클래스의 모든 메서드 상세 설명 추가**

| 클래스 | 설명된 메서드 | 변경 가능 |
|--------|--------------|----------|
| Application | `main()` | 고정 |
| DatabaseConfig | 생성자, `load()`, `notBlank()` | 고정 |
| DatabaseConnection | 생성자, `openConnection()`, `close()` | 고정 |
| Logger | `init()`, `info()`, `warn()`, `error()` | 고정 |
| ErrorHandler | `toUserMessage()` | 고정 |
| ProjectRepository | `findProjectById()`, `searchProjectsByShipName()`, `totalOrderAmount()`, `topSuppliersByAmount()`, `emissionSumByType()`, `emissionSumTotal()` | 고정 |
| SupplierRepository | `listSuppliers()`, `recentPurchaseOrders()` | 고정 |
| OrderRepository | `insertPurchaseOrder()`, `insertPurchaseOrderLine()` | 고정 |
| DeliveryRepository | `insertDelivery()`, `insertDeliveryLine()` | 고정 |
| InventoryRepository | `addInventory()` | 고정 |
| SequenceGenerator | `nextId()` | 고정 |
| OrderTransactionService | `createOrderWithInitialDelivery()` | 고정 |
| ConsoleMenu | 생성자, `run()` | **(변경 가능)** ⚠️ |
| ProjectDashboard | `run()`, `nvl()` | **(변경 가능)** ⚠️ |
| OrderRegistration | `run()` | **(변경 가능)** ⚠️ |
| SupplierReport | `run()`, `showSupplierDetail()`, `nvl()`, `cut()` | **(변경 가능)** ⚠️ |
| InputHelper | `readLine()`, `readInt()`, `readIntOptional()`, `readDoubleOptional()`, `readCsvTokensUpper()`, `isAllDigits()` | **(변경 가능)** ⚠️ |

✅ **각 메서드별 포함 내용**

| 항목 | 설명 |
|------|------|
| 파라미터/반환값 | 각 메서드의 입력과 출력 설명 |
| 알고리즘/동작 흐름 | 줄글로 상세 설명 |
| SQL 쿼리 설명 | DAO 클래스의 쿼리 동작 방식 |
| 트랜잭션 처리 | Service 클래스의 트랜잭션 관리 |
| 요구사항 매핑 | 과제 요구사항 충족 여부 |