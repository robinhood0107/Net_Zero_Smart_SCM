package hw10.service;

import hw10.config.DatabaseConfig;
import hw10.db.DatabaseConnection;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 시스템 설정 및 로그 관련 서비스.
 * 로그 파일 파싱 및 시스템 상태 체크.
 */
@Service
public class SettingService {

    private static final String LOG_FILE_PATH = "logs/app.log";
    private static final DateTimeFormatter LOG_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 로그 파일 필터링 및 검색 조회.
     * 대량 로그의 경우 limit 제한.
     */
    public Map<String, Object> getLogs(String level, int limit, String search) {
        List<Map<String, Object>> logs = new ArrayList<>();

        Path logPath = Paths.get(LOG_FILE_PATH);
        if (!Files.exists(logPath)) {
            return Map.of(
                    "logs", logs,
                    "total", 0,
                    "filtered", 0);
        }

        try {

            List<String> lines = Files.readAllLines(logPath);

            // 로그 정규식 패턴. 날짜, 레벨, 메시지 추출.
            Pattern logPattern = Pattern.compile(
                    "\\[(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\]" +
                            "\\s+" +
                            "\\[(INFO|WARNING|SEVERE)\\]" +
                            "\\s+" +
                            "(.+)");

            List<Map<String, Object>> parsedLogs = new ArrayList<>();
            Map<String, Object> currentLog = null;
            StringBuilder currentMessage = new StringBuilder();

            // 파일 라인별 로그 파싱.
            for (String line : lines) {
                Matcher matcher = logPattern.matcher(line);
                if (matcher.find()) {

                    // 이전 로그 저장 후 새 로그 시작.
                    if (currentLog != null) {
                        finalizeLogEntry(parsedLogs, currentLog, currentMessage, level, search);
                    }

                    currentLog = new HashMap<>();
                    currentLog.put("timestamp", matcher.group(1));
                    currentLog.put("level", matcher.group(2));

                    String msgStart = matcher.group(3).trim();
                    currentLog.put("source", extractSource(msgStart));

                    currentMessage = new StringBuilder(msgStart);
                } else {

                    // 멀티라인 로그 처리 (예: 스택트레이스)
                    if (currentLog != null) {
                        currentMessage.append("\n").append(line);
                    }
                }
            }

            if (currentLog != null) {
                finalizeLogEntry(parsedLogs, currentLog, currentMessage, level, search);
            }

            // 최신순 정렬.
            Collections.reverse(parsedLogs);

            // 개수 제한.
            if (parsedLogs.size() > limit) {
                parsedLogs = parsedLogs.subList(0, limit);
            }

            return Map.of(
                    "logs", parsedLogs,
                    "total", lines.size(),
                    "filtered", parsedLogs.size());
        } catch (IOException e) {

            return Map.of(
                    "logs", logs,
                    "total", 0,
                    "filtered", 0);
        }
    }

    private void finalizeLogEntry(List<Map<String, Object>> logs, Map<String, Object> log, StringBuilder messageBuffer,
            String levelFilter, String searchFilter) {
        String fullMessage = messageBuffer.toString();
        log.put("fullMessage", fullMessage);
        log.put("message", fullMessage.length() > 200 ? fullMessage.substring(0, 200) + "..." : fullMessage);

        String logLevel = (String) log.get("level");
        if (levelFilter != null && !levelFilter.isEmpty() && !logLevel.equalsIgnoreCase(levelFilter)) {
            return;
        }

        if (searchFilter != null && !searchFilter.isEmpty()) {
            if (!fullMessage.toLowerCase().contains(searchFilter.toLowerCase())) {
                return;
            }
        }

        logs.add(log);
    }

    // 메시지 내용을 통한 발생 위치 추론.
    private String extractSource(String message) {

        Pattern classPattern = Pattern
                .compile("(\\w+Service|\\w+Controller|\\w+Repository|\\w+Dao|\\w+Config|\\w+Handler)");
        Matcher matcher = classPattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }

        if (message.contains("TRANSACTION") || message.contains("트랜잭션")) {
            return "Transaction";
        }
        if (message.contains("DB") || message.contains("데이터베이스") || message.contains("Database")) {
            return "Database";
        }
        if (message.contains("Connection") || message.contains("연결")) {
            return "Connection";
        }

        return "System";
    }

    /**
     * DB 연결 상태 및 응답 시간 등 시스템 상태 확인.
     */
    public Map<String, Object> getSystemStatus() {
        boolean dbConnected = false;
        int activeConnections = 0;
        long queryLatency = 0;

        try {
            DatabaseConfig config = DatabaseConfig.load();
            if (config != null && config.dbUrl != null) {
                try (DatabaseConnection dbConn = new DatabaseConnection(config);
                        Connection conn = dbConn.openConnection()) {
                    if (conn != null) {
                        dbConnected = !conn.isClosed();
                        if (dbConnected) {

                            // 핑 쿼리 실행 및 응답 시간 측정.
                            long start = System.currentTimeMillis();
                            conn.createStatement().executeQuery("SELECT 1");
                            queryLatency = System.currentTimeMillis() - start;
                            activeConnections = 1;
                        }
                    }
                }
            }
        } catch (Exception e) {

        }

        return Map.of(
                "status", dbConnected ? "정상" : "오류",
                "dbConnected", dbConnected,
                "activeConnections", activeConnections,
                "queryLatency", queryLatency,
                "lastSync", LocalDateTime.now().format(LOG_DATE_FORMAT));
    }

}
