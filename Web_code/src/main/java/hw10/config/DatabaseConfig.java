package hw10.config;

import hw10.util.Logger;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * DB 접속 설정 로더
 * 
 * [과제 요구사항] DB 접속 정보를 하드코딩하지 말 것!
 * 그래서 환경변수 또는 config.properties 파일에서 읽어오게 만듦.
 * 
 * 우선순위:
 * 1. 환경변수 (DB_URL, DB_USER, DB_PASSWORD)
 * 2. config.properties 파일
 */
public final class DatabaseConfig {
    
    // DB 접속에 필요한 정보들 (final이라 한번 세팅되면 못 바꿈)
    public final String dbUrl;       // jdbc:postgresql://localhost:5432/scm_db 이런 형태
    public final String dbUser;      // DB 계정 이름
    public final String dbPassword;  // DB 비밀번호

    // private 생성자: 외부에서 new로 직접 못 만들게 막음
    // 대신 load() 메서드로만 생성 가능
    private DatabaseConfig(String dbUrl, String dbUser, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    /**
     * 설정 로드하는 메서드 (static이라 객체 없이 호출 가능)
     * cpp에서 static 함수랑 같음
     */
    public static DatabaseConfig load() {
        // 1순위: 환경변수에서 읽기 시도
        // System.getenv()는 OS 환경변수 가져오는 함수
        String envUrl = System.getenv("DB_URL");
        String envUser = System.getenv("DB_USER");
        String envPass = System.getenv("DB_PASSWORD");

        // 환경변수 다 있으면 그거 쓰고 리턴
        if (notBlank(envUrl) && notBlank(envUser) && envPass != null) {
            return new DatabaseConfig(envUrl, envUser, envPass);
        }

        // 2순위: config.properties 파일에서 읽기
        // Properties는 Java에서 key=value 형태 파일 읽는 클래스
        Properties p = new Properties();
        try (InputStream is = new FileInputStream("config.properties")) {
            p.load(is);  // 파일 읽어서 Properties에 로드
            
            String url = p.getProperty("db.url");
            String user = p.getProperty("db.user");
            String pass = p.getProperty("db.password");
            
            if (notBlank(url) && notBlank(user) && pass != null) {
                return new DatabaseConfig(url, user, pass);
            }
        } catch (Exception e) {
            // 파일 없거나 읽기 실패하면 경고만 남김
            Logger.warn("config.properties 로드 실패(환경변수 미설정 시 실행 불가): " + e.getMessage());
        }

        // 둘 다 없으면 null로 반환 (나중에 DatabaseConnection에서 예외 터짐)
        return new DatabaseConfig(null, null, null);
    }

    // 문자열이 비어있는지 체크하는 헬퍼 함수
    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
