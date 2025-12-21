package hw10;

import hw10.config.DatabaseConfig;
import hw10.db.DatabaseConnection;
import hw10.ui.ConsoleMenu;
import hw10.util.Logger;

/**
 * 메인 클래스 - 프로그램 시작점
 * 
 * 걍 main 함수 있는 클래스임. cpp의 int main() 같은 거라고 보면 됨.
 * Java에서는 public static void main(String[] args) 이렇게 씀.
 */
public final class Application {
    
    public static void main(String[] args) {
        // 로그 시스템 초기화 (콘솔 + 파일 로그 둘 다 켜짐)
        Logger.init();
        
        // [과제 요구사항] 애플리케이션 시작 로그
        Logger.info("애플리케이션 시작");

        // DB 설정 로드 (환경변수 또는 config.properties에서 읽어옴)
        // 하드코딩 안 하고 외부에서 가져오는 거임 - 과제에서 요구한 거
        DatabaseConfig config = DatabaseConfig.load();
        
        // try-with-resources: Java에서 자원 자동 해제하는 문법
        // cpp의 RAII 패턴이랑 비슷함. 블록 끝나면 자동으로 close() 호출됨
        try (DatabaseConnection db = new DatabaseConnection(config)) {
            // [과제 요구사항] DB 접속 성공 로그
            Logger.info("DB 접속 성공");
            
            // 콘솔 메뉴 실행 (여기서 기능 1,2,3 다 돌아감)
            new ConsoleMenu(db).run();
            
        } catch (Exception e) {
            // [과제 요구사항] DB 접속 실패해도 프로그램 터지면 안 됨
            // 로그에는 상세 에러, 사용자한테는 간단한 메시지만 보여줌
            Logger.error("DB 접속 실패 또는 치명적 오류", e);
            System.out.println("[오류] 프로그램을 시작할 수 없습니다. DB 설정/연결을 확인하세요.");
            
        } finally {
            // [과제 요구사항] 애플리케이션 종료 로그
            Logger.info("애플리케이션 종료");
        }
    }
}
