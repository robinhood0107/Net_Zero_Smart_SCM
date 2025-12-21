package hw10;

import hw10.util.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 애플리케이션 시작 클래스.
 */
@SpringBootApplication
public class Application {

    /**
     * 메인 메서드.
     * 로거 초기화 및 스프링 부트 실행.
     */
    public static void main(String[] args) {

        // 로거 초기화
        Logger.init();
        Logger.info("애플리케이션 시작");

        // 스프링 애플리케이션 구동
        SpringApplication.run(Application.class, args);

        Logger.info("서버 시작 완료: http://localhost:8080");
    }
}
