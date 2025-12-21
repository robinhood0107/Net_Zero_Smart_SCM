package hw10.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 재고 UPSERT (기능 2용)
 * 
 * [과제 요구사항]
 * - 창고·부품별 Inventory 수량을 증가시킴
 * - 기존 레코드가 없으면 신규 INSERT
 * - 기존 레코드가 있으면 UPDATE
 * 
 * PostgreSQL의 ON CONFLICT 문법 사용함 (UPSERT)
 */
public final class InventoryRepository {

    /**
     * 재고 추가/갱신 (UPSERT)
     * 
     * (WarehouseID, PartID)가 UNIQUE KEY임.
     * 같은 조합 있으면 수량만 증가, 없으면 새로 INSERT.
     * 
     * ON CONFLICT DO UPDATE 문법:
     * - CONFLICT 발생하면 INSERT 대신 UPDATE 실행
     * - EXCLUDED는 INSERT 하려던 값 참조
     * 
     * @param conn DB 커넥션
     * @param warehouseId 창고 ID
     * @param partId 부품 ID
     * @param deltaQty 추가할 수량
     */
    public void addInventory(Connection conn, int warehouseId, int partId, int deltaQty) throws SQLException {
        String sql = """
                INSERT INTO Inventory(WarehouseID, PartID, Quantity)
                VALUES (?, ?, ?)
                ON CONFLICT (WarehouseID, PartID)
                DO UPDATE SET Quantity = Inventory.Quantity + EXCLUDED.Quantity
                """;
        // EXCLUDED.Quantity = INSERT 하려던 값 (= deltaQty)
        // 기존 수량 + 추가 수량으로 업데이트
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, warehouseId);
            ps.setInt(2, partId);
            ps.setInt(3, deltaQty);
            ps.executeUpdate();
        }
    }
}
