package hw10.service;

import hw10.repository.ProjectRepository;
import hw10.dto.ProjectDto;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 프로젝트 관련 서비스.
 * 프로젝트 상세 조회, 검색, 대시보드 통계 처리 담당.
 */
@Service
public class ProjectService {

    private final DataSource dataSource;
    private final ProjectRepository projectRepository;

    public ProjectService(DataSource dataSource) {
        this.dataSource = dataSource;
        this.projectRepository = new ProjectRepository();
    }

    /**
     * 프로젝트 ID로 상세 정보 조회. 미존재 시 null 반환.
     */
    public ProjectDto.ProjectBasic getProject(int projectId) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            var basic = projectRepository.findProjectById(conn, projectId);
            if (basic == null) {
                return null;
            }

            return new ProjectDto.ProjectBasic(
                    basic.projectId(),
                    basic.shipName(),
                    basic.shipType(),
                    basic.contractDate(),
                    basic.deliveryDueDate(),
                    basic.status());
        }
    }

    /**
     * 프로젝트 검색 및 페이징 처리.
     */
    public List<ProjectDto.ProjectSearchItem> searchProjects(String keyword, int page, int limit) throws SQLException {
        int offset = page * limit;
        try (Connection conn = dataSource.getConnection()) {
            var results = projectRepository.searchProjectsByShipName(conn, keyword, limit, offset);
            return results.stream()
                    .map(r -> new ProjectDto.ProjectSearchItem(
                            r.projectId(),
                            r.shipName(),
                            r.shipType(),
                            r.status()))
                    .toList();
        }
    }

    /**
     * 프로젝트 대시보드 통계 데이터 수집.
     * 총 발주액, 탄소 배출량(유형별), 상위 공급사, 탄소 집약도 계산.
     */
    public ProjectDto.DashboardStats getDashboardStats(int projectId) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {

            var projectBasic = projectRepository.findProjectById(conn, projectId);
            if (projectBasic == null) {
                throw new IllegalArgumentException("프로젝트를 찾을 수 없습니다: " + projectId);
            }

            // 총 발주 금액.
            double totalAmount = projectRepository.totalOrderAmount(conn, projectId);
            // 총 탄소 배출량.
            double totalEmission = projectRepository.emissionSumTotal(conn, projectId);
            // 운송 과정 탄소 배출량.
            double transportEmission = projectRepository.emissionSumByType(conn, projectId, "운송");
            // 보관 과정 탄소 배출량.
            double storageEmission = projectRepository.emissionSumByType(conn, projectId, "보관");

            // 가공 및 생산 합산하여 처리 과정 배출량으로 간주.
            double processingEmission = projectRepository.emissionSumByType(conn, projectId, "가공")
                    + projectRepository.emissionSumByType(conn, projectId, "생산");
            // 상위 3개 공급사 조회.
            var topSuppliers = projectRepository.topSuppliersByAmount(conn, projectId, 3);

            // 탄소 집약도 계산.
            Double carbonIntensity = projectRepository.calculateCarbonIntensity(conn, projectId);

            ProjectDto.ProjectBasic project = new ProjectDto.ProjectBasic(
                    projectBasic.projectId(),
                    projectBasic.shipName(),
                    projectBasic.shipType(),
                    projectBasic.contractDate(),
                    projectBasic.deliveryDueDate(),
                    projectBasic.status());

            List<ProjectDto.SupplierAmount> suppliers = topSuppliers.stream()
                    .map(s -> new ProjectDto.SupplierAmount(
                            s.supplierId(),
                            s.name(),
                            s.amount()))
                    .toList();

            return new ProjectDto.DashboardStats(
                    project,
                    totalAmount,
                    totalEmission,
                    transportEmission,
                    storageEmission,
                    processingEmission,
                    suppliers,
                    carbonIntensity,
                    null);
        }
    }
}
