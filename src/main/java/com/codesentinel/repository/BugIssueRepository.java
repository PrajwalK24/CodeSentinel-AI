package com.codesentinel.repository;

import com.codesentinel.model.BugIssue;
import com.codesentinel.model.Severity;
import com.codesentinel.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BugIssueRepository extends JpaRepository<BugIssue, Long> {
    List<BugIssue> findByReportIdOrderByLineNumberAsc(Long reportId);

    @Query("select i.severity, count(i) from BugIssue i where i.report.submission.user = :user group by i.severity")
    List<Object[]> severityDistributionByUser(@Param("user") User user);

    @Query("select i.issueType, count(i) from BugIssue i where i.report.submission.user = :user group by i.issueType order by count(i) desc")
    List<Object[]> issueTypeDistributionByUser(@Param("user") User user);

    @Query("select i.severity, count(i) from BugIssue i group by i.severity")
    List<Object[]> severityDistribution();

    @Query("select i.issueType, count(i) from BugIssue i group by i.issueType order by count(i) desc")
    List<Object[]> issueTypeDistribution();

    long countBySeverity(Severity severity);
}
