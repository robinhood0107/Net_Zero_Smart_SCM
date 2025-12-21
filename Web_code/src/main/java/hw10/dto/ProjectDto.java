package hw10.dto;

import java.sql.Date;
import java.util.List;

/**
 * 프로젝트 관련 데이터 객체 모음.
 */
public class ProjectDto {

        /**
         * 프로젝트 기본 정보. 선박명, 타입, 계약일, 납기일, 상태 등.
         */
        public record ProjectBasic(
                        int projectId,
                        String shipName,
                        String shipType,
                        Date contractDate,
                        Date deliveryDueDate,
                        String status) {
        }

        /**
         * 공급업체별 발주 금액 통계용.
         */
        public record SupplierAmount(
                        int supplierId,
                        String name,
                        double amount) {
        }

        /**
         * 프로젝트 검색 결과 목록용 요약 정보.
         */
        public record ProjectSearchItem(
                        int projectId,
                        String shipName,
                        String shipType,
                        String status) {
        }

        /**
         * 대시보드 차트용 종합 통계 데이터.
         * 총 발주액, 탄소 배출량(운송/보관/가공), 상위 공급사, 탄소 집약도 등 포함.
         */
        public record DashboardStats(
                        ProjectBasic project,
                        double totalOrderAmount,
                        double totalEmission,
                        double transportEmission,
                        double storageEmission,
                        double processingEmission,
                        List<SupplierAmount> topSuppliers,
                        Double carbonIntensity,
                        Double shipCII) {
        }
}
