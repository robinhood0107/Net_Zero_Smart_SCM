package hw10.util;

import java.sql.SQLException;

/**
 * SQLException -> 사용자 친화적 메시지 변환기
 * 
 * [과제 요구사항]
 * 사용자에게 이해 가능한 에러 메시지를 출력하고,
 * 개발용 상세 내용은 로그로 남긴다.
 * 
 * PostgreSQL은 에러 발생하면 SQLSTATE 코드를 반환함.
 * 이 코드 보고 어떤 종류의 에러인지 판단 가능.
 * 
 * 참고: PostgreSQL SQLSTATE 코드
 * - 23503: foreign_key_violation (FK 위반)
 * - 23505: unique_violation (UNIQUE 위반)
 * - 23514: check_violation (CHECK 위반)
 * - 40001: serialization_failure (직렬화 실패)
 * - 40P01: deadlock_detected (교착상태)
 */
public final class ErrorHandler {
    
    // 인스턴스 생성 막음 (static 메서드만 사용)
    private ErrorHandler() {}

    /**
     * SQLException을 사용자가 이해할 수 있는 메시지로 변환
     * 
     * @param e SQLException 예외 객체
     * @return 사용자에게 보여줄 메시지
     */
    public static String toUserMessage(SQLException e) {
        String state = e.getSQLState();
        
        // SQLSTATE 없으면 기본 메시지
        if (state == null) return "DB 오류가 발생했습니다.";

        // switch expression (Java 14+)
        // cpp의 switch랑 비슷한데 -> 화살표로 쓰면 break 필요 없음
        return switch (state) {
            case "23503" -> "외래키 제약조건 위반입니다. (예: 존재하지 않는 ProjectID/SupplierID/PartID/WarehouseID)";
            case "23505" -> "중복 데이터(UNIQUE 제약조건 위반)입니다.";
            case "23514" -> "값 범위/상태 값(CHECK 제약조건)을 위반했습니다.";
            case "40P01" -> "DB 교착상태로 트랜잭션이 실패했습니다. 잠시 후 다시 시도하세요.";
            case "40001" -> "트랜잭션 충돌로 실패했습니다. 다시 시도하세요.";
            default -> "DB 처리 중 오류가 발생했습니다. (SQLSTATE=" + state + ")";
        };
    }
}
