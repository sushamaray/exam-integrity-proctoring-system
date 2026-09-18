package com.uiu.examintegrity.exam.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByStatus(ExamStatus status);

    List<Exam> findByCourseIdAndStatus(Long courseId, ExamStatus status);

    List<Exam> findByCourseIdOrderByStartsAtDesc(Long courseId);

    List<Exam> findByCourseIdInOrderByStartsAtDesc(Collection<Long> courseIds);

    @Query("""
            select e from Exam e join fetch e.course
             where e.course.id in :courseIds
               and e.status in :statuses
               and e.startsAt <= :now and e.endsAt > :now
             order by e.endsAt
            """)
    List<Exam> findOpen(Collection<Long> courseIds, Collection<ExamStatus> statuses, Instant now);
}
