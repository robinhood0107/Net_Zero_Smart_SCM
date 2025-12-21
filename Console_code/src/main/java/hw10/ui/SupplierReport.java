package hw10.ui;

import hw10.dao.SupplierRepository;
import hw10.db.DatabaseConnection;
import hw10.util.InputHelper;

import java.sql.Connection;
import java.util.List;
import java.util.Scanner;

/**
 * [기능 3] 공급업체 ESG 및 지연 납품 리포트
 * 
 * [과제 요구사항]
 * 1. 공급업체 목록 화면
 *    - 공급업체ID, 이름, 국가, ESG 등급, 총 발주 금액, 지연 납품 비율
 *    - 지연 납품 비율은 Delivery.status = '지연' 기준
 * 
 * 2. 필터 기능
 *    - ESG 등급 다중 선택 (A,B,C,D)
 *    - 지연 납품 비율 상한/하한
 * 
 * 3. 상세 화면
 *    - 특정 공급업체 선택하면 최근 N개 발주서 목록
 *    - 다음/이전 페이징
 */
public final class SupplierReport {
    
    private SupplierReport() {}

    /**
     * 기능3 메인 실행 메서드
     */
    public static void run(DatabaseConnection db, Scanner sc) throws Exception {
        System.out.println();
        System.out.println("========== 공급업체 ESG/지연 납품 리포트 ==========");

        // ========== 필터 입력 받기 ==========
        // ESG 등급 필터 (콤마로 구분해서 여러 개 입력 가능)
        List<String> grades = InputHelper.readCsvTokensUpper(sc, "ESG 등급 필터(A,B,... / Enter=전체)> ");
        
        // 지연 비율 필터 (% 단위로 입력받아서 0~1 비율로 변환)
        Double minPct = InputHelper.readDoubleOptional(sc, "지연 납품 비율 하한(%, Enter 가능)> ");
        Double maxPct = InputHelper.readDoubleOptional(sc, "지연 납품 비율 상한(%, Enter 가능)> ");

        Double minRatio = null;
        Double maxRatio = null;
        if (minPct != null) minRatio = minPct / 100.0;  // % -> 비율 변환
        if (maxPct != null) maxRatio = maxPct / 100.0;

        // ========== 공급업체 목록 조회 ==========
        try (Connection conn = db.openConnection()) {
            SupplierRepository repository = new SupplierRepository();
            List<SupplierRepository.SupplierRow> rows = repository.listSuppliers(conn, grades, minRatio, maxRatio);

            if (rows.isEmpty()) {
                System.out.println("[안내] 조건에 맞는 공급업체가 없습니다.");
                return;
            }

            // 표 형태로 출력
            System.out.println();
            System.out.printf("%-10s %-20s %-10s %-5s %-15s %-15s%n",
                    "SupplierID", "Name", "Country", "ESG", "TotalOrder(원)", "DelayRatio");
            System.out.println("--------------------------------------------------------------------------");
            for (SupplierRepository.SupplierRow r : rows) {
                // 지연 비율을 보기 좋게 포맷팅 (예: "33.3% (1/3)")
                String ratioStr = String.format("%.1f%% (%d/%d)", 
                        r.delayRatio() * 100.0, r.delayedDeliveries(), r.totalDeliveries());
                System.out.printf("%-10d %-20s %-10s %-5s %-15.0f %-15s%n",
                        r.supplierId(), cut(r.name(), 20), nvl(r.country()), nvl(r.esgGrade()), 
                        r.totalOrderAmount(), ratioStr);
            }

            // ========== 상세 보기 루프 ==========
            while (true) {
                String s = InputHelper.readLine(sc, "상세보기 SupplierID 입력(Enter=돌아가기)> ");
                if (s.isEmpty()) return;  // 빈 입력이면 메인 메뉴로
                if (!InputHelper.isAllDigits(s)) {
                    System.out.println("[안내] SupplierID는 정수입니다.");
                    continue;
                }
                int supplierId = Integer.parseInt(s);
                showSupplierDetail(conn, repository, sc, supplierId);
            }
        }
    }

    /**
     * 특정 공급업체의 최근 발주서 상세 보기 (페이징 지원)
     */
    private static void showSupplierDetail(Connection conn, SupplierRepository repository, 
                                          Scanner sc, int supplierId) throws Exception {
        // 한 페이지에 몇 개 보여줄지
        Integer nOpt = InputHelper.readIntOptional(sc, "최근 몇 개 발주서를 볼까요? (기본 5)> ");
        int pageSize = (nOpt == null || nOpt <= 0) ? 5 : nOpt;

        int page = 0;  // 현재 페이지 (0-indexed)
        
        while (true) {
            int offset = page * pageSize;  // SQL OFFSET 계산
            List<SupplierRepository.SupplierPoRow> pos = repository.recentPurchaseOrders(conn, supplierId, pageSize, offset);

            System.out.println();
            System.out.println("----- 공급업체 " + supplierId + " 최근 발주서 (page " + (page + 1) + ") -----");
            
            if (pos.isEmpty()) {
                System.out.println("(표시할 발주서가 없습니다)");
            } else {
                for (SupplierRepository.SupplierPoRow po : pos) {
                    // 지연 여부: Delivery.status='지연'인 게 있으면 Y
                    System.out.printf("POID=%d | %s | 상태=%s | 지연=%s%n",
                            po.poid(), nvl(po.orderDate()), nvl(po.status()), po.delayed() ? "Y" : "N");
                }
            }
            
            // 페이징 네비게이션
            System.out.print("[n]다음 [p]이전 [q]종료 > ");
            String cmd = sc.nextLine().trim().toLowerCase();
            
            if (cmd.equals("q") || cmd.isEmpty()) return;  // 종료
            if (cmd.equals("n")) {
                if (pos.isEmpty()) {
                    System.out.println("[안내] 다음 페이지가 없습니다.");
                } else {
                    page++;
                }
            } else if (cmd.equals("p")) {
                if (page == 0) System.out.println("[안내] 이미 첫 페이지입니다.");
                else page--;
            } else {
                System.out.println("[안내] n/p/q 중에서 선택하세요.");
            }
        }
    }

    /**
     * null이면 "-" 반환
     */
    private static String nvl(Object o) {
        return o == null ? "-" : String.valueOf(o);
    }

    /**
     * 문자열이 max보다 길면 잘라서 ... 붙임
     */
    private static String cut(String s, int max) {
        if (s == null) return "-";
        return s.length() <= max ? s : s.substring(0, Math.max(0, max - 1)) + "…";
    }
}
