package supernova.whokie.user.infrastructure.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import supernova.whokie.user.Users;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByEmail(String email);

    List<Users> findByKakaoIdIn(List<Long> kakaoId);

    List<Users> findByIdIn(List<Long> ids);

    Page<Users> findByNameContainingOrEmailContaining(String name, String email, Pageable pageable);

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT u FROM Users u WHERE u.id = :id")
    Optional<Users> findByIdWithOptimisticLock(Long id);
}
