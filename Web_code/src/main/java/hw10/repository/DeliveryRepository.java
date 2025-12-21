package hw10.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 배송 정보 DB 저장 리포지토리.
 */
public final class DeliveryRepository {

    /**
     * 배송(Delivery) 테이블에 데이터 1건 추가.
     * ID, POID, 도착일, 운송수단, 거리, 상태 포함.
     */
    public void insertDelivery(Connection conn, int deliveryId, int poid, Date actualArrivalDate,
            String transportMode, Double distanceKm, String status) throws SQLException {
        // 배송 정보 삽입 SQL.
        String sql = """
                INSERT INTO Delivery(DeliveryID, POID, ActualArrivalDate, TransportMode, DistanceKm, Status)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deliveryId);
            ps.setInt(2, poid);
            ps.setDate(3, actualArrivalDate);
            ps.setString(4, transportMode);

            // 거리(distanceKm)가 null이면 DB에 null 저장, 아니면 값 저장.
            if (distanceKm == null) {
                ps.setNull(5, java.sql.Types.DOUBLE);
            } else {
                ps.setDouble(5, distanceKm);
            }

            ps.setString(6, status);
            ps.executeUpdate();
        }
    }

    /**
     * 배송 상세 품목(DeliveryLine) 추가.
     * 라인 번호(LineNo), 수량, 검수 결과 저장.
     */
    public void insertDeliveryLine(Connection conn, int deliveryId, int poid, int lineNo,
            int receivedQty, String inspectionResult) throws SQLException {
        String sql = """
                INSERT INTO DeliveryLine(DeliveryID, POID, LineNo, ReceivedQty, InspectionResult)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deliveryId);
            ps.setInt(2, poid);
            ps.setInt(3, lineNo);
            ps.setInt(4, receivedQty);
            ps.setString(5, inspectionResult);
            ps.executeUpdate();
        }
    }
}
