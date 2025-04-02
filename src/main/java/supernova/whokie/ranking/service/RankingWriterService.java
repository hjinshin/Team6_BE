package supernova.whokie.ranking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import supernova.whokie.group.Groups;
import supernova.whokie.ranking.Ranking;
import supernova.whokie.ranking.constants.RankingConstants;
import supernova.whokie.ranking.infrastructure.repoistory.RankingRepository;

@Service
@RequiredArgsConstructor
public class RankingWriterService {
    private final RankingRepository rankingRepository;

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

    @Transactional
    public void increaseRankingCountByUserAndQuestionAndGroups(Long userId, String question, Groups group) {
        Ranking ranking = rankingRepository.findByUserIdAndQuestionAndGroups(userId, question, group)
                .orElseGet(() -> createRanking(userId, question, group));
        ranking.increaseCount();
    }
}
