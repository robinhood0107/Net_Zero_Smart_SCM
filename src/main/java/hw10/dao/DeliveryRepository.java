package hw10.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 납품/납품상세 INSERT (기능 2용)
 * 
 * Delivery: 납품 헤더
 * DeliveryLine: 납품에 포함된 각 품목의 수량
 */
public final class DeliveryRepository {

    /**
     * 납품(Delivery) INSERT
     * 
     * [과제 요구사항]
     * - Delivery에 레코드 1건 삽입
     * - delivery_date = 오늘 날짜
     * - status = '정상' 등
     * 
     * @param conn DB 커넥션
     * @param deliveryId 납품 ID
     * @param poid 발주서 ID (FK)
     * @param actualArrivalDate 실제 도착일 (보통 오늘)
     * @param transportMode 운송 수단
     * @param distanceKm 운송 거리 (null 가능)
     * @param status 상태 ('정상입고', '지연' 등)
     */
    public void insertDelivery(Connection conn, int deliveryId, int poid, Date actualArrivalDate,
                               String transportMode, Double distanceKm, String status) throws SQLException {
        String sql = """
                INSERT INTO Delivery(DeliveryID, POID, ActualArrivalDate, TransportMode, DistanceKm, Status)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deliveryId);
            ps.setInt(2, poid);
            ps.setDate(3, actualArrivalDate);
            ps.setString(4, transportMode);
            
            // distanceKm가 null이면 SQL NULL로 설정
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
     * 납품상세(DeliveryLine) INSERT
     * 
     * [과제 요구사항]
     * - DeliveryLine에 각 항목 delivered_qty 일부 삽입 (예: 50%)
     * 
     * @param conn DB 커넥션
     * @param deliveryId 납품 ID (FK)
     * @param poid 발주서 ID (FK, 복합키)
     * @param lineNo 라인 번호 (FK, 복합키)
     * @param receivedQty 실제 입고 수량
     * @param inspectionResult 검수 결과
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
