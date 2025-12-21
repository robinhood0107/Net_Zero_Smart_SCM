package hw10.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

/**
 * ID 자동 생성 유틸리티 리포지토리.
 * 테이블별 ID 조회 및 생성.
 */
public final class SequenceGenerator {

    // ID 생성 가능 테이블.컬럼 목록.
    private static final Set<String> ALLOWED = Set.of(
            "PurchaseOrder.POID",
            "Delivery.DeliveryID",
            "CarbonEmissionRecord.RecordID");

    /**
     * 다음 ID 번호 채번.
     * tableDotCol: "테이블명.컬럼명" 형식 필수.
     */
    public int nextId(Connection conn, String tableDotCol) throws SQLException {

        // 허용된 컬럼 확인 (SQL 인젝션 방지).
        if (!ALLOWED.contains(tableDotCol)) {
            throw new IllegalArgumentException("허용되지 않은 ID 컬럼: " + tableDotCol);
        }

        // 테이블 및 컬럼 분리.
        String[] parts = tableDotCol.split("\\.");
        String table = parts[0];
        String col = parts[1];

        // 최대값 + 1 조회 쿼리. 미존재 시 1.
        String sql = "SELECT COALESCE(MAX(" + col + "), 0) + 1 AS next_id FROM " + table;

        try (PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt("next_id");
        }
    }
}
