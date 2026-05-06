package com.codesentinel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "bug_issues")
public class BugIssue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id")
    private AnalysisReport report;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    @Column(name = "issue_type", nullable = false, length = 80)
    private String issueType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Column(nullable = false, length = 700)
    private String description;

    @Column(nullable = false, length = 700)
    private String suggestion;

    @Column(name = "code_snippet", columnDefinition = "TEXT")
    private String codeSnippet;
}
