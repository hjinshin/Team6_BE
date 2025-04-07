package supernova.whokie.profilequestion;

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
public class ProfileQuestionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileQuestionRepository profileQuestionRepository;

    @BeforeEach
    void setUp() {
        Users user = createUser(1);
        for (int i = 1; i <= 5; i++) {
            createProfileQuestion(i, user);
        }
    }

    @Test
    @DisplayName("프로필 질문 조회 통합테스트")
    void getProfileQuestionsTest() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", "1");

        Long userId = 1L;

        // when
        mockMvc.perform(get("/api/profile/question/1")
                .requestAttr("userId", userId)
                .requestAttr("role", "USER")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(5))
            .andDo(result -> {
                System.out.println(result.getResponse().getContentAsString());
            });

        // then
    }

    @Test
    @DisplayName("프로필 질문 삭제 통합테스트")
    void deleteProfileQuestionTest() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", "1");

        Long userId = 1L;

        // when
        mockMvc.perform(delete("/api/profile/question/1")
                .requestAttr("userId", "1")
                .requestAttr("role", "USER")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("삭제가 완료되었습니다."))
            .andDo(result -> {
                System.out.println(result.getResponse().getContentAsString());
            });

    }

    @Test
    @DisplayName("프로필 질문 생성 통합테스트")
    void createProfileQuestionTest() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", "1");

        String requestBody = String.format("{\"content\": %d}", 1);

        // when
        mockMvc.perform(post("/api/profile/question")
                .requestAttr("userId", "1")
                .requestAttr("role", "USER")
                .content(requestBody)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("질문이 등록되었습니다."))
            .andDo(result -> {
                System.out.println(result.getResponse().getContentAsString());
            });
    }

    private Users createUser(int index) {
        Users user = Users.builder()
            .name("Test User " + index)
            .email("test@example.com")
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

    private void createProfileQuestion(int index, Users user) {
        ProfileQuestion profileQuestion = ProfileQuestion.builder()
            .content("question " + index)
            .user(user)
            .profileQuestionStatus(true)
            .build();
        profileQuestionRepository.save(profileQuestion);
    }

}
