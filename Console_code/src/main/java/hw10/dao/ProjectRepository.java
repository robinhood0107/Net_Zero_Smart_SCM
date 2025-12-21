package hw10.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 프로젝트 조회/집계 SQL 모음 (기능 1용)
 * 
 * DAO = Data Access Object
 * 데이터베이스 접근 로직만 모아놓은 클래스.
 * cpp에서 DB 관련 함수들 따로 분리해놓는 거랑 같은 패턴.
 * 
 * [과제 요구사항]
 * - 집계 함수 (SUM, COALESCE)
 * - GROUP BY
 * - JOIN
 * - LIMIT
 * 이런 거 다 여기서 사용함
 */
public final class ProjectRepository {

    // ========== record 정의 ==========
    // record는 Java 16+에서 추가된 불변 데이터 클래스
    // cpp의 struct랑 비슷한데 getter 자동 생성됨
    // 예: basic.projectId() 이렇게 값 가져옴
    
    /** 프로젝트 기본 정보 */
    public record ProjectBasic(
            int projectId,
            String shipName,
            String shipType,
            java.sql.Date contractDate,
            java.sql.Date deliveryDueDate,
            String status
    ) {}

    /** 공급업체별 발주 금액 */
    public record SupplierAmount(int supplierId, String name, double amount) {}

    /** 프로젝트 검색 결과 항목 */
    public record ProjectSearchItem(int projectId, String shipName, String shipType, String status) {}

    /**
     * ProjectID로 프로젝트 기본 정보 조회
     * 
     * @param conn DB 커넥션
     * @param projectId 조회할 프로젝트 ID
     * @return 프로젝트 정보 또는 null (없으면)
     */
    public ProjectBasic findProjectById(Connection conn, int projectId) throws SQLException {
        // """ 이건 Text Block (Java 15+)
        // 여러 줄 문자열 쓸 때 편함
        String sql = """
                SELECT ProjectID, ShipName, ShipType, ContractDate, DeliveryDueDate, Status
                FROM ShipProject
                WHERE ProjectID = ?
                """;
        
        // PreparedStatement: SQL 인젝션 방지용
        // ?에 값 바인딩해서 안전하게 실행
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);  // 첫 번째 ?에 projectId 바인딩
            
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;  // 결과 없으면 null
                return new ProjectBasic(
                        rs.getInt("ProjectID"),
                        rs.getString("ShipName"),
                        rs.getString("ShipType"),
                        rs.getDate("ContractDate"),
                        rs.getDate("DeliveryDueDate"),
                        rs.getString("Status")
                );
            }
        }
    }

    /**
     * 선박명으로 프로젝트 검색 (부분 일치)
     * 
     * ILIKE는 PostgreSQL에서 대소문자 무시 LIKE임
     * 
     * @param conn DB 커넥션
     * @param keyword 검색 키워드
     * @param limit 최대 결과 수
     * @return 검색 결과 리스트
     */
    public List<ProjectSearchItem> searchProjectsByShipName(Connection conn, String keyword, int limit) throws SQLException {
        String sql = """
                SELECT ProjectID, ShipName, ShipType, Status
                FROM ShipProject
                WHERE ShipName ILIKE ?
                ORDER BY ProjectID
                LIMIT ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // %keyword% = 키워드 포함하는 모든 문자열
            ps.setString(1, "%" + keyword + "%");
            ps.setInt(2, limit);
            
            try (ResultSet rs = ps.executeQuery()) {
                List<ProjectSearchItem> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new ProjectSearchItem(
                            rs.getInt("ProjectID"),
                            rs.getString("ShipName"),
                            rs.getString("ShipType"),
                            rs.getString("Status")
                    ));
                }
                return out;
            }
        }
    }

    /**
     * 프로젝트의 총 발주 금액 계산
     * 
     * [과제 요구사항] JOIN + SUM 집계 사용
     * 
     * PurchaseOrder와 PurchaseOrderLine JOIN해서
     * Quantity * UnitPriceAtOrder 다 더함
     * 
     * @param conn DB 커넥션
     * @param projectId 프로젝트 ID
     * @return 총 발주 금액
     */
    public double totalOrderAmount(Connection conn, int projectId) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(pol.Quantity * pol.UnitPriceAtOrder), 0) AS total_amount
                FROM PurchaseOrder po
                JOIN PurchaseOrderLine pol ON pol.POID = po.POID
                WHERE po.ProjectID = ?
                """;
        // COALESCE: 첫 번째 NULL 아닌 값 반환
        // 발주 없으면 SUM이 NULL 나오니까 0으로 바꿔줌
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble("total_amount");
            }
        }
    }

    /**
     * 공급업체별 발주 금액 상위 N개
     * 
     * [과제 요구사항] GROUP BY + ORDER BY + LIMIT 사용
     * 
     * @param conn DB 커넥션
     * @param projectId 프로젝트 ID
     * @param topN 상위 몇 개
     * @return 공급업체별 금액 리스트 (내림차순)
     */
    public List<SupplierAmount> topSuppliersByAmount(Connection conn, int projectId, int topN) throws SQLException {
        String sql = """
                SELECT s.SupplierID, s.Name, SUM(pol.Quantity * pol.UnitPriceAtOrder) AS amount
                FROM PurchaseOrder po
                JOIN PurchaseOrderLine pol ON pol.POID = po.POID
                JOIN Supplier s ON s.SupplierID = po.SupplierID
                WHERE po.ProjectID = ?
                GROUP BY s.SupplierID, s.Name
                ORDER BY amount DESC
                LIMIT ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.setInt(2, topN);
            try (ResultSet rs = ps.executeQuery()) {
                List<SupplierAmount> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new SupplierAmount(
                            rs.getInt("SupplierID"),
                            rs.getString("Name"),
                            rs.getDouble("amount")
                    ));
                }
                return out;
            }
        }
    }

    /**
     * 프로젝트 탄소 배출 합계 (특정 타입만)
     * 
     * 프로젝트에 직접 연결된 것 + 해당 프로젝트의 Delivery에 연결된 것 포함
     * 
     * @param conn DB 커넥션
     * @param projectId 프로젝트 ID
     * @param emissionType 배출 타입 ("운송" 또는 "보관")
     * @return 탄소 배출량 합계 (kgCO2e)
     */
    public double emissionSumByType(Connection conn, int projectId, String emissionType) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(c.CO2eAmount), 0) AS s
                FROM CarbonEmissionRecord c
                WHERE c.EmissionType = ?
                  AND (
                        c.ProjectID = ?
                        OR c.DeliveryID IN (
                            SELECT d.DeliveryID
                            FROM Delivery d
                            JOIN PurchaseOrder po ON po.POID = d.POID
                            WHERE po.ProjectID = ?
                        )
                  )
                """;
        // 서브쿼리: 해당 프로젝트의 발주서에 연결된 Delivery 찾기
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, emissionType);
            ps.setInt(2, projectId);
            ps.setInt(3, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble("s");
            }
        }
    }

    /**
     * 프로젝트 전체 탄소 배출 합계 (타입 무관)
     * 
     * @param conn DB 커넥션
     * @param projectId 프로젝트 ID
     * @return 전체 탄소 배출량 합계
     */
    public double emissionSumTotal(Connection conn, int projectId) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(c.CO2eAmount), 0) AS s
                FROM CarbonEmissionRecord c
                WHERE (
                        c.ProjectID = ?
                        OR c.DeliveryID IN (
                            SELECT d.DeliveryID
                            FROM Delivery d
                            JOIN PurchaseOrder po ON po.POID = d.POID
                            WHERE po.ProjectID = ?
                        )
                  )
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.setInt(2, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble("s");
            }
        }
    }
}
