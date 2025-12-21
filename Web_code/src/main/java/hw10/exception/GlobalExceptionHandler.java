package hw10.exception;

import hw10.util.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLException;

/**
 * 전역 예외 처리 핸들러.
 * API 호출 시 발생하는 예외를 가로채서 표준 응답 반환.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 잘못된 인자 예외 처리.
     * 400 Bad Request 반환.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        Logger.warn("잘못된 요청 파라미터: " + e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST", e.getMessage()));
    }

    /**
     * DB SQL 관련 예외 처리.
     * 500 Internal Server Error 반환.
     */
    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ErrorResponse> handleSQLException(SQLException e) {
        Logger.error("데이터베이스 오류 발생", e);

        String message = "데이터베이스 오류가 발생했습니다.";

        if (e.getMessage() != null && (e.getMessage().contains("롤백") ||
                e.getMessage().contains("Rollback") ||
                e.getMessage().contains("rollback") ||
                e.getMessage().contains("실패"))) {
            message = "트랜잭션 롤백 알림: " + e.getMessage();
        }

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("DATABASE_ERROR", message));
    }

    /**
     * 리소스 없음 예외 처리.
     * 정적 리소스(파비콘 등) 요청 실패 시 로그 남기지 않음.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException e) {
        String resourcePath = e.getResourcePath();

        if (resourcePath.equals("/favicon.ico") || resourcePath.endsWith(".ico")) {
            return ResponseEntity.notFound().build();
        }

        if (resourcePath.startsWith("/.well-known/") ||
                resourcePath.contains("chrome.devtools") ||
                resourcePath.contains("appspecific")) {
            return ResponseEntity.notFound().build();
        }

        if (resourcePath.equals("/robots.txt") ||
                resourcePath.equals("/sitemap.xml") ||
                resourcePath.startsWith("/apple-touch-icon")) {
            return ResponseEntity.notFound().build();
        }

        Logger.warn("정적 리소스를 찾을 수 없음: " + resourcePath);
        return ResponseEntity.notFound().build();
    }

    /**
     * 기타 모든 예외 처리.
     * 500 Internal Server Error 반환.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        Logger.error("서버 오류 발생", e);

        String message = "서버 오류가 발생했습니다.";
        if (e.getMessage() != null) {
            message = "API 오류 (트랜잭션 롤백됨): " + e.getMessage();
        }

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", message));
    }

    public record ErrorResponse(String code, String message) {
    }
}
