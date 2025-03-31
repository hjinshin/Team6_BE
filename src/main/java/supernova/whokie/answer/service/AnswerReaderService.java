package supernova.whokie.answer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import supernova.whokie.answer.Answer;
import supernova.whokie.answer.infrastructure.repository.AnswerRepository;
import supernova.whokie.global.constants.MessageConstants;
import supernova.whokie.global.exception.EntityNotFoundException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnswerReaderService {

    private final AnswerRepository answerRepository;

    @Transactional(readOnly = true)
    public Page<Answer> getAnswerList(Pageable pageable, Long userId, LocalDateTime startDate,
                                      LocalDateTime endDate) {
        return answerRepository.findByPickedIdAndCreatedAtBetweenOrderByCreatedAtDesc(pageable, userId, startDate,
                endDate);
    }

    @Transactional(readOnly = true)
    public List<Integer> getAnswerRecordDays(Long userId, LocalDateTime startDate, LocalDateTime endDate){
        return answerRepository.findDistinctDaysWithCreatedAtBetween(userId, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public Answer getAnswerById(Long answerId) {
        return answerRepository.findById(answerId).orElseThrow(() -> new EntityNotFoundException(
                MessageConstants.ANSWER_NOT_FOUND_MESSAGE));
    }
}
