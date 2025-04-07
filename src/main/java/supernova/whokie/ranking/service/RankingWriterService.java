package supernova.whokie.ranking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import supernova.whokie.global.annotation.RedissonLock;
import supernova.whokie.group.Groups;
import supernova.whokie.group.infrastructure.repository.GroupRepository;
import supernova.whokie.ranking.Ranking;
import supernova.whokie.ranking.constants.RankingConstants;
import supernova.whokie.ranking.infrastructure.repoistory.RankingRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingWriterService {

    private final RankingRepository rankingRepository;
    private final GroupRepository groupRepository;

    @Transactional
    public void save(Ranking ranking) {
        rankingRepository.save(ranking);
    }

    @Transactional
    public Ranking createRanking(Long userId, String question, Groups groups) {
        Ranking ranking = Ranking.builder()
            .question(question)
            .count(RankingConstants.DEFAULT_RANKING_COUNT)
            .userId(userId)
            .groups(groups)
            .build();
        return rankingRepository.save(ranking);
    }

    @RedissonLock(value = "#userId+#content+#group.getId()")
    public void increaseRankingCountByUserAndQuestionAndGroups(Long userId, String content,
        Groups group) {

        Ranking ranking = rankingRepository.findByUserIdAndQuestionAndGroups(userId, content,
                group)
            .orElseGet(() -> createRanking(userId, content, group));

        ranking.increaseCount();

        rankingRepository.save(ranking);

    }
}
