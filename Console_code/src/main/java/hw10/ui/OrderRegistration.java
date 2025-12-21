package hw10.ui;

import hw10.db.DatabaseConnection;
import hw10.service.OrderTransactionService;
import hw10.util.ErrorHandler;
import hw10.util.InputHelper;
import hw10.util.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * [기능 2] 발주 및 납품 관리 + 트랜잭션 처리
 * 
 * [과제 요구사항]
 * 1. 신규 발주 등록
 *    - 입력: ProjectID, SupplierID, 발주항목(PartID, Quantity, UnitPrice) 목록
 *    - PurchaseOrder INSERT, PurchaseOrderLine INSERT
 *    - 상태는 '요청' 사용
 * 
 * 2. 초기 납품 및 재고 반영
 *    - Delivery INSERT (오늘 날짜, status='정상입고')
 *    - DeliveryLine INSERT (발주 수량의 50%)
 *    - 창고ID 입력받아서 Inventory UPSERT
 * 
 * 3. 트랜잭션 요구사항
 *    - 전 과정을 하나의 트랜잭션으로 묶음
 *    - 오류 시 롤백 + "발주 등록 실패" 메시지
 *    - 성공 시 커밋 + "발주 등록 완료" 메시지
 *    - 명시적 BEGIN/COMMIT/ROLLBACK 사용
 */
public final class OrderRegistration {
    
    private OrderRegistration() {}

    /**
     * 기능2 메인 실행 메서드
     */
    public static void run(DatabaseConnection db, Scanner sc) throws Exception {
        System.out.println();
        System.out.println("========== 발주 등록(트랜잭션) ==========");

        // ========== 1. 사용자 입력 받기 ==========
        int projectId = InputHelper.readInt(sc, "ProjectID> ");
        int supplierId = InputHelper.readInt(sc, "SupplierID> ");
        String engineer = InputHelper.readLine(sc, "담당 엔지니어명(Enter 가능)> ");

        // 발주 항목 개수
        int count = InputHelper.readInt(sc, "발주 항목 개수> ");
        if (count <= 0) {
            System.out.println("[안내] 항목 개수는 1 이상이어야 합니다.");
            return;
        }

        // 각 발주 항목 입력받기
        List<OrderTransactionService.OrderLineInput> lines = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            System.out.println("- 항목 " + i);
            int partId = InputHelper.readInt(sc, "  PartID> ");
            int qty = InputHelper.readInt(sc, "  Quantity> ");
            if (qty <= 0) {
                System.out.println("[안내] 수량은 1 이상이어야 합니다.");
                return;
            }
            Double price = InputHelper.readDoubleOptional(sc, "  UnitPriceAtOrder(숫자)> ");
            if (price == null || price < 0) {
                System.out.println("[안내] 단가는 0 이상 숫자여야 합니다.");
                return;
            }
            // record는 Java 16+에서 추가된 불변 데이터 클래스 (cpp의 struct 비슷)
            lines.add(new OrderTransactionService.OrderLineInput(partId, qty, price));
        }

        // 창고 ID (재고 반영용)
        int warehouseId = InputHelper.readInt(sc, "입고할 WarehouseID> ");

        // 운송 정보 (Delivery에 들어감)
        String transportMode = InputHelper.readLine(sc, "운송 수단(Enter 시 '트럭')> ");
        if (transportMode.isEmpty()) transportMode = "트럭";
        Double distanceKm = InputHelper.readDoubleOptional(sc, "운송 거리(km, Enter 가능)> ");
        if (distanceKm != null && distanceKm < 0) {
            System.out.println("[안내] 운송 거리는 0 이상이어야 합니다.");
            return;
        }

        // ========== 2. 트랜잭션 실행 ==========
        OrderTransactionService service = new OrderTransactionService(db);
        try {
            // 트랜잭션으로 발주 + 납품 + 재고 전부 처리
            OrderTransactionService.TransactionResult result = service.createOrderWithInitialDelivery(
                    projectId, supplierId, engineer, lines, warehouseId, transportMode, distanceKm
            );
            
            // [과제 요구사항] 성공 시 "발주 등록 완료" 메시지
            System.out.println("[완료] 발주 등록 완료");
            System.out.println("- 생성된 POID: " + result.poid());
            System.out.println("- 생성된 DeliveryID(초기 납품): " + result.deliveryId());
            
        } catch (SQLException e) {
            // [과제 요구사항] 실패 시 "발주 등록 실패" + 롤백
            Logger.error("발주 등록 실패(SQL)", e);
            System.out.println("[실패] 발주 등록 실패 (전체 작업 롤백됨)");
            System.out.println("       " + ErrorHandler.toUserMessage(e));
        } catch (Exception e) {
            Logger.error("발주 등록 실패", e);
            System.out.println("[실패] 발주 등록 실패 (전체 작업 롤백됨)");
            System.out.println("       입력값/외래키/제약조건/DB 상태를 확인하세요.");
        }
    }
}
