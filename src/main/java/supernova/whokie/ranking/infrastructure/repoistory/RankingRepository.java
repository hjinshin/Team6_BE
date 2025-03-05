package supernova.whokie.ranking.infrastructure.repoistory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import supernova.whokie.group.Groups;
import supernova.whokie.ranking.Ranking;

import java.util.List;
import java.util.Optional;

public interface RankingRepository extends JpaRepository<Ranking, Long> {

    List<Ranking> findTop3ByUserIdOrderByCountDesc(Long userId);

    @Query("SELECT r FROM Ranking r WHERE r.groups.id = :groupId")
    List<Ranking> findAllByGroupIdFetchJoinUsers(@Param("groupId") Long groupId);

    Optional<Ranking> findByUserIdAndQuestionAndGroups(Long userId, String question, Groups groups);
}
