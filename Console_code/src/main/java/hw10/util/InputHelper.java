package hw10.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * 콘솔 입력 헬퍼 클래스
 * 
 * 사용자 입력 받을 때 편하게 쓰려고 만든 유틸.
 * - 잘못된 입력(숫자 아닌데 숫자 달라고 한 경우 등) 알아서 다시 입력받음
 * - Optional 버전은 Enter 치면 null 반환 (필수 아닌 입력용)
 * 
 * cpp의 cin >> 이랑 비슷한데, 예외처리 같은 거 다 해놓은 버전이라고 보면 됨.
 */
public final class InputHelper {
    
    // 인스턴스 생성 막음
    private InputHelper() {}

    /**
     * 문자열 한 줄 입력받기
     * 
     * @param sc Scanner 객체 (System.in 읽는 녀석)
     * @param prompt 화면에 출력할 프롬프트 ("뭐시기 입력> " 같은 거)
     * @return 입력받은 문자열 (앞뒤 공백 제거됨)
     */
    public static String readLine(Scanner sc, String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();  // trim()은 앞뒤 공백 제거
    }

    /**
     * 정수 입력받기 (필수)
     * 숫자 아닌 거 입력하면 다시 입력받음
     * 
     * @param sc Scanner
     * @param prompt 프롬프트
     * @return 입력받은 정수
     */
    public static int readInt(Scanner sc, String prompt) {
        while (true) {
            String s = readLine(sc, prompt);
            try {
                return Integer.parseInt(s);  // 문자열 -> 정수 변환
            } catch (NumberFormatException e) {
                // 변환 실패하면 다시 입력받음
                System.out.println("[안내] 정수를 입력하세요.");
            }
        }
    }

    /**
     * 정수 입력받기 (선택)
     * Enter만 치면 null 반환
     * 
     * @param sc Scanner
     * @param prompt 프롬프트
     * @return 정수 또는 null
     */
    public static Integer readIntOptional(Scanner sc, String prompt) {
        while (true) {
            String s = readLine(sc, prompt);
            if (s.isEmpty()) return null;  // 빈 입력이면 null
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                System.out.println("[안내] 정수를 입력하거나 Enter로 건너뛰세요.");
            }
        }
    }

    /**
     * 실수(double) 입력받기 (선택)
     * Enter만 치면 null 반환
     * 
     * @param sc Scanner
     * @param prompt 프롬프트
     * @return 실수 또는 null
     */
    public static Double readDoubleOptional(Scanner sc, String prompt) {
        while (true) {
            String s = readLine(sc, prompt);
            if (s.isEmpty()) return null;
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                System.out.println("[안내] 숫자를 입력하거나 Enter로 건너뛰세요.");
            }
        }
    }

    /**
     * 콤마로 구분된 문자열 입력받아서 리스트로 반환
     * 예: "A,B,C" 입력하면 ["A", "B", "C"] 리턴
     * 전부 대문자로 변환됨
     * 
     * @param sc Scanner
     * @param prompt 프롬프트
     * @return 문자열 리스트 (빈 입력이면 빈 리스트)
     */
    public static List<String> readCsvTokensUpper(Scanner sc, String prompt) {
        String s = readLine(sc, prompt);
        List<String> out = new ArrayList<>();
        if (s.isEmpty()) return out;
        
        // 콤마로 쪼개서 하나씩 처리
        for (String t : s.split(",")) {
            String x = t.trim().toUpperCase();  // 공백 제거 + 대문자 변환
            if (!x.isEmpty()) out.add(x);
        }
        return out;
    }

    /**
     * 문자열이 숫자로만 이루어졌는지 체크
     * 
     * @param s 문자열
     * @return 숫자로만 되어있으면 true
     */
    public static boolean isAllDigits(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return true;
    }
}
