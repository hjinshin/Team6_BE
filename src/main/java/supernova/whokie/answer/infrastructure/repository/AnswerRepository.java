package supernova.whokie.answer.infrastructure.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import supernova.whokie.answer.Answer;

import java.time.LocalDateTime;
import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    @Query("SELECT a FROM Answer a JOIN FETCH a.question q WHERE a.pickedId = :pickedId AND a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    Page<Answer> findByPickedIdAndCreatedAtBetweenOrderByCreatedAtDesc(Pageable pageable, Long pickedId, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT DISTINCT FUNCTION('day', p.createdAt) FROM Answer p WHERE p.pickedId = :userId AND p.createdAt BETWEEN :startDate AND :endDate")
    List<Integer> findDistinctDaysWithCreatedAtBetween(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}