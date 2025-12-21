package hw10.controller;

import hw10.dto.ProjectDto;
import hw10.service.ProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;

/**
 * 프로젝트 관련 API 컨트롤러.
 * 프로젝트 상세, 검색, 대시보드 통계 제공.
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /**
     * 프로젝트 상세 정보 조회. 미존재 시 404 반환.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDto.ProjectBasic> getProject(@PathVariable int id) throws SQLException {
        ProjectDto.ProjectBasic project = projectService.getProject(id);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(project);
    }

    /**
     * 프로젝트 검색 API. 페이징 지원.
     * limit: 페이지 당 개수, page: 페이지 번호 (0부터 시작).
     */
    @GetMapping("/search")
    public ResponseEntity<List<ProjectDto.ProjectSearchItem>> searchProjects(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) throws SQLException {
        List<ProjectDto.ProjectSearchItem> results = projectService.searchProjects(keyword, page, limit);
        return ResponseEntity.ok(results);
    }

    /**
     * 프로젝트 대시보드 통계 데이터 조회.
     * 탄소 배출, 집약도, 공급사 정보 등 제공.
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<ProjectDto.DashboardStats> getDashboardStats(@PathVariable int id) throws SQLException {
        ProjectDto.DashboardStats stats = projectService.getDashboardStats(id);
        return ResponseEntity.ok(stats);
    }
}
