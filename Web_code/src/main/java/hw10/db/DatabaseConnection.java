package hw10.db;

import hw10.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DB 연결 관리.
 * AutoCloseable 구현으로 리소스 자동 정리.
 */
public final class DatabaseConnection implements AutoCloseable {

    private final DatabaseConfig config;

    /**
     * 생성자.
     * 설정 검증 및 초기화.
     */
    public DatabaseConnection(DatabaseConfig config) {
        this.config = config;

        // 필수 설정 값 확인
        if (config.dbUrl == null || config.dbUser == null || config.dbPassword == null) {
            throw new IllegalStateException("DB 설정이 비어 있습니다. 환경변수 또는 config.properties를 설정하세요.");
        }
    }

    /**
     * DB 연결 생성.
     */
    public Connection openConnection() throws SQLException {

        return DriverManager.getConnection(config.dbUrl, config.dbUser, config.dbPassword);
    }

    public void close() {

    }
}
