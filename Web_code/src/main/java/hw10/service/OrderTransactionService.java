package hw10.service;

import hw10.repository.DeliveryRepository;
import hw10.repository.SequenceGenerator;
import hw10.repository.InventoryRepository;
import hw10.repository.OrderRepository;
import hw10.util.Logger;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;

import java.util.List;

/**
 * 발주 등록 트랜잭션 서비스.
 * 발주서 생성, 상세 품목 추가, 초기 납품 기록 일괄 처리.
 * 오류 발생 시 전체 롤백.
 */
@Service
public class OrderTransactionService {

    public record OrderLineInput(int partId, int quantity, double unitPrice) {
    }

    public record TransactionResult(int poid, int deliveryId) {
    }

    private final DataSource dataSource;

    private final SequenceGenerator sequenceGenerator = new SequenceGenerator();
    private final OrderRepository orderRepository = new OrderRepository();
    private final DeliveryRepository deliveryRepository = new DeliveryRepository();
    private final InventoryRepository inventoryRepository = new InventoryRepository();

    public OrderTransactionService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public TransactionResult createOrderWithInitialDelivery(
            int projectId,
            int supplierId,
            String engineerName,
            String status,
            List<OrderLineInput> lines,
            int warehouseId,
            String transportMode,
            Double distanceKm) throws Exception {

        // 교착상태(Deadlock) 발생 시 최대 3회 재시도.
        int maxRetries = 3;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try (Connection conn = dataSource.getConnection()) {

                // 수동 커밋 모드 전환. 트랜잭션 시작.
                conn.setAutoCommit(false);

                // 격리 수준 READ_COMMITTED 설정. 커밋된 데이터만 읽기.
                conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

                Logger.info("트랜잭션 시작(attempt=" + attempt + ")");

                try {

                    int poid = sequenceGenerator.nextId(conn, "PurchaseOrder.POID");
                    Date today = new Date(System.currentTimeMillis());
                    // 상태 값 미존재 시 기본값 '요청' 설정.
                    String orderStatus = (status != null && !status.trim().isEmpty()) ? status : "요청";
                    // 발주서 메인 정보 저장.
                    orderRepository.insertPurchaseOrder(conn, poid, today, orderStatus, engineerName, projectId,
                            supplierId);

                    int lineNo = 1;
                    for (OrderLineInput li : lines) {
                        // 발주 상세 품목 저장.
                        orderRepository.insertPurchaseOrderLine(conn, poid, lineNo, li.partId(), li.quantity(),
                                li.unitPrice(), null);
                        lineNo++;
                    }

                    // 초기 납품 데이터 생성. 금일 날짜, '정상입고'.
                    int deliveryId = sequenceGenerator.nextId(conn, "Delivery.DeliveryID");
                    deliveryRepository.insertDelivery(conn, deliveryId, poid, today, transportMode, distanceKm, "정상입고");

                    lineNo = 1;
                    for (OrderLineInput li : lines) {

                        // 초기 수량 발주량의 50% 설정.
                        int received = (int) Math.round(li.quantity() * 0.5);

                        // 0개일 경우 1개로 처리.
                        if (received == 0 && li.quantity() > 0) {
                            received = 1;
                        }
                        deliveryRepository.insertDeliveryLine(conn, deliveryId, poid, lineNo, received, "초기납품(자동)");

                        // 창고 재고 업데이트. 미존재 시 삽입, 존재 시 수량 추가.
                        inventoryRepository.addInventory(conn, warehouseId, li.partId(), received);
                        lineNo++;
                    }

                    // DB 반영 (커밋).
                    conn.commit();

                    Logger.info("트랜잭션 커밋 완료 - 발주서 ID: " + poid + ", 납품서 ID: " + deliveryId);
                    return new TransactionResult(poid, deliveryId);

                } catch (SQLException e) {

                    try {
                        // 오류 발생 시 롤백 처리.
                        conn.rollback();
                        Logger.warn("트랜잭션 롤백 - 오류 발생: " + e.getMessage());
                    } catch (SQLException re) {
                        Logger.error("롤백 실패", re);
                        throw new SQLException("롤백 처리 중 오류 발생", re);
                    }

                    // 데드락(40P01) 발생 시 재시도.
                    if ("40P01".equals(e.getSQLState()) && attempt < maxRetries) {
                        Logger.warn("교착상태 감지(40P01) -> 재시도: " + e.getMessage());
                        // 대기 후 재시도.
                        try {
                            Thread.sleep(200L * attempt);
                        } catch (InterruptedException ignored) {
                        }
                        continue;
                    }

                    throw e;
                }
            }
        }

        throw new IllegalStateException("트랜잭션 재시도 초과");
    }
}
