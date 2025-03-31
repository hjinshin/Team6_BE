package supernova.whokie.profile.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import supernova.whokie.profile.Profile;

import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findByUsersId(Long userId);

    @Query("SELECT p FROM Profile p JOIN FETCH p.users u WHERE u.id = :userId")
    Optional<Profile> findProfileWithMemberByUsersId(Long userId);
}
