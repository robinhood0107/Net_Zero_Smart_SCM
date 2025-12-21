package hw10.service;

import hw10.repository.ProjectRepository;
import hw10.dto.OrderDto;
import hw10.dto.ProjectDto;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 발주 관련 서비스.
 * 옵션(프로젝트, 공급사, 부품, 창고) 제공 및 발주 생성.
 */
@Service
public class OrderService {

    private final DataSource dataSource;
    private final ProjectRepository projectRepository;

    // 생성자. 리포지토리 초기화.
    public OrderService(DataSource dataSource) {
        this.dataSource = dataSource;
        this.projectRepository = new ProjectRepository();
    }

    /**
     * 프로젝트 검색 및 드롭다운 옵션 제공.
     * 키워드 없을 시 전체 조회 (최대 20개).
     */
    public List<ProjectDto.ProjectSearchItem> getProjectOptions(String keyword) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            var projects = projectRepository.searchProjectsByShipName(
                    conn, keyword.isEmpty() ? "%" : keyword, 20, 0);
            return projects.stream()
                    .map(p -> new ProjectDto.ProjectSearchItem(
                            p.projectId(),
                            p.shipName(),
                            p.shipType(),
                            p.status()))
                    .toList();
        }
    }

    /**
     * 공급업체 목록 전체 조회. 이름 순 정렬.
     */
    public List<OrderDto.SupplierOption> getSupplierOptions() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT SupplierID, Name, Country FROM Supplier ORDER BY Name";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                    ResultSet rs = ps.executeQuery()) {
                List<OrderDto.SupplierOption> suppliers = new ArrayList<>();
                while (rs.next()) {
                    suppliers.add(new OrderDto.SupplierOption(
                            rs.getInt("SupplierID"),
                            rs.getString("Name"),
                            rs.getString("Country")));
                }
                return suppliers;
            }
        }
    }

    /**
     * 부품 검색. 키워드 존재 시 이름 검색, 부재 시 전체 조회 (50개).
     * 평균 단가 포함하여 반환.
     */
    public List<OrderDto.PartOption> searchParts(String keyword) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {

            String sql;
            // 키워드 미존재 시 전체 조회 (상위 50개)
            if (keyword == null || keyword.trim().isEmpty()) {
                sql = """
                        SELECT p.PartID, p.Name, p.Unit,
                               COALESCE(AVG(sp.UnitPrice), 0.0) AS UnitPrice
                        FROM Part p
                        LEFT JOIN SupplierPart sp ON p.PartID = sp.PartID
                        GROUP BY p.PartID, p.Name, p.Unit
                        ORDER BY p.Name
                        LIMIT 50
                        """;
            } else {
                // 키워드 존재 시 이름 검색 (상위 20개)
                sql = """
                        SELECT p.PartID, p.Name, p.Unit,
                               COALESCE(AVG(sp.UnitPrice), 0.0) AS UnitPrice
                        FROM Part p
                        LEFT JOIN SupplierPart sp ON p.PartID = sp.PartID
                        WHERE p.Name ILIKE ?
                        GROUP BY p.PartID, p.Name, p.Unit
                        ORDER BY p.Name
                        LIMIT 20
                        """;
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (keyword != null && !keyword.trim().isEmpty()) {
                    ps.setString(1, "%" + keyword + "%");
                }
                try (ResultSet rs = ps.executeQuery()) {
                    List<OrderDto.PartOption> parts = new ArrayList<>();
                    while (rs.next()) {
                        parts.add(new OrderDto.PartOption(
                                rs.getInt("PartID"),
                                rs.getString("Name"),
                                rs.getString("Unit"),
                                rs.getDouble("UnitPrice")));
                    }
                    return parts;
                }
            }
        }
    }

    /**
     * 창고 목록 조회. 이름 순 정렬.
     */
    public List<OrderDto.WarehouseOption> getWarehouseOptions() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT WarehouseID, Name, Location FROM Warehouse ORDER BY Name";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                    ResultSet rs = ps.executeQuery()) {
                List<OrderDto.WarehouseOption> warehouses = new ArrayList<>();
                while (rs.next()) {
                    warehouses.add(new OrderDto.WarehouseOption(
                            rs.getInt("WarehouseID"),
                            rs.getString("Name"),
                            rs.getString("Location")));
                }
                return warehouses;
            }
        }
    }

    /**
     * 특정 창고의 현재 재고 현황 조회.
     * 부품별 수량 표시.
     */
    public List<OrderDto.InventoryItem> getWarehouseInventory(int warehouseId) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            String sql = """
                    SELECT i.PartID, p.Name AS PartName, p.Unit, i.Quantity
                    FROM Inventory i
                    INNER JOIN Part p ON i.PartID = p.PartID
                    WHERE i.WarehouseID = ?
                    ORDER BY p.Name
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, warehouseId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<OrderDto.InventoryItem> inventory = new ArrayList<>();
                    while (rs.next()) {
                        inventory.add(new OrderDto.InventoryItem(
                                rs.getInt("PartID"),
                                rs.getString("PartName"),
                                rs.getString("Unit"),
                                rs.getInt("Quantity")));
                    }
                    return inventory;
                }
            }
        }
    }

    /**
     * 발주 생성 트랜잭션 실행.
     * OrderTransactionService 호출 및 결과(POID) 반환.
     */
    public OrderDto.OrderResponse createOrder(OrderDto.OrderRequest request) throws SQLException {
        OrderTransactionService transactionService = new OrderTransactionService(dataSource);

        // 입력 라인 변환.
        List<OrderTransactionService.OrderLineInput> lines = request.lines().stream()
                .map(line -> new OrderTransactionService.OrderLineInput(
                        line.partId(),
                        line.quantity(),
                        line.unitPrice()))
                .toList();

        try {
            // 트랜잭션 서비스 호출 및 작업 수행.
            OrderTransactionService.TransactionResult result = transactionService.createOrderWithInitialDelivery(
                    request.projectId(),
                    request.supplierId(),
                    request.engineerName(),
                    request.status(),
                    lines,
                    request.warehouseId(),
                    request.transportMode(),
                    request.distanceKm());

            return new OrderDto.OrderResponse(
                    result.poid(),
                    result.deliveryId(),
                    "발주 등록 완료");
        } catch (SQLException e) {
            // SQL 에러 메시지 포함 전달.
            throw new SQLException("발주 등록 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            // 기타 에러 SQL 예외로 래핑하여 전달.
            throw new SQLException("발주 등록 실패: " + e.getMessage(), e);
        }
    }
}
