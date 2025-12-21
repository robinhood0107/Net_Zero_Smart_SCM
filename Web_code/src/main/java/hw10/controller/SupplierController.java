package hw10.controller;

import hw10.dto.SupplierDto;
import hw10.service.SupplierService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;

/**
 * 공급업체 API 컨트롤러.
 * 업체 목록, 상세 정보, 발주 내역 조회 담당.
 */
@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    /**
     * 공급업체 목록 조회.
     * ESG 등급 및 지연율 필터링 가능.
     */
    @GetMapping
    public ResponseEntity<List<SupplierDto.SupplierRow>> listSuppliers(
            @RequestParam(required = false) List<String> esgGrades,
            @RequestParam(required = false) Double minRatio,
            @RequestParam(required = false) Double maxRatio) throws SQLException {
        List<SupplierDto.SupplierRow> suppliers = supplierService.listSuppliers(esgGrades, minRatio, maxRatio);
        return ResponseEntity.ok(suppliers);
    }

    /**
     * 특정 공급업체 상세 정보 조회.
     * 최근 발주 내역 포함.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SupplierDto.SupplierDetail> getSupplier(@PathVariable int id) throws SQLException {
        SupplierDto.SupplierDetail detail = supplierService.getSupplierDetail(id);
        return ResponseEntity.ok(detail);
    }

    /**
     * 공급업체 발주 내역 페이징 조회.
     */
    @GetMapping("/{id}/orders")
    public ResponseEntity<List<SupplierDto.SupplierPoRow>> getSupplierOrders(
            @PathVariable int id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) throws SQLException {
        List<SupplierDto.SupplierPoRow> orders = supplierService.getSupplierOrders(id, page, size);
        return ResponseEntity.ok(orders);
    }
}
