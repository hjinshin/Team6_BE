package supernova.whokie.redis.entity;


import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.util.concurrent.TimeUnit;

@RedisHash("payReady")
@Builder
@AllArgsConstructor
@Getter
public class PayToken {

    @Id
    private Long id;

    @NotNull
    private String tid;

    @NotNull
    @TimeToLive(unit = TimeUnit.SECONDS)
    private Long expiresIn;
}
