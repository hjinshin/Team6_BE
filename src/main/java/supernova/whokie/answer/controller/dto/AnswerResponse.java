package supernova.whokie.answer.controller.dto;

import lombok.Builder;
import supernova.whokie.answer.Answer;
import supernova.whokie.answer.service.dto.AnswerModel;
import supernova.whokie.user.controller.dto.UserResponse;
import supernova.whokie.user.service.dto.UserModel;

import java.time.LocalDate;
import java.util.List;

public class AnswerResponse {

    @Builder
    public record Refresh(
            List<UserModel.PickedInfo> users
    ) {
        public static Refresh from(AnswerModel.Refresh refresh){
            return Refresh.builder()
                    .users(refresh.users())
                    .build();
        }

    }

    @Builder
    public record Record(
            Long answerId,
            Long questionId,
            String questionContent,
            int hintCount,
            LocalDate createdAt
    ) {
        public static Record from(Answer answer) {
            return new Record(
                    answer.getId(),
                    answer.getQuestion().getId(),
                    answer.getQuestion().getContent(),
                    answer.getHintCount(),
                    answer.getCreatedAt().toLocalDate()
            );
        }
    }

    @Builder
    public record Hints(
            List<Hint> hints
    ) {

    }

    @Builder
    public record Hint(
            int hintNum,
            Boolean valid,
            String content
    ) {

    }
}
