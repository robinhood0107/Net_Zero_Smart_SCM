package hw10.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 공급업체 리포트 SQL 모음 (기능 3용)
 * 
 * [과제 요구사항]
 * - 공급업체 목록 (ESG, 지연비율 포함)
 * - 필터링 (ESG 등급, 지연비율 범위)
 * - 상세 (최근 발주서 + 페이징)
 */
public final class SupplierRepository {

    /** 공급업체 목록 행 (지연 비율 계산 포함) */
    public record SupplierRow(
            int supplierId,
            String name,
            String country,
            String esgGrade,
            double totalOrderAmount,
            int delayedDeliveries,   // 지연된 납품 건수
            int totalDeliveries,     // 전체 납품 건수
            double delayRatio        // 지연 비율 (0~1)
    ) {}

    /** 발주서 상세 행 */
    public record SupplierPoRow(
            int poid,
            java.sql.Date orderDate,
            String status,
            boolean delayed  // 지연 여부
    ) {}

    /**
     * 공급업체 목록 조회 (필터 적용 가능)
     * 
     * [과제 요구사항]
     * - 지연 납품 비율 = Delivery.status='지연' 기준
     * - ESG 등급 다중 선택 필터
     * - 지연 비율 상한/하한 필터
     * 
     * WITH CTE 사용해서 복잡한 쿼리 구조화함
     * CTE = Common Table Expression (임시 테이블 같은 거)
     * 
     * @param conn DB 커넥션
     * @param esgGrades ESG 등급 필터 (null이면 전체)
     * @param minRatio 지연비율 하한 (null이면 제한 없음)
     * @param maxRatio 지연비율 상한 (null이면 제한 없음)
     * @return 공급업체 리스트
     */
    public List<SupplierRow> listSuppliers(Connection conn,
                                          List<String> esgGrades,
                                          Double minRatio,
                                          Double maxRatio) throws SQLException {
        // StringBuilder: 동적으로 SQL 문자열 만들 때 사용
        // cpp의 stringstream이랑 비슷
        StringBuilder sb = new StringBuilder();
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
        // WHERE 1=1 트릭: 이후에 AND 조건 붙이기 편하려고

        // 동적으로 바인딩할 파라미터들
        List<Object> params = new ArrayList<>();

        // ESG 등급 필터 추가
        if (esgGrades != null && !esgGrades.isEmpty()) {
            sb.append(" AND ESGGrade IN (");
            for (int i = 0; i < esgGrades.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append("?");  // 플레이스홀더
                params.add(esgGrades.get(i));
            }
            sb.append(")");
        }
        
        // 지연비율 하한 필터
        if (minRatio != null) {
            sb.append(" AND delay_ratio >= ?");
            params.add(minRatio);
        }
        
        // 지연비율 상한 필터
        if (maxRatio != null) {
            sb.append(" AND delay_ratio <= ?");
            params.add(maxRatio);
        }

        // 정렬: 발주금액 내림차순, 지연비율 내림차순
        sb.append(" ORDER BY total_order_amount DESC, delay_ratio DESC, SupplierID");

        try (PreparedStatement ps = conn.prepareStatement(sb.toString())) {
            // 파라미터 바인딩
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));  // JDBC는 1-indexed
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
                            rs.getDouble("delay_ratio")
                    ));
                }
                return out;
            }
        }
    }

    /**
     * 특정 공급업체의 최근 발주서 조회 (페이징)
     * 
     * [과제 요구사항] 최근 N개 발주서 + 지연 여부 요약
     * 
     * @param conn DB 커넥션
     * @param supplierId 공급업체 ID
     * @param limit 한 페이지 크기
     * @param offset 건너뛸 개수 (페이지 * 페이지크기)
     * @return 발주서 리스트
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
        // EXISTS 서브쿼리: 해당 발주서에 지연 납품이 있는지 체크
        
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
                            rs.getBoolean("delayed")
                    ));
                }
                return out;
            }
        }
    }
}
