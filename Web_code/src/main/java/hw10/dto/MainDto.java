package hw10.dto;

/**
 * 메인 페이지용 데이터 객체.
 */
public class MainDto {

    /**
     * 메인화면 요약 정보(카드 4개) 레코드.
     * 활성 프로젝트 수, 탄소 감축량, 지연 배송, ESG 등급 평균 포함.
     */
    public record MainSummary(
            int activeProjects,
            double carbonReduction,
            int delayedDeliveries,
            String avgEsgGrade) {
    }
}
