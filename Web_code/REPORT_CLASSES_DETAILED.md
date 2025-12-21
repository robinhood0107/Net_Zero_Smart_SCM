# 2. 구현한 클래스 설명

본 보고서에서는 탄소중립 스마트 SCM 플랫폼 애플리케이션의 전체 클래스 구조와 각 클래스의 역할, 메서드의 상세한 동작 방식을 설명합니다. 본 애플리케이션은 Java 언어를 사용하여 구현되었으며, Spring Boot 프레임워크를 기반으로 계층형 아키텍처를 채택하고 있습니다.

## 2.1 클래스 다이어그램 및 전체 아키텍처

애플리케이션의 전체 구조는 계층형 아키텍처(Layered Architecture) 패턴을 따르고 있습니다. 클라이언트로부터의 HTTP 요청은 Controller 계층에서 수신되며, 이는 Service 계층으로 전달되어 비즈니스 로직이 처리됩니다. Service 계층은 Repository 계층을 통해 데이터베이스에 접근하며, 각 계층 간의 데이터 전송은 DTO(Data Transfer Object)를 통해 이루어집니다.

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

이러한 계층 구조는 관심사의 분리(Separation of Concerns) 원칙을 준수하여 각 계층이 명확한 책임을 가지도록 설계되었습니다. Controller 계층은 HTTP 요청과 응답 처리에만 집중하고, Service 계층은 비즈니스 로직과 트랜잭션 관리에 집중하며, Repository 계층은 데이터 접근 로직에만 집중합니다.

## 2.2 패키지별 클래스 상세 설명

### 2.2.1 hw10 패키지 - 애플리케이션 진입점

#### Application 클래스

`Application` 클래스는 Spring Boot 애플리케이션의 진입점(Entry Point) 역할을 담당하는 핵심 클래스입니다. 이 클래스는 `@SpringBootApplication` 어노테이션을 통해 Spring Boot의 자동 설정 기능을 활성화하며, 이 어노테이션은 내부적으로 `@Configuration`, `@EnableAutoConfiguration`, `@ComponentScan` 세 가지 어노테이션을 포함합니다.

`main` 메서드는 프로그램 실행 시 가장 먼저 호출되는 진입점으로, 애플리케이션의 초기화 과정을 담당합니다. 먼저 `Logger.init()` 메서드를 호출하여 로깅 시스템을 초기화하는데, 이는 이후 모든 로그 기록이 정상적으로 작동하도록 보장하기 위한 필수적인 단계입니다. 로깅 시스템 초기화 후에는 "애플리케이션 시작" 메시지를 로그에 기록하여 애플리케이션의 시작을 명시적으로 표시합니다.

이어서 `SpringApplication.run(Application.class, args)` 메서드를 호출하여 Spring IoC 컨테이너를 초기화하고 모든 빈(Bean)을 생성합니다. 이 과정에서 Spring은 클래스패스에 있는 모든 컴포넌트를 스캔하여 자동으로 빈으로 등록하며, 의존성 주입을 통해 각 빈 간의 관계를 설정합니다. 동시에 내장 톰캣 서버가 시작되어 HTTP 요청을 수신할 준비를 하며, 기본적으로 8080 포트에서 서비스를 제공합니다. 서버 시작이 완료되면 "서버 시작 완료: http://localhost:8080" 메시지를 출력하여 사용자에게 서비스 준비 상태를 알립니다.

### 2.2.2 hw10.config 패키지 - 설정 관리

#### DatabaseConfig 클래스

`DatabaseConfig` 클래스는 데이터베이스 연결 정보를 다양한 소스로부터 로드하고 관리하는 설정 클래스입니다. 이 클래스는 불변 클래스(final class)로 설계되어 있으며, 모든 필드가 `final`로 선언되어 있어 한 번 설정된 값은 변경할 수 없도록 보장합니다. 이러한 불변성은 설정 값이 런타임에 의도치 않게 변경되는 것을 방지하여 애플리케이션의 안정성을 높입니다.

`load()` 정적 메서드는 데이터베이스 설정을 로드하는 핵심 메서드로, 여러 소스를 우선순위에 따라 순차적으로 확인합니다. 첫 번째로 시스템 환경변수를 확인하는데, `System.getenv()` 메서드를 사용하여 `DB_URL`, `DB_USER`, `DB_PASSWORD` 환경변수를 읽습니다. 환경변수는 운영 환경에서 보안상 가장 안전한 방법으로 간주되며, 특히 컨테이너 기반 배포 환경에서 널리 사용됩니다. 모든 환경변수가 존재하면 즉시 `DatabaseConfig` 객체를 생성하여 반환합니다.

환경변수에서 설정을 찾지 못한 경우, 두 번째로 클래스패스에 있는 `application.properties` 파일을 확인합니다. 이는 Spring Boot의 표준 설정 파일 형식을 따르며, `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password` 키를 사용합니다. `loadPropertiesFromClasspath()` 메서드는 클래스 로더를 통해 리소스를 스트림으로 읽어 `Properties` 객체로 변환하며, 파일이 없거나 읽기 실패 시 `null`을 반환하여 다음 소스를 시도할 수 있도록 합니다.

세 번째와 네 번째로는 레거시 `config.properties` 파일을 확인합니다. 먼저 클래스패스에서 확인하고, 그 다음 현재 작업 디렉토리에서 직접 파일을 읽습니다. 레거시 파일은 `db.url`, `db.user`, `db.password` 키 형식을 사용하며, 파일이 없거나 읽기 실패 시 예외를 무시하고 경고 로그만 기록합니다. 모든 소스에서 설정을 찾지 못한 경우, 모든 필드가 `null`인 `DatabaseConfig` 객체를 반환하며, 이는 `DatabaseConnection` 생성자에서 `IllegalStateException`을 발생시켜 설정 누락을 명확히 알립니다.

### 2.2.3 hw10.db 패키지 - 데이터베이스 연결

#### DatabaseConnection 클래스

`DatabaseConnection` 클래스는 JDBC 연결 객체를 생성하고 관리하는 래퍼 클래스입니다. 이 클래스는 `AutoCloseable` 인터페이스를 구현하여 `try-with-resources` 구문과 함께 사용할 수 있도록 설계되었으며, 자동 리소스 관리를 통해 메모리 누수를 방지합니다.

생성자는 `DatabaseConfig` 객체를 받아 필수 설정 값의 유효성을 검증합니다. `dbUrl`, `dbUser`, `dbPassword` 중 하나라도 `null`이면 `IllegalStateException`을 발생시켜 설정 오류를 즉시 감지합니다. 이는 컴파일 타임이 아닌 런타임에 설정 오류를 발견할 수 있도록 하며, 사용자에게 명확한 오류 메시지를 제공합니다.

`openConnection()` 메서드는 실제 물리적 데이터베이스 연결을 생성하는 핵심 메서드입니다. `DriverManager.getConnection()` 정적 메서드를 호출하여 PostgreSQL JDBC 드라이버가 자동으로 로드되도록 하며, 전달된 URL, 사용자명, 비밀번호를 사용하여 데이터베이스 서버와의 연결을 수립합니다. 연결 성공 시 `Connection` 객체를 반환하며, 연결 실패 시 `SQLException`을 발생시켜 호출부에서 적절히 처리할 수 있도록 합니다. `close()` 메서드는 현재 빈 메서드로 구현되어 있으나, 향후 연결 풀링 기능을 추가할 경우 연결을 풀에 반환하는 로직을 구현할 수 있도록 확장성을 고려하여 설계되었습니다.

### 2.2.4 hw10.controller 패키지 - REST API 컨트롤러

#### MainController 클래스

`MainController` 클래스는 메인 대시보드 관련 REST API 요청을 처리하는 컨트롤러입니다. 이 클래스는 `@RestController`와 `@RequestMapping("/api/main")` 어노테이션을 사용하여 RESTful API 엔드포인트를 정의하며, Spring의 의존성 주입을 통해 `MainService` 인스턴스를 주입받습니다.

`getSummary()` 메서드는 `GET /api/main/summary` 엔드포인트를 처리하며, 대시보드 상단에 표시될 4개의 요약 카드 데이터를 조회합니다. 이 메서드는 `mainService.getSummary()`를 호출하여 활성 프로젝트 수, 탄소 감축량, 지연 배송 건수, 평균 ESG 등급을 조회하고, 조회된 데이터를 `MainDto.MainSummary` 객체로 포장하여 `ResponseEntity.ok()`로 래핑하여 반환합니다. HTTP 상태 코드 200(OK)와 함께 JSON 형식으로 응답하며, `SQLException`이 발생하면 `GlobalExceptionHandler`가 자동으로 처리하여 500 에러를 반환합니다.

#### ProjectController 클래스

`ProjectController` 클래스는 프로젝트 관련 REST API 요청을 처리하는 컨트롤러로, 프로젝트 조회, 검색, 대시보드 통계 조회 기능을 제공합니다. `getProject()` 메서드는 `GET /api/projects/{id}` 엔드포인트를 처리하며, URL 경로에서 프로젝트 ID를 추출하여 상세 정보를 조회합니다. 조회 결과가 `null`이면 `ResponseEntity.notFound().build()`를 반환하여 HTTP 404 상태 코드를 전송하며, 결과가 있으면 프로젝트 기본 정보를 JSON으로 반환합니다.

`searchProjects()` 메서드는 `GET /api/projects/search` 엔드포인트를 처리하며, 선박명 검색어와 페이징 파라미터를 받아 검색 결과를 리스트로 반환합니다. 쿼리 파라미터에서 `keyword`, `page`, `limit` 값을 추출하며, `page`와 `limit`는 기본값이 각각 0과 10으로 설정되어 있어 생략 가능합니다. 서비스 계층에서 `offset = page * limit`로 계산하여 페이징 처리를 수행하며, 검색 결과는 JSON 배열로 반환됩니다.

`getDashboardStats()` 메서드는 [기능 1] 요구사항을 구현하는 핵심 메서드로, `GET /api/projects/{id}/stats` 엔드포인트를 처리합니다. 이 메서드는 프로젝트 대시보드에 필요한 모든 통계 데이터를 수집하며, 총 발주 금액, 운송/보관/가공 단계별 탄소 배출량, 상위 3개 공급업체, 탄소 집약도 등을 포함한 `DashboardStats` 객체를 반환합니다. 각 통계는 `ProjectRepository`의 다양한 메서드를 통해 계산되며, 복잡한 SQL 쿼리를 사용하여 데이터베이스에서 직접 집계합니다.

#### OrderController 클래스

`OrderController` 클래스는 발주 관련 REST API 요청을 처리하는 컨트롤러로, [기능 2] 요구사항에 따라 발주 등록 트랜잭션 처리 기능을 제공합니다. `createOrder()` 메서드는 `POST /api/orders` 엔드포인트를 처리하며, 클라이언트로부터 JSON 형식의 발주 요청 데이터를 받아 트랜잭션을 실행합니다. 성공 시 생성된 `POID`와 `DeliveryID`를 포함한 응답을 반환하며, 실패 시 `SQLException`이 발생하여 `GlobalExceptionHandler`가 500 에러를 반환합니다.

또한 이 컨트롤러는 발주 등록 화면을 구성하기 위한 다양한 옵션 조회 API를 제공합니다. `getProjectList()`, `getSupplierList()`, `getWarehouseList()` 메서드는 각각 프로젝트, 공급업체, 창고 목록을 조회하여 드롭다운 메뉴를 채우는 데 사용됩니다. `searchParts()` 메서드는 부품 검색 기능을 제공하며, 키워드가 없으면 상위 50개 부품을 반환하고, 키워드가 있으면 `ILIKE` 연산자를 사용하여 부분 일치 검색을 수행하고 상위 20개를 반환합니다. `getWarehouseInventory()` 메서드는 특정 창고의 현재 재고 현황을 조회하여 발주 등록 시 선택한 창고의 재고를 확인하는 데 사용됩니다.

#### SupplierController 클래스

`SupplierController` 클래스는 공급업체 관련 REST API 요청을 처리하는 컨트롤러로, [기능 3] 요구사항에 따라 ESG 등급 및 지연율 필터링 기능을 제공합니다. `listSuppliers()` 메서드는 `GET /api/suppliers` 엔드포인트를 처리하며, ESG 등급 다중 선택 및 지연율 필터를 적용하여 공급업체 리포트 목록을 조회합니다. 쿼리 파라미터로 `esgGrades`(리스트), `minRatio`, `maxRatio`를 받아 동적 쿼리를 생성하며, 필터 조건에 맞는 공급업체만 반환합니다.

`getSupplier()` 메서드는 특정 공급업체의 상세 정보를 조회하며, `getSupplierOrders()` 메서드는 특정 업체의 발주 이력을 페이징하여 조회합니다. 이 메서드들은 [기능 3] 요구사항에 따라 최근 N개 발주서 목록과 각 발주서의 상태, 지연 여부를 제공합니다.

#### SettingController 클래스

`SettingController` 클래스는 시스템 설정 관련 REST API 요청을 처리하는 컨트롤러로, [기능 4] 요구사항에 따라 로그 조회 및 시스템 상태 확인 기능을 제공합니다. `getLogs()` 메서드는 `GET /api/settings/logs` 엔드포인트를 처리하며, 서버 로그 파일을 읽어 레벨 및 검색어 조건에 맞게 필터링하여 반환합니다. `getSystemStatus()` 메서드는 `GET /api/settings/status` 엔드포인트를 처리하며, DB 연결 상태, 응답 속도 등 시스템 상태를 점검합니다.

### 2.2.5 hw10.service 패키지 - 비즈니스 로직

#### MainService 클래스

`MainService` 클래스는 메인 대시보드의 비즈니스 로직을 처리하는 서비스 클래스입니다. 이 클래스는 Spring의 `DataSource`를 주입받아 데이터베이스 연결을 관리하며, 각 통계 데이터를 조회하는 private 메서드들을 포함하고 있습니다.

`getSummary()` 메서드는 대시보드 요약 정보를 조회하는 핵심 메서드로, 데이터베이스 연결을 획득한 후 네 가지 통계를 순차적으로 조회합니다. `countActiveProjects()` 메서드는 `ShipProject` 테이블에서 상태가 '인도완료'가 아닌 프로젝트 수를 카운트하며, `getTotalCarbonReduction()` 메서드는 기준값 1000에서 총 탄소 배출량을 차감하여 감축량을 계산합니다. `countDelayedDeliveries()` 메서드는 상태가 '지연'인 배송 건수를 카운트하며, `getAverageEsgGrade()` 메서드는 공급업체의 ESG 등급을 점수로 환산하여 평균을 계산한 후 다시 등급 문자로 변환합니다. 모든 통계 데이터를 조회한 후 `MainDto.MainSummary` 객체로 포장하여 반환합니다.

#### ProjectService 클래스

`ProjectService` 클래스는 프로젝트 관련 비즈니스 로직을 처리하는 서비스 클래스입니다. 이 클래스는 `ProjectRepository`를 사용하여 데이터베이스 조회 작업을 수행하며, 조회된 데이터를 DTO로 변환하여 반환합니다.

`getDashboardStats()` 메서드는 [기능 1] 요구사항을 구현하는 핵심 메서드로, 프로젝트 대시보드에 필요한 모든 통계 데이터를 수집합니다. 먼저 프로젝트 기본 정보를 조회하여 프로젝트가 존재하는지 확인하며, 존재하지 않으면 `IllegalArgumentException`을 발생시킵니다. 이어서 `ProjectRepository`의 다양한 메서드를 호출하여 총 발주 금액, 전체 탄소 배출량, 운송/보관/가공 단계별 탄소 배출량, 상위 3개 공급업체, 탄소 집약도를 조회합니다. 각 통계는 복잡한 SQL 쿼리를 사용하여 데이터베이스에서 직접 집계되며, JOIN, GROUP BY, 집계 함수 등을 활용합니다. 모든 데이터를 수집한 후 `DashboardStats` 객체로 포장하여 반환합니다.

#### OrderService 클래스

`OrderService` 클래스는 발주 관련 비즈니스 로직을 처리하는 서비스 클래스입니다. 이 클래스는 발주 생성, 옵션 조회 기능을 제공하며, 발주 생성 시 `OrderTransactionService`를 사용하여 트랜잭션을 처리합니다.

`createOrder()` 메서드는 발주 생성 요청을 처리하는 메서드로, `OrderTransactionService` 인스턴스를 생성하고 DTO 데이터를 변환하여 트랜잭션 메서드를 호출합니다. `request.lines()`를 `OrderTransactionService.OrderLineInput` 리스트로 변환하며, 발생한 예외를 `SQLException`으로 래핑하여 상위로 전파합니다. 성공 시 생성된 `POID`와 `DeliveryID`를 포함한 `OrderResponse` 객체를 반환합니다.

또한 이 서비스는 발주 등록 화면을 구성하기 위한 다양한 옵션 조회 메서드를 제공합니다. `getProjectOptions()`, `getSupplierOptions()`, `getWarehouseOptions()` 메서드는 각각 프로젝트, 공급업체, 창고 목록을 조회하며, `searchParts()` 메서드는 부품 검색 기능을 제공합니다. `getWarehouseInventory()` 메서드는 특정 창고의 현재 재고 현황을 조회하여 `Inventory` 테이블과 `Part` 테이블을 조인하여 부품명과 단위 정보를 함께 조회합니다.

#### OrderTransactionService 클래스

`OrderTransactionService` 클래스는 발주 등록 트랜잭션을 처리하는 핵심 서비스 클래스로, [기능 2] 요구사항에 따라 발주서 생성, 납품 기록 생성, 재고 반영을 하나의 트랜잭션으로 묶어 처리합니다. 이 클래스는 애플리케이션에서 가장 복잡한 비즈니스 로직을 담당하며, 트랜잭션의 원자성, 일관성, 격리성, 지속성(ACID) 속성을 보장합니다.

`createOrderWithInitialDelivery()` 메서드는 트랜잭션 로직의 핵심으로, 최대 3회 재시도 로직을 포함하고 있습니다. 먼저 데이터베이스 연결을 획득한 후 `conn.setAutoCommit(false)`를 호출하여 자동 커밋 모드를 비활성화하고 수동 트랜잭션 제어를 시작합니다. 이어서 `conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED)`를 호출하여 격리 수준을 READ_COMMITTED로 설정합니다. 이 격리 수준은 커밋된 데이터만 읽을 수 있어 Dirty Read를 방지하며, 동시성과 일관성의 균형을 맞춘 수준입니다.

트랜잭션이 시작되면 먼저 `SequenceGenerator`를 사용하여 발주서 ID(POID)를 생성합니다. 이 ID 생성기는 `SELECT COALESCE(MAX(POID), 0) + 1` 쿼리를 실행하여 다음 ID를 계산하며, 허용된 테이블.컬럼 조합만 사용할 수 있도록 보안 검증을 수행합니다. ID 생성 후 `OrderRepository.insertPurchaseOrder()` 메서드를 호출하여 `PurchaseOrder` 테이블에 발주서 레코드를 삽입합니다. 이어서 `OrderRepository.insertPurchaseOrderLine()` 메서드를 반복 호출하여 각 발주 품목을 `PurchaseOrderLine` 테이블에 삽입합니다.

발주서와 발주 품목이 모두 삽입되면 초기 납품 데이터를 생성합니다. `SequenceGenerator`를 사용하여 납품서 ID(DeliveryID)를 생성한 후 `DeliveryRepository.insertDelivery()` 메서드를 호출하여 `Delivery` 테이블에 초기 납품 레코드를 삽입합니다. 납품 상태는 '정상입고'로 설정하고, 날짜는 오늘 날짜를 사용합니다. 이어서 `DeliveryRepository.insertDeliveryLine()` 메서드를 반복 호출하여 각 납품 품목을 `DeliveryLine` 테이블에 삽입합니다. 납품 수량은 발주 수량의 50%로 자동 계산되며, 0개일 경우 1개로 보정됩니다.

납품 품목이 모두 삽입되면 창고 재고를 반영합니다. `InventoryRepository.addInventory()` 메서드를 호출하여 각 부품의 재고를 증가시키며, 이 메서드는 PostgreSQL의 `ON CONFLICT DO UPDATE` 구문을 사용하여 UPSERT를 수행합니다. 기존 재고가 있으면 수량을 더하고, 없으면 새로 삽입합니다.

모든 작업이 성공적으로 완료되면 `conn.commit()`을 호출하여 변경사항을 데이터베이스에 영구 반영합니다. 만약 어느 단계에서든 `SQLException`이 발생하면 `conn.rollback()`을 호출하여 모든 변경사항을 취소합니다. 특히 교착상태(Deadlock)가 발생한 경우, `e.getSQLState()`가 "40P01"인지 확인하고, 재시도 횟수가 남아있으면 지수 백오프(Exponential Backoff)를 적용하여 대기 후 재시도합니다. 1회차는 200ms, 2회차는 400ms, 3회차는 600ms 대기하며, 모든 재시도가 실패하면 `IllegalStateException`을 발생시킵니다.

이러한 트랜잭션 처리 방식은 [기능 2] 요구사항을 충실히 구현하며, 발주서 생성부터 재고 반영까지의 모든 과정이 하나의 트랜잭션으로 묶여 있어 어느 단계에서든 오류가 발생하면 전체 작업이 롤백됩니다. 또한 교착상태 발생 시 자동 재시도 로직을 포함하여 동시성 문제를 해결합니다.

#### SupplierService 클래스

`SupplierService` 클래스는 공급업체 관련 비즈니스 로직을 처리하는 서비스 클래스로, [기능 3] 요구사항에 따라 ESG 등급 및 지연율 필터링 기능을 제공합니다. `listSuppliers()` 메서드는 `SupplierRepository`의 복잡한 CTE(Common Table Expression) 쿼리 결과를 DTO로 변환하며, ESG 등급 다중 선택 및 지연율 필터를 지원합니다.

`getSupplierDetail()` 메서드는 공급업체 상세 정보를 조회하는 메서드로, 먼저 모든 공급업체 목록을 조회한 후 `supplierId`로 필터링하여 해당 업체를 찾습니다. 업체가 없으면 `IllegalArgumentException`을 발생시키며, 업체가 있으면 `supplierRepository.recentPurchaseOrders()` 메서드를 호출하여 최근 5개 발주 내역을 조회합니다. 기본 정보와 발주 내역을 조합하여 `SupplierDetail` 객체를 생성하여 반환합니다.

#### SettingService 클래스

`SettingService` 클래스는 시스템 설정 및 로그 관련 비즈니스 로직을 처리하는 서비스 클래스로, [기능 4] 요구사항에 따라 로그 조회 및 시스템 상태 확인 기능을 제공합니다. `getLogs()` 메서드는 `logs/app.log` 파일을 라인 단위로 읽고 정규식으로 파싱하여 구조화된 JSON 데이터로 변환합니다. 정규식 패턴을 사용하여 타임스탬프, 로그 레벨, 메시지를 추출하며, 멀티라인 로그(스택 트레이스 등)를 처리합니다. 레벨 필터와 검색어 필터를 적용하여 조건에 맞는 로그만 반환하며, 최신순으로 정렬하고 개수 제한을 적용합니다.

`getSystemStatus()` 메서드는 시스템 상태를 점검하는 메서드로, `DatabaseConfig.load()`로 설정을 로드한 후 `DatabaseConnection`을 생성하여 연결을 시도합니다. `SELECT 1` 쿼리를 실행하여 응답 시간을 측정하며, 결과를 `Map`으로 반환합니다. 이 메서드는 [기능 4] 요구사항에 따라 DB 연결 상태, 응답 속도 등을 확인합니다.

### 2.2.6 hw10.repository 패키지 - 데이터 접근 계층

#### ProjectRepository 클래스

`ProjectRepository` 클래스는 프로젝트 관련 데이터베이스 조회 작업을 수행하는 리포지토리 클래스입니다. 이 클래스는 [기능 1] 요구사항에 따라 프로젝트 정보 조회, 통계 계산, 탄소 배출량 집계 등의 기능을 제공합니다.

`findProjectById()` 메서드는 프로젝트 ID로 상세 정보를 조회하는 메서드로, `PreparedStatement`를 사용하여 파라미터화된 쿼리를 실행합니다. 결과가 없으면 `null`을 반환하며, 결과가 있으면 `ProjectBasic` 레코드를 생성하여 반환합니다. `searchProjectsByShipName()` 메서드는 선박명으로 프로젝트를 검색하는 메서드로, `ILIKE` 연산자를 사용하여 대소문자 구분 없이 부분 일치 검색을 수행합니다. 페이징 처리를 위해 `LIMIT`과 `OFFSET`을 사용하며, `ProjectID` 순으로 정렬하여 일관된 결과를 보장합니다.

`totalOrderAmount()` 메서드는 [기능 1] 요구사항에 따라 프로젝트의 총 발주 금액을 계산하는 메서드입니다. `PurchaseOrder`와 `PurchaseOrderLine` 테이블을 `POID`로 조인하며, `WHERE po.ProjectID = ?` 조건으로 특정 프로젝트의 발주만 필터링합니다. `SUM(pol.Quantity * pol.UnitPriceAtOrder)`로 각 품목의 금액(수량 × 단가)을 합산하며, `COALESCE(..., 0)`로 NULL 값을 0으로 처리합니다.

`topSuppliersByAmount()` 메서드는 [기능 1] 요구사항에 따라 공급업체별 발주 금액 상위 3개를 조회하는 메서드입니다. `PurchaseOrder`, `PurchaseOrderLine`, `Supplier` 테이블을 조인하며, `GROUP BY s.SupplierID, s.Name`으로 공급업체별로 그룹화합니다. `SUM(pol.Quantity * pol.UnitPriceAtOrder)`로 각 공급업체의 총 발주 금액을 계산하며, `ORDER BY amount DESC`로 금액 내림차순 정렬합니다. `LIMIT ?`로 상위 N개만 조회하며, 결과를 `SupplierAmount` 리스트로 변환하여 반환합니다.

`emissionSumByType()` 메서드는 [기능 1] 요구사항에 따라 운송/보관 관련 탄소 배출 합계를 계산하는 메서드입니다. `CarbonEmissionRecord` 테이블에서 배출 기록을 조회하며, `WHERE c.EmissionType = ?` 조건으로 특정 유형의 배출만 필터링합니다. 배출 기록이 프로젝트에 직접 연결된 경우(`c.ProjectID = ?`) 또는 배송을 통해 간접 연결된 경우(`c.DeliveryID IN (서브쿼리)`)를 모두 포함하며, 서브쿼리에서 `Delivery`와 `PurchaseOrder`를 조인하여 해당 프로젝트의 배송을 찾습니다. `SUM(c.CO2eAmount)`로 배출량을 합산하며, `COALESCE(..., 0)`로 NULL 값을 0으로 처리합니다.

`calculateCarbonIntensity()` 메서드는 [기능 1] 요구사항에 따라 "탄소 집약도(kg CO₂e / 백만 원)" 지표를 계산하는 메서드입니다. 먼저 `emissionSumTotal()`과 `totalOrderAmount()` 메서드를 호출하여 총 배출량과 총 발주 금액을 조회합니다. 발주 금액이 0이면 계산 불가이므로 `null`을 반환하며, 발주 금액이 있으면 `(총 배출량 / 총 발주금액) * 1,000,000` 공식으로 탄소 집약도를 계산합니다. `Math.round(intensity * 10.0) / 10.0`로 소수점 첫째 자리까지 반올림하여 반환합니다.

#### SupplierRepository 클래스

`SupplierRepository` 클래스는 공급업체 관련 데이터베이스 조회 작업을 수행하는 리포지토리 클래스로, [기능 3] 요구사항에 따라 ESG 등급 및 지연율 필터링 기능을 제공합니다. `listSuppliers()` 메서드는 복잡한 CTE(Common Table Expression) 쿼리를 사용하여 공급업체 목록을 조회하며, Java 코드에서 `StringBuilder`로 동적 쿼리를 생성하여 ESG 필터 등을 적용합니다.

CTE는 세 단계로 구성됩니다. 첫 번째 단계(`order_totals`)에서는 `PurchaseOrder`와 `PurchaseOrderLine`을 조인하여 공급사별 총 발주 금액을 계산합니다. 두 번째 단계(`delivery_stats`)에서는 `PurchaseOrder`와 `Delivery`를 조인하여 공급사별 납품 통계를 계산하며, `COUNT(*)`로 전체 납품 건수를 계산하고 `SUM(CASE WHEN d.Status = '지연' THEN 1 ELSE 0 END)`로 지연 납품 건수를 계산합니다. 세 번째 단계(`base`)에서는 `Supplier` 테이블을 기준으로 `LEFT JOIN`을 수행하며, `COALESCE`로 NULL 값을 0으로 처리합니다. 지연율은 `delayed_deliveries / total_deliveries`로 계산하며, `total_deliveries`가 0이면 지연율을 0으로 설정합니다.

동적 WHERE 절은 `StringBuilder`를 사용하여 생성하며, ESG 등급 필터, 최소 지연율 필터, 최대 지연율 필터를 조건부로 추가합니다. 파라미터는 `List<Object>`에 순서대로 추가하며, `PreparedStatement`에 동적으로 바인딩합니다. 결과는 발주 금액 내림차순, 지연율 내림차순, SupplierID 오름차순으로 정렬됩니다.

`recentPurchaseOrders()` 메서드는 특정 공급업체의 최근 발주 내역을 조회하는 메서드로, `EXISTS` 서브쿼리를 사용하여 해당 발주서에 지연 배송이 있는지 확인합니다. 지연 배송이 있으면 `delayed = TRUE`, 없으면 `FALSE`로 설정하며, `OrderDate DESC, POID DESC`로 최신순 정렬합니다. `LIMIT`과 `OFFSET`으로 페이징 처리를 수행합니다.

#### OrderRepository 클래스

`OrderRepository` 클래스는 발주서(PurchaseOrder) 및 발주 품목(PurchaseOrderLine) 데이터를 데이터베이스에 저장하는 리포지토리입니다. `insertPurchaseOrder()` 메서드는 발주 기본 정보를 `PurchaseOrder` 테이블에 삽입하며, `PreparedStatement`를 사용하여 파라미터화된 INSERT 쿼리를 실행합니다. 모든 파라미터를 순서대로 바인딩하며, 외래키 제약조건 위반 시 `SQLException`이 발생합니다. 트랜잭션 내에서 호출되므로 커밋 전까지는 임시 상태입니다.

`insertPurchaseOrderLine()` 메서드는 발주 상세 품목을 `PurchaseOrderLine` 테이블에 삽입하며, `lineNo`는 발주서 내에서 품목의 순서를 나타냅니다. `requestedDueDate`가 `null`이면 `ps.setNull(6, java.sql.Types.DATE)`로 NULL을 설정합니다. 발주서 생성 시 여러 품목을 반복 호출하여 저장합니다.

#### DeliveryRepository 클래스

`DeliveryRepository` 클래스는 납품(Delivery) 및 납품 품목(DeliveryLine) 데이터를 데이터베이스에 저장하는 리포지토리입니다. `insertDelivery()` 메서드는 초기 납품 레코드를 `Delivery` 테이블에 삽입하며, [기능 2] 요구사항에 따라 발주 직후 "초기 납품" 1건을 자동 생성합니다. `distanceKm`가 `null`이면 `ps.setNull(5, java.sql.Types.DOUBLE)`로 NULL을 설정합니다.

`insertDeliveryLine()` 메서드는 납품 상세 품목을 `DeliveryLine` 테이블에 삽입하며, `receivedQty`는 발주 수량의 50%로 자동 계산됩니다. [기능 2] 요구사항에 따라 초기 납품 수량을 자동 설정합니다.

#### InventoryRepository 클래스

`InventoryRepository` 클래스는 창고 재고(Inventory) 데이터를 관리하는 리포지토리로, UPSERT 기능을 제공합니다. `addInventory()` 메서드는 PostgreSQL의 `ON CONFLICT` 구문을 사용하여 UPSERT를 구현하며, [기능 2] 요구사항에 따라 납품 시 창고 재고를 자동으로 증가시킵니다. `(WarehouseID, PartID)` 조합이 기본키 또는 유니크 제약조건을 가정하며, 레코드가 없으면 `INSERT`로 새 레코드를 생성하고, 레코드가 있으면 `UPDATE`로 기존 `Quantity`에 `deltaQty`를 더합니다. `EXCLUDED`는 INSERT 시도한 값을 참조하는 키워드입니다. 트랜잭션 내에서 호출되므로 롤백 시 재고 변경도 함께 취소됩니다.

#### SequenceGenerator 클래스

`SequenceGenerator` 클래스는 데이터베이스 테이블의 기본키(Primary Key) 값을 자동 생성하는 유틸리티 클래스입니다. 이 클래스는 시퀀스 대신 MAX 값 기반 ID 생성을 사용하며, 허용된 테이블.컬럼 조합만 ID 생성을 허용하여 SQL 인젝션을 방지합니다.

`nextId()` 메서드는 다음 ID 번호를 생성하는 메서드로, 먼저 `ALLOWED.contains(tableDotCol)`로 허용된 컬럼인지 확인합니다. 허용되지 않은 경우 `IllegalArgumentException`을 발생시켜 SQL 인젝션 공격을 방지합니다. 이어서 `tableDotCol.split("\\.")`로 테이블명과 컬럼명을 분리하며, `SELECT COALESCE(MAX(컬럼명), 0) + 1 AS next_id FROM 테이블명` 쿼리를 동적으로 생성합니다. `COALESCE(MAX(...), 0)`로 NULL 값을 0으로 처리하며, `+ 1`로 다음 ID를 계산합니다. `PreparedStatement`를 사용하지만 테이블명과 컬럼명은 동적으로 구성되며, 허용 목록 검증으로 안전성을 보장합니다.

동시성 환경에서 두 트랜잭션이 동시에 같은 ID를 생성할 수 있으나, 트랜잭션 격리 수준(READ_COMMITTED)과 재시도 로직으로 이를 완화합니다. 프로덕션 환경에서는 시퀀스(SEQUENCE) 사용을 권장합니다.

### 2.2.7 hw10.dto 패키지 - 데이터 전송 객체

DTO(Data Transfer Object) 패키지는 계층 간 데이터 전송을 위한 불변 객체들을 포함합니다. Java 14+의 `record` 기능을 사용하여 간결하고 안전한 DTO를 구현했으며, 모든 필드가 `final`로 선언되어 불변성을 보장합니다.

`MainDto` 클래스는 메인 대시보드용 데이터 전송 객체로, `MainSummary` 레코드를 포함합니다. 이 레코드는 활성 프로젝트 수, 탄소 감축량, 지연 배송 건수, 평균 ESG 등급을 포함하며, Spring이 자동으로 JSON으로 변환하여 HTTP 응답에 포함합니다.

`OrderDto` 클래스는 발주 관련 데이터 전송 객체로, `OrderRequest`, `OrderLineInput`, `OrderResponse`, `SupplierOption`, `PartOption`, `WarehouseOption`, `InventoryItem` 레코드를 포함합니다. `OrderRequest`는 발주 등록 API의 요청 본문으로 사용되며, 프로젝트 ID, 공급업체 ID, 발주 품목 리스트, 창고 ID 등을 포함합니다. `OrderResponse`는 발주 등록 결과 응답으로, 생성된 발주서 ID와 납품서 ID를 포함합니다.

`ProjectDto` 클래스는 프로젝트 관련 데이터 전송 객체로, `ProjectBasic`, `SupplierAmount`, `ProjectSearchItem`, `DashboardStats` 레코드를 포함합니다. `DashboardStats`는 프로젝트 대시보드 통계 데이터로, [기능 1] 요구사항에 따라 총 발주 금액, 탄소 배출량(운송/보관/가공), 상위 공급업체, 탄소 집약도 등을 포함합니다.

`SupplierDto` 클래스는 공급업체 관련 데이터 전송 객체로, `SupplierRow`, `SupplierPoRow`, `SupplierDetail` 레코드를 포함합니다. `SupplierRow`는 공급업체 목록 행 데이터로, ESG 등급, 지연 납품 비율 등 통계를 포함하며, [기능 3] 요구사항에 따라 공급업체 ESG 및 지연 납품 리포트에 사용됩니다. `SupplierDetail`은 공급업체 상세 정보로, 기본 정보와 최근 발주 내역 리스트를 포함합니다.

### 2.2.8 hw10.exception 및 hw10.util 패키지

#### GlobalExceptionHandler 클래스

`GlobalExceptionHandler` 클래스는 애플리케이션 전역에서 발생하는 예외를 처리하는 예외 핸들러로, [기능 4] 요구사항에 따라 사용자에게 이해 가능한 에러 메시지를 제공합니다. 이 클래스는 `@RestControllerAdvice` 어노테이션을 사용하여 모든 컨트롤러에서 발생하는 예외를 가로채 처리합니다.

`handleIllegalArgument()` 메서드는 `IllegalArgumentException`을 처리하며, 잘못된 요청 파라미터로 인한 예외를 HTTP 400 Bad Request로 반환합니다. `handleSQLException()` 메서드는 `SQLException`을 처리하며, 데이터베이스 관련 예외를 HTTP 500 Internal Server Error로 반환합니다. 이 메서드는 `Logger.error()`로 에러 로그를 기록하며, 개발용 상세 내용(스택 트레이스)은 로그에 남깁니다. 예외 메시지에 "롤백", "Rollback", "rollback", "실패" 키워드가 포함된 경우, 메시지를 "트랜잭션 롤백 알림: [에러 메시지]"로 변경하여 사용자에게 롤백 사실을 명확히 알립니다.

`handleNoResourceFound()` 메서드는 `NoResourceFoundException`을 처리하며, 정적 리소스를 찾을 수 없는 경우를 HTTP 404 Not Found로 반환합니다. 특정 리소스 경로(파비콘, 웹 표준 리소스 등)는 로그를 남기지 않으며, 그 외의 리소스는 `Logger.warn()`으로 경고 로그를 기록합니다. `handleException()` 메서드는 위에서 처리되지 않은 모든 예외를 처리하는 fallback 메서드로, HTTP 500 Internal Server Error를 반환합니다.

#### Logger 클래스

`Logger` 클래스는 애플리케이션 로깅을 관리하는 유틸리티 클래스로, [기능 4] 요구사항에 따라 파일 로그와 콘솔 로그를 동시에 출력합니다. 이 클래스는 Java 표준 로깅 API(`java.util.logging`)를 사용하며, 커스텀 포맷터를 구현하여 로그 포맷을 정의합니다.

`init()` 메서드는 로거를 초기화하는 메서드로, 중복 초기화를 방지하기 위해 `initialized` 플래그를 사용합니다. 로거 설정에서 `LOGGER.setUseParentHandlers(false)`를 호출하여 부모 핸들러를 비활성화하며, `LOGGER.setLevel(Level.INFO)`를 호출하여 로그 레벨을 INFO로 설정합니다. 커스텀 포맷터는 `[YYYY-MM-DD HH:MM:SS] [LEVEL] 메시지` 형식으로 로그를 포맷하며, 예외가 있으면 예외 정보와 스택 트레이스 첫 줄을 추가합니다.

콘솔 핸들러는 `ConsoleHandler`를 생성하고 INFO 레벨로 설정하며, 커스텀 포맷터를 적용합니다. 파일 핸들러는 `Files.createDirectories(Path.of("logs"))`로 로그 디렉토리를 생성하며, `FileHandler("logs/app.log", true)`를 생성합니다. `true` 파라미터는 이어쓰기 모드를 의미하며, 기존 파일에 추가합니다. `IOException` 발생 시 경고 로그만 기록하고 계속 진행합니다.

`info()`, `warn()`, `error()` 메서드는 각각 INFO, WARNING, SEVERE 레벨 로그를 기록하며, 콘솔과 파일에 동시에 출력됩니다. [기능 4] 요구사항에 따라 애플리케이션 시작/종료, DB 접속, 트랜잭션, 오류를 로그로 기록합니다.

## 2.3 클래스 간 관계 및 데이터 흐름

애플리케이션의 클래스 간 관계는 계층형 아키텍처를 따르며, 각 계층은 명확한 책임을 가집니다. Controller 계층은 HTTP 요청을 받아 Service 계층을 호출하고 JSON 응답을 반환하며, Service 계층은 비즈니스 로직을 처리하고 트랜잭션을 관리하며 Repository 계층을 호출합니다. Repository 계층은 데이터베이스 쿼리를 실행하고 결과를 도메인 객체로 변환하며, DTO 계층은 계층 간 데이터 전송을 위한 불변 객체입니다.

Spring Framework의 의존성 주입(Dependency Injection)을 사용하여 느슨한 결합을 구현했으며, 모든 Controller는 생성자 기반 주입을 통해 Service 인스턴스를 받습니다. 모든 Service는 생성자 기반 주입을 통해 DataSource 인스턴스를 받으며, Repository는 Service에서 직접 인스턴스화됩니다.

[기능 2] 발주 등록 트랜잭션의 실행 흐름은 다음과 같습니다. `OrderController.createOrder()`가 HTTP 요청을 받아 `OrderService.createOrder()`를 호출하며, 이는 `OrderTransactionService.createOrderWithInitialDelivery()`를 호출합니다. 트랜잭션이 시작되면 `SequenceGenerator`로 발주서 ID를 생성하고, `OrderRepository`로 발주서와 발주 품목을 삽입합니다. 이어서 `DeliveryRepository`로 납품서와 납품 품목을 삽입하며, `InventoryRepository`로 재고를 반영합니다. 모든 작업이 성공하면 커밋하며, 실패하면 롤백합니다.

모든 예외는 `GlobalExceptionHandler`에서 처리되며, 예외 발생 시 `Logger.error()`로 로그를 기록하고 사용자 친화적 메시지를 생성하여 `ResponseEntity`로 반환합니다.

## 2.4 주요 설계 패턴 및 원칙

애플리케이션은 여러 설계 패턴과 원칙을 준수하여 구현되었습니다. Repository 패턴을 사용하여 데이터 접근 로직을 캡슐화하여 비즈니스 로직과 분리했으며, DTO 패턴을 사용하여 계층 간 데이터 전송을 위한 전용 객체를 사용하여 도메인 모델을 보호했습니다. 의존성 주입 패턴을 사용하여 Spring Framework를 통해 의존성을 외부에서 주입하여 테스트 용이성을 높였으며, 불변 객체 패턴을 사용하여 `record` 클래스를 사용하여 DTO의 불변성을 보장했습니다.

SOLID 원칙 중 단일 책임 원칙(SRP)을 준수하여 각 클래스는 하나의 책임만 가지며, 의존성 역전 원칙(DIP)을 준수하여 고수준 모듈(Service)이 저수준 모듈(Repository)에 의존하지 않고 추상화에 의존합니다. 개방-폐쇄 원칙(OCP)을 준수하여 확장에는 열려있고 수정에는 닫혀있도록 인터페이스를 활용할 수 있는 구조입니다.

## 2.5 요구사항 구현 현황

[기능 1] 프로젝트 대시보드 및 탄소·비용 리포트는 완전히 구현되었습니다. 프로젝트 기본 정보 조회, 총 발주 금액 계산, 공급업체별 발주 금액 상위 3개 조회, 운송/보관 관련 탄소 배출 합계 계산, 프로젝트 전체 탄소 배출 합계 계산, 탄소 집약도 계산이 모두 구현되었습니다.

[기능 2] 발주 및 납품 관리 + 트랜잭션 처리도 완전히 구현되었습니다. 발주서 생성, 발주 품목 등록, 초기 납품 자동 생성, 납품 품목 등록, 창고 재고 반영이 모두 하나의 트랜잭션으로 묶여 처리되며, 롤백 처리 및 재시도 로직이 구현되었습니다.

[기능 3] 공급업체 ESG 및 지연 납품 리포트도 완전히 구현되었습니다. 공급업체 목록 조회, ESG 등급 필터링(다중 선택 지원), 지연 납품 비율 필터링(상한/하한 지원), 공급업체 상세 정보 조회, 최근 N개 발주서 목록 조회가 모두 구현되었습니다.

[기능 4] 예외 처리·로그·환경 설정도 완전히 구현되었습니다. 전역 예외 처리, 사용자 친화적 에러 메시지 제공, 로그 시스템(파일 로그 및 콘솔 로그), 환경 설정 분리(환경변수, application.properties, config.properties 지원)가 모두 구현되었습니다.

## 2.6 결론

본 애플리케이션은 계층형 아키텍처를 기반으로 하여 각 계층의 책임을 명확히 분리하고, Spring Framework의 의존성 주입을 활용하여 유연하고 테스트 가능한 구조를 구현했습니다. 트랜잭션 처리에서는 [기능 2] 요구사항을 충실히 구현하여 발주서 생성부터 재고 반영까지의 모든 과정을 하나의 트랜잭션으로 묶고, 교착상태 발생 시 자동 재시도 로직을 포함하여 동시성 문제를 해결했습니다. 예외 처리 및 로깅에서는 [기능 4] 요구사항에 따라 사용자에게 이해 가능한 메시지를 제공하면서도 개발자를 위한 상세한 로그를 기록하여 디버깅을 용이하게 했습니다. 모든 클래스와 메서드는 명확한 주석과 문서화를 통해 코드의 가독성과 유지보수성을 높였으며, Java의 최신 기능(record, try-with-resources 등)을 적극 활용하여 간결하고 안전한 코드를 작성했습니다.

