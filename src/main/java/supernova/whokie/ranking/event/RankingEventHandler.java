package supernova.whokie.ranking.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import supernova.whokie.group.Groups;
import supernova.whokie.group.service.GroupReaderService;
import supernova.whokie.ranking.service.RankingWriterService;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingEventHandler {

    private final RankingWriterService rankingWriterService;
    private final GroupReaderService groupReaderService;

    //    @Async
    @EventListener
    public void increaseRanking(RankingEventDto.Increase event) {
        Long pickedId = event.pickedId();
        String content = event.question().getContent();

        Groups group = groupReaderService.getGroupById(event.question().getGroupId());
        rankingWriterService.increaseRankingCountByUserAndQuestionAndGroups(pickedId,
            content, group);
    }

}
