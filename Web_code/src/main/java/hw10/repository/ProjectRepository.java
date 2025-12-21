package hw10.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 프로젝트 정보 조회 및 통계 리포지토리.
 */
public final class ProjectRepository {

    // 프로젝트 기본 정보 레코드.
    public record ProjectBasic(
            int projectId,
            String shipName,
            String shipType,
            java.sql.Date contractDate,
            java.sql.Date deliveryDueDate,
            String status) {
    }

    // 공급사별 금액 통계 레코드.
    public record SupplierAmount(int supplierId, String name, double amount) {
    }

    // 프로젝트 검색용 요약 레코드.
    public record ProjectSearchItem(int projectId, String shipName, String shipType, String status) {
    }

    /**
     * 프로젝트 ID로 상세 정보 조회.
     * 미존재 시 null 반환.
     */
    public ProjectBasic findProjectById(Connection conn, int projectId) throws SQLException {

        String sql = """
                SELECT ProjectID, ShipName, ShipType, ContractDate, DeliveryDueDate, Status
                FROM ShipProject
                WHERE ProjectID = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next())
                    return null;
                return new ProjectBasic(
                        rs.getInt("ProjectID"),
                        rs.getString("ShipName"),
                        rs.getString("ShipType"),
                        rs.getDate("ContractDate"),
                        rs.getDate("DeliveryDueDate"),
                        rs.getString("Status"));
            }
        }
    }

    /**
     * 선박 이름으로 프로젝트 검색 (부분 일치).
     * 페이징 처리 (limit, offset).
     */
    public List<ProjectSearchItem> searchProjectsByShipName(Connection conn, String keyword, int limit, int offset)
            throws SQLException {
        String sql = """
                SELECT ProjectID, ShipName, ShipType, Status
                FROM ShipProject
                WHERE ShipName ILIKE ?
                ORDER BY ProjectID
                LIMIT ? OFFSET ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + keyword + "%");
            ps.setInt(2, limit);
            ps.setInt(3, offset);

            try (ResultSet rs = ps.executeQuery()) {
                List<ProjectSearchItem> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new ProjectSearchItem(
                            rs.getInt("ProjectID"),
                            rs.getString("ShipName"),
                            rs.getString("ShipType"),
                            rs.getString("Status")));
                }
                return out;
            }
        }
    }

    /**
     * 특정 프로젝트의 총 발주 금액 계산.
     * (수량 * 단가) 합계 산출.
     */
    public double totalOrderAmount(Connection conn, int projectId) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(pol.Quantity * pol.UnitPriceAtOrder), 0) AS total_amount
                FROM PurchaseOrder po
                JOIN PurchaseOrderLine pol ON pol.POID = po.POID
                WHERE po.ProjectID = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble("total_amount");
            }
        }
    }

    /**
     * 발주 상위 공급사 조회 (Top N).
     * 금액 기준 내림차순 정렬.
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
                            rs.getDouble("amount")));
                }
                return out;
            }
        }
    }

    /**
     * 배출 유형별(운송/보관/가공) 탄소 배출량 합계.
     * 프로젝트 자체 배출 및 배송 배출 합산.
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
     * 프로젝트 전체 탄소 배출량 총합 산출.
     * 유형 불문 합산.
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

    /**
     * 탄소 집약도(Carbon Intensity) 계산.
     * 총 배출량 / 총 발주금액 * 1,000,000.
     * 소수점 첫째 자리 반올림.
     */
    public Double calculateCarbonIntensity(Connection conn, int projectId) throws SQLException {

        // 총 배출량 조회.
        double totalEmissionKg = emissionSumTotal(conn, projectId);

        // 총 발주금액 조회.
        double totalAmountKRW = totalOrderAmount(conn, projectId);

        // 금액 0일 경우 계산 불가, null 반환.
        if (totalAmountKRW == 0)
            return null;

        // 집약도 계산 및 반올림 후 반환.
        double intensity = (totalEmissionKg / totalAmountKRW) * 1000000.0;
        return Math.round(intensity * 10.0) / 10.0;
    }
}
