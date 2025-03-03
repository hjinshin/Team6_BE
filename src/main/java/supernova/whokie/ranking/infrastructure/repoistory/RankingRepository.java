package supernova.whokie.ranking.infrastructure.repoistory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import supernova.whokie.group.Groups;
import supernova.whokie.ranking.Ranking;
import supernova.whokie.user.Users;

import java.util.List;
import java.util.Optional;

public interface RankingRepository extends JpaRepository<Ranking, Long> {

    List<Ranking> findTop3ByUsers_IdOrderByCountDesc(Long userId);

    @Query("SELECT r FROM Ranking r JOIN FETCH r.users WHERE r.groups.id = :groupId")
    List<Ranking> findAllByGroupIdFetchJoinUsers(@Param("groupId") Long groupId);

    Optional<Ranking> findByUsersAndQuestionAndGroups(Users users, String question, Groups groups);
}
