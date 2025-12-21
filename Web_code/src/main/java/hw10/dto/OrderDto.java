package hw10.dto;

import java.util.List;

/**
 * 발주 관련 데이터 객체 모음.
 */
public class OrderDto {

        /**
         * 발주 요청 데이터.
         * 프로젝트, 공급사, 엔지니어, 상태, 품목, 창고, 운송수단, 거리 등 포함.
         */
        public record OrderRequest(
                        int projectId,
                        int supplierId,
                        String engineerName,
                        String status,
                        List<OrderLineInput> lines,
                        int warehouseId,
                        String transportMode,
                        Double distanceKm) {
        }

        /**
         * 발주 품목 입력용 레코드. ID, 수량, 단가 포함.
         */
        public record OrderLineInput(
                        int partId,
                        int quantity,
                        double unitPrice) {
        }

        /**
         * 발주 처리 결과 반환용. POID, DeliveryID, 결과 메시지 제공.
         */
        public record OrderResponse(
                        int poid,
                        int deliveryId,
                        String message) {
        }

        /**
         * 공급업체 드롭다운 선택용. ID, 이름, 국가 정보 포함.
         */
        public record SupplierOption(
                        int supplierId,
                        String name,
                        String country) {
        }

        /**
         * 부품 검색 결과용. 부품 정보 및 단가 포함.
         */
        public record PartOption(
                        int partId,
                        String name,
                        String unit,
                        double unitPrice) {
        }

        /**
         * 창고 선택용. 창고 ID, 이름, 위치 정보.
         */
        public record WarehouseOption(
                        int warehouseId,
                        String name,
                        String location) {
        }

        /**
         * 창고 재고 목록용. 부품 및 현재 수량 표시.
         */
        public record InventoryItem(
                        int partId,
                        String partName,
                        String unit,
                        int quantity) {
        }
}
