# 2. 구현한 클래스 설명

본 문서에서는 탄소중립 스마트 SCM 플랫폼 애플리케이션의 클래스 구조와 **모든 클래스 및 메서드**에 대해 상세히 설명합니다.

---

## 2.1 클래스 다이어그램 (Class Diagram)

### 전체 아키텍처 (Layered Architecture)

애플리케이션은 **Client** -> **Controller** -> **Service** -> **Repository** -> **Database** 순서의 계층적 구조를 가집니다.

```
[ Client ]
    │ (HTTP Request)
    ▼
[ Controller Layer ] ──────────────────────────┐
│ MainController, ProjectController, etc.      │ ◀── API 엔드포인트
└──────────────────────────────────────────────┘
    │ (DTO)
    ▼
[ Service Layer ] ─────────────────────────────┐
│ MainService, OrderService, ProjectService... │ ◀── 비즈니스 로직 & 트랜잭션
└──────────────────────────────────────────────┘
    │ (Domain Objects)
    ▼
[ Repository Layer ] ──────────────────────────┐ ──▶ [ SequenceGenerator ]
│ ProjectRepository, OrderRepository...        │      (PK 생성)
└──────────────────────────────────────────────┘
    │ (JDBC / SQL)
    ▼
[ Database (PostgreSQL) ]
```

---

## 2.2 패키지 및 클래스 상세 분석

### 2.2.1 `hw10` 패키지 (메인)

#### 1. `Application` 클래스

**파일:** `hw10/Application.java`  
**패키지:** `hw10`  
**어노테이션:** `@SpringBootApplication`  
**역할:** Spring Boot 애플리케이션의 시작점(Entry Point)입니다. 애플리케이션 전체 생명주기를 관리합니다.

##### 클래스 구조 및 의존성

이 클래스는 Spring Boot의 자동 설정 기능을 활용하여 웹 애플리케이션을 구동합니다. `@SpringBootApplication` 어노테이션은 다음 세 가지 어노테이션을 포함합니다:
- `@Configuration`: 스프링 설정 클래스로 인식
- `@EnableAutoConfiguration`: 자동 설정 활성화
- `@ComponentScan`: 컴포넌트 스캔 활성화

##### 메서드 상세 설명

**`public static void main(String[] args)`**
- **매개변수:** `String[] args` - 명령줄 인자 배열
- **반환값:** 없음 (void)
- **동작 방식:**
  1. 프로그램 실행 시 가장 먼저 호출되는 진입점(Entry Point)입니다.
  2. `Logger.init()` 메서드를 호출하여 로깅 시스템을 초기화합니다. 이는 모든 로그 기록이 정상적으로 작동하도록 보장합니다.
  3. `Logger.info("애플리케이션 시작")`을 통해 애플리케이션 시작을 로그에 기록합니다.
  4. `SpringApplication.run(Application.class, args)`를 호출하여:
     - Spring IoC 컨테이너를 초기화하고 모든 빈(Bean)을 생성합니다.
     - 내장 톰캣 서버를 시작하여 HTTP 요청을 수신할 준비를 합니다.
     - 기본 포트는 8080입니다.
  5. 서버 시작 완료 후 `Logger.info("서버 시작 완료: http://localhost:8080")` 메시지를 출력합니다.
- **예외 처리:** Spring Boot가 내부적으로 예외를 처리하며, 초기화 실패 시 애플리케이션이 종료됩니다.

---

### 2.2.2 `hw10.config` 패키지 (설정)

#### 1. `DatabaseConfig` 클래스

**파일:** `hw10/config/DatabaseConfig.java`  
**패키지:** `hw10.config`  
**접근 제어자:** `public final class` (불변 클래스)  
**역할:** 데이터베이스 연결 정보를 다양한 소스로부터 로드하고 관리하는 설정 클래스입니다. 환경변수, 설정 파일 등 여러 소스를 우선순위에 따라 확인합니다.

##### 클래스 구조 및 필드

```java
public final String dbUrl;      // 데이터베이스 연결 URL
public final String dbUser;     // 데이터베이스 사용자명
public final String dbPassword; // 데이터베이스 비밀번호
```

모든 필드는 `final`로 선언되어 불변성을 보장합니다. 생성자에서 한 번 설정되면 변경할 수 없습니다.

##### 생성자 상세 설명

**`private DatabaseConfig(String dbUrl, String dbUser, String dbPassword)`**
- **접근 제어자:** `private` (외부에서 직접 인스턴스화 불가, `load()` 메서드를 통해서만 생성)
- **매개변수:**
  - `String dbUrl`: PostgreSQL 연결 URL (예: `jdbc:postgresql://localhost:5432/scmdb`)
  - `String dbUser`: 데이터베이스 사용자명
  - `String dbPassword`: 데이터베이스 비밀번호
- **동작 방식:**
  - 세 개의 매개변수를 받아 각각의 `final` 필드에 할당합니다.
  - 불변 객체 패턴을 사용하여 설정 값이 런타임에 변경되지 않도록 보장합니다.
- **반환값:** 없음 (생성자)

##### 메서드 상세 설명

**`public static DatabaseConfig load()`**
- **접근 제어자:** `public static`
- **매개변수:** 없음
- **반환값:** `DatabaseConfig` 객체 (설정이 없으면 모든 필드가 null인 객체)
- **동작 방식:**
  이 메서드는 다음 우선순위에 따라 데이터베이스 설정을 로드합니다:
  
  **1순위: 시스템 환경변수**
  - `System.getenv("DB_URL")`, `System.getenv("DB_USER")`, `System.getenv("DB_PASSWORD")`를 확인합니다.
  - 모든 값이 존재하면 즉시 `DatabaseConfig` 객체를 생성하여 반환합니다.
  - 환경변수는 운영 환경에서 보안상 가장 안전한 방법입니다.
  
  **2순위: application.properties (클래스패스)**
  - `loadPropertiesFromClasspath("application.properties")`를 호출합니다.
  - Spring Boot 표준 설정 파일 형식을 따릅니다.
  - `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password` 키를 읽습니다.
  - 모든 값이 존재하면 `DatabaseConfig` 객체를 생성하여 반환합니다.
  
  **3순위: config.properties (클래스패스)**
  - `loadPropertiesFromClasspath("config.properties")`를 호출합니다.
  - 레거시 설정 파일 형식을 지원합니다.
  - `db.url`, `db.user`, `db.password` 키를 읽습니다.
  
  **4순위: config.properties (현재 디렉토리)**
  - `FileInputStream`을 사용하여 현재 작업 디렉토리의 `config.properties` 파일을 직접 읽습니다.
  - 파일이 없거나 읽기 실패 시 예외를 무시하고 다음 단계로 진행합니다.
  - `Logger.warn()`을 통해 경고 메시지를 기록합니다.
  
  **최종 단계:**
  - 모든 소스에서 설정을 찾지 못한 경우, 모든 필드가 `null`인 `DatabaseConfig` 객체를 반환합니다.
  - 이 경우 `DatabaseConnection` 생성자에서 `IllegalStateException`이 발생합니다.

**`private static Properties loadPropertiesFromClasspath(String filename)`**
- **접근 제어자:** `private static`
- **매개변수:** `String filename` - 클래스패스에서 찾을 파일명
- **반환값:** `Properties` 객체 (파일이 없거나 읽기 실패 시 `null`)
- **동작 방식:**
  1. `DatabaseConfig.class.getClassLoader().getResourceAsStream(filename)`을 사용하여 클래스패스에서 리소스를 스트림으로 읽습니다.
  2. `try-with-resources` 구문을 사용하여 자동으로 스트림을 닫습니다.
  3. `Properties.load(InputStream)`을 호출하여 키-값 쌍을 파싱합니다.
  4. 파일이 없거나 예외가 발생하면 `null`을 반환합니다 (정상적인 동작).
- **예외 처리:** 모든 예외를 내부에서 처리하고 `null`을 반환하여 상위 메서드가 다음 소스를 시도할 수 있도록 합니다.

**`private static boolean notBlank(String s)`**
- **접근 제어자:** `private static`
- **매개변수:** `String s` - 검증할 문자열
- **반환값:** `boolean` - 문자열이 null이 아니고 공백이 아닌 경우 `true`
- **동작 방식:**
  1. `s == null`이면 `false`를 반환합니다.
  2. `s.trim().isEmpty()`가 `true`이면 (공백만 있는 경우) `false`를 반환합니다.
  3. 그 외의 경우 `true`를 반환합니다.
- **용도:** 설정 값의 유효성을 검사하는 헬퍼 메서드입니다.

---

### 2.2.3 `hw10.db` 패키지 (DB 연결)

#### 1. `DatabaseConnection` 클래스

**파일:** `hw10/db/DatabaseConnection.java`  
**패키지:** `hw10.db`  
**접근 제어자:** `public final class`  
**구현 인터페이스:** `AutoCloseable`  
**역할:** JDBC 연결 객체(`java.sql.Connection`)를 생성하고 관리하는 래퍼 클래스입니다. `AutoCloseable` 인터페이스를 구현하여 `try-with-resources` 구문과 함께 사용할 수 있습니다.

##### 클래스 구조 및 필드

```java
private final DatabaseConfig config;  // 데이터베이스 설정 정보
```

`config` 필드는 `final`로 선언되어 생성 후 변경할 수 없습니다.

##### 생성자 상세 설명

**`public DatabaseConnection(DatabaseConfig config)`**
- **매개변수:** `DatabaseConfig config` - 데이터베이스 연결 설정 객체
- **동작 방식:**
  1. 전달받은 `config` 객체를 필드에 저장합니다.
  2. 필수 설정 값 검증을 수행합니다:
     - `config.dbUrl == null`이면 `IllegalStateException` 발생
     - `config.dbUser == null`이면 `IllegalStateException` 발생
     - `config.dbPassword == null`이면 `IllegalStateException` 발생
  3. 예외 메시지: `"DB 설정이 비어 있습니다. 환경변수 또는 config.properties를 설정하세요."`
- **예외:** `IllegalStateException` - 필수 설정이 누락된 경우

##### 메서드 상세 설명

**`public Connection openConnection() throws SQLException`**
- **매개변수:** 없음
- **반환값:** `java.sql.Connection` - 데이터베이스 연결 객체
- **동작 방식:**
  1. `DriverManager.getConnection()` 정적 메서드를 호출합니다.
  2. 매개변수로 `config.dbUrl`, `config.dbUser`, `config.dbPassword`를 전달합니다.
  3. PostgreSQL JDBC 드라이버가 자동으로 로드되어 물리적 데이터베이스 연결을 생성합니다.
  4. 연결 성공 시 `Connection` 객체를 반환합니다.
- **예외:** `SQLException` - 연결 실패 시 (예: DB 서버 다운, 잘못된 인증 정보, 네트워크 오류)
- **사용 예시:**
```java
try (DatabaseConnection dbConn = new DatabaseConnection(config);
     Connection conn = dbConn.openConnection()) {
    // DB 작업 수행
}
```

**`public void close()`**
- **매개변수:** 없음
- **반환값:** 없음 (void)
- **동작 방식:**
  - 현재는 빈 메서드로 구현되어 있습니다.
  - 실제 `Connection` 객체의 종료는 호출부에서 `Connection.close()`를 직접 호출하여 관리합니다.
  - `AutoCloseable` 인터페이스 구현 요구사항을 충족하기 위해 존재합니다.
- **참고:** 향후 연결 풀링(Connection Pooling) 기능을 추가할 경우 이 메서드에서 연결을 풀에 반환하는 로직을 구현할 수 있습니다.

---

### 2.2.4 `hw10.controller` 패키지 (API 컨트롤러)

REST API 요청을 받아 Service를 호출하고 응답을 반환합니다.

#### 1. `MainController` 클래스

**파일:** `hw10/controller/MainController.java`  
**패키지:** `hw10.controller`  
**어노테이션:** `@RestController`, `@RequestMapping("/api/main")`  
**역할:** 메인 대시보드 관련 REST API 요청을 처리하는 컨트롤러입니다. HTTP 요청을 받아 Service 계층을 호출하고 결과를 JSON으로 반환합니다.

##### 클래스 구조 및 의존성

```java
private final MainService mainService;  // 메인 서비스 의존성
```

Spring의 의존성 주입(Dependency Injection)을 통해 `MainService` 인스턴스를 주입받습니다.

##### 생성자 상세 설명

**`public MainController(MainService mainService)`**
- **매개변수:** `MainService mainService` - 메인 서비스 인스턴스
- **동작 방식:**
  - Spring Framework가 생성자 기반 의존성 주입을 수행합니다.
  - `mainService` 필드에 주입된 인스턴스를 저장합니다.
- **의존성 주입 방식:** 생성자 주입(Constructor Injection)을 사용하여 불변성을 보장하고 테스트 용이성을 높입니다.

##### 메서드 상세 설명

**`@GetMapping("/summary") public ResponseEntity<MainDto.MainSummary> getSummary() throws SQLException`**
- **HTTP 메서드:** `GET`
- **엔드포인트:** `/api/main/summary`
- **매개변수:** 없음
- **반환값:** `ResponseEntity<MainDto.MainSummary>` - HTTP 상태 코드와 함께 요약 데이터를 포함한 응답
- **동작 방식:**
  1. 클라이언트로부터 GET 요청을 받습니다.
  2. `mainService.getSummary()`를 호출하여 대시보드 요약 정보를 조회합니다.
  3. 조회된 `MainDto.MainSummary` 객체를 `ResponseEntity.ok()`로 래핑하여 반환합니다.
  4. HTTP 상태 코드 200 (OK)와 함께 JSON 형식으로 응답합니다.
- **응답 데이터 구조:**
  - `activeProjects`: 활성 프로젝트 수 (int)
  - `carbonReduction`: 탄소 감축량 (double, kg CO₂e)
  - `delayedDeliveries`: 지연 배송 건수 (int)
  - `avgEsgGrade`: 평균 ESG 등급 (String, "A", "B", "C", "D" 중 하나)
- **예외 처리:** `SQLException`이 발생하면 `GlobalExceptionHandler`가 처리하여 500 에러를 반환합니다.

#### 2. `ProjectController` 클래스

**파일:** `hw10/controller/ProjectController.java`  
**패키지:** `hw10.controller`  
**어노테이션:** `@RestController`, `@RequestMapping("/api/projects")`  
**역할:** 프로젝트 관련 REST API 요청을 처리하는 컨트롤러입니다. 프로젝트 조회, 검색, 통계 조회 기능을 제공합니다.

##### 클래스 구조 및 의존성

```java
private final ProjectService projectService;  // 프로젝트 서비스 의존성
```

##### 생성자 상세 설명

**`public ProjectController(ProjectService projectService)`**
- **매개변수:** `ProjectService projectService` - 프로젝트 서비스 인스턴스
- **동작 방식:** 생성자 기반 의존성 주입을 통해 `projectService` 필드를 초기화합니다.

##### 메서드 상세 설명

**`@GetMapping("/{id}") public ResponseEntity<ProjectDto.ProjectBasic> getProject(@PathVariable int id) throws SQLException`**
- **HTTP 메서드:** `GET`
- **엔드포인트:** `/api/projects/{id}` (예: `/api/projects/1`)
- **매개변수:**
  - `@PathVariable int id` - 프로젝트 ID (URL 경로에서 추출)
- **반환값:** `ResponseEntity<ProjectDto.ProjectBasic>` - 프로젝트 기본 정보 또는 404 응답
- **동작 방식:**
  1. URL 경로에서 프로젝트 ID를 추출합니다.
  2. `projectService.getProject(id)`를 호출하여 프로젝트 정보를 조회합니다.
  3. 조회 결과가 `null`이면 `ResponseEntity.notFound().build()`를 반환하여 HTTP 404 상태 코드를 전송합니다.
  4. 조회 결과가 있으면 `ResponseEntity.ok(project)`를 반환하여 HTTP 200과 함께 JSON 데이터를 전송합니다.
- **응답 데이터 구조 (ProjectDto.ProjectBasic):**
  - `projectId`: 프로젝트 ID (int)
  - `shipName`: 선박명 (String)
  - `shipType`: 선종 (String)
  - `contractDate`: 계약일 (java.sql.Date)
  - `deliveryDueDate`: 인도예정일 (java.sql.Date)
  - `status`: 프로젝트 상태 (String)

**`@GetMapping("/search") public ResponseEntity<List<ProjectDto.ProjectSearchItem>> searchProjects(@RequestParam String keyword, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int limit) throws SQLException`**
- **HTTP 메서드:** `GET`
- **엔드포인트:** `/api/projects/search?keyword=검색어&page=0&limit=10`
- **매개변수:**
  - `@RequestParam String keyword` - 선박명 검색어 (필수)
  - `@RequestParam(defaultValue = "0") int page` - 페이지 번호 (기본값: 0, 0부터 시작)
  - `@RequestParam(defaultValue = "10") int limit` - 페이지당 항목 수 (기본값: 10)
- **반환값:** `ResponseEntity<List<ProjectDto.ProjectSearchItem>>` - 검색 결과 리스트
- **동작 방식:**
  1. 쿼리 파라미터에서 검색어, 페이지 번호, 페이지 크기를 추출합니다.
  2. `projectService.searchProjects(keyword, page, limit)`를 호출합니다.
  3. 서비스 계층에서 페이징 처리를 수행합니다 (`offset = page * limit`).
  4. 검색 결과 리스트를 JSON 배열로 반환합니다.
- **페이징 처리:**
  - `offset = page * limit`로 계산됩니다.
  - 예: `page=0, limit=10` → `offset=0` (첫 10개)
  - 예: `page=1, limit=10` → `offset=10` (다음 10개)
- **응답 데이터 구조:** `ProjectDto.ProjectSearchItem` 리스트
  - 각 항목: `projectId`, `shipName`, `shipType`, `status`

**`@GetMapping("/{id}/stats") public ResponseEntity<ProjectDto.DashboardStats> getDashboardStats(@PathVariable int id) throws SQLException`**
- **HTTP 메서드:** `GET`
- **엔드포인트:** `/api/projects/{id}/stats` (예: `/api/projects/1/stats`)
- **매개변수:**
  - `@PathVariable int id` - 프로젝트 ID
- **반환값:** `ResponseEntity<ProjectDto.DashboardStats>` - 대시보드 통계 데이터
- **동작 방식:**
  1. [기능 1] 프로젝트 대시보드 통계 조회 기능을 구현합니다.
  2. `projectService.getDashboardStats(id)`를 호출합니다.
  3. 서비스 계층에서 다음 데이터를 집계합니다:
     - 총 발주 금액 (발주서 + 발주 항목 조인)
     - 운송 관련 탄소 배출 합계
     - 보관 관련 탄소 배출 합계
     - 가공/생산 관련 탄소 배출 합계
     - 프로젝트 전체 탄소 배출 합계
     - 공급업체별 발주 금액 상위 3개
     - 탄소 집약도 (kg CO₂e / 백만 원)
  4. 모든 통계 데이터를 포함한 `DashboardStats` 객체를 반환합니다.
- **응답 데이터 구조 (ProjectDto.DashboardStats):**
  - `project`: 프로젝트 기본 정보 (`ProjectBasic`)
  - `totalOrderAmount`: 총 발주 금액 (double)
  - `totalEmission`: 전체 탄소 배출량 (double, kg CO₂e)
  - `transportEmission`: 운송 탄소 배출량 (double)
  - `storageEmission`: 보관 탄소 배출량 (double)
  - `processingEmission`: 가공/생산 탄소 배출량 (double)
  - `topSuppliers`: 상위 3개 공급업체 리스트 (`List<SupplierAmount>`)
  - `carbonIntensity`: 탄소 집약도 (Double, null 가능)
  - `shipCII`: 선박 CII 지표 (Double, 현재 null)

#### 3. `OrderController` 클래스

**파일:** `hw10/controller/OrderController.java`  
**패키지:** `hw10.controller`  
**어노테이션:** `@RestController`, `@RequestMapping("/api/orders")`  
**역할:** 발주 관련 REST API 요청을 처리하는 컨트롤러입니다. 발주 생성, 프로젝트/공급사/부품/창고 목록 조회 기능을 제공합니다.

##### 클래스 구조 및 의존성

```java
private final OrderService orderService;  // 발주 서비스 의존성
```

##### 생성자 상세 설명

**`public OrderController(OrderService orderService)`**
- **매개변수:** `OrderService orderService` - 발주 서비스 인스턴스
- **동작 방식:** 생성자 기반 의존성 주입을 통해 `orderService` 필드를 초기화합니다.

##### 메서드 상세 설명

**`@PostMapping public ResponseEntity<OrderDto.OrderResponse> createOrder(@RequestBody OrderDto.OrderRequest request) throws SQLException`**
- **HTTP 메서드:** `POST`
- **엔드포인트:** `/api/orders`
- **매개변수:**
  - `@RequestBody OrderDto.OrderRequest request` - 발주 요청 데이터 (JSON 본문에서 역직렬화)
- **반환값:** `ResponseEntity<OrderDto.OrderResponse>` - 발주 생성 결과
- **동작 방식:**
  1. [기능 2] 발주 등록 트랜잭션 처리 기능을 구현합니다.
  2. 클라이언트로부터 JSON 형식의 발주 요청 데이터를 받습니다.
  3. `orderService.createOrder(request)`를 호출하여 트랜잭션을 실행합니다.
  4. 성공 시 생성된 `POID`와 `DeliveryID`를 포함한 응답을 반환합니다.
  5. 실패 시 `SQLException`이 발생하며, `GlobalExceptionHandler`가 500 에러를 반환합니다.
- **요청 데이터 구조 (OrderDto.OrderRequest):**
  - `projectId`: 프로젝트 ID (int)
  - `supplierId`: 공급업체 ID (int)
  - `engineerName`: 담당 엔지니어 이름 (String)
  - `status`: 발주 상태 (String, 기본값: "요청")
  - `lines`: 발주 품목 리스트 (`List<OrderLineInput>`)
    - 각 품목: `partId`, `quantity`, `unitPrice`
  - `warehouseId`: 입고 창고 ID (int)
  - `transportMode`: 운송 수단 (String)
  - `distanceKm`: 운송 거리 (Double, km)
- **응답 데이터 구조 (OrderDto.OrderResponse):**
  - `poid`: 생성된 발주서 ID (int)
  - `deliveryId`: 생성된 납품서 ID (int)
  - `message`: 결과 메시지 (String, "발주 등록 완료")
- **예외 처리:** `SQLException` 발생 시 "발주 등록 실패: [에러 메시지]" 형태로 래핑하여 상위로 전달합니다.

**`@GetMapping("/projects") public ResponseEntity<List<ProjectDto.ProjectSearchItem>> getProjectList(@RequestParam(defaultValue = "") String keyword) throws SQLException`**
- **HTTP 메서드:** `GET`
- **엔드포인트:** `/api/orders/projects?keyword=검색어`
- **매개변수:**
  - `@RequestParam(defaultValue = "") String keyword` - 검색어 (기본값: 빈 문자열)
- **반환값:** `ResponseEntity<List<ProjectDto.ProjectSearchItem>>` - 프로젝트 목록
- **동작 방식:**
  1. 드롭다운 메뉴 구성용 프로젝트 목록을 조회합니다.
  2. `orderService.getProjectOptions(keyword)`를 호출합니다.
  3. 키워드가 없으면 전체 조회 (최대 20개), 있으면 검색 결과를 반환합니다.
- **용도:** 발주 등록 화면에서 프로젝트 선택 드롭다운을 채우는 데 사용됩니다.

**`@GetMapping("/suppliers") public ResponseEntity<List<OrderDto.SupplierOption>> getSupplierList() throws SQLException`**
- **HTTP 메서드:** `GET`
- **엔드포인트:** `/api/orders/suppliers`
- **매개변수:** 없음
- **반환값:** `ResponseEntity<List<OrderDto.SupplierOption>>` - 공급업체 목록
- **동작 방식:**
  1. 모든 공급업체를 이름 순으로 정렬하여 조회합니다.
  2. `orderService.getSupplierOptions()`를 호출합니다.
  3. 각 공급업체의 ID, 이름, 국가 정보를 반환합니다.
- **응답 데이터 구조:** `OrderDto.SupplierOption` 리스트
  - 각 항목: `supplierId`, `name`, `country`

**`@GetMapping("/parts") public ResponseEntity<List<OrderDto.PartOption>> searchParts(@RequestParam(defaultValue = "") String keyword) throws SQLException`**
- **HTTP 메서드:** `GET`
- **엔드포인트:** `/api/orders/parts?keyword=검색어`
- **매개변수:**
  - `@RequestParam(defaultValue = "") String keyword` - 부품명 검색어
- **반환값:** `ResponseEntity<List<OrderDto.PartOption>>` - 부품 목록
- **동작 방식:**
  1. 부품 검색 기능을 제공합니다.
  2. `orderService.searchParts(keyword)`를 호출합니다.
  3. 키워드가 없으면 상위 50개 부품을 반환합니다.
  4. 키워드가 있으면 `ILIKE` 연산자를 사용하여 부분 일치 검색을 수행하고 상위 20개를 반환합니다.
  5. 각 부품의 평균 단가(`AVG(SupplierPart.UnitPrice)`)를 함께 조회합니다.
- **응답 데이터 구조:** `OrderDto.PartOption` 리스트
  - 각 항목: `partId`, `name`, `unit`, `unitPrice`

**`@GetMapping("/warehouses") public ResponseEntity<List<OrderDto.WarehouseOption>> getWarehouseList() throws SQLException`**
- **HTTP 메서드:** `GET`
- **엔드포인트:** `/api/orders/warehouses`
- **매개변수:** 없음
- **반환값:** `ResponseEntity<List<OrderDto.WarehouseOption>>` - 창고 목록
- **동작 방식:**
  1. 모든 창고를 이름 순으로 정렬하여 조회합니다.
  2. `orderService.getWarehouseOptions()`를 호출합니다.
  3. 각 창고의 ID, 이름, 위치 정보를 반환합니다.
- **응답 데이터 구조:** `OrderDto.WarehouseOption` 리스트
  - 각 항목: `warehouseId`, `name`, `location`

**`@GetMapping("/warehouses/{warehouseId}/inventory") public ResponseEntity<List<OrderDto.InventoryItem>> getWarehouseInventory(@PathVariable int warehouseId) throws SQLException`**
- **HTTP 메서드:** `GET`
- **엔드포인트:** `/api/orders/warehouses/{warehouseId}/inventory`
- **매개변수:**
  - `@PathVariable int warehouseId` - 창고 ID
- **반환값:** `ResponseEntity<List<OrderDto.InventoryItem>>` - 재고 목록
- **동작 방식:**
  1. 특정 창고의 현재 재고 현황을 조회합니다.
  2. `orderService.getWarehouseInventory(warehouseId)`를 호출합니다.
  3. `Inventory` 테이블과 `Part` 테이블을 조인하여 부품명과 단위 정보를 함께 조회합니다.
  4. 부품명 순으로 정렬하여 반환합니다.
- **응답 데이터 구조:** `OrderDto.InventoryItem` 리스트
  - 각 항목: `partId`, `partName`, `unit`, `quantity`
- **용도:** 발주 등록 시 선택한 창고의 현재 재고를 확인하는 데 사용됩니다.

#### 4. `SupplierController` 클래스

**파일:** `hw10/controller/SupplierController.java`

- **`listSuppliers(esgGrades, minRatio, maxRatio)`** (`GET /api/suppliers`)
  - [기능 3] ESG 등급 다중 선택 및 지연율 필터를 적용하여 공급업체 리포트 목록을 조회합니다.
- **`getSupplier(id)`**
  - 특정 공급업체의 상세 정보를 조회합니다.
- **`getSupplierOrders(id, page, size)`**
  - 특정 업체의 발주 이력을 페이징하여 조회합니다.

#### 5. `SettingController` 클래스

**파일:** `hw10/controller/SettingController.java`

- **`getLogs(level, limit, search)`** (`GET /api/settings/logs`)
  - 서버 로그 파일을 읽어 조건에 맞게 필터링하여 반환합니다.
- **`getSystemStatus()`** (`GET /api/settings/status`)
  - DB 연결 상태, 응답 속도 등 시스템 상태를 점검합니다.

---

### 2.2.5 `hw10.service` 패키지 (비즈니스 로직)

트랜잭션 관리와 비즈니스 규칙을 수행합니다.

#### 1. `MainService` 클래스

**파일:** `hw10/service/MainService.java`  
**패키지:** `hw10.service`  
**어노테이션:** `@Service`  
**역할:** 메인 대시보드의 비즈니스 로직을 처리하는 서비스 클래스입니다. 데이터베이스에서 통계 데이터를 조회하고 계산합니다.

##### 클래스 구조 및 의존성

```java
private final DataSource dataSource;  // 데이터베이스 연결 소스
```

Spring의 `DataSource`를 주입받아 데이터베이스 연결을 관리합니다.

##### 생성자 상세 설명

**`public MainService(DataSource dataSource)`**
- **매개변수:** `DataSource dataSource` - 데이터베이스 연결 소스
- **동작 방식:** Spring이 `DataSource` 빈을 주입하여 `dataSource` 필드를 초기화합니다.

##### 메서드 상세 설명

**`public MainDto.MainSummary getSummary() throws SQLException`**
- **매개변수:** 없음
- **반환값:** `MainDto.MainSummary` - 대시보드 요약 정보
- **동작 방식:**
  1. `dataSource.getConnection()`을 호출하여 데이터베이스 연결을 획득합니다.
  2. `try-with-resources` 구문을 사용하여 연결을 자동으로 닫습니다.
  3. 다음 4개의 통계를 순차적으로 조회합니다:
     - `countActiveProjects(conn)`: 활성 프로젝트 수
     - `getTotalCarbonReduction(conn)`: 탄소 감축량
     - `countDelayedDeliveries(conn)`: 지연 배송 건수
     - `getAverageEsgGrade(conn)`: 평균 ESG 등급
  4. 조회된 값들을 `MainDto.MainSummary` 객체로 포장하여 반환합니다.
- **예외 처리:** `SQLException`이 발생하면 상위로 전파됩니다.

**`private int countActiveProjects(Connection conn) throws SQLException`**
- **접근 제어자:** `private`
- **매개변수:** `Connection conn` - 데이터베이스 연결 객체
- **반환값:** `int` - 활성 프로젝트 수
- **동작 방식:**
  1. SQL 쿼리: `SELECT COUNT(*) FROM ShipProject WHERE Status != '인도완료'`
  2. `PreparedStatement`를 사용하여 쿼리를 실행합니다.
  3. `ResultSet.next()`를 호출하여 첫 번째 행으로 이동합니다.
  4. `rs.getInt(1)`로 카운트 값을 추출합니다.
  5. `try-with-resources`로 자동 리소스 해제를 보장합니다.
- **비즈니스 로직:** '인도완료' 상태가 아닌 모든 프로젝트를 활성 프로젝트로 간주합니다.

**`private double getTotalCarbonReduction(Connection conn) throws SQLException`**
- **접근 제어자:** `private`
- **매개변수:** `Connection conn` - 데이터베이스 연결 객체
- **반환값:** `double` - 탄소 감축량 (kg CO₂e)
- **동작 방식:**
  1. SQL 쿼리: `SELECT COALESCE(SUM(CO2eAmount), 0) FROM CarbonEmissionRecord`
  2. `COALESCE` 함수를 사용하여 NULL 값을 0으로 처리합니다.
  3. 모든 탄소 배출 기록의 합계를 계산합니다.
  4. 기준값 1000에서 총 배출량을 차감하여 감축량을 계산합니다.
  5. `Math.max(0, 1000 - totalEmission)`을 사용하여 음수 값이 나오지 않도록 보장합니다.
- **비즈니스 로직:** 기준값(1000 kg CO₂e) 대비 실제 배출량을 비교하여 감축량을 산출합니다.

**`private int countDelayedDeliveries(Connection conn) throws SQLException`**
- **접근 제어자:** `private`
- **매개변수:** `Connection conn` - 데이터베이스 연결 객체
- **반환값:** `int` - 지연 배송 건수
- **동작 방식:**
  1. SQL 쿼리: `SELECT COUNT(*) FROM Delivery WHERE Status = '지연'`
  2. 상태가 '지연'인 배송 레코드의 개수를 카운트합니다.
  3. 결과를 정수로 반환합니다.
- **비즈니스 로직:** 배송 상태가 '지연'인 경우를 지연 배송으로 간주합니다.

**`private String getAverageEsgGrade(Connection conn) throws SQLException`**
- **접근 제어자:** `private`
- **매개변수:** `Connection conn` - 데이터베이스 연결 객체
- **반환값:** `String` - 평균 ESG 등급 ("A", "B", "C", "D" 중 하나)
- **동작 방식:**
  1. SQL 쿼리에서 `CASE` 문을 사용하여 ESG 등급을 점수로 환산합니다:
     - 'A' → 4점
     - 'B' → 3점
     - 'C' → 2점
     - 'D' → 1점
     - 기타 → 0점
  2. `AVG()` 집계 함수로 평균 점수를 계산합니다.
  3. `WHERE ESGGrade IS NOT NULL` 조건으로 NULL 값을 제외합니다.
  4. 평균 점수를 다시 등급 문자로 변환합니다:
     - `avg >= 3.5` → "A"
     - `avg >= 2.5` → "B"
     - `avg >= 1.5` → "C"
     - 그 외 → "D"
- **비즈니스 로직:** 모든 공급업체의 ESG 등급을 평균내어 전체 평균 등급을 산출합니다.

#### 2. `ProjectService` 클래스

**파일:** `hw10/service/ProjectService.java`

- **`getDashboardStats(projectId)`**
  - `ProjectRepository`를 통해 다음 데이터를 조회 및 계산합니다:
    1. 총 발주 금액 (발주서+세부항목 조인)
    2. 운송/보관/가공 단계별 탄소 배출량 합계
    3. 발주 금액 기준 상위 3개 공급업체
    4. 탄소 집약도 (배출량 / 비용 백만원) 지표 계산

#### 3. `OrderService` 클래스

**파일:** `hw10/service/OrderService.java`

- **`createOrder(request)`**
  - `OrderTransactionService`를 인스턴스화하고 DTO 데이터를 변환하여 트랜잭션 메서드를 호출합니다. 발생한 예외를 `SQLException`으로 래핑하여 상위로 던집니다.
- **`searchParts`**, **`getOptions`** 관련 메서드들
  - 단순 조회 쿼리들을 실행하고 DTO로 매핑합니다.

#### 4. `OrderTransactionService` 클래스 (핵심)

**파일:** `hw10/service/OrderTransactionService.java`  
**패키지:** `hw10.service`  
**어노테이션:** `@Service`  
**역할:** 발주 등록 트랜잭션을 처리하는 핵심 서비스 클래스입니다. [기능 2] 요구사항에 따라 발주서 생성, 납품 기록 생성, 재고 반영을 하나의 트랜잭션으로 묶어 처리합니다.

##### 클래스 구조 및 의존성

```java
private final DataSource dataSource;  // 데이터베이스 연결 소스
private final SequenceGenerator sequenceGenerator = new SequenceGenerator();  // ID 생성기
private final OrderRepository orderRepository = new OrderRepository();  // 발주 리포지토리
private final DeliveryRepository deliveryRepository = new DeliveryRepository();  // 납품 리포지토리
private final InventoryRepository inventoryRepository = new InventoryRepository();  // 재고 리포지토리
```

##### 내부 레코드 클래스

**`public record OrderLineInput(int partId, int quantity, double unitPrice)`**
- 발주 품목 입력 데이터를 담는 불변 레코드입니다.
- `partId`: 부품 ID
- `quantity`: 수량
- `unitPrice`: 단가

**`public record TransactionResult(int poid, int deliveryId)`**
- 트랜잭션 실행 결과를 담는 레코드입니다.
- `poid`: 생성된 발주서 ID
- `deliveryId`: 생성된 납품서 ID

##### 생성자 상세 설명

**`public OrderTransactionService(DataSource dataSource)`**
- **매개변수:** `DataSource dataSource` - 데이터베이스 연결 소스
- **동작 방식:** `dataSource` 필드를 초기화하고, 리포지토리 인스턴스들을 생성합니다.

##### 메서드 상세 설명

**`public TransactionResult createOrderWithInitialDelivery(int projectId, int supplierId, String engineerName, String status, List<OrderLineInput> lines, int warehouseId, String transportMode, Double distanceKm) throws Exception`**
- **매개변수:**
  - `int projectId`: 프로젝트 ID
  - `int supplierId`: 공급업체 ID
  - `String engineerName`: 담당 엔지니어 이름
  - `String status`: 발주 상태 (null이면 "요청"으로 기본값 설정)
  - `List<OrderLineInput> lines`: 발주 품목 리스트
  - `int warehouseId`: 입고 창고 ID
  - `String transportMode`: 운송 수단
  - `Double distanceKm`: 운송 거리 (km)
- **반환값:** `TransactionResult` - 생성된 발주서 ID와 납품서 ID
- **동작 방식 (전체 흐름):**

  **1단계: 재시도 루프 설정**
  - 최대 3회 재시도 로직을 구현합니다.
  - `for (int attempt = 1; attempt <= maxRetries; attempt++)` 루프를 사용합니다.

  **2단계: 데이터베이스 연결 획득**
  - `dataSource.getConnection()`을 호출하여 연결을 획득합니다.
  - `try-with-resources`로 자동 연결 해제를 보장합니다.

  **3단계: 트랜잭션 시작**
  - `conn.setAutoCommit(false)`: 자동 커밋 모드를 비활성화하여 수동 트랜잭션 제어를 시작합니다.
  - `conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED)`: 격리 수준을 READ_COMMITTED로 설정합니다.
    - READ_COMMITTED: 커밋된 데이터만 읽을 수 있어 Dirty Read를 방지합니다.
    - 동시성과 일관성의 균형을 맞춘 수준입니다.
  - `Logger.info("트랜잭션 시작(attempt=" + attempt + ")")`로 로그를 기록합니다.

  **4단계: 발주서 ID 생성 및 발주서 삽입**
  - `sequenceGenerator.nextId(conn, "PurchaseOrder.POID")`를 호출하여 다음 발주서 ID를 생성합니다.
  - `new Date(System.currentTimeMillis())`로 오늘 날짜를 생성합니다.
  - 상태 값이 없으면 기본값 "요청"을 사용합니다.
  - `orderRepository.insertPurchaseOrder(conn, poid, today, orderStatus, engineerName, projectId, supplierId)`를 호출하여 `PurchaseOrder` 테이블에 레코드를 삽입합니다.

  **5단계: 발주 품목 삽입 (반복)**
  - `lines` 리스트를 순회하며 각 품목을 처리합니다.
  - `lineNo`를 1부터 시작하여 순차적으로 증가시킵니다.
  - `orderRepository.insertPurchaseOrderLine(conn, poid, lineNo, li.partId(), li.quantity(), li.unitPrice(), null)`를 호출하여 `PurchaseOrderLine` 테이블에 각 품목을 삽입합니다.
  - `RequestedDueDate`는 현재 `null`로 설정됩니다.

  **6단계: 납품서 ID 생성 및 초기 납품 삽입**
  - `sequenceGenerator.nextId(conn, "Delivery.DeliveryID")`를 호출하여 다음 납품서 ID를 생성합니다.
  - `deliveryRepository.insertDelivery(conn, deliveryId, poid, today, transportMode, distanceKm, "정상입고")`를 호출하여 `Delivery` 테이블에 초기 납품 레코드를 삽입합니다.
  - 상태는 "정상입고"로 설정하고, 날짜는 오늘 날짜를 사용합니다.

  **7단계: 납품 품목 삽입 및 재고 반영 (반복)**
  - `lines` 리스트를 다시 순회합니다.
  - 각 품목에 대해:
    1. 초기 수량 계산: `received = (int) Math.round(li.quantity() * 0.5)` (발주 수량의 50%)
    2. 0개일 경우 1개로 보정: `if (received == 0 && li.quantity() > 0) received = 1;`
    3. `deliveryRepository.insertDeliveryLine(conn, deliveryId, poid, lineNo, received, "초기납품(자동)")`를 호출하여 `DeliveryLine` 테이블에 납품 품목을 삽입합니다.
    4. `inventoryRepository.addInventory(conn, warehouseId, li.partId(), received)`를 호출하여 창고 재고를 증가시킵니다.
      - 이 메서드는 `ON CONFLICT DO UPDATE` 구문을 사용하여 UPSERT를 수행합니다.
      - 기존 재고가 있으면 수량을 더하고, 없으면 새로 삽입합니다.

  **8단계: 트랜잭션 커밋**
  - 모든 작업이 성공적으로 완료되면 `conn.commit()`을 호출하여 변경사항을 데이터베이스에 영구 반영합니다.
  - `Logger.info("트랜잭션 커밋 완료 - 발주서 ID: " + poid + ", 납품서 ID: " + deliveryId)`로 성공 로그를 기록합니다.
  - `TransactionResult` 객체를 생성하여 반환합니다.

  **9단계: 예외 처리 및 롤백**
  - `SQLException`이 발생하면:
    1. `conn.rollback()`을 호출하여 모든 변경사항을 취소합니다.
    2. `Logger.warn("트랜잭션 롤백 - 오류 발생: " + e.getMessage())`로 롤백 로그를 기록합니다.
    3. 롤백 자체가 실패하면 `Logger.error("롤백 실패", re)`로 에러 로그를 기록하고 예외를 다시 던집니다.
    4. **교착상태(Deadlock) 처리:**
       - `e.getSQLState()`가 "40P01"인 경우 (PostgreSQL 교착상태 에러 코드)
       - `attempt < maxRetries`인 경우 재시도를 수행합니다.
       - `Thread.sleep(200L * attempt)`로 지수 백오프(Exponential Backoff)를 적용합니다.
         - 1회차: 200ms 대기
         - 2회차: 400ms 대기
         - 3회차: 600ms 대기
       - `continue`로 루프의 다음 반복으로 진행합니다.
    5. 교착상태가 아니거나 재시도 횟수를 초과한 경우 예외를 다시 던집니다.

  **10단계: 재시도 초과 처리**
  - 모든 재시도가 실패한 경우 `IllegalStateException("트랜잭션 재시도 초과")`를 발생시킵니다.

- **트랜잭션 원자성 보장:**
  - 모든 작업이 하나의 트랜잭션으로 묶여 있어, 어느 단계에서든 오류가 발생하면 전체 작업이 롤백됩니다.
  - 발주서만 생성되고 납품서 생성이 실패하는 경우, 발주서도 함께 롤백됩니다.
  - 재고 반영이 실패하면 발주서와 납품서 모두 롤백됩니다.

- **동시성 제어:**
  - READ_COMMITTED 격리 수준으로 Dirty Read를 방지합니다.
  - 교착상태 발생 시 자동 재시도로 일시적인 동시성 문제를 해결합니다.

- **예외 처리:**
  - `SQLException`: 데이터베이스 관련 오류
  - `IllegalStateException`: 재시도 횟수 초과
  - 모든 예외는 상위로 전파되어 `GlobalExceptionHandler`가 처리합니다.

#### 5. `SupplierService` 클래스

**파일:** `hw10/service/SupplierService.java`  
**패키지:** `hw10.service`  
**어노테이션:** `@Service`  
**역할:** 공급업체 관련 비즈니스 로직을 처리하는 서비스 클래스입니다. [기능 3] 요구사항에 따라 ESG 및 지연율 필터링 기능을 제공합니다.

##### 클래스 구조 및 의존성

```java
private final DataSource dataSource;
private final SupplierRepository supplierRepository;
```

##### 생성자 상세 설명

**`public SupplierService(DataSource dataSource)`**
- **매개변수:** `DataSource dataSource` - 데이터베이스 연결 소스
- **동작 방식:** `dataSource`를 초기화하고 `SupplierRepository` 인스턴스를 생성합니다.

##### 메서드 상세 설명

**`public List<SupplierDto.SupplierRow> listSuppliers(List<String> esgGrades, Double minRatio, Double maxRatio) throws SQLException`**
- **매개변수:**
  - `List<String> esgGrades` - ESG 등급 필터 (null 가능)
  - `Double minRatio` - 최소 지연율 (null 가능)
  - `Double maxRatio` - 최대 지연율 (null 가능)
- **반환값:** `List<SupplierDto.SupplierRow>` - 공급업체 목록
- **동작 방식:**
  1. `supplierRepository.listSuppliers(conn, esgGrades, minRatio, maxRatio)`를 호출합니다.
  2. 결과를 `SupplierDto.SupplierRow` 리스트로 변환합니다.
  3. Java Stream API를 사용하여 매핑합니다.
- **비즈니스 로직:** [기능 3] 요구사항에 따라 ESG 등급 다중 선택 및 지연율 필터를 지원합니다.

**`public SupplierDto.SupplierDetail getSupplierDetail(int supplierId) throws SQLException`**
- **매개변수:** `int supplierId` - 공급업체 ID
- **반환값:** `SupplierDto.SupplierDetail` - 공급업체 상세 정보
- **동작 방식:**
  1. 모든 공급업체 목록을 조회합니다.
  2. `supplierId`로 필터링하여 해당 업체를 찾습니다.
  3. 없으면 `IllegalArgumentException`을 발생시킵니다.
  4. `supplierRepository.recentPurchaseOrders(conn, supplierId, 5, 0)`를 호출하여 최근 5개 발주 내역을 조회합니다.
  5. `SupplierDetail` 객체를 생성하여 반환합니다.
- **비즈니스 로직:** [기능 3] 요구사항에 따라 공급업체 상세 화면에서 최근 N개 발주서 목록을 제공합니다.

**`public List<SupplierDto.SupplierPoRow> getSupplierOrders(int supplierId, int page, int size) throws SQLException`**
- **매개변수:**
  - `int supplierId` - 공급업체 ID
  - `int page` - 페이지 번호
  - `int size` - 페이지당 항목 수
- **반환값:** `List<SupplierDto.SupplierPoRow>` - 발주 내역 리스트
- **동작 방식:**
  1. `offset = page * size`로 오프셋을 계산합니다.
  2. `supplierRepository.recentPurchaseOrders(conn, supplierId, size, offset)`를 호출합니다.
  3. 결과를 `SupplierPoRow` 리스트로 변환합니다.
- **비즈니스 로직:** 페이징 기능을 제공하여 대량의 발주 내역을 효율적으로 조회합니다.

#### 6. `SettingService` 클래스

**파일:** `hw10/service/SettingService.java`  
**패키지:** `hw10.service`  
**어노테이션:** `@Service`  
**역할:** 시스템 설정 및 로그 관련 비즈니스 로직을 처리하는 서비스 클래스입니다. [기능 4] 요구사항에 따라 로그 조회 및 시스템 상태 확인 기능을 제공합니다.

##### 클래스 구조 및 상수

```java
private static final String LOG_FILE_PATH = "logs/app.log";
private static final DateTimeFormatter LOG_DATE_FORMAT = 
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
```

##### 메서드 상세 설명

**`public Map<String, Object> getLogs(String level, int limit, String search)`**
- **매개변수:**
  - `String level` - 로그 레벨 필터 ("INFO", "WARNING", "SEVERE", null 가능)
  - `int limit` - 조회할 최대 개수
  - `String search` - 검색어 (null 가능)
- **반환값:** `Map<String, Object>` - 로그 데이터 및 메타데이터
- **동작 방식:**
  1. `logs/app.log` 파일이 없으면 빈 결과를 반환합니다.
  2. `Files.readAllLines(logPath)`로 모든 라인을 읽습니다.
  3. 정규식 패턴으로 로그를 파싱합니다:
     - 패턴: `\[(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})\]\s+\[(INFO|WARNING|SEVERE)\]\s+(.+)`
     - 그룹 1: 타임스탬프
     - 그룹 2: 로그 레벨
     - 그룹 3: 메시지 시작 부분
  4. 멀티라인 로그(스택 트레이스 등)를 처리합니다.
  5. 레벨 필터와 검색어 필터를 적용합니다.
  6. 최신순으로 정렬하고 개수 제한을 적용합니다.
  7. 결과를 `Map`으로 반환합니다:
     - `logs`: 파싱된 로그 리스트
     - `total`: 전체 라인 수
     - `filtered`: 필터링된 로그 수
- **비즈니스 로직:** [기능 4] 요구사항에 따라 로그 파일을 읽어 조건에 맞게 필터링하여 반환합니다.

**`private void finalizeLogEntry(...)`**
- 로그 항목을 최종화하고 필터를 적용하는 헬퍼 메서드입니다.
- 메시지가 200자 이상이면 잘라서 표시합니다.

**`private String extractSource(String message)`**
- 로그 메시지에서 발생 위치를 추론하는 헬퍼 메서드입니다.
- 클래스명 패턴을 찾거나 키워드로 추론합니다.

**`public Map<String, Object> getSystemStatus()`**
- **매개변수:** 없음
- **반환값:** `Map<String, Object>` - 시스템 상태 정보
- **동작 방식:**
  1. `DatabaseConfig.load()`로 설정을 로드합니다.
  2. `DatabaseConnection`을 생성하여 연결을 시도합니다.
  3. `SELECT 1` 쿼리를 실행하여 응답 시간을 측정합니다.
  4. 결과를 `Map`으로 반환합니다:
     - `status`: "정상" 또는 "오류"
     - `dbConnected`: DB 연결 여부 (boolean)
     - `activeConnections`: 활성 연결 수
     - `queryLatency`: 쿼리 응답 시간 (ms)
     - `lastSync`: 마지막 동기화 시간
- **비즈니스 로직:** [기능 4] 요구사항에 따라 시스템 상태를 점검합니다.

---

### 2.2.6 `hw10.repository` 패키지 (DAO)

#### 1. `ProjectRepository` 클래스

**파일:** `hw10/repository/ProjectRepository.java`  
**패키지:** `hw10.repository`  
**접근 제어자:** `public final class`  
**역할:** 프로젝트 관련 데이터베이스 조회 작업을 수행하는 리포지토리 클래스입니다. 프로젝트 정보 조회, 통계 계산, 탄소 배출량 집계 등의 기능을 제공합니다.

##### 내부 레코드 클래스

**`public record ProjectBasic(int projectId, String shipName, String shipType, java.sql.Date contractDate, java.sql.Date deliveryDueDate, String status)`**
- 프로젝트 기본 정보를 담는 불변 레코드입니다.

**`public record SupplierAmount(int supplierId, String name, double amount)`**
- 공급업체별 발주 금액 통계를 담는 레코드입니다.

**`public record ProjectSearchItem(int projectId, String shipName, String shipType, String status)`**
- 프로젝트 검색 결과 항목을 담는 레코드입니다.

##### 메서드 상세 설명

**`public ProjectBasic findProjectById(Connection conn, int projectId) throws SQLException`**
- **매개변수:**
  - `Connection conn`: 데이터베이스 연결 객체
  - `int projectId`: 프로젝트 ID
- **반환값:** `ProjectBasic` 객체 또는 `null` (프로젝트가 없는 경우)
- **SQL 쿼리:**
```sql
SELECT ProjectID, ShipName, ShipType, ContractDate, DeliveryDueDate, Status
FROM ShipProject
WHERE ProjectID = ?
```
- **동작 방식:**
  1. `PreparedStatement`를 사용하여 파라미터화된 쿼리를 실행합니다.
  2. `ps.setInt(1, projectId)`로 프로젝트 ID를 바인딩합니다.
  3. `ResultSet.next()`를 호출하여 결과를 확인합니다.
  4. 결과가 없으면 `null`을 반환합니다.
  5. 결과가 있으면 `ProjectBasic` 레코드를 생성하여 반환합니다.
- **예외 처리:** `SQLException`이 발생하면 상위로 전파됩니다.

**`public List<ProjectSearchItem> searchProjectsByShipName(Connection conn, String keyword, int limit, int offset) throws SQLException`**
- **매개변수:**
  - `Connection conn`: 데이터베이스 연결 객체
  - `String keyword`: 선박명 검색어 (부분 일치)
  - `int limit`: 조회할 최대 개수
  - `int offset`: 건너뛸 레코드 수 (페이징용)
- **반환값:** `List<ProjectSearchItem>` - 검색 결과 리스트
- **SQL 쿼리:**
```sql
SELECT ProjectID, ShipName, ShipType, Status
FROM ShipProject
WHERE ShipName ILIKE ?
ORDER BY ProjectID
LIMIT ? OFFSET ?
```
- **동작 방식:**
  1. `ILIKE` 연산자를 사용하여 대소문자 구분 없이 부분 일치 검색을 수행합니다.
  2. `ps.setString(1, "%" + keyword + "%")`로 와일드카드를 포함한 검색어를 바인딩합니다.
  3. `ps.setInt(2, limit)`, `ps.setInt(3, offset)`로 페이징 파라미터를 바인딩합니다.
  4. `ProjectID` 순으로 정렬하여 일관된 결과를 보장합니다.
  5. `ResultSet`을 순회하며 `ProjectSearchItem` 리스트를 생성합니다.
- **용도:** 프로젝트 검색 및 페이징 처리에 사용됩니다.

**`public double totalOrderAmount(Connection conn, int projectId) throws SQLException`**
- **매개변수:**
  - `Connection conn`: 데이터베이스 연결 객체
  - `int projectId`: 프로젝트 ID
- **반환값:** `double` - 총 발주 금액
- **SQL 쿼리:**
```sql
SELECT COALESCE(SUM(pol.Quantity * pol.UnitPriceAtOrder), 0) AS total_amount
FROM PurchaseOrder po
JOIN PurchaseOrderLine pol ON pol.POID = po.POID
WHERE po.ProjectID = ?
```
- **동작 방식:**
  1. `PurchaseOrder`와 `PurchaseOrderLine` 테이블을 `POID`로 조인합니다.
  2. `WHERE po.ProjectID = ?` 조건으로 특정 프로젝트의 발주만 필터링합니다.
  3. `SUM(pol.Quantity * pol.UnitPriceAtOrder)`로 각 품목의 금액(수량 × 단가)을 합산합니다.
  4. `COALESCE(..., 0)`로 NULL 값을 0으로 처리합니다 (발주가 없는 경우).
  5. 결과를 `double`로 반환합니다.
- **비즈니스 로직:** [기능 1] 요구사항에 따라 프로젝트의 총 발주 금액을 계산합니다.

**`public List<SupplierAmount> topSuppliersByAmount(Connection conn, int projectId, int topN) throws SQLException`**
- **매개변수:**
  - `Connection conn`: 데이터베이스 연결 객체
  - `int projectId`: 프로젝트 ID
  - `int topN`: 상위 N개 (예: 3)
- **반환값:** `List<SupplierAmount>` - 상위 공급업체 리스트
- **SQL 쿼리:**
```sql
SELECT s.SupplierID, s.Name, SUM(pol.Quantity * pol.UnitPriceAtOrder) AS amount
FROM PurchaseOrder po
JOIN PurchaseOrderLine pol ON pol.POID = po.POID
JOIN Supplier s ON s.SupplierID = po.SupplierID
WHERE po.ProjectID = ?
GROUP BY s.SupplierID, s.Name
ORDER BY amount DESC
LIMIT ?
```
- **동작 방식:**
  1. `PurchaseOrder`, `PurchaseOrderLine`, `Supplier` 테이블을 조인합니다.
  2. `WHERE po.ProjectID = ?` 조건으로 특정 프로젝트의 발주만 필터링합니다.
  3. `GROUP BY s.SupplierID, s.Name`으로 공급업체별로 그룹화합니다.
  4. `SUM(pol.Quantity * pol.UnitPriceAtOrder)`로 각 공급업체의 총 발주 금액을 계산합니다.
  5. `ORDER BY amount DESC`로 금액 내림차순 정렬합니다.
  6. `LIMIT ?`로 상위 N개만 조회합니다.
  7. 결과를 `SupplierAmount` 리스트로 변환하여 반환합니다.
- **비즈니스 로직:** [기능 1] 요구사항에 따라 공급업체별 발주 금액 상위 3개를 조회합니다.

**`public double emissionSumByType(Connection conn, int projectId, String emissionType) throws SQLException`**
- **매개변수:**
  - `Connection conn`: 데이터베이스 연결 객체
  - `int projectId`: 프로젝트 ID
  - `String emissionType`: 배출 유형 ("운송", "보관", "가공", "생산" 등)
- **반환값:** `double` - 해당 유형의 탄소 배출량 합계 (kg CO₂e)
- **SQL 쿼리:**
```sql
SELECT COALESCE(SUM(c.CO2eAmount), 0) AS s
FROM CarbonEmissionRecord c
WHERE c.EmissionType = ?
  AND (
        c.ProjectID = ?
        OR c.DeliveryID IN (
            SELECT d.DeliveryID
            FROM Delivery d
            JOIN PurchaseOrder po ON po.POID = d.POID
            WHERE po.ProjectID = ?
        )
      )
```
- **동작 방식:**
  1. `CarbonEmissionRecord` 테이블에서 배출 기록을 조회합니다.
  2. `WHERE c.EmissionType = ?` 조건으로 특정 유형의 배출만 필터링합니다.
  3. 배출 기록이 프로젝트에 직접 연결된 경우(`c.ProjectID = ?`) 또는 배송을 통해 간접 연결된 경우(`c.DeliveryID IN (서브쿼리)`)를 모두 포함합니다.
  4. 서브쿼리에서 `Delivery`와 `PurchaseOrder`를 조인하여 해당 프로젝트의 배송을 찾습니다.
  5. `SUM(c.CO2eAmount)`로 배출량을 합산합니다.
  6. `COALESCE(..., 0)`로 NULL 값을 0으로 처리합니다.
- **비즈니스 로직:** [기능 1] 요구사항에 따라 운송/보관 관련 탄소 배출 합계를 계산합니다.

**`public double emissionSumTotal(Connection conn, int projectId) throws SQLException`**
- **매개변수:**
  - `Connection conn`: 데이터베이스 연결 객체
  - `int projectId`: 프로젝트 ID
- **반환값:** `double` - 프로젝트 전체 탄소 배출량 합계 (kg CO₂e)
- **SQL 쿼리:**
```sql
SELECT COALESCE(SUM(c.CO2eAmount), 0) AS s
FROM CarbonEmissionRecord c
WHERE (
        c.ProjectID = ?
        OR c.DeliveryID IN (
            SELECT d.DeliveryID
            FROM Delivery d
            JOIN PurchaseOrder po ON po.POID = d.POID
            WHERE po.ProjectID = ?
        )
      )
```
- **동작 방식:**
  1. `emissionSumByType`과 유사하지만 `EmissionType` 조건 없이 모든 유형의 배출을 합산합니다.
  2. 프로젝트에 직접 연결된 배출과 배송을 통해 간접 연결된 배출을 모두 포함합니다.
- **비즈니스 로직:** [기능 1] 요구사항에 따라 프로젝트 전체 탄소 배출 합계를 계산합니다.

**`public Double calculateCarbonIntensity(Connection conn, int projectId) throws SQLException`**
- **매개변수:**
  - `Connection conn`: 데이터베이스 연결 객체
  - `int projectId`: 프로젝트 ID
- **반환값:** `Double` - 탄소 집약도 (kg CO₂e / 백만 원) 또는 `null` (발주 금액이 0인 경우)
- **동작 방식:**
  1. `emissionSumTotal(conn, projectId)`를 호출하여 총 배출량을 조회합니다.
  2. `totalOrderAmount(conn, projectId)`를 호출하여 총 발주 금액을 조회합니다.
  3. 발주 금액이 0이면 계산 불가이므로 `null`을 반환합니다.
  4. 탄소 집약도 계산: `(총 배출량 / 총 발주금액) * 1,000,000`
     - 단위: kg CO₂e / 백만 원
  5. `Math.round(intensity * 10.0) / 10.0`로 소수점 첫째 자리까지 반올림합니다.
- **비즈니스 로직:** [기능 1] 요구사항에 따라 "탄소 집약도(kg CO₂e / 백만 원)" 지표를 계산합니다.

#### 2. `SupplierRepository` 클래스

**파일:** `hw10/repository/SupplierRepository.java`  
**패키지:** `hw10.repository`  
**접근 제어자:** `public final class`  
**역할:** 공급업체 관련 데이터베이스 조회 작업을 수행하는 리포지토리입니다. [기능 3] 요구사항에 따라 ESG 등급 및 지연율 필터링 기능을 제공합니다.

##### 내부 레코드 클래스

**`public record SupplierRow(int supplierId, String name, String country, String esgGrade, double totalOrderAmount, int delayedDeliveries, int totalDeliveries, double delayRatio)`**
- 공급업체 목록 행 데이터를 담는 레코드입니다.
- `delayRatio`: 지연 납품 비율 (0.0 ~ 1.0)

**`public record SupplierPoRow(int poid, java.sql.Date orderDate, String status, boolean delayed)`**
- 공급업체의 발주 이력을 담는 레코드입니다.
- `delayed`: 지연 여부 (boolean)

##### 메서드 상세 설명

**`public List<SupplierRow> listSuppliers(Connection conn, List<String> esgGrades, Double minRatio, Double maxRatio) throws SQLException`**
- **매개변수:**
  - `Connection conn`: 데이터베이스 연결 객체
  - `List<String> esgGrades`: ESG 등급 필터 (예: ["A", "B"], null 가능)
  - `Double minRatio`: 최소 지연율 필터 (0.0 ~ 1.0, null 가능)
  - `Double maxRatio`: 최대 지연율 필터 (0.0 ~ 1.0, null 가능)
- **반환값:** `List<SupplierRow>` - 공급업체 목록
- **SQL 쿼리 구조 (CTE 사용):**
```sql
WITH order_totals AS (
  -- 1단계: 공급사별 총 발주 금액 계산
  SELECT po.SupplierID, SUM(pol.Quantity * pol.UnitPriceAtOrder) AS total_amount
  FROM PurchaseOrder po
  JOIN PurchaseOrderLine pol ON pol.POID = po.POID
  GROUP BY po.SupplierID
),
delivery_stats AS (
  -- 2단계: 공급사별 납품 건수 및 지연 건수 계산
  SELECT po.SupplierID,
         COUNT(*) AS total_deliveries,
         SUM(CASE WHEN d.Status = '지연' THEN 1 ELSE 0 END) AS delayed_deliveries
  FROM PurchaseOrder po
  JOIN Delivery d ON d.POID = po.POID
  GROUP BY po.SupplierID
),
base AS (
  -- 3단계: 공급업체 정보와 통계 조인 및 지연율 계산
  SELECT s.SupplierID, s.Name, s.Country, s.ESGGrade,
         COALESCE(ot.total_amount, 0) AS total_order_amount,
         COALESCE(ds.delayed_deliveries, 0) AS delayed_deliveries,
         COALESCE(ds.total_deliveries, 0) AS total_deliveries,
         CASE WHEN COALESCE(ds.total_deliveries, 0) = 0 THEN 0
              ELSE (COALESCE(ds.delayed_deliveries, 0)::float / ds.total_deliveries)
         END AS delay_ratio
  FROM Supplier s
  LEFT JOIN order_totals ot ON ot.SupplierID = s.SupplierID
  LEFT JOIN delivery_stats ds ON ds.SupplierID = s.SupplierID
)
SELECT * FROM base WHERE 1=1
  [동적 WHERE 절 추가]
ORDER BY total_order_amount DESC, delay_ratio DESC, SupplierID
```
- **동작 방식:**
  1. **CTE 1단계 (order_totals):**
     - `PurchaseOrder`와 `PurchaseOrderLine`을 조인하여 공급사별 총 발주 금액을 계산합니다.
     - `GROUP BY po.SupplierID`로 그룹화합니다.
  
  2. **CTE 2단계 (delivery_stats):**
     - `PurchaseOrder`와 `Delivery`를 조인하여 공급사별 납품 통계를 계산합니다.
     - `COUNT(*)`로 전체 납품 건수를 계산합니다.
     - `SUM(CASE WHEN d.Status = '지연' THEN 1 ELSE 0 END)`로 지연 납품 건수를 계산합니다.
  
  3. **CTE 3단계 (base):**
     - `Supplier` 테이블을 기준으로 `LEFT JOIN`을 수행합니다.
     - `COALESCE`로 NULL 값을 0으로 처리합니다.
     - 지연율 계산: `delayed_deliveries / total_deliveries`
     - `total_deliveries`가 0이면 지연율을 0으로 설정합니다.
     - `::float` 캐스팅으로 정수 나눗셈을 방지합니다.
  
  4. **동적 WHERE 절 생성:**
     - `StringBuilder`를 사용하여 쿼리를 동적으로 구성합니다.
     - ESG 등급 필터: `esgGrades`가 있으면 `AND ESGGrade IN (?, ?, ...)` 추가
     - 최소 지연율 필터: `minRatio`가 있으면 `AND delay_ratio >= ?` 추가
     - 최대 지연율 필터: `maxRatio`가 있으면 `AND delay_ratio <= ?` 추가
  
  5. **파라미터 바인딩:**
     - `List<Object> params`에 필터 값을 순서대로 추가합니다.
     - `PreparedStatement`에 동적으로 바인딩합니다.
  
  6. **정렬:**
     - 발주 금액 내림차순, 지연율 내림차순, SupplierID 오름차순으로 정렬합니다.
  
  7. **결과 변환:**
     - `ResultSet`을 순회하며 `SupplierRow` 리스트를 생성합니다.
  
- **비즈니스 로직:** [기능 3] 요구사항에 따라 ESG 등급 및 지연 납품 비율 필터링을 지원합니다.

**`public List<SupplierPoRow> recentPurchaseOrders(Connection conn, int supplierId, int limit, int offset) throws SQLException`**
- **매개변수:**
  - `Connection conn`: 데이터베이스 연결 객체
  - `int supplierId`: 공급업체 ID
  - `int limit`: 조회할 최대 개수
  - `int offset`: 건너뛸 레코드 수 (페이징용)
- **반환값:** `List<SupplierPoRow>` - 발주 이력 리스트
- **SQL 쿼리:**
```sql
SELECT po.POID, po.OrderDate, po.Status,
       CASE WHEN EXISTS (
           SELECT 1 FROM Delivery d
           WHERE d.POID = po.POID AND d.Status = '지연'
       ) THEN TRUE ELSE FALSE END AS delayed
FROM PurchaseOrder po
WHERE po.SupplierID = ?
ORDER BY po.OrderDate DESC, po.POID DESC
LIMIT ? OFFSET ?
```
- **동작 방식:**
  1. 특정 공급업체의 발주서를 조회합니다.
  2. `EXISTS` 서브쿼리를 사용하여 해당 발주서에 지연 배송이 있는지 확인합니다.
  3. 지연 배송이 있으면 `delayed = TRUE`, 없으면 `FALSE`로 설정합니다.
  4. `OrderDate DESC, POID DESC`로 최신순 정렬합니다.
  5. `LIMIT`과 `OFFSET`으로 페이징 처리를 수행합니다.
- **비즈니스 로직:** [기능 3] 요구사항에 따라 최근 N개 발주서 목록과 지연 여부를 제공합니다.

#### 3. `OrderRepository` 클래스

**파일:** `hw10/repository/OrderRepository.java`  
**패키지:** `hw10.repository`  
**접근 제어자:** `public final class`  
**역할:** 발주서(PurchaseOrder) 및 발주 품목(PurchaseOrderLine) 데이터를 데이터베이스에 저장하는 리포지토리입니다.

##### 메서드 상세 설명

**`public void insertPurchaseOrder(Connection conn, int poid, Date orderDate, String status, String engineerName, int projectId, int supplierId) throws SQLException`**
- **매개변수:**
  - `Connection conn`: 데이터베이스 연결 객체 (트랜잭션 내에서 사용)
  - `int poid`: 발주서 ID
  - `Date orderDate`: 발주일
  - `String status`: 발주 상태 ("요청", "발주완료" 등)
  - `String engineerName`: 담당 엔지니어 이름
  - `int projectId`: 프로젝트 ID
  - `int supplierId`: 공급업체 ID
- **반환값:** 없음 (void)
- **SQL 쿼리:**
```sql
INSERT INTO PurchaseOrder(POID, OrderDate, Status, EngineerName, ProjectID, SupplierID)
VALUES (?, ?, ?, ?, ?, ?)
```
- **동작 방식:**
  1. `PreparedStatement`를 사용하여 파라미터화된 INSERT 쿼리를 실행합니다.
  2. 모든 파라미터를 순서대로 바인딩합니다.
  3. `ps.executeUpdate()`를 호출하여 레코드를 삽입합니다.
  4. 외래키 제약조건 위반 시 `SQLException`이 발생합니다.
- **트랜잭션 처리:** 트랜잭션 내에서 호출되므로 커밋 전까지는 임시 상태입니다.

**`public void insertPurchaseOrderLine(Connection conn, int poid, int lineNo, int partId, int qty, double unitPrice, Date requestedDueDate) throws SQLException`**
- **매개변수:**
  - `Connection conn`: 데이터베이스 연결 객체
  - `int poid`: 발주서 ID
  - `int lineNo`: 라인 번호 (1부터 시작)
  - `int partId`: 부품 ID
  - `int qty`: 수량
  - `double unitPrice`: 단가
  - `Date requestedDueDate`: 요청 납기일 (null 가능)
- **반환값:** 없음 (void)
- **SQL 쿼리:**
```sql
INSERT INTO PurchaseOrderLine(POID, LineNo, PartID, Quantity, UnitPriceAtOrder, RequestedDueDate)
VALUES (?, ?, ?, ?, ?, ?)
```
- **동작 방식:**
  1. 발주서의 각 품목을 `PurchaseOrderLine` 테이블에 삽입합니다.
  2. `lineNo`는 발주서 내에서 품목의 순서를 나타냅니다.
  3. `requestedDueDate`가 `null`이면 `ps.setNull(6, java.sql.Types.DATE)`로 NULL을 설정합니다.
- **용도:** 발주서 생성 시 여러 품목을 반복 호출하여 저장합니다.

#### 4. `DeliveryRepository` 클래스

**파일:** `hw10/repository/DeliveryRepository.java`  
**패키지:** `hw10.repository`  
**접근 제어자:** `public final class`  
**역할:** 납품(Delivery) 및 납품 품목(DeliveryLine) 데이터를 데이터베이스에 저장하는 리포지토리입니다.

##### 메서드 상세 설명

**`public void insertDelivery(Connection conn, int deliveryId, int poid, Date actualArrivalDate, String transportMode, Double distanceKm, String status) throws SQLException`**
- **매개변수:**
  - `Connection conn`: 데이터베이스 연결 객체
  - `int deliveryId`: 납품서 ID
  - `int poid`: 발주서 ID (외래키)
  - `Date actualArrivalDate`: 실제 도착일
  - `String transportMode`: 운송 수단
  - `Double distanceKm`: 운송 거리 (km, null 가능)
  - `String status`: 납품 상태 ("정상입고", "지연" 등)
- **반환값:** 없음 (void)
- **SQL 쿼리:**
```sql
INSERT INTO Delivery(DeliveryID, POID, ActualArrivalDate, TransportMode, DistanceKm, Status)
VALUES (?, ?, ?, ?, ?, ?)
```
- **동작 방식:**
  1. 초기 납품 레코드를 `Delivery` 테이블에 삽입합니다.
  2. `distanceKm`가 `null`이면 `ps.setNull(5, java.sql.Types.DOUBLE)`로 NULL을 설정합니다.
  3. [기능 2] 요구사항에 따라 발주 직후 "초기 납품" 1건을 자동 생성합니다.
- **비즈니스 로직:** 발주 등록 시 자동으로 초기 납품 기록을 생성합니다.

**`public void insertDeliveryLine(Connection conn, int deliveryId, int poid, int lineNo, int receivedQty, String inspectionResult) throws SQLException`**
- **매개변수:**
  - `Connection conn`: 데이터베이스 연결 객체
  - `int deliveryId`: 납품서 ID
  - `int poid`: 발주서 ID
  - `int lineNo`: 라인 번호 (발주 품목과 일치)
  - `int receivedQty`: 실제 수령 수량
  - `String inspectionResult`: 검수 결과 ("초기납품(자동)" 등)
- **반환값:** 없음 (void)
- **SQL 쿼리:**
```sql
INSERT INTO DeliveryLine(DeliveryID, POID, LineNo, ReceivedQty, InspectionResult)
VALUES (?, ?, ?, ?, ?)
```
- **동작 방식:**
  1. 납품서의 각 품목을 `DeliveryLine` 테이블에 삽입합니다.
  2. `receivedQty`는 발주 수량의 50%로 자동 계산됩니다.
  3. [기능 2] 요구사항에 따라 초기 납품 수량을 자동 설정합니다.

#### 5. `InventoryRepository` 클래스

**파일:** `hw10/repository/InventoryRepository.java`  
**패키지:** `hw10.repository`  
**접근 제어자:** `public final class`  
**역할:** 창고 재고(Inventory) 데이터를 관리하는 리포지토리입니다. UPSERT 기능을 제공합니다.

##### 메서드 상세 설명

**`public void addInventory(Connection conn, int warehouseId, int partId, int deltaQty) throws SQLException`**
- **매개변수:**
  - `Connection conn`: 데이터베이스 연결 객체
  - `int warehouseId`: 창고 ID
  - `int partId`: 부품 ID
  - `int deltaQty`: 증가할 수량 (음수면 차감)
- **반환값:** 없음 (void)
- **SQL 쿼리:**
```sql
INSERT INTO Inventory(WarehouseID, PartID, Quantity)
VALUES (?, ?, ?)
ON CONFLICT (WarehouseID, PartID)
DO UPDATE SET Quantity = Inventory.Quantity + EXCLUDED.Quantity
```
- **동작 방식:**
  1. PostgreSQL의 `ON CONFLICT` 구문을 사용하여 UPSERT를 구현합니다.
  2. `(WarehouseID, PartID)` 조합이 기본키 또는 유니크 제약조건을 가정합니다.
  3. 레코드가 없으면: `INSERT`로 새 레코드를 생성하고 `Quantity = deltaQty`로 설정합니다.
  4. 레코드가 있으면: `UPDATE`로 기존 `Quantity`에 `deltaQty`를 더합니다.
  5. `EXCLUDED`는 INSERT 시도한 값을 참조하는 키워드입니다.
- **비즈니스 로직:** [기능 2] 요구사항에 따라 납품 시 창고 재고를 자동으로 증가시킵니다. 기존 재고가 없으면 신규 생성, 있으면 수량을 더합니다.
- **트랜잭션 처리:** 트랜잭션 내에서 호출되므로 롤백 시 재고 변경도 함께 취소됩니다.

#### 6. `SequenceGenerator` 클래스

**파일:** `hw10/repository/SequenceGenerator.java`  
**패키지:** `hw10.repository`  
**접근 제어자:** `public final class`  
**역할:** 데이터베이스 테이블의 기본키(Primary Key) 값을 자동 생성하는 유틸리티 클래스입니다. 시퀀스 대신 MAX 값 기반 ID 생성을 사용합니다.

##### 클래스 구조 및 상수

```java
private static final Set<String> ALLOWED = Set.of(
    "PurchaseOrder.POID",
    "Delivery.DeliveryID",
    "CarbonEmissionRecord.RecordID"
);
```

허용된 테이블.컬럼 조합만 ID 생성을 허용하여 SQL 인젝션을 방지합니다.

##### 메서드 상세 설명

**`public int nextId(Connection conn, String tableDotCol) throws SQLException`**
- **매개변수:**
  - `Connection conn`: 데이터베이스 연결 객체
  - `String tableDotCol`: "테이블명.컬럼명" 형식의 문자열 (예: "PurchaseOrder.POID")
- **반환값:** `int` - 다음 ID 값
- **동작 방식:**
  1. **보안 검증:**
     - `ALLOWED.contains(tableDotCol)`로 허용된 컬럼인지 확인합니다.
     - 허용되지 않은 경우 `IllegalArgumentException`을 발생시킵니다.
     - SQL 인젝션 공격을 방지합니다.
  
  2. **테이블 및 컬럼 분리:**
     - `tableDotCol.split("\\.")`로 점(.)을 기준으로 분리합니다.
     - `parts[0]`은 테이블명, `parts[1]`은 컬럼명입니다.
  
  3. **동적 SQL 쿼리 생성:**
     - 쿼리: `SELECT COALESCE(MAX(컬럼명), 0) + 1 AS next_id FROM 테이블명`
     - `COALESCE(MAX(...), 0)`로 NULL 값을 0으로 처리합니다 (테이블이 비어있는 경우).
     - `+ 1`로 다음 ID를 계산합니다.
  
  4. **쿼리 실행:**
     - `PreparedStatement`를 사용하지만 테이블명과 컬럼명은 동적으로 구성됩니다.
     - 허용 목록 검증으로 안전성을 보장합니다.
     - `ResultSet`에서 `next_id` 값을 추출하여 반환합니다.
  
- **주의사항:**
  - 동시성 환경에서 두 트랜잭션이 동시에 같은 ID를 생성할 수 있습니다.
  - 트랜잭션 격리 수준(READ_COMMITTED)과 재시도 로직으로 이를 완화합니다.
  - 프로덕션 환경에서는 시퀀스(SEQUENCE) 사용을 권장합니다.
  
- **사용 예시:**
```java
int poid = sequenceGenerator.nextId(conn, "PurchaseOrder.POID");
// 결과: 기존 최대값이 100이면 101을 반환
```

---

### 2.2.7 `hw10.dto` 패키지 (데이터 객체)

계층 간 데이터 전송을 위한 불변 객체(`record`)들입니다. Java 14+의 `record` 기능을 사용하여 간결하고 안전한 DTO를 구현했습니다.

#### 1. `MainDto` 클래스

**파일:** `hw10/dto/MainDto.java`  
**패키지:** `hw10.dto`  
**역할:** 메인 대시보드용 데이터 전송 객체입니다.

##### 내부 레코드 클래스

**`public record MainSummary(int activeProjects, double carbonReduction, int delayedDeliveries, String avgEsgGrade)`**
- **필드:**
  - `activeProjects`: 활성 프로젝트 수 (int)
  - `carbonReduction`: 탄소 감축량 (double, kg CO₂e)
  - `delayedDeliveries`: 지연 배송 건수 (int)
  - `avgEsgGrade`: 평균 ESG 등급 (String, "A", "B", "C", "D")
- **용도:** 메인 대시보드 상단의 4개 요약 카드 데이터를 전송합니다.
- **JSON 직렬화:** Spring이 자동으로 JSON으로 변환하여 HTTP 응답에 포함합니다.

#### 2. `OrderDto` 클래스

**파일:** `hw10/dto/OrderDto.java`  
**패키지:** `hw10.dto`  
**역할:** 발주 관련 데이터 전송 객체입니다.

##### 내부 레코드 클래스

**`public record OrderRequest(int projectId, int supplierId, String engineerName, String status, List<OrderLineInput> lines, int warehouseId, String transportMode, Double distanceKm)`**
- **필드:**
  - `projectId`: 프로젝트 ID
  - `supplierId`: 공급업체 ID
  - `engineerName`: 담당 엔지니어 이름
  - `status`: 발주 상태
  - `lines`: 발주 품목 리스트
  - `warehouseId`: 입고 창고 ID
  - `transportMode`: 운송 수단
  - `distanceKm`: 운송 거리 (null 가능)
- **용도:** 발주 등록 API의 요청 본문으로 사용됩니다.

**`public record OrderLineInput(int partId, int quantity, double unitPrice)`**
- 발주 품목 입력 데이터입니다.
- **필드:**
  - `partId`: 부품 ID
  - `quantity`: 수량
  - `unitPrice`: 단가

**`public record OrderResponse(int poid, int deliveryId, String message)`**
- 발주 등록 결과 응답입니다.
- **필드:**
  - `poid`: 생성된 발주서 ID
  - `deliveryId`: 생성된 납품서 ID
  - `message`: 결과 메시지

**`public record SupplierOption(int supplierId, String name, String country)`**
- 공급업체 드롭다운 옵션입니다.

**`public record PartOption(int partId, String name, String unit, double unitPrice)`**
- 부품 검색 결과입니다.
- `unitPrice`: 평균 단가

**`public record WarehouseOption(int warehouseId, String name, String location)`**
- 창고 드롭다운 옵션입니다.

**`public record InventoryItem(int partId, String partName, String unit, int quantity)`**
- 창고 재고 항목입니다.
- `partName`: 부품명 (Part 테이블과 조인하여 조회)

#### 3. `ProjectDto` 클래스

**파일:** `hw10/dto/ProjectDto.java`  
**패키지:** `hw10.dto`  
**역할:** 프로젝트 관련 데이터 전송 객체입니다.

##### 내부 레코드 클래스

**`public record ProjectBasic(int projectId, String shipName, String shipType, java.sql.Date contractDate, java.sql.Date deliveryDueDate, String status)`**
- 프로젝트 기본 정보입니다.
- **용도:** 프로젝트 상세 조회 및 대시보드에 사용됩니다.

**`public record SupplierAmount(int supplierId, String name, double amount)`**
- 공급업체별 발주 금액 통계입니다.
- **용도:** 대시보드에서 상위 공급업체를 표시하는 데 사용됩니다.

**`public record ProjectSearchItem(int projectId, String shipName, String shipType, String status)`**
- 프로젝트 검색 결과 항목입니다.
- **용도:** 프로젝트 검색 및 드롭다운에 사용됩니다.

**`public record DashboardStats(ProjectBasic project, double totalOrderAmount, double totalEmission, double transportEmission, double storageEmission, double processingEmission, List<SupplierAmount> topSuppliers, Double carbonIntensity, Double shipCII)`**
- 프로젝트 대시보드 통계 데이터입니다.
- **필드:**
  - `project`: 프로젝트 기본 정보
  - `totalOrderAmount`: 총 발주 금액
  - `totalEmission`: 전체 탄소 배출량
  - `transportEmission`: 운송 탄소 배출량
  - `storageEmission`: 보관 탄소 배출량
  - `processingEmission`: 가공/생산 탄소 배출량
  - `topSuppliers`: 상위 3개 공급업체 리스트
  - `carbonIntensity`: 탄소 집약도 (null 가능)
  - `shipCII`: 선박 CII 지표 (현재 null)
- **용도:** [기능 1] 프로젝트 대시보드 리포트에 사용됩니다.

#### 4. `SupplierDto` 클래스

**파일:** `hw10/dto/SupplierDto.java`  
**패키지:** `hw10.dto`  
**역할:** 공급업체 관련 데이터 전송 객체입니다.

##### 내부 레코드 클래스

**`public record SupplierRow(int supplierId, String name, String country, String esgGrade, double totalOrderAmount, int delayedDeliveries, int totalDeliveries, double delayRatio)`**
- 공급업체 목록 행 데이터입니다.
- **필드:**
  - `delayRatio`: 지연 납품 비율 (0.0 ~ 1.0)
- **용도:** [기능 3] 공급업체 ESG 및 지연 납품 리포트에 사용됩니다.

**`public record SupplierPoRow(int poid, java.sql.Date orderDate, String status, boolean delayed)`**
- 공급업체의 발주 이력입니다.
- `delayed`: 지연 여부 (boolean)

**`public record SupplierDetail(SupplierRow supplier, List<SupplierPoRow> recentOrders)`**
- 공급업체 상세 정보입니다.
- **필드:**
  - `supplier`: 공급업체 기본 정보 및 통계
  - `recentOrders`: 최근 발주 내역 리스트 (최대 5개)
- **용도:** [기능 3] 공급업체 상세 화면에 사용됩니다.

---

### 2.2.8 `hw10.exception` 및 `util`

#### 1. `GlobalExceptionHandler` 클래스

**파일:** `hw10/exception/GlobalExceptionHandler.java`  
**패키지:** `hw10.exception`  
**어노테이션:** `@RestControllerAdvice`  
**역할:** 애플리케이션 전역에서 발생하는 예외를 처리하는 예외 핸들러입니다. [기능 4] 요구사항에 따라 사용자에게 이해 가능한 에러 메시지를 제공합니다.

##### 클래스 구조

**내부 레코드 클래스:**
```java
public record ErrorResponse(String code, String message)
```
- 에러 응답 데이터를 담는 불변 레코드입니다.
- `code`: 에러 코드 (예: "DATABASE_ERROR")
- `message`: 사용자에게 표시할 에러 메시지

##### 메서드 상세 설명

**`@ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e)`**
- **처리 예외:** `IllegalArgumentException`
- **반환값:** `ResponseEntity<ErrorResponse>` - HTTP 400 Bad Request
- **동작 방식:**
  1. 잘못된 요청 파라미터로 인한 예외를 처리합니다.
  2. `Logger.warn("잘못된 요청 파라미터: " + e.getMessage())`로 경고 로그를 기록합니다.
  3. `ErrorResponse` 객체를 생성하여 HTTP 400 상태 코드와 함께 반환합니다.
- **용도:** 클라이언트가 잘못된 데이터를 전송한 경우를 처리합니다.

**`@ExceptionHandler(SQLException.class) public ResponseEntity<ErrorResponse> handleSQLException(SQLException e)`**
- **처리 예외:** `SQLException`
- **반환값:** `ResponseEntity<ErrorResponse>` - HTTP 500 Internal Server Error
- **동작 방식:**
  1. 데이터베이스 관련 예외를 처리합니다.
  2. `Logger.error("데이터베이스 오류 발생", e)`로 에러 로그를 기록합니다.
     - 개발용 상세 내용(스택 트레이스)은 로그에 남깁니다.
  3. 기본 메시지: "데이터베이스 오류가 발생했습니다."
  4. 예외 메시지에 "롤백", "Rollback", "rollback", "실패" 키워드가 포함된 경우:
     - 메시지를 "트랜잭션 롤백 알림: [에러 메시지]"로 변경합니다.
     - 사용자에게 롤백 사실을 명확히 알립니다.
  5. HTTP 500 상태 코드와 함께 `ErrorResponse`를 반환합니다.
- **비즈니스 로직:** [기능 4] 요구사항에 따라 DB 접속 실패, 쿼리 오류, 제약조건 위반 상황에서 프로그램이 중단되지 않고 사용자에게 이해 가능한 메시지를 제공합니다.

**`@ExceptionHandler(NoResourceFoundException.class) public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException e)`**
- **처리 예외:** `NoResourceFoundException`
- **반환값:** `ResponseEntity<Void>` - HTTP 404 Not Found
- **동작 방식:**
  1. 정적 리소스를 찾을 수 없는 경우를 처리합니다.
  2. 특정 리소스 경로는 로그를 남기지 않습니다:
     - `/favicon.ico`, `*.ico`: 파비콘 요청
     - `/.well-known/*`: 웹 표준 리소스
     - `chrome.devtools`, `appspecific`: 브라우저 개발자 도구 리소스
     - `/robots.txt`, `/sitemap.xml`: 검색 엔진 리소스
     - `/apple-touch-icon*`: iOS 아이콘
  3. 그 외의 리소스는 `Logger.warn()`으로 경고 로그를 기록합니다.
  4. HTTP 404 상태 코드를 반환합니다.
- **용도:** 불필요한 404 에러 로그를 줄이고 중요한 리소스 누락만 로깅합니다.

**`@ExceptionHandler(Exception.class) public ResponseEntity<ErrorResponse> handleException(Exception e)`**
- **처리 예외:** `Exception` (모든 예외의 최상위 클래스)
- **반환값:** `ResponseEntity<ErrorResponse>` - HTTP 500 Internal Server Error
- **동작 방식:**
  1. 위에서 처리되지 않은 모든 예외를 처리합니다 (fallback).
  2. `Logger.error("서버 오류 발생", e)`로 에러 로그를 기록합니다.
  3. 기본 메시지: "서버 오류가 발생했습니다."
  4. 예외 메시지가 있으면 "API 오류 (트랜잭션 롤백됨): [에러 메시지]"로 변경합니다.
  5. HTTP 500 상태 코드와 함께 `ErrorResponse`를 반환합니다.
- **용도:** 예상치 못한 예외를 처리하여 애플리케이션이 중단되지 않도록 보장합니다.

#### 2. `Logger` 클래스

**파일:** `hw10/util/Logger.java`  
**패키지:** `hw10.util`  
**접근 제어자:** `public final class`  
**역할:** 애플리케이션 로깅을 관리하는 유틸리티 클래스입니다. [기능 4] 요구사항에 따라 파일 로그와 콘솔 로그를 동시에 출력합니다.

##### 클래스 구조 및 필드

```java
private static final java.util.logging.Logger LOGGER = 
    java.util.logging.Logger.getLogger("scm");
private static boolean initialized = false;
```

- `LOGGER`: Java 표준 로깅 API의 Logger 인스턴스
- `initialized`: 초기화 여부를 추적하는 플래그 (중복 초기화 방지)

##### 생성자

**`private Logger()`**
- **접근 제어자:** `private` (인스턴스화 방지)
- **용도:** 유틸리티 클래스이므로 인스턴스를 생성할 수 없도록 합니다.

##### 메서드 상세 설명

**`public static void init()`**
- **접근 제어자:** `public static`
- **매개변수:** 없음
- **반환값:** 없음 (void)
- **동작 방식:**
  1. **중복 초기화 방지:**
     - `initialized` 플래그를 확인하여 이미 초기화되었으면 즉시 반환합니다.
     - `initialized = true`로 설정합니다.
  
  2. **로거 설정:**
     - `LOGGER.setUseParentHandlers(false)`: 부모 핸들러를 비활성화하여 중복 로그를 방지합니다.
     - `LOGGER.setLevel(Level.INFO)`: 로그 레벨을 INFO로 설정합니다.
  
  3. **커스텀 포맷터 생성:**
     - `Formatter`를 구현하여 로그 포맷을 정의합니다.
     - 포맷: `[YYYY-MM-DD HH:MM:SS] [LEVEL] 메시지`
     - 예외가 있으면 예외 정보와 스택 트레이스 첫 줄을 추가합니다.
  
  4. **콘솔 핸들러 설정:**
     - `ConsoleHandler`를 생성하고 INFO 레벨로 설정합니다.
     - 커스텀 포맷터를 적용합니다.
     - `LOGGER.addHandler(ch)`로 핸들러를 등록합니다.
  
  5. **파일 핸들러 설정:**
     - `Files.createDirectories(Path.of("logs"))`로 로그 디렉토리를 생성합니다.
     - `FileHandler("logs/app.log", true)`를 생성합니다.
       - `true`: 이어쓰기 모드 (기존 파일에 추가)
     - INFO 레벨로 설정하고 커스텀 포맷터를 적용합니다.
     - `LOGGER.addHandler(fh)`로 핸들러를 등록합니다.
     - `IOException` 발생 시 경고 로그만 기록하고 계속 진행합니다.
  
- **비즈니스 로직:** [기능 4] 요구사항에 따라 애플리케이션 시작/종료, DB 접속, 트랜잭션, 오류를 로그로 기록합니다.

**`public static void info(String msg)`**
- **접근 제어자:** `public static`
- **매개변수:** `String msg` - 로그 메시지
- **반환값:** 없음 (void)
- **동작 방식:**
  1. `LOGGER.info(msg)`를 호출하여 INFO 레벨 로그를 기록합니다.
  2. 콘솔과 파일에 동시에 출력됩니다.
- **용도:** 일반 정보 메시지를 기록합니다 (예: "애플리케이션 시작", "트랜잭션 커밋 완료").

**`public static void warn(String msg)`**
- **접근 제어자:** `public static`
- **매개변수:** `String msg` - 경고 메시지
- **반환값:** 없음 (void)
- **동작 방식:**
  1. `LOGGER.warning(msg)`를 호출하여 WARNING 레벨 로그를 기록합니다.
  2. 콘솔과 파일에 동시에 출력됩니다.
- **용도:** 경고 메시지를 기록합니다 (예: "트랜잭션 롤백", "교착상태 감지").

**`public static void error(String msg, Throwable t)`**
- **접근 제어자:** `public static`
- **매개변수:**
  - `String msg` - 에러 메시지
  - `Throwable t` - 예외 객체 (null 가능)
- **반환값:** 없음 (void)
- **동작 방식:**
  1. `LOGGER.log(Level.SEVERE, msg, t)`를 호출하여 SEVERE 레벨 로그를 기록합니다.
  2. 예외 객체가 있으면 스택 트레이스도 함께 기록됩니다.
  3. 콘솔과 파일에 동시에 출력됩니다.
- **용도:** 심각한 에러를 기록합니다 (예: "데이터베이스 오류 발생", "롤백 실패").

---

## 2.3 클래스 간 관계 및 데이터 흐름

### 2.3.1 계층 구조

애플리케이션은 **계층형 아키텍처(Layered Architecture)**를 따릅니다:

1. **Controller 계층**: HTTP 요청을 받아 Service 계층을 호출하고 JSON 응답을 반환합니다.
2. **Service 계층**: 비즈니스 로직을 처리하고 트랜잭션을 관리합니다. Repository 계층을 호출합니다.
3. **Repository 계층**: 데이터베이스 쿼리를 실행하고 결과를 도메인 객체로 변환합니다.
4. **DTO 계층**: 계층 간 데이터 전송을 위한 불변 객체입니다.

### 2.3.2 의존성 주입

Spring Framework의 **의존성 주입(Dependency Injection)**을 사용하여 느슨한 결합을 구현했습니다:

- 모든 Controller는 생성자 기반 주입을 통해 Service 인스턴스를 받습니다.
- 모든 Service는 생성자 기반 주입을 통해 DataSource 인스턴스를 받습니다.
- Repository는 Service에서 직접 인스턴스화됩니다 (향후 Spring Bean으로 전환 가능).

### 2.3.3 트랜잭션 처리 흐름

[기능 2] 발주 등록 트랜잭션의 실행 흐름:

```
1. OrderController.createOrder()
   ↓
2. OrderService.createOrder()
   ↓
3. OrderTransactionService.createOrderWithInitialDelivery()
   ├─ Connection 획득
   ├─ setAutoCommit(false) → 트랜잭션 시작
   ├─ setTransactionIsolation(READ_COMMITTED)
   ├─ SequenceGenerator.nextId() → POID 생성
   ├─ OrderRepository.insertPurchaseOrder() → 발주서 삽입
   ├─ OrderRepository.insertPurchaseOrderLine() → 발주 품목 삽입 (반복)
   ├─ SequenceGenerator.nextId() → DeliveryID 생성
   ├─ DeliveryRepository.insertDelivery() → 납품서 삽입
   ├─ DeliveryRepository.insertDeliveryLine() → 납품 품목 삽입 (반복)
   ├─ InventoryRepository.addInventory() → 재고 반영 (반복)
   ├─ commit() → 커밋 (성공 시)
   └─ rollback() → 롤백 (실패 시)
```

### 2.3.4 예외 처리 흐름

모든 예외는 `GlobalExceptionHandler`에서 처리됩니다:

```
예외 발생
   ↓
GlobalExceptionHandler.@ExceptionHandler()
   ├─ Logger.error() → 로그 기록
   ├─ 사용자 친화적 메시지 생성
   └─ ResponseEntity 반환 (HTTP 상태 코드 포함)
```

---

## 2.4 주요 설계 패턴 및 원칙

### 2.4.1 사용된 설계 패턴

1. **Repository 패턴**: 데이터 접근 로직을 캡슐화하여 비즈니스 로직과 분리했습니다.
2. **DTO 패턴**: 계층 간 데이터 전송을 위한 전용 객체를 사용하여 도메인 모델을 보호했습니다.
3. **의존성 주입 패턴**: Spring Framework를 통해 의존성을 외부에서 주입하여 테스트 용이성을 높였습니다.
4. **불변 객체 패턴**: `record` 클래스를 사용하여 DTO의 불변성을 보장했습니다.

### 2.4.2 SOLID 원칙 준수

1. **단일 책임 원칙 (SRP)**: 각 클래스는 하나의 책임만 가집니다.
   - Controller: HTTP 요청/응답 처리
   - Service: 비즈니스 로직 처리
   - Repository: 데이터 접근 처리

2. **의존성 역전 원칙 (DIP)**: 고수준 모듈(Service)이 저수준 모듈(Repository)에 의존하지 않고 추상화에 의존합니다.

3. **개방-폐쇄 원칙 (OCP)**: 확장에는 열려있고 수정에는 닫혀있도록 인터페이스를 활용할 수 있는 구조입니다.

---

## 2.5 요구사항 구현 현황

### 2.5.1 [기능 1] 프로젝트 대시보드 및 탄소·비용 리포트

✅ **구현 완료**
- 프로젝트 기본 정보 조회 (`ProjectController.getProject()`)
- 총 발주 금액 계산 (`ProjectRepository.totalOrderAmount()`)
- 공급업체별 발주 금액 상위 3개 (`ProjectRepository.topSuppliersByAmount()`)
- 운송/보관 관련 탄소 배출 합계 (`ProjectRepository.emissionSumByType()`)
- 프로젝트 전체 탄소 배출 합계 (`ProjectRepository.emissionSumTotal()`)
- 탄소 집약도 계산 (`ProjectRepository.calculateCarbonIntensity()`)

### 2.5.2 [기능 2] 발주 및 납품 관리 + 트랜잭션 처리

✅ **구현 완료**
- 발주서 생성 (`OrderRepository.insertPurchaseOrder()`)
- 발주 품목 등록 (`OrderRepository.insertPurchaseOrderLine()`)
- 초기 납품 자동 생성 (`DeliveryRepository.insertDelivery()`)
- 납품 품목 등록 (`DeliveryRepository.insertDeliveryLine()`)
- 창고 재고 반영 (`InventoryRepository.addInventory()`)
- 트랜잭션 처리 (`OrderTransactionService.createOrderWithInitialDelivery()`)
- 롤백 처리 및 재시도 로직 구현

### 2.5.3 [기능 3] 공급업체 ESG 및 지연 납품 리포트

✅ **구현 완료**
- 공급업체 목록 조회 (`SupplierRepository.listSuppliers()`)
- ESG 등급 필터링 (다중 선택 지원)
- 지연 납품 비율 필터링 (상한/하한 지원)
- 공급업체 상세 정보 조회 (`SupplierService.getSupplierDetail()`)
- 최근 N개 발주서 목록 조회 (`SupplierRepository.recentPurchaseOrders()`)

### 2.5.4 [기능 4] 예외 처리·로그·환경 설정

✅ **구현 완료**
- 전역 예외 처리 (`GlobalExceptionHandler`)
- 사용자 친화적 에러 메시지 제공
- 로그 시스템 (`Logger` 클래스)
  - 파일 로그 (`logs/app.log`)
  - 콘솔 로그
  - 애플리케이션 시작/종료 기록
  - DB 접속 성공/실패 기록
  - 트랜잭션 시작/커밋/롤백 기록
  - 주요 오류 발생 시 메시지 기록
- 환경 설정 분리 (`DatabaseConfig`)
  - 환경변수 우선
  - `application.properties` 지원
  - 레거시 `config.properties` 지원

---

## 2.6 결론

본 애플리케이션은 **계층형 아키텍처**를 기반으로 하여 각 계층의 책임을 명확히 분리하고, **Spring Framework**의 의존성 주입을 활용하여 유연하고 테스트 가능한 구조를 구현했습니다.

**트랜잭션 처리**에서는 [기능 2] 요구사항을 충실히 구현하여 발주서 생성부터 재고 반영까지의 모든 과정을 하나의 트랜잭션으로 묶고, 교착상태 발생 시 자동 재시도 로직을 포함하여 동시성 문제를 해결했습니다.

**예외 처리 및 로깅**에서는 [기능 4] 요구사항에 따라 사용자에게 이해 가능한 메시지를 제공하면서도 개발자를 위한 상세한 로그를 기록하여 디버깅을 용이하게 했습니다.

모든 클래스와 메서드는 **명확한 주석과 문서화**를 통해 코드의 가독성과 유지보수성을 높였으며, **Java의 최신 기능(record, try-with-resources 등)**을 적극 활용하여 간결하고 안전한 코드를 작성했습니다.
