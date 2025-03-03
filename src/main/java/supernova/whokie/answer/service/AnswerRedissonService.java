package supernova.whokie.answer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import supernova.whokie.answer.service.dto.AnswerCommand;
import supernova.whokie.global.annotation.RedissonLock;
import supernova.whokie.global.constants.MessageConstants;
import supernova.whokie.global.exception.InvalidEntityException;
import supernova.whokie.question.Question;
import supernova.whokie.question.service.QuestionReaderService;

@Service
@RequiredArgsConstructor
public class AnswerRedissonService {
    private final AnswerService answerService;
    private final QuestionReaderService questionReaderService;

    @RedissonLock(value = "#command.questionId() + ':' + #command.pickedId()")
    public void answerToCommonQuestion(Long userId, AnswerCommand.CommonAnswer command) {
        Question question = questionReaderService.getQuestionById(command.questionId());
        answerService.answerToQuestion(userId, command.pickedId(), question);
    }

    @RedissonLock(value = "#command.questionId() + ':' + #command.pickedId()")
    public void answerToGroupQuestion(Long userId, AnswerCommand.Group command) {
        Question question = questionReaderService.getQuestionById(command.questionId());
        if(question.isNotCorrectGroupQuestion(command.groupId())) {
            throw new InvalidEntityException(MessageConstants.GROUP_NOT_FOUND_MESSAGE);
        }

        answerService.answerToQuestion(userId, command.pickedId(), question);
    }
}
