package hw10.controller;

import hw10.dto.MainDto;
import hw10.service.MainService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;

/**
 * 메인 대시보드 API 컨트롤러.
 * /api/main 요청 처리.
 */
@RestController
@RequestMapping("/api/main")
public class MainController {

    private final MainService mainService;

    public MainController(MainService mainService) {
        this.mainService = mainService;
    }

    /**
     * 대시보드 상단 요약 정보(활성 프로젝트, 탄소 감축, 지연 등) 반환.
     */
    @GetMapping("/summary")
    public ResponseEntity<MainDto.MainSummary> getSummary() throws SQLException {
        MainDto.MainSummary summary = mainService.getSummary();
        return ResponseEntity.ok(summary);
    }
}
