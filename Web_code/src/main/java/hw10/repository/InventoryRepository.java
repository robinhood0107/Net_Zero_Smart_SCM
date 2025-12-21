package hw10.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 창고 재고 관리 리포지토리.
 */
public final class InventoryRepository {

    /**
     * 창고 부품 수량 증감 처리. (deltaQty 음수 시 차감)
     * 존재 시 수량 업데이트, 미존재 시 신규 삽입 (Upsert).
     */
    public void addInventory(Connection conn, int warehouseId, int partId, int deltaQty) throws SQLException {
        // 재고 존재 시 합산, 미존재 시 신규 생성 쿼리.
        String sql = """
                INSERT INTO Inventory(WarehouseID, PartID, Quantity)
                VALUES (?, ?, ?)
                ON CONFLICT (WarehouseID, PartID)
                DO UPDATE SET Quantity = Inventory.Quantity + EXCLUDED.Quantity
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, warehouseId);
            ps.setInt(2, partId);
            ps.setInt(3, deltaQty);
            ps.executeUpdate();
        }
    }
}
