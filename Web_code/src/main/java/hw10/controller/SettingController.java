package hw10.controller;

import hw10.service.SettingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 설정 화면 API 컨트롤러.
 * 시스템 로그 및 상태 정보 제공.
 */
@RestController
@RequestMapping("/api/settings")
public class SettingController {

    private final SettingService settingService;

    public SettingController(SettingService settingService) {
        this.settingService = settingService;
    }

    /**
     * 시스템 로그 조회 API.
     * 레벨(INFO/WARN 등) 및 검색어 필터링 가능.
     * limit 통한 개수 제한.
     */
    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> getLogs(
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String search) {
        try {
            Map<String, Object> result = settingService.getLogs(level, limit, search);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "로그 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 시스템 상태(DB 연결 등) 조회.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        try {
            Map<String, Object> status = settingService.getSystemStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "상태 조회 실패: " + e.getMessage()));
        }
    }

}
