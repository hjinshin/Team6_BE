package supernova.whokie.redis.service;

import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import supernova.config.EmbeddedRedisConfig;
import supernova.whokie.answer.service.AnswerRedissonService;
import supernova.whokie.answer.service.dto.AnswerCommand;
import supernova.whokie.group.Groups;
import supernova.whokie.group.infrastructure.repository.GroupRepository;
import supernova.whokie.question.Question;
import supernova.whokie.question.QuestionStatus;
import supernova.whokie.question.infrastructure.repository.QuestionRepository;
import supernova.whokie.ranking.Ranking;
import supernova.whokie.ranking.infrastructure.repoistory.RankingRepository;
import supernova.whokie.redis.entity.RedisVisitCount;
import supernova.whokie.redis.infrastructure.repository.RedisVisitCountRepository;
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
@MockBean({S3Client.class, S3Template.class, S3Presigner.class})
@TestPropertySource(properties = {
    "jwt.secret=abcd",
    "url.secret-key=abcd"
})
@Import(EmbeddedRedisConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class RaceConditionTest {

    @Autowired
    private RedisVisitCountRepository redisVisitCountRepository;

    @Autowired
    private RedisVisitService redisVisitService;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerRedissonService answerRedissonService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private RankingRepository rankingRepository;

    @Autowired
    private RedissonClient redissonClient;

    Users user;
    Groups group;
    Question question;

    @BeforeEach
    void setUp() {
        redissonClient.getKeys().flushall();
        user = createUser();
        group = createGroup();
        question = createQuestion(user, group);
    }

    @Test
    @DisplayName("동시 방문자 수 증가 테스트")
    void visitProfileConcurrentlyTest() throws InterruptedException {
        // given
        RedisVisitCount redisVisitCount = createVisitCount();
        Long hostId = redisVisitCount.getHostId();
        String visitorIp = "visitorIp";
        int oldDailyVisited = redisVisitCount.getDailyVisited();
        int oldTotalVisited = redisVisitCount.getTotalVisited();

        int threadCount = 100; // 스레드 개수
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            int finalI = i;
            executorService.submit(() -> {
                try {
                    redisVisitService.visitProfile(hostId, visitorIp + finalI);
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
        RedisVisitCount actual = redisVisitCountRepository.findById(hostId).orElseThrow();

        assertAll(
            () -> assertThat(actual.getDailyVisited()).isEqualTo(oldDailyVisited + threadCount),
            () -> assertThat(actual.getTotalVisited()).isEqualTo(oldTotalVisited + threadCount)
        );
    }

    @Test
    @DisplayName("동시 응답 RedissonLock 테스트")
    void AnswerCountConcurrentlyTest() throws InterruptedException {
        // given
        AnswerCommand.CommonAnswer command = AnswerCommand.CommonAnswer.builder().questionId(question.getId()).pickedId(user.getId()).build();
        int threadCount = 100; // 스레드 개수
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    answerRedissonService.answerToCommonQuestion(user.getId(), command);
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
        Ranking actual = rankingRepository.findByUsersAndQuestionAndGroups(user, question.getContent(), group)
            .orElseThrow();

        assertAll(
            () -> assertThat(actual.getCount()).isEqualTo(threadCount)
        );
    }

    private RedisVisitCount createVisitCount() {
        RedisVisitCount redisVisitCount = RedisVisitCount.builder()
            .hostId(1L)
            .dailyVisited(0)
            .totalVisited(10)
            .build();
        redisVisitCountRepository.save(redisVisitCount);
        return redisVisitCount;
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

        userRepository.save(user);
        return user;
    }

    private Groups createGroup() {
        Groups group = Groups.builder()
            .groupName("test")
            .description("test")
            .groupImageUrl("test")
            .build();

        groupRepository.save(group);
        return group;
    }

    private Question createQuestion(Users user, Groups group) {
        Question question = Question.builder()
                .content("question1")
                .questionStatus(QuestionStatus.APPROVED)
                .groupId(group.getId())
                .writer(user)
                .build();
        questionRepository.save(question);
        return question;
    }
}

