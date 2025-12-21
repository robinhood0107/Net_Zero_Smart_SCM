package hw10.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.*;

/**
 * 로깅 유틸리티.
 * 콘솔 및 파일 로그 동시 출력 구성.
 */
public final class Logger {

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("scm");

    private static boolean initialized = false;

    private Logger() {
    }

    /**
     * 로거 초기화.
     * 포맷터 설정 및 핸들러 등록.
     */
    public static void init() {
        if (initialized)
            return;
        initialized = true;

        LOGGER.setUseParentHandlers(false);
        LOGGER.setLevel(Level.INFO);

        Formatter customFormatter = new Formatter() {
            @Override
            public String format(LogRecord record) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("[%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS] ", record.getMillis()));
                sb.append(String.format("[%s] ", record.getLevel().getName()));
                sb.append(record.getMessage());
                sb.append(System.lineSeparator());

                if (record.getThrown() != null) {
                    Throwable t = record.getThrown();
                    sb.append("  예외: ").append(t.getClass().getSimpleName());
                    if (t.getMessage() != null) {
                        sb.append(" - ").append(t.getMessage());
                    }
                    sb.append(System.lineSeparator());

                    StackTraceElement[] stack = t.getStackTrace();
                    if (stack.length > 0) {
                        sb.append("  위치: ").append(stack[0].toString());
                        sb.append(System.lineSeparator());
                    }
                }

                return sb.toString();
            }
        };

        // 콘솔 핸들러 설정
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(Level.INFO);
        ch.setFormatter(customFormatter);
        LOGGER.addHandler(ch);

        try {

            Files.createDirectories(Path.of("logs"));

            // 파일 핸들러 설정 (이어쓰기 모드)
            FileHandler fh = new FileHandler("logs/app.log", true);
            fh.setLevel(Level.INFO);
            fh.setFormatter(customFormatter);
            LOGGER.addHandler(fh);
        } catch (IOException e) {

            LOGGER.log(Level.WARNING, "파일 로그 초기화 실패: " + e.getMessage());
        }
    }

    public static void info(String msg) {
        LOGGER.info(msg);
    }

    public static void warn(String msg) {
        LOGGER.warning(msg);
    }

    public static void error(String msg, Throwable t) {
        LOGGER.log(Level.SEVERE, msg, t);
    }
}
