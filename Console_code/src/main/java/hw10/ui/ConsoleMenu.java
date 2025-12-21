package hw10.ui;

import hw10.db.DatabaseConnection;
import hw10.util.ErrorHandler;
import hw10.util.Logger;

import java.sql.SQLException;
import java.util.Scanner;

/**
 * 메인 메뉴 UI 클래스
 * 
 * 콘솔 기반 메뉴. 무한 루프 돌면서 사용자 입력 받음.
 * 1번 누르면 기능1, 2번 누르면 기능2, ... 이런 식.
 * 
 * [과제 요구사항]
 * 예외 발생해도 프로그램 안 죽음 -> try-catch로 잡아서 에러 메시지만 출력
 */
public final class ConsoleMenu {
    
    private final DatabaseConnection db;  // DB 연결 객체
    private final Scanner sc = new Scanner(System.in);  // 콘솔 입력용

    public ConsoleMenu(DatabaseConnection db) {
        this.db = db;
    }

    /**
     * 메뉴 무한 루프 시작
     */
    public void run() {
        while (true) {
            // 메뉴 출력
            System.out.println();
            System.out.println("========== 탄소중립 스마트 SCM (HW10) ==========");
            System.out.println("1) 프로젝트 대시보드 및 탄소·비용 리포트");
            System.out.println("2) 발주 등록(트랜잭션) + 초기 납품 + 재고 반영");
            System.out.println("3) 공급업체 ESG 및 지연 납품 리포트");
            System.out.println("0) 종료");
            System.out.print("선택> ");
            String choice = sc.nextLine().trim();

            try {
                // switch expression (Java 14+)
                // cpp switch랑 비슷한데 -> 쓰면 break 필요 없음
                switch (choice) {
                    case "1" -> ProjectDashboard.run(db, sc);    // 기능1 실행
                    case "2" -> OrderRegistration.run(db, sc);   // 기능2 실행
                    case "3" -> SupplierReport.run(db, sc);      // 기능3 실행
                    case "0" -> {
                        return;  // 루프 탈출 -> 프로그램 종료
                    }
                    default -> System.out.println("[안내] 0~3 중에서 선택하세요.");
                }
            } catch (SQLException e) {
                // [과제 요구사항] SQL 에러는 로그에 상세히, 사용자에겐 간단히
                Logger.error("SQL 오류", e);
                System.out.println("[오류] " + ErrorHandler.toUserMessage(e));
            } catch (Exception e) {
                // 그 외 에러도 마찬가지
                Logger.error("기능 실행 중 예외", e);
                System.out.println("[오류] 요청을 처리하지 못했습니다. 입력값/DB 상태를 확인하세요.");
            }
        }
    }
}
