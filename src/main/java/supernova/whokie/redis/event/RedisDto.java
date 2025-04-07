package supernova.whokie.redis.event;

import lombok.Builder;

public class RedisDto {
    @Builder
    public record Visit(
            Long hostId,
            String visitorIp
    ) {
        public static RedisDto.Visit toDto(Long hostId, String visitorIp) {
            return Visit.builder()
                    .hostId(hostId)
                    .visitorIp(visitorIp)
                    .build();
        }
    }
}
