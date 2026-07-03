package com.codesentinel.repository;

import com.codesentinel.model.AnalysisReport;
import com.codesentinel.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AnalysisReportRepository extends JpaRepository<AnalysisReport, Long> {
    Optional<AnalysisReport> findBySubmissionId(Long submissionId);

    @Query("select r from AnalysisReport r join fetch r.submission s where s.user = :user order by r.createdAt desc")
    List<AnalysisReport> findByUser(@Param("user") User user);

    @Query("select coalesce(sum(r.totalBugs), 0) from AnalysisReport r")
    long sumAllBugs();

    @Query("select coalesce(sum(r.totalBugs), 0) from AnalysisReport r where r.submission.user = :user")
    long sumBugsByUser(@Param("user") User user);

    @Query("select coalesce(sum(r.criticalCount), 0) from AnalysisReport r where r.submission.user = :user")
    long sumCriticalByUser(@Param("user") User user);

    @Query("select coalesce(avg(r.complexityScore), 0) from AnalysisReport r where r.submission.user = :user")
    double avgComplexityByUser(@Param("user") User user);

    @Query("select r.riskLevel, count(r) from AnalysisReport r where r.submission.user = :user group by r.riskLevel")
    List<Object[]> riskDistribution(@Param("user") User user);

    @Query("select r from AnalysisReport r join fetch r.submission s where r.createdAt >= :since order by r.createdAt")
    List<AnalysisReport> findCreatedAfter(@Param("since") LocalDateTime since);

    @Query("select r.riskLevel, count(r) from AnalysisReport r group by r.riskLevel")
    List<Object[]> riskDistribution();
}
