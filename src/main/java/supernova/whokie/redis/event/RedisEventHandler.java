package supernova.whokie.redis.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import supernova.whokie.redis.service.RedisVisitService;

@Component
@RequiredArgsConstructor
public class RedisEventHandler {
    private final RedisVisitService redisVisitService;

    @EventListener
    public void redisVisitListener(RedisDto.Visit event) {
        redisVisitService.visitProfile(event);
    }
}
