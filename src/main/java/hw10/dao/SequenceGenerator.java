package hw10.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

/**
 * PK 생성 도우미
 * 
 * 원래 PostgreSQL에서는 SERIAL이나 SEQUENCE 써서 자동 생성하는데,
 * 우리 스키마에는 그게 없어서 MAX(id)+1 방식으로 수동 생성.
 * 
 * 주의: 동시에 여러 사용자가 INSERT하면 충돌 가능
 * 과제 데모 용도로만 쓰기 (실무에선 SEQUENCE 쓰셈)
 */
public final class SequenceGenerator {
    
    // 허용된 테이블.컬럼 목록 (SQL 인젝션 방지용)
    private static final Set<String> ALLOWED = Set.of(
            "PurchaseOrder.POID",
            "Delivery.DeliveryID",
            "CarbonEmissionRecord.RecordID"
    );

    /**
     * 다음 ID 생성
     * 
     * 현재 MAX 값 + 1 반환
     * 
     * @param conn DB 커넥션
     * @param tableDotCol "테이블명.컬럼명" 형태 (예: "PurchaseOrder.POID")
     * @return 새 ID 값
     */
    public int nextId(Connection conn, String tableDotCol) throws SQLException {
        // 허용된 테이블만 사용 가능 (보안)
        if (!ALLOWED.contains(tableDotCol)) {
            throw new IllegalArgumentException("허용되지 않은 ID 컬럼: " + tableDotCol);
        }
        
        // "PurchaseOrder.POID" -> ["PurchaseOrder", "POID"]
        String[] parts = tableDotCol.split("\\.");
        String table = parts[0];
        String col = parts[1];
        
        // MAX 값 조회
        String sql = "SELECT COALESCE(MAX(" + col + "), 0) + 1 AS next_id FROM " + table;
        // COALESCE: 테이블 비어있으면 MAX가 NULL이니까 0으로 바꿔서 1 반환
        
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt("next_id");
        }
    }
}
