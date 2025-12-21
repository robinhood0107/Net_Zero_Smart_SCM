package hw10.db;

import hw10.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DB 연결 관리 클래스
 * 
 * JDBC로 PostgreSQL 연결하는 거임.
 * cpp에서 libpq 쓰는 거랑 비슷한데, Java는 JDBC라는 표준 API 씀.
 * 
 * AutoCloseable 구현해서 try-with-resources 문법 쓸 수 있게 함.
 */
public final class DatabaseConnection implements AutoCloseable {
    
    private final DatabaseConfig config;

    /**
     * 생성자 - 설정 받아서 저장
     * 설정이 비어있으면 여기서 바로 예외 던짐
     */
    public DatabaseConnection(DatabaseConfig config) {
        this.config = config;
        
        // 설정 하나라도 없으면 예외
        if (config.dbUrl == null || config.dbUser == null || config.dbPassword == null) {
            throw new IllegalStateException("DB 설정이 비어 있습니다. 환경변수 또는 config.properties를 설정하세요.");
        }
    }

    /**
     * 새 DB 커넥션 열기
     * 
     * 호출할 때마다 새 Connection 객체 리턴함.
     * cpp에서 PQconnectdb() 호출하는 거랑 비슷함.
     * 
     * @return java.sql.Connection 객체 (SQL 실행할 때 필요)
     * @throws SQLException 연결 실패하면 예외 발생
     */
    public Connection openConnection() throws SQLException {
        // DriverManager.getConnection()이 실제로 DB에 연결하는 함수
        return DriverManager.getConnection(config.dbUrl, config.dbUser, config.dbPassword);
    }

    /**
     * AutoCloseable 인터페이스 구현
     * try-with-resources 블록 끝나면 자동 호출됨
     * 여기서는 딱히 할 거 없어서 비워둠
     */
    @Override
    public void close() {
        // 특별히 정리할 리소스 없음
    }
}
