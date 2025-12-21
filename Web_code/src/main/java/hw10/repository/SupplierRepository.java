package hw10.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 공급업체 정보 조회 및 통계 리포지토리.
 */
public final class SupplierRepository {

    // 공급업체 목록용 레코드. 지연율 통계 포함.
    public record SupplierRow(
            int supplierId,
            String name,
            String country,
            String esgGrade,
            double totalOrderAmount,
            int delayedDeliveries,
            int totalDeliveries,
            double delayRatio) {
    }

    // 공급업체 상세용 최근 발주 기록.
    public record SupplierPoRow(
            int poid,
            java.sql.Date orderDate,
            String status,
            boolean delayed) {
    }

    /**
     * 공급업체 목록 조회.
     * ESG 등급, 지연율 필터링 지원.
     * CTE(Common Table Expressions) 사용 통계 계산.
     */
    public List<SupplierRow> listSuppliers(Connection conn,
            List<String> esgGrades,
            Double minRatio,
            Double maxRatio) throws SQLException {

        StringBuilder sb = new StringBuilder();
        // 1. 업체별 총 발주 금액 계산 (order_totals)
        // 2. 업체별 배송 건수 및 지연 건수 계산 (delivery_stats)
        // 3. 업체 정보 및 통계 병합 (base)
        sb.append("""
                WITH order_totals AS (
                  SELECT po.SupplierID, SUM(pol.Quantity * pol.UnitPriceAtOrder) AS total_amount
                  FROM PurchaseOrder po
                  JOIN PurchaseOrderLine pol ON pol.POID = po.POID
                  GROUP BY po.SupplierID
                ),
                delivery_stats AS (
                  SELECT po.SupplierID,
                         COUNT(*) AS total_deliveries,
                         SUM(CASE WHEN d.Status = '지연' THEN 1 ELSE 0 END) AS delayed_deliveries
                  FROM PurchaseOrder po
                  JOIN Delivery d ON d.POID = po.POID
                  GROUP BY po.SupplierID
                ),
                base AS (
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
                SELECT *
                FROM base
                WHERE 1=1
                """);

        // 파라미터 리스트.
        List<Object> params = new ArrayList<>();

        // ESG 등급 필터 적용.
        if (esgGrades != null && !esgGrades.isEmpty()) {
            sb.append(" AND ESGGrade IN (");
            for (int i = 0; i < esgGrades.size(); i++) {
                if (i > 0)
                    sb.append(", ");
                sb.append("?");
                params.add(esgGrades.get(i));
            }
            sb.append(")");
        }

        // 최소 지연율 필터 적용.
        if (minRatio != null) {
            sb.append(" AND delay_ratio >= ?");
            params.add(minRatio);
        }

        // 최대 지연율 필터 적용.
        if (maxRatio != null) {
            sb.append(" AND delay_ratio <= ?");
            params.add(maxRatio);
        }

        // 발주 금액 내림차순, 지연율 내림차순 정렬.
        sb.append(" ORDER BY total_order_amount DESC, delay_ratio DESC, SupplierID");

        try (PreparedStatement ps = conn.prepareStatement(sb.toString())) {
            // 파라미터 바인딩
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                List<SupplierRow> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new SupplierRow(
                            rs.getInt("SupplierID"),
                            rs.getString("Name"),
                            rs.getString("Country"),
                            rs.getString("ESGGrade"),
                            rs.getDouble("total_order_amount"),
                            rs.getInt("delayed_deliveries"),
                            rs.getInt("total_deliveries"),
                            rs.getDouble("delay_ratio")));
                }
                return out;
            }
        }
    }

    /**
     * 특정 공급업체의 최근 발주 내역 조회.
     * 배송 지연 여부 포함.
     */
    public List<SupplierPoRow> recentPurchaseOrders(Connection conn, int supplierId,
            int limit, int offset) throws SQLException {
        String sql = """
                SELECT po.POID, po.OrderDate, po.Status,
                       CASE WHEN EXISTS (
                           SELECT 1 FROM Delivery d
                           WHERE d.POID = po.POID AND d.Status = '지연'
                       ) THEN TRUE ELSE FALSE END AS delayed
                FROM PurchaseOrder po
                WHERE po.SupplierID = ?
                ORDER BY po.OrderDate DESC, po.POID DESC
                LIMIT ? OFFSET ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, supplierId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);

            try (ResultSet rs = ps.executeQuery()) {
                List<SupplierPoRow> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new SupplierPoRow(
                            rs.getInt("POID"),
                            rs.getDate("OrderDate"),
                            rs.getString("Status"),
                            rs.getBoolean("delayed")));
                }
                return out;
            }
        }
    }
}
