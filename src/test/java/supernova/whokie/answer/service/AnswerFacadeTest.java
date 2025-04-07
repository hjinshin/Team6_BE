package supernova.whokie.answer.service;

import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import supernova.whokie.answer.infrastructure.repository.AnswerRepository;
import supernova.whokie.answer.service.dto.AnswerCommand;
import supernova.whokie.group.Groups;
import supernova.whokie.group.infrastructure.repository.GroupRepository;
import supernova.whokie.question.Question;
import supernova.whokie.question.QuestionStatus;
import supernova.whokie.question.infrastructure.repository.QuestionRepository;
import supernova.whokie.ranking.Ranking;
import supernova.whokie.ranking.infrastructure.repoistory.RankingRepository;
import supernova.whokie.user.Gender;
import supernova.whokie.user.Role;
import supernova.whokie.user.Users;
import supernova.whokie.user.infrastructure.repository.UserRepository;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@MockBean({S3Client.class, S3Template.class, S3Presigner.class, RedissonClient.class})
@TestPropertySource(properties = {
        "jwt.secret=abcd",
        "url.secret-key=abcd"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AnswerFacadeTest {

    @Autowired
    private AnswerFacade answerFacade;

    @Autowired
    RankingRepository rankingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    Users user;
    Groups group;
    Question question;

    @BeforeEach
    void setUp() {
        user = createUser();
        group = createGroup();
        question = createQuestion();
    }

    @Test
    @DisplayName("Answer To Question 동시성 테스트")
    void answerToQuestionConcurrentlyTest() throws InterruptedException {
        // given
        Long userId = user.getId();
        int point = user.getPoint();
        long answerCount = answerRepository.count();
        AnswerCommand.CommonAnswer command = AnswerCommand.CommonAnswer.builder().questionId(question.getId()).pickedId(userId).build();
        int threadCount = 5; // 스레드 개수
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    answerFacade.answerToCommonQuestion(userId, command);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        // then
        Users actual = userRepository.findById(userId).get();
        assertAll(
                () -> assertThat(actual.getPoint()).isEqualTo(point + threadCount * 5),
                () -> assertThat(answerRepository.count()).isEqualTo(answerCount + threadCount)
        );
    }

    private Users createUser() {
        Users user = Users.builder()
                .name("test")
                .email("test@gmail.com")
                .point(1000)
                .birthDate(LocalDate.now())
                .kakaoId(1L)
                .gender(Gender.M)
                .role(Role.USER)
                .build();

        return userRepository.save(user);
    }

    private Groups createGroup() {
        Groups group = Groups.builder()
                .groupName("test")
                .description("test")
                .groupImageUrl("test")
                .build();

        return groupRepository.save(group);
    }

    private Ranking createRanking(Users user, Groups group) {
        Ranking ranking = Ranking.builder()
                .question("test")
                .count(0)
                .userId(user.getId())
                .groups(group)
                .build();

        return rankingRepository.save(ranking);
    }

    private Question createQuestion() {
        Question question = Question.builder()
                .content("question")
                .questionStatus(QuestionStatus.APPROVED)
                .groupId(group.getId())
                .writer(user)
                .build();
        return questionRepository.save(question);
    }
}