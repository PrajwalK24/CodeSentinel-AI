package com.codesentinel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "analysis_reports")
public class AnalysisReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id")
    private CodeSubmission submission;

    @Column(name = "total_bugs", nullable = false)
    private int totalBugs;

    @Column(name = "critical_count", nullable = false)
    private int criticalCount;

    @Column(name = "major_count", nullable = false)
    private int majorCount;

    @Column(name = "minor_count", nullable = false)
    private int minorCount;

    @Column(name = "complexity_score", nullable = false)
    private int complexityScore;

    @Column(name = "time_complexity", length = 40)
    private String timeComplexity = "O(1)";

    @Column(name = "space_complexity", length = 40)
    private String spaceComplexity = "O(1)";

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Column(name = "analysis_duration_ms", nullable = false)
    private long analysisDurationMs;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber ASC")
    private List<BugIssue> issues = new ArrayList<>();
}
