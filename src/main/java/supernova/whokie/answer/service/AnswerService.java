package supernova.whokie.answer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import supernova.whokie.answer.Answer;
import supernova.whokie.answer.constants.AnswerConstants;
import supernova.whokie.answer.service.dto.AnswerCommand;
import supernova.whokie.answer.service.dto.AnswerModel;
import supernova.whokie.friend.Friend;
import supernova.whokie.friend.service.FriendReaderService;
import supernova.whokie.global.constants.MessageConstants;
import supernova.whokie.global.exception.InvalidEntityException;
import supernova.whokie.group.service.GroupReaderService;
import supernova.whokie.pointrecord.PointRecordOption;
import supernova.whokie.pointrecord.constants.PointConstants;
import supernova.whokie.pointrecord.event.PointRecordEventDto;
import supernova.whokie.question.Question;
import supernova.whokie.question.service.QuestionReaderService;
import supernova.whokie.ranking.service.RankingWriterService;
import supernova.whokie.s3.service.S3Service;
import supernova.whokie.user.Users;
import supernova.whokie.user.service.UserReaderService;
import supernova.whokie.user.service.UserService;
import supernova.whokie.user.service.dto.UserModel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private final ApplicationEventPublisher eventPublisher;
    private final UserReaderService userReaderService;
    private final AnswerReaderService answerReaderService;
    private final QuestionReaderService questionReaderService;
    private final GroupReaderService groupReaderService;
    private final AnswerWriterService answerWriterService;
    private final FriendReaderService friendReaderService;
    private final S3Service s3Service;
    private final RankingWriterService rankingWriterService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public Page<AnswerModel.Record> getAnswerRecord(Pageable pageable, Long userId, LocalDate date) {
        LocalDateTime startDate;
        LocalDateTime endDate;

        if (date == null) {
            startDate = AnswerConstants.DEFAULT_START_DATE;
            endDate = LocalDateTime.now();
        } else {
            startDate = date.withDayOfMonth(1).atStartOfDay();
            endDate = date.withDayOfMonth(date.lengthOfMonth()).atTime(LocalTime.MAX);
        }

        // 지정된 기간 내의 데이터를 조회
        Page<Answer> answers = answerReaderService.getAnswerList(pageable, userId, startDate, endDate);
        return answers.map(AnswerModel.Record::from);
    }

    @Transactional(readOnly = true)
    public AnswerModel.RecordDays getAnswerRecordDays(Long userId, LocalDate date) {
        LocalDateTime startDate = date.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endDate = date.withDayOfMonth(date.lengthOfMonth()).atTime(LocalTime.MAX);

        List<Integer> answerRecordDays = answerReaderService.getAnswerRecordDays(userId, startDate, endDate);

        return AnswerModel.RecordDays.from(answerRecordDays);
    }

    @Transactional(readOnly = true)
    public AnswerModel.Refresh refreshAnswerList(Long userId) {
        Users user = userReaderService.getUserById(userId);

        List<Friend> allFriends = friendReaderService.getAllByHostUser(user);

        List<UserModel.PickedInfo> friendsInfoList = allFriends.stream()
                .map(friend -> {
                    String imageUrl = friend.getFriendUser().getImageUrl();
                    if (user.isImageUrlStoredInS3()) {
                        imageUrl = s3Service.getSignedUrl(imageUrl);
                    }
                    return UserModel.PickedInfo.from(friend.getFriendUser(), imageUrl);
                }).toList();

        return AnswerModel.Refresh.from(friendsInfoList);
    }

    @Transactional
    public void purchaseHint(Long userId, AnswerCommand.Purchase command) {
        Users user = userReaderService.getUserById(userId);
        Answer answer = answerReaderService.getAnswerById(command.answerId());

        if (answer.isNotPicked(userId)){
            throw new InvalidEntityException(MessageConstants.NOT_PICKED_USER_MESSAGE);
        }

        //포인트 감소
        int decreasedPoint = user.decreasePointsByHintCount(answer.getHintCount());

        answer.increaseHintCount();

        // 포인트 기록
        PointRecordEventDto.Earn pointEvent = PointRecordEventDto.Earn.toDto(userId, decreasedPoint, decreasedPoint,
                PointRecordOption.USED, PointConstants.POINT_USE_MESSAGE);
        eventPublisher.publishEvent(pointEvent);
    }

    @Transactional(readOnly = true)
    public List<AnswerModel.Hint> getHints(Long userId, Long answerId) {
        Answer answer = answerReaderService.getAnswerById(answerId);
        Users picker = userReaderService.getUserById(answer.getPickerId());

        if (answer.isNotPicked(userId)){
            throw new InvalidEntityException(MessageConstants.NOT_PICKED_USER_MESSAGE);
        }

        List<AnswerModel.Hint> allHints = new ArrayList<>();

        for (int i = 1; i <= AnswerConstants.MAX_HINT_COUNT; i++) {
            boolean valid = (i <= answer.getHintCount());
            allHints.add(AnswerModel.Hint.from(answer, i, valid, picker));
        }

        return allHints;
    }

    @Retryable(
            retryFor = {ObjectOptimisticLockingFailureException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 100))
    @Transactional
    public void answerToQuestion(Long userId, Long pickedId, Question question) {
        Answer answer = Answer.create(question, userId, pickedId, AnswerConstants.DEFAULT_HINT_COUNT);
        answerWriterService.save(answer);

        // Users Point 증가
        userService.increasePoint(userId);
    }
}
