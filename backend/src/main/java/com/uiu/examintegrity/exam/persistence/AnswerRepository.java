package com.uiu.examintegrity.exam.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AnswerRepository extends JpaRepository<AnswerEntity, Long> {
    List<AnswerEntity> findBySessionId(Long sessionId);

    @Query("select a from AnswerEntity a left join fetch a.option where a.session.id = :sessionId")
    List<AnswerEntity> findWithOptionBySessionId(Long sessionId);

    Optional<AnswerEntity> findBySessionIdAndQuestionId(Long sessionId, Long questionId);

    @Query("""
            select a from AnswerEntity a join fetch a.question left join fetch a.option
             where a.session.exam.id = :examId
            """)
    List<AnswerEntity> findForExam(Long examId);

    @Query("""
            select a from AnswerEntity a join fetch a.question left join fetch a.option
             where a.session.id in :sessionIds
            """)
    List<AnswerEntity> findForSessions(Collection<Long> sessionIds);

    List<AnswerEntity> findByQuestionId(Long questionId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO answers (session_id, question_id, option_id, text_answer,
                                 first_seen_at, answered_at, updated_at)
            VALUES (:sessionId, :questionId, :optionId, :textAnswer, :at, :at, :at)
            ON CONFLICT (session_id, question_id) DO UPDATE
               SET option_id   = EXCLUDED.option_id,
                   text_answer = EXCLUDED.text_answer,
                   answered_at = EXCLUDED.answered_at,
                   updated_at  = EXCLUDED.updated_at
            """, nativeQuery = true)
    int upsert(Long sessionId, Long questionId, Long optionId, String textAnswer, Instant at);
}
