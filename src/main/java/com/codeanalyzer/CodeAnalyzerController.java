package com.codeanalyzer;

import com.codeanalyzer.index.IndexLevel;
import com.codeanalyzer.index.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Paths;
import java.util.List;

/**
 * Spring Boot 服务封装（示例）
 */
@RestController
@RequestMapping("/api/code")
public class CodeAnalyzerController {
    private final CodeAnalyzerSystem codeAnalyzerSystem;

    @Autowired
    public CodeAnalyzerController(CodeAnalyzerSystem codeAnalyzerSystem) {
        this.codeAnalyzerSystem = codeAnalyzerSystem;
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeProject(@RequestParam String projectPath) {
        try {
            codeAnalyzerSystem.analyzeProject(Paths.get(projectPath));
            return ResponseEntity.ok("Project analyzed successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .body("Error analyzing project: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<SearchResult>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "ALL") IndexLevel level,
            @RequestParam(defaultValue = "10") int maxResults) {
        try {
            List<SearchResult> results = codeAnalyzerSystem.search(query, level, maxResults);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/search/relation")
    public ResponseEntity<List<SearchResult>> searchByRelation(
            @RequestParam String relationType,
            @RequestParam String target,
            @RequestParam(defaultValue = "10") int maxResults) {
        try {
            List<SearchResult> results = codeAnalyzerSystem.searchByRelation(relationType, target, maxResults);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/search/semantic")
    public ResponseEntity<List<SearchResult>> semanticSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int maxResults) {
        try {
            List<SearchResult> results = codeAnalyzerSystem.semanticSearch(query, maxResults);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
