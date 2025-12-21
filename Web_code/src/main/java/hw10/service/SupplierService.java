package hw10.service;

import hw10.repository.SupplierRepository;
import hw10.dto.SupplierDto;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 공급업체 관련 서비스.
 * 공급업체 목록 조회, 상세 정보, 발주 내역 처리.
 */
@Service
public class SupplierService {

    private final DataSource dataSource;
    private final SupplierRepository supplierRepository;

    public SupplierService(DataSource dataSource) {
        this.dataSource = dataSource;
        this.supplierRepository = new SupplierRepository();
    }

    /**
     * 공급업체 목록 조회.
     * ESG 등급 및 지연율 필터링 지원.
     */
    public List<SupplierDto.SupplierRow> listSuppliers(
            List<String> esgGrades,
            Double minRatio,
            Double maxRatio) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            var suppliers = supplierRepository.listSuppliers(conn, esgGrades, minRatio, maxRatio);
            return suppliers.stream()
                    .map(s -> new SupplierDto.SupplierRow(
                            s.supplierId(),
                            s.name(),
                            s.country(),
                            s.esgGrade(),
                            s.totalOrderAmount(),
                            s.delayedDeliveries(),
                            s.totalDeliveries(),
                            s.delayRatio()))
                    .toList();
        }
    }

    /**
     * 업체 상세 정보 및 최근 발주 내역(5건) 조회.
     */
    public SupplierDto.SupplierDetail getSupplierDetail(int supplierId) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {

            var all = supplierRepository.listSuppliers(conn, null, null, null);
            var supplier = all.stream()
                    .filter(s -> s.supplierId() == supplierId)
                    .findFirst()
                    .orElse(null);

            if (supplier == null) {
                throw new IllegalArgumentException("공급업체를 찾을 수 없습니다: " + supplierId);
            }

            var recentOrders = supplierRepository.recentPurchaseOrders(conn, supplierId, 5, 0);

            SupplierDto.SupplierRow supplierDto = new SupplierDto.SupplierRow(
                    supplier.supplierId(),
                    supplier.name(),
                    supplier.country(),
                    supplier.esgGrade(),
                    supplier.totalOrderAmount(),
                    supplier.delayedDeliveries(),
                    supplier.totalDeliveries(),
                    supplier.delayRatio());

            List<SupplierDto.SupplierPoRow> ordersDto = recentOrders.stream()
                    .map(o -> new SupplierDto.SupplierPoRow(
                            o.poid(),
                            o.orderDate(),
                            o.status(),
                            o.delayed()))
                    .toList();

            return new SupplierDto.SupplierDetail(supplierDto, ordersDto);
        }
    }

    /**
     * 특정 업체의 발주 내역 페이징 조회.
     */
    public List<SupplierDto.SupplierPoRow> getSupplierOrders(int supplierId, int page, int size) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            int offset = page * size;
            var orders = supplierRepository.recentPurchaseOrders(conn, supplierId, size, offset);
            return orders.stream()
                    .map(o -> new SupplierDto.SupplierPoRow(
                            o.poid(),
                            o.orderDate(),
                            o.status(),
                            o.delayed()))
                    .toList();
        }
    }
}
