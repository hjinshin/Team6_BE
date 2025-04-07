package supernova.whokie.redis.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import supernova.whokie.redis.service.RedisVisitService;

@Component
@RequiredArgsConstructor
public class RedisEventHandler {
    private final RedisVisitService redisVisitService;

    @Async
    @EventListener
    public void redisVisitListener(RedisDto.Visit event) {
        if (!redisVisitService.checkVisited(event.hostId(), event.visitorIp())) {
            redisVisitService.visitProfile(event);
        }
    }
}
