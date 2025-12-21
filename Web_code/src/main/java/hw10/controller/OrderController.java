package hw10.controller;

import hw10.dto.OrderDto;
import hw10.dto.ProjectDto;
import hw10.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;

/**
 * 발주 관련 API 컨트롤러.
 * 발주 생성 및 프로젝트/공급사/부품/창고 목록 조회.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 발주 등록.
     * 트랜잭션 처리를 통해 DB 저장. 실패 시 500 에러 반환.
     */
    @PostMapping
    public ResponseEntity<OrderDto.OrderResponse> createOrder(@RequestBody OrderDto.OrderRequest request)
            throws SQLException {
        try {
            OrderDto.OrderResponse response = orderService.createOrder(request);
            return ResponseEntity.ok(response);
        } catch (SQLException e) {
            // 에러 메시지 포함 예외 전달. GlobalExceptionHandler 처리 예정.
            throw new SQLException("발주 등록 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 프로젝트 목록 조회 (드롭다운용).
     * 키워드 검색 지원.
     */
    @GetMapping("/projects")
    public ResponseEntity<List<ProjectDto.ProjectSearchItem>> getProjectList(
            @RequestParam(defaultValue = "") String keyword) throws SQLException {
        List<ProjectDto.ProjectSearchItem> projects = orderService.getProjectOptions(keyword);
        return ResponseEntity.ok(projects);
    }

    /**
     * 공급업체 목록 조회 (드롭다운용).
     */
    @GetMapping("/suppliers")
    public ResponseEntity<List<OrderDto.SupplierOption>> getSupplierList() throws SQLException {
        List<OrderDto.SupplierOption> suppliers = orderService.getSupplierOptions();
        return ResponseEntity.ok(suppliers);
    }

    /**
     * 부품 검색 API.
     * 키워드 미존재 시 50개, 존재 시 20개 반환.
     */
    @GetMapping("/parts")
    public ResponseEntity<List<OrderDto.PartOption>> searchParts(
            @RequestParam(defaultValue = "") String keyword) throws SQLException {
        List<OrderDto.PartOption> parts = orderService.searchParts(keyword);
        return ResponseEntity.ok(parts);
    }

    /**
     * 창고 목록 조회 (드롭다운용).
     */
    @GetMapping("/warehouses")
    public ResponseEntity<List<OrderDto.WarehouseOption>> getWarehouseList() throws SQLException {
        List<OrderDto.WarehouseOption> warehouses = orderService.getWarehouseOptions();
        return ResponseEntity.ok(warehouses);
    }

    /**
     * 특정 창고 재고 현황 조회.
     * 선택한 창고의 부품 수량 확인.
     */
    @GetMapping("/warehouses/{warehouseId}/inventory")
    public ResponseEntity<List<OrderDto.InventoryItem>> getWarehouseInventory(
            @PathVariable int warehouseId) throws SQLException {
        List<OrderDto.InventoryItem> inventory = orderService.getWarehouseInventory(warehouseId);
        return ResponseEntity.ok(inventory);
    }

}
