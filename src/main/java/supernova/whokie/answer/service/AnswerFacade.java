package supernova.whokie.answer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import supernova.whokie.answer.service.dto.AnswerCommand;
import supernova.whokie.global.constants.MessageConstants;
import supernova.whokie.global.exception.InvalidEntityException;
import supernova.whokie.question.Question;
import supernova.whokie.question.service.QuestionReaderService;

@Service
@RequiredArgsConstructor
public class AnswerFacade {
    private final AnswerService answerService;
    private final QuestionReaderService questionReaderService;
    private final ApplicationEventPublisher eventPublisher;

    public void answerToCommonQuestion(Long userId, AnswerCommand.CommonAnswer command) {
        Question question = questionReaderService.getQuestionById(command.questionId());
        answerToQuestionAndNotify(userId, command.pickedId(), question);
    }

    public void answerToGroupQuestion(Long userId, AnswerCommand.Group command) {
        Question question = questionReaderService.getQuestionById(command.questionId());
        if(question.isNotCorrectGroupQuestion(command.groupId())) {
            throw new InvalidEntityException(MessageConstants.GROUP_NOT_FOUND_MESSAGE);
        }
        answerToQuestionAndNotify(userId, command.pickedId(), question);

    }

    private void answerToQuestionAndNotify(Long userId, Long pickedId, Question question) {
        answerService.answerToQuestion(userId, pickedId, question);

//        // Ranking Count 증가
//        Groups group = groupReaderService.getGroupById(question.getGroupId());
//        rankingWriterService.increaseRankingCountByUserAndQuestionAndGroups(pickedId, question.getContent(), group);
//
//        // 웹 알림 전송
//        AlarmEventDto.Alarm alarmEvent = AlarmEventDto.Alarm.toDto(pickedId, question.getContent());
//        eventPublisher.publishEvent(alarmEvent);
//
//        // 포인트 기록
//        PointRecordEventDto.Earn pointEvent = PointRecordEventDto.Earn.toDto(userId, AnswerConstants.ANSWER_POINT, 0,
//                PointRecordOption.EARN, PointConstants.POINT_EARN_MESSAGE);
//        eventPublisher.publishEvent(pointEvent);
    }
}
