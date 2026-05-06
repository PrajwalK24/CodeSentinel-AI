package com.codesentinel.repository;

import com.codesentinel.model.CodeSubmission;
import com.codesentinel.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CodeSubmissionRepository extends JpaRepository<CodeSubmission, Long> {
    List<CodeSubmission> findTop8ByUserOrderBySubmittedAtDesc(User user);
    List<CodeSubmission> findByUserOrderBySubmittedAtDesc(User user);
    List<CodeSubmission> findTop20ByOrderBySubmittedAtDesc();
    @Query("select s from CodeSubmission s left join fetch s.report where s.user = :user order by s.submittedAt desc")
    List<CodeSubmission> findByUserWithReport(@Param("user") User user);

    @Query("select s from CodeSubmission s join fetch s.user left join fetch s.report order by s.submittedAt desc")
    List<CodeSubmission> findAllWithUserAndReport();

    long countByUser(User user);
    long countBySubmittedAtAfter(LocalDateTime since);

    @Query("select s.language, count(s) from CodeSubmission s group by s.language")
    List<Object[]> countByLanguage();
}
