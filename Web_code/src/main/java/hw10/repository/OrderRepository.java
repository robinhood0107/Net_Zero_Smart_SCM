package hw10.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 발주(PurchaseOrder) 정보 저장 리포지토리.
 */
public final class OrderRepository {

    /**
     * 발주 기본 정보(PurchaseOrder) DB 저장.
     * POID, 날짜, 상태, 담당자, 프로젝트ID, 공급사ID 포함.
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
            ps.executeUpdate();
        }
    }

    /**
     * 발주 상세 품목(PurchaseOrderLine) 저장.
     * 부품, 수량, 단가, 납기일 정보 포함.
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
            ps.setDate(6, requestedDueDate);
            ps.executeUpdate();
        }
    }
}
