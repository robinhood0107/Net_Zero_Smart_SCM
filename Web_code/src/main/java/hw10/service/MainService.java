package hw10.service;

import hw10.dto.MainDto;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 메인 페이지 비즈니스 로직 담당.
 * 대시보드 요약 정보(활성 프로젝트, 탄소, 지연, ESG) 계산 및 제공.
 */
@Service
public class MainService {

    // DB 데이터 소스.
    private final DataSource dataSource;

    // 생성자. 데이터 소스 주입.
    public MainService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 메인 화면 상단 카드 4개 요약 정보 조회.
     * 활성 프로젝트 수, 탄소 감축량, 배송 지연 건수, 평균 ESG 등급.
     */
    public MainDto.MainSummary getSummary() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            // 프로젝트 가동 수 확인.
            int activeProjects = countActiveProjects(conn);
            // 탄소 감축량 계산.
            double carbonReduction = getTotalCarbonReduction(conn);
            // 배송 지연 건수 확인.
            int delayedDeliveries = countDelayedDeliveries(conn);
            // 공급업체 ESG 등급 평균 산출.
            String avgEsgGrade = getAverageEsgGrade(conn);

            // DTO 포장 및 반환.
            return new MainDto.MainSummary(
                    activeProjects,
                    carbonReduction,
                    delayedDeliveries,
                    avgEsgGrade);
        }
    }

    // '인도완료' 제외 프로젝트 개수 쿼리 실행.
    private int countActiveProjects(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM ShipProject WHERE Status != '인도완료'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    // 총 탄소 배출량 조회 및 기준값(1000) 대비 감축량 계산. (음수 시 0 처리)
    private double getTotalCarbonReduction(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(SUM(CO2eAmount), 0) FROM CarbonEmissionRecord";
        try (PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            double totalEmission = rs.getDouble(1);
            // 기준값 1000에서 배출량 차감하여 감축량 산출.
            return Math.max(0, 1000 - totalEmission);
        }
    }

    // 상태가 '지연'인 배송 건수 계산.
    private int countDelayedDeliveries(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Delivery WHERE Status = '지연'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    // ESG 등급 평균 산출. A=4점, B=3점 등 점수 환산 후 평균.
    private String getAverageEsgGrade(Connection conn) throws SQLException {
        String sql = """
                SELECT AVG(
                    CASE ESGGrade
                        WHEN 'A' THEN 4
                        WHEN 'B' THEN 3
                        WHEN 'C' THEN 2
                        WHEN 'D' THEN 1
                        ELSE 0
                    END
                ) as avg_grade
                FROM Supplier
                WHERE ESGGrade IS NOT NULL
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            double avg = rs.getDouble("avg_grade");

            // 평균 점수 등급 변환 및 반올림 적용.
            if (avg >= 3.5)
                return "A";
            if (avg >= 2.5)
                return "B";
            if (avg >= 1.5)
                return "C";
            return "D";
        }
    }
}
