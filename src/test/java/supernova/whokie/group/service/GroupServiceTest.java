package supernova.whokie.group.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import supernova.whokie.group.Groups;
import supernova.whokie.group.service.dto.GroupCommand;
import supernova.whokie.group.service.dto.GroupModel;
import supernova.whokie.groupmember.GroupMember;
import supernova.whokie.groupmember.GroupRole;
import supernova.whokie.groupmember.GroupStatus;
import supernova.whokie.groupmember.provider.InviteCodeProvider;
import supernova.whokie.groupmember.service.GroupMemberReaderService;
import supernova.whokie.groupmember.service.GroupMemberWriterService;
import supernova.whokie.s3.service.S3Service;
import supernova.whokie.user.Users;
import supernova.whokie.user.service.UserReaderService;

@ExtendWith(MockitoExtension.class)
public class GroupServiceTest {

    @InjectMocks
    private GroupService groupService;
    @Mock
    private GroupWriterService groupWriterService;
    @Mock
    private GroupReaderService groupReaderService;
    @Mock
    private UserReaderService userReaderService;
    @Mock
    private GroupMemberWriterService groupMemberService;
    @Mock
    private GroupMemberReaderService groupMemberReaderService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private S3Service s3Service;
    @Mock
    private InviteCodeProvider inviteCodeProvider;

    private Users testUser;
    private Groups testGroup;

    @BeforeEach
    void setUp() {
        testUser = createUser();
        testGroup = createGroup();
    }

    @Test
    @DisplayName("그룹 생성 테스트")
    void createGroupTest() {
        // given
        GroupCommand.Create command = GroupCommand.Create.builder()
            .groupName("group1")
            .groupDescription("description1")
            .groupImageUrl("image1")
            .build();
        given(userReaderService.getUserById(testUser.getId())).willReturn(testUser);

        // when
        GroupModel.Info actual = groupService.createGroup(command, testUser.getId());

        // then
        assertAll(
            () -> assertThat(actual.groupName()).isEqualTo("group1"),
            () -> assertThat(actual.groupDescription()).isEqualTo("description1"),
            () -> assertThat(actual.groupImageUrl()).isEqualTo("image1")
        );
    }

    @Test
    @DisplayName("그룹 수정 테스트")
    void modifyGroupTest() {
        // given
        GroupCommand.Modify command = GroupCommand.Modify.builder()
            .groupId(1L)
            .groupName("modifiedName")
            .description("modifiedDescription")
            .build();
        Groups group = createGroup();

        given(groupMemberReaderService.getByUserIdAndGroupId(testUser.getId(), command.groupId()))
            .willReturn(createGroupMember(testUser, GroupRole.LEADER, 1L));
        given(groupReaderService.getGroupById(command.groupId())).willReturn(group);

        // when
        groupService.modifyGroup(testUser.getId(), command);

        // then
        assertAll(
            () -> assertThat(group.getGroupName()).isEqualTo("modifiedName"),
            () -> assertThat(group.getDescription()).isEqualTo("modifiedDescription")
        );
    }

    private Users createUser() {
        return Users.builder().id(1L).name("name1").build();
    }

    private Groups createGroup() {
        return Groups.builder()
            .id(1L)
            .groupName("group1")
            .description("group1Description")
            .groupImageUrl("tset")
            .build();
    }

    private GroupMember createGroupMember(Users user, GroupRole groupRole, Long id) {
        return GroupMember.builder()
            .id(id)
            .user(user)
            .group(testGroup)
            .groupRole(groupRole)
            .groupStatus(GroupStatus.APPROVED)
            .build();
    }

}
