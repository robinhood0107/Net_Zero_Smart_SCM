package hw10.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 발주서/발주항목 INSERT (기능 2용)
 * 
 * PurchaseOrder: 발주서 헤더
 * PurchaseOrderLine: 발주서에 포함된 각 품목
 */
public final class OrderRepository {

    /**
     * 발주서(PurchaseOrder) INSERT
     * 
     * [과제 요구사항]
     * - PurchaseOrder에 발주서 1건 삽입
     * - 상태는 '요청' 또는 '발주완료' 사용
     * 
     * @param conn DB 커넥션
     * @param poid 발주서 ID (미리 생성해서 넘김)
     * @param orderDate 발주일
     * @param status 상태 ('요청', '발주완료' 등)
     * @param engineerName 담당 엔지니어
     * @param projectId 프로젝트 ID (FK)
     * @param supplierId 공급업체 ID (FK)
     */
    public void insertPurchaseOrder(Connection conn, int poid, Date orderDate, String status,
                                    String engineerName, int projectId, int supplierId) throws SQLException {
        String sql = """
                INSERT INTO PurchaseOrder(POID, OrderDate, Status, EngineerName, ProjectID, SupplierID)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, poid);
            ps.setDate(2, orderDate);
            ps.setString(3, status);
            ps.setString(4, engineerName);
            ps.setInt(5, projectId);
            ps.setInt(6, supplierId);
            ps.executeUpdate();  // INSERT 실행
        }
    }

    /**
     * 발주항목(PurchaseOrderLine) INSERT
     * 
     * [과제 요구사항] PurchaseOrderLine에 각 발주 항목 삽입
     * 
     * @param conn DB 커넥션
     * @param poid 발주서 ID (FK)
     * @param lineNo 라인 번호 (1, 2, 3, ...)
     * @param partId 부품 ID (FK)
     * @param qty 수량
     * @param unitPrice 단가
     * @param requestedDueDate 요청 납기일 (null 가능)
     */
    public void insertPurchaseOrderLine(Connection conn, int poid, int lineNo, int partId,
                                        int qty, double unitPrice, Date requestedDueDate) throws SQLException {
        String sql = """
                INSERT INTO PurchaseOrderLine(POID, LineNo, PartID, Quantity, UnitPriceAtOrder, RequestedDueDate)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, poid);
            ps.setInt(2, lineNo);
            ps.setInt(3, partId);
            ps.setInt(4, qty);
            ps.setDouble(5, unitPrice);
            ps.setDate(6, requestedDueDate);  // null이면 NULL 들어감
            ps.executeUpdate();
        }
    }
}
