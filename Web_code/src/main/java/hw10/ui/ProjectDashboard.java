package hw10.ui;

import hw10.dao.ProjectRepository;
import hw10.db.DatabaseConnection;
import hw10.util.InputHelper;

import java.sql.Connection;
import java.util.List;
import java.util.Scanner;

/**
 * [기능 1] 프로젝트 대시보드 및 탄소·비용 리포트
 * 
 * [과제 요구사항]
 * 1. 프로젝트ID 또는 선박명 일부 입력받음
 * 2. 프로젝트 기본 정보 출력 (ID, 선박명, 선종, 계약일, 인도예정일, 상태)
 * 3. 비용 정보: 총 발주 금액, 공급업체별 상위 3개
 * 4. 탄소 정보: 운송/보관/전체 탄소배출 합
 * 5. 지표: 탄소 집약도 계산
 * 6. 결과 없으면 안내 메시지
 * 7. 모든 계산은 DB SELECT로 (집계 함수, GROUP BY, JOIN 사용)
 */
public final class ProjectDashboard {
    
    // 인스턴스 생성 막음
    private ProjectDashboard() {}

    /**
     * 기능1 메인 실행 메서드
     * 
     * @param db DB 연결 객체
     * @param sc Scanner (콘솔 입력)
     */
    public static void run(DatabaseConnection db, Scanner sc) throws Exception {
        // 사용자한테 프로젝트ID 또는 선박명 입력받음
        String key = InputHelper.readLine(sc, "프로젝트ID(pid) 또는 선박명 일부를 입력> ");
        if (key.isEmpty()) {
            System.out.println("[안내] 입력이 비었습니다.");
            return;
        }

        // DB 커넥션 열기 (try-with-resources: 블록 끝나면 자동 close)
        try (Connection conn = db.openConnection()) {
            ProjectRepository repository = new ProjectRepository();
            Integer projectId = null;

            // 입력값이 숫자인지 체크해서 분기
            if (InputHelper.isAllDigits(key)) {
                // 숫자면 바로 ProjectID로 사용
                projectId = Integer.parseInt(key);
            } else {
                // 문자열이면 선박명으로 검색 (ILIKE 사용)
                List<ProjectRepository.ProjectSearchItem> items = repository.searchProjectsByShipName(conn, key, 10);
                if (items.isEmpty()) {
                    System.out.println("[안내] 해당 선박명으로 프로젝트를 찾지 못했습니다.");
                    return;
                }
                // 검색 결과 보여주기
                System.out.println();
                System.out.println("검색 결과(최대 10개):");
                for (ProjectRepository.ProjectSearchItem it : items) {
                    System.out.printf("- PID=%d | %s | %s | %s%n",
                            it.projectId(), it.shipName(), nvl(it.shipType()), nvl(it.status()));
                }
                // 사용자가 직접 ProjectID 선택
                projectId = InputHelper.readInt(sc, "조회할 ProjectID 입력> ");
            }

            // ========== 프로젝트 기본 정보 조회 ==========
            ProjectRepository.ProjectBasic basic = repository.findProjectById(conn, projectId);
            if (basic == null) {
                System.out.println("[안내] 해당 ProjectID의 프로젝트가 없습니다.");
                return;
            }

            // ========== 비용 정보 조회 ==========
            // [과제 요구사항] JOIN + SUM 집계 사용
            double totalCost = repository.totalOrderAmount(conn, projectId);
            
            // [과제 요구사항] GROUP BY + ORDER BY + LIMIT 사용
            List<ProjectRepository.SupplierAmount> top3 = repository.topSuppliersByAmount(conn, projectId, 3);

            // ========== 탄소 정보 조회 ==========
            double transport = repository.emissionSumByType(conn, projectId, "운송");
            double storage = repository.emissionSumByType(conn, projectId, "보관");
            double totalEmission = repository.emissionSumTotal(conn, projectId);

            // ========== 결과 출력 ==========
            System.out.println();
            System.out.println("========== 프로젝트 대시보드 ==========");
            
            // 기본 정보
            System.out.println("[프로젝트 기본 정보]");
            System.out.printf("프로젝트ID: %d%n", basic.projectId());
            System.out.printf("선박명: %s%n", basic.shipName());
            System.out.printf("선종: %s%n", nvl(basic.shipType()));
            System.out.printf("계약일: %s%n", nvl(basic.contractDate()));
            System.out.printf("인도예정일: %s%n", nvl(basic.deliveryDueDate()));
            System.out.printf("상태: %s%n", nvl(basic.status()));

            // 비용 정보
            System.out.println();
            System.out.println("[비용 정보]");
            System.out.printf("총 발주 금액: %.0f 원%n", totalCost);
            System.out.println("공급업체별 발주 금액 Top 3:");
            if (top3.isEmpty()) {
                System.out.println("- (발주 내역 없음)");
            } else {
                for (int i = 0; i < top3.size(); i++) {
                    ProjectRepository.SupplierAmount s = top3.get(i);
                    System.out.printf("%d) SupplierID=%d | %s | %.0f 원%n", i + 1, s.supplierId(), s.name(), s.amount());
                }
            }

            // 탄소 정보
            System.out.println();
            System.out.println("[탄소 정보]");
            System.out.printf("운송 탄소배출 합: %.3f kgCO2e%n", transport);
            System.out.printf("보관 탄소배출 합: %.3f kgCO2e%n", storage);
            System.out.printf("프로젝트 전체 탄소배출 합: %.3f kgCO2e%n", totalEmission);

            // [과제 요구사항] 지표 1개 이상 정의 및 계산
            System.out.println();
            System.out.println("[지표]");
            if (totalCost <= 0) {
                System.out.println("탄소 집약도(kgCO2e/백만원): N/A (총 발주 금액 0)");
            } else {
                // 탄소 집약도 = 전체 탄소배출량 / (총비용/백만)
                double intensity = totalEmission / (totalCost / 1_000_000.0);
                System.out.printf("탄소 집약도(kgCO2e/백만원): %.3f%n", intensity);
            }
        }
    }

    /**
     * null이면 "-" 반환하는 헬퍼 함수
     * 출력할 때 null 대신 보기 좋게 하려고
     */
    private static String nvl(Object o) {
        return o == null ? "-" : String.valueOf(o);
    }
}
