package hw10.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.*;

/**
 * 로그 유틸리티 클래스
 * 
 * [과제 요구사항]
 * - 시작/종료 로그
 * - DB 접속 성공/실패 로그
 * - 트랜잭션 begin/commit/rollback 로그
 * - 주요 오류 메시지 로그
 * 
 * 콘솔 + 파일(logs/app.log) 둘 다 출력함.
 * java.util.logging 패키지 사용함 (Java 기본 제공 로그 라이브러리)
 */
public final class Logger {
    
    // Java 표준 로거 가져옴 (이름은 "scm"으로 설정)
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("scm");
    
    // 초기화 한번만 하려고 플래그 둠
    private static boolean initialized = false;

    // private 생성자: 이 클래스는 인스턴스 만들 필요 없음 (static 메서드만 사용)
    private Logger() {}

    /**
     * 로그 시스템 초기화
     * 프로그램 시작할 때 딱 한번 호출하면 됨
     */
    public static void init() {
        if (initialized) return;  // 이미 초기화됐으면 스킵
        initialized = true;

        // 부모 핸들러 끄기 (기본 콘솔 핸들러 중복 방지)
        LOGGER.setUseParentHandlers(false);
        LOGGER.setLevel(Level.INFO);

        // 1. 콘솔 로그 핸들러 추가
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(Level.INFO);
        ch.setFormatter(new SimpleFormatter());  // 간단한 포맷
        LOGGER.addHandler(ch);

        // 2. 파일 로그 핸들러 추가 (logs/app.log)
        try {
            // logs 폴더 없으면 만듦
            Files.createDirectories(Path.of("logs"));
            
            // true = append 모드 (기존 로그 뒤에 계속 추가)
            FileHandler fh = new FileHandler("logs/app.log", true);
            fh.setLevel(Level.INFO);
            fh.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fh);
        } catch (IOException e) {
            // 파일 로그 실패해도 프로그램 안 멈춤
            LOGGER.log(Level.WARNING, "파일 로그 초기화 실패: " + e.getMessage());
        }
    }

    /**
     * INFO 레벨 로그 (일반 정보)
     */
    public static void info(String msg) {
        LOGGER.info(msg);
    }

    /**
     * WARNING 레벨 로그 (경고)
     */
    public static void warn(String msg) {
        LOGGER.warning(msg);
    }

    /**
     * SEVERE 레벨 로그 (심각한 오류)
     * @param msg 메시지
     * @param t 예외 객체 (스택트레이스 같이 찍힘)
     */
    public static void error(String msg, Throwable t) {
        LOGGER.log(Level.SEVERE, msg, t);
    }
}
