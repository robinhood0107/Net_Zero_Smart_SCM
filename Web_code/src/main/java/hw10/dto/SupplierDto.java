package hw10.dto;

import java.sql.Date;
import java.util.List;

/**
 * 공급업체 관련 데이터 객체.
 */
public class SupplierDto {

        /**
         * 공급업체 목록 조회용 정보.
         * ESG 등급, 지연율 등 통계 포함.
         */
        public record SupplierRow(
                        int supplierId,
                        String name,
                        String country,
                        String esgGrade,
                        double totalOrderAmount,
                        int delayedDeliveries,
                        int totalDeliveries,
                        double delayRatio) {
        }

        /**
         * 공급업체 최근 발주 이력. 주문일, 지연 여부 포함.
         */
        public record SupplierPoRow(
                        int poid,
                        Date orderDate,
                        String status,
                        boolean delayed) {
        }

        /**
         * 공급업체 상세 페이지용. 업체 정보 및 최근 발주 내역 포함.
         */
        public record SupplierDetail(
                        SupplierRow supplier,
                        List<SupplierPoRow> recentOrders) {
        }
}
