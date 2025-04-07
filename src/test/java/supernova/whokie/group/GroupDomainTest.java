package supernova.whokie.group;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class GroupDomainTest {

    @Test
    @DisplayName("그룹 수정 테스트")
    void updateGroupTest() {
        // given
        Groups group = createGroup();

        // when
        group.modify("그룹2", "그룹2 설명");

        // then
        assertThat(group.getGroupName()).isEqualTo("그룹2");
        assertThat(group.getDescription()).isEqualTo("그룹2 설명");
    }

    @Test
    @DisplayName("그룹 이미지 수정 테스트")
    void updateGroupImageUrlTest() {
        // given
        Groups group = createGroup();

        // when
        group.modifyImageUrl("test2");

        // then
        assertThat(group.getGroupImageUrl()).isEqualTo("test2");
    }

    private Groups createGroup() {
        return Groups.builder()
            .groupName("그룹1")
            .description("그룹1 설명")
            .groupImageUrl("test")
            .build();
    }


}
