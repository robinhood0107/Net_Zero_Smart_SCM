package hw10.service;

import hw10.dao.DeliveryRepository;
import hw10.dao.SequenceGenerator;
import hw10.dao.InventoryRepository;
import hw10.dao.OrderRepository;
import hw10.db.DatabaseConnection;
import hw10.util.Logger;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * [기능 2] 발주 등록 처리 - 트랜잭션 서비스
 * 
 * Service 레이어: 비즈니스 로직 담당
 * DAO는 단순 DB 접근만, Service에서 여러 DAO 조합해서 트랜잭션 처리
 * 
 * [과제 요구사항]
 * - PurchaseOrder + PurchaseOrderLine + Delivery + DeliveryLine + Inventory
 *   이 모든 작업을 하나의 트랜잭션으로 묶음
 * - 중간 오류 발생 시 전체 ROLLBACK
 * - 교착상태(Deadlock) 발생 시 재시도
 * - 트랜잭션 isolation level 설정
 */
public final class OrderTransactionService {
    
    /** 발주 항목 입력 데이터 (record = 불변 데이터 클래스) */
    public record OrderLineInput(int partId, int quantity, double unitPrice) {}
    
    /** 트랜잭션 결과 (생성된 ID들 반환) */
    public record TransactionResult(int poid, int deliveryId) {}

    private final DatabaseConnection db;
    
    // DAO 객체들 (각각 DB 테이블 접근 담당)
    private final SequenceGenerator sequenceGenerator = new SequenceGenerator();
    private final OrderRepository orderRepository = new OrderRepository();
    private final DeliveryRepository deliveryRepository = new DeliveryRepository();
    private final InventoryRepository inventoryRepository = new InventoryRepository();

    public OrderTransactionService(DatabaseConnection db) {
        this.db = db;
    }

    /**
     * 발주 + 초기 납품 + 재고 반영을 하나의 트랜잭션으로 처리
     * 
     * @param projectId 프로젝트 ID
     * @param supplierId 공급업체 ID
     * @param engineerName 담당 엔지니어
     * @param lines 발주 항목 리스트
     * @param warehouseId 입고할 창고 ID
     * @param transportMode 운송 수단
     * @param distanceKm 운송 거리
     * @return 생성된 POID와 DeliveryID
     * @throws Exception 트랜잭션 실패 시
     */
    public TransactionResult createOrderWithInitialDelivery(
            int projectId,
            int supplierId,
            String engineerName,
            List<OrderLineInput> lines,
            int warehouseId,
            String transportMode,
            Double distanceKm
    ) throws Exception {
        
        // [과제 요구사항] 교착상태 발생 시 재시도
        int maxRetries = 3;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try (Connection conn = db.openConnection()) {
                
                // [과제 요구사항] 명시적 트랜잭션 시작
                // setAutoCommit(false) = 자동 커밋 끔 = 트랜잭션 시작
                conn.setAutoCommit(false);
                
                // [과제 요구사항] 트랜잭션 isolation level 설정
                // READ_COMMITTED: 커밋된 데이터만 읽음 (PostgreSQL 기본값)
                conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                
                // [과제 요구사항] 트랜잭션 시작 로그
                Logger.info("트랜잭션 시작(attempt=" + attempt + ")");

                try {
                    // ========== 1. 발주서 생성 ==========
                    int poid = sequenceGenerator.nextId(conn, "PurchaseOrder.POID");
                    Date today = Date.valueOf(LocalDate.now());  // 오늘 날짜

                    orderRepository.insertPurchaseOrder(conn, poid, today, "요청", engineerName, projectId, supplierId);

                    // ========== 2. 발주 항목 생성 ==========
                    int lineNo = 1;
                    for (OrderLineInput li : lines) {
                        orderRepository.insertPurchaseOrderLine(conn, poid, lineNo, li.partId(), li.quantity(), li.unitPrice(), null);
                        lineNo++;
                    }

                    // ========== 3. 초기 납품 생성 ==========
                    int deliveryId = sequenceGenerator.nextId(conn, "Delivery.DeliveryID");
                    deliveryRepository.insertDelivery(conn, deliveryId, poid, today, transportMode, distanceKm, "정상입고");

                    // ========== 4. 납품 상세 + 재고 반영 ==========
                    // [과제 요구사항] 발주 수량의 50% 입고
                    lineNo = 1;
                    for (OrderLineInput li : lines) {
                        int received = (li.quantity() + 1) / 2;  // 50% 올림 (5개면 3개)
                        deliveryRepository.insertDeliveryLine(conn, deliveryId, poid, lineNo, received, "초기납품(자동)");
                        
                        // [과제 요구사항] Inventory UPSERT
                        inventoryRepository.addInventory(conn, warehouseId, li.partId(), received);
                        lineNo++;
                    }

                    // ========== 5. 커밋 ==========
                    // [과제 요구사항] 정상 완료 시 커밋
                    conn.commit();
                    
                    // [과제 요구사항] 트랜잭션 커밋 로그
                    Logger.info("트랜잭션 커밋(po=" + poid + ", delivery=" + deliveryId + ")");
                    return new TransactionResult(poid, deliveryId);
                    
                } catch (SQLException e) {
                    // ========== 롤백 ==========
                    // [과제 요구사항] 중간 오류 발생 시 전체 롤백
                    try {
                        conn.rollback();
                        // [과제 요구사항] 트랜잭션 롤백 로그
                        Logger.info("트랜잭션 롤백");
                    } catch (SQLException re) {
                        Logger.error("롤백 실패", re);
                    }

                    // [과제 요구사항] 교착상태(Deadlock) 발생 시 재시도
                    // 40P01 = PostgreSQL deadlock_detected
                    if ("40P01".equals(e.getSQLState()) && attempt < maxRetries) {
                        Logger.warn("교착상태 감지(40P01) -> 재시도: " + e.getMessage());
                        // 잠깐 대기 후 재시도 (attempt마다 대기 시간 증가)
                        try { Thread.sleep(200L * attempt); } catch (InterruptedException ignored) {}
                        continue;  // 다음 attempt로
                    }
                    
                    // 재시도 안 하는 에러거나 재시도 횟수 초과
                    throw e;
                }
            }
        }
        
        // 여기까지 오면 재시도 다 실패한 것
        throw new IllegalStateException("트랜잭션 재시도 초과");
    }
}
