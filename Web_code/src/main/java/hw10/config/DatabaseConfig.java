package hw10.config;

import hw10.util.Logger;

import java.io.InputStream;
import java.util.Properties;

/**
 * 데이터베이스 설정 관리.
 */
public final class DatabaseConfig {

    public final String dbUrl;
    public final String dbUser;
    public final String dbPassword;

    private DatabaseConfig(String dbUrl, String dbUser, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    /**
     * DB 설정 로드.
     * 환경변수 -> application.properties -> config.properties 순서로 확인.
     */
    public static DatabaseConfig load() {

        String envUrl = System.getenv("DB_URL");
        String envUser = System.getenv("DB_USER");
        String envPass = System.getenv("DB_PASSWORD");

        // 환경변수 확인
        if (notBlank(envUrl) && notBlank(envUser) && envPass != null) {
            return new DatabaseConfig(envUrl, envUser, envPass);
        }

        // application.properties 확인
        Properties appProps = loadPropertiesFromClasspath("application.properties");
        if (appProps != null) {
            String url = appProps.getProperty("spring.datasource.url");
            String user = appProps.getProperty("spring.datasource.username");
            String pass = appProps.getProperty("spring.datasource.password");

            if (notBlank(url) && notBlank(user) && pass != null) {
                return new DatabaseConfig(url, user, pass);
            }
        }

        // 레거시 config.properties 확인 (클래스패스)
        Properties legacyProps = loadPropertiesFromClasspath("config.properties");
        if (legacyProps != null) {
            String url = legacyProps.getProperty("db.url");
            String user = legacyProps.getProperty("db.user");
            String pass = legacyProps.getProperty("db.password");

            if (notBlank(url) && notBlank(user) && pass != null) {
                return new DatabaseConfig(url, user, pass);
            }
        }

        // 레거시 config.properties 확인 (현재 디렉토리)
        try {
            java.io.FileInputStream fis = new java.io.FileInputStream("config.properties");
            Properties p = new Properties();
            p.load(fis);
            fis.close();

            String url = p.getProperty("db.url");
            String user = p.getProperty("db.user");
            String pass = p.getProperty("db.password");

            if (notBlank(url) && notBlank(user) && pass != null) {
                return new DatabaseConfig(url, user, pass);
            }
        } catch (Exception e) {
            Logger.warn("config.properties 로드 실패: " + e.getMessage());
        }

        return new DatabaseConfig(null, null, null);
    }

    private static Properties loadPropertiesFromClasspath(String filename) {
        try (InputStream is = DatabaseConfig.class.getClassLoader().getResourceAsStream(filename)) {
            if (is != null) {
                Properties p = new Properties();
                p.load(is);
                return p;
            }
        } catch (Exception e) {
            // 파일 없으면 null 반환 (정상)
        }
        return null;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
