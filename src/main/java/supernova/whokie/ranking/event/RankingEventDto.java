package supernova.whokie.ranking.event;

import lombok.Builder;
import supernova.whokie.question.Question;

public class RankingEventDto {

    @Builder
    public record Increase(
        Long pickedId,
        Question question
    ) {

        public static RankingEventDto.Increase toDto(Long pickedId, Question question) {
            return Increase.builder()
                .pickedId(pickedId)
                .question(question)
                .build();
        }
    }
}
