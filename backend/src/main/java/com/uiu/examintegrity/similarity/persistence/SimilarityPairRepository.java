package com.uiu.examintegrity.similarity.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SimilarityPairRepository extends JpaRepository<SimilarityPair, Long> {
    List<SimilarityPair> findByExamIdOrderByScoreDesc(Long examId);

    List<SimilarityPair> findByExamIdAndScoreGreaterThanEqualOrderByScoreDesc(
            Long examId, BigDecimal threshold);

    @Query("""
            select p from SimilarityPair p
              join fetch p.sessionA sa join fetch sa.user
              join fetch p.sessionB sb join fetch sb.user
              left join fetch p.question
             where p.exam.id = :examId
             order by p.score desc
            """)
    List<SimilarityPair> findGraph(Long examId);

    @Query("""
            select p from SimilarityPair p
              join fetch p.exam e join fetch e.course
              join fetch p.sessionA sa join fetch sa.user
              join fetch p.sessionB sb join fetch sb.user
              left join fetch p.question
             where p.id = :id
            """)
    Optional<SimilarityPair> findDetail(Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from SimilarityPair p where p.exam.id = :examId")
    int deleteByExamId(Long examId);
}
