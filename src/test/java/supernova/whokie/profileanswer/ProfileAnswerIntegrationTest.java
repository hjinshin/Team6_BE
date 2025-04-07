package supernova.whokie.profileanswer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.awspring.cloud.s3.S3Template;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import supernova.whokie.profileanswer.infrastructure.repository.ProfileAnswerRepository;
import supernova.whokie.profilequestion.ProfileQuestion;
import supernova.whokie.profilequestion.infrastructure.repository.ProfileQuestionRepository;
import supernova.whokie.user.Gender;
import supernova.whokie.user.Role;
import supernova.whokie.user.Users;
import supernova.whokie.user.infrastructure.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "jwt.secret=abcd",
    "url.secret-key=abcd",
    "spring.sql.init.mode=never"
})
@MockBean({S3Client.class, S3Template.class, S3Presigner.class, RedissonClient.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ProfileAnswerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProfileAnswerRepository profileAnswerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileQuestionRepository profileQuestionRepository;

    @BeforeEach
    void setUp() {
        Users user1 = createUser(1);
        Users user2 = createUser(2);
        ProfileQuestion profileQuestion = createProfileQuestion(1, user1);
        for (int i = 1; i <= 5; i++) {
            createProfileAnswer(i, user2, profileQuestion);
        }
    }

    @Test
    @DisplayName("프로필 답변 조회 테스트")
    void getProfileAnswerPagingTest() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", "2");

        // when
        mockMvc.perform(get("/api/profile/answer")
                .requestAttr("userId", "2")
                .requestAttr("role", "USER")
                .param("page", "0")
                .param("size", "10")
                .param("user-id", "1")
                .param("question-id", "1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(5))
            .andDo(result -> {
                System.out.println(result.getResponse().getContentAsString());
            });
    }

    @Test
    @DisplayName("프로필 답변 삭제 테스트")
    void deleteProfileAnswerTest() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", "2");

        // when
        mockMvc.perform(delete("/api/profile/answer/1")
                .requestAttr("userId", "1")
                .requestAttr("role", "USER")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("message"))
            .andDo(result -> {
                System.out.println(result.getResponse().getContentAsString());
            });
    }

    @Test
    @DisplayName("프로필 답변 제작 테스트")
    void postProfileAnswerTest() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", "2");

        String requestBody = String.format("{\"content\": %d, \"profileQuestionId\": %d}", 1, 1);

        // when
        mockMvc.perform(post("/api/profile/answer")
                .requestAttr("userId", "2")
                .requestAttr("role", "USER")
                .content(requestBody)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("저장에 성공했습니다."))
            .andDo(result -> {
                System.out.println(result.getResponse().getContentAsString());
            });
    }

    private Users createUser(int index) {
        Users user = Users.builder()
            .name("Test User " + index)
            .email("test@example.com" + index)
            .point(100)
            .birthDate(LocalDate.now())
            .kakaoId(1234567890L)
            .gender(Gender.M)
            .imageUrl("default_image_url.jpg")
            .role(Role.USER)
            .build();
        userRepository.save(user);
        return user;
    }

    private ProfileQuestion createProfileQuestion(int index, Users user) {
        ProfileQuestion profileQuestion = ProfileQuestion.builder()
            .content("question " + index)
            .user(user)
            .profileQuestionStatus(true)
            .build();
        profileQuestionRepository.save(profileQuestion);
        return profileQuestion;
    }

    private void createProfileAnswer(int index, Users user, ProfileQuestion profileQuestion) {
        ProfileAnswer profileAnswer = ProfileAnswer.builder()
            .content("answer " + index)
            .answeredUser(user)
            .profileQuestion(profileQuestion)
            .build();
        profileAnswerRepository.save(profileAnswer);
    }

}
