package supernova.whokie.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;
import supernova.whokie.global.annotation.RedissonLock;
import supernova.whokie.profile.service.ProfileVisitReadService;
import supernova.whokie.redis.constants.RedisConstants;
import supernova.whokie.redis.entity.RedisVisitCount;
import supernova.whokie.redis.entity.RedisVisitor;
import supernova.whokie.redis.event.RedisDto;
import supernova.whokie.redis.infrastructure.repository.RedisVisitCountRepository;
import supernova.whokie.redis.infrastructure.repository.RedisVisitorRepository;
import supernova.whokie.redis.service.dto.RedisCommand;
import supernova.whokie.redis.util.RedisUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisVisitService {

    private final RedisVisitorRepository redisVisitorRepository;
    private final RedisVisitCountRepository redisVisitCountRepository;
    private final ProfileVisitReadService profileVisitReadService;
    private final RedissonClient redissonClient;

    //    @RedissonLock(value = "#event.hostId()")
    public void visitProfile(RedisDto.Visit event) {
//        RedisVisitCount redisVisitCount = findVisitCountByHostId(event.hostId());
        visit(event.hostId());
//        redisVisitCount.visit();
//        redisVisitCountRepository.save(redisVisitCount);
    }

    public RedisVisitCount findVisitCountByHostId(Long hostId) {
        return redisVisitCountRepository.findById(hostId)
            .orElseGet(() -> {
                RedisCommand.Visited command = profileVisitReadService.findVisitCountById(hostId);
                return command.toRedisEntity();
            });
    }

    public List<RedisVisitCount> findAllVisitCounts() {
        List<RedisVisitCount> visitCountList = new ArrayList<>();
        redisVisitCountRepository.findAll().forEach(visitCountList::add);
        return visitCountList;
    }


    public void updateAllVisitCounts(List<RedisVisitCount> visitCounts) {
        visitCounts.forEach(RedisVisitCount::updateVisited);
        redisVisitCountRepository.saveAll(visitCounts);
    }

    @RedissonLock(value = "#hostId + #visitorIp")
    public boolean checkVisited(Long hostId, String visitorIp) {
        String id = RedisUtil.generateVisitorId(hostId, visitorIp);
        boolean exist = redisVisitorRepository.existsById(id);
        // 방문자 로그 기록
        saveVisitor(hostId, visitorIp);

        return exist;
    }

    public void saveVisitor(Long hostId, String visitorIp) {
        String id = RedisUtil.generateVisitorId(hostId, visitorIp);

        RedisVisitor redisVisitor = RedisVisitor.builder()
            .id(id)
            .hostId(hostId)
            .visitorIp(visitorIp)
            .visitTime(LocalDateTime.now())
            .build();
        redisVisitorRepository.save(redisVisitor);
    }

    public List<RedisVisitor> findAllVisitors() {
        List<RedisVisitor> visitorList = new ArrayList<>();
        redisVisitorRepository.findAll().forEach(visitorList::add);
        return visitorList;
    }

    public void deleteAllVisitors(List<RedisVisitor> visitors) {
        List<String> ids = visitors.stream().map(RedisVisitor::getId).toList();
        redisVisitorRepository.deleteAllById(ids);
    }

    private void visit(Long hostId) {
        RMap<String, Integer> visitMap = redissonClient.getMap(
            RedisConstants.VISIT_COUNT_KEY + ":" + hostId, StringCodec.INSTANCE);
        visitMap.addAndGet(RedisConstants.DAILY_VISIT_COUNT_FIELD, 1);
        visitMap.addAndGet(RedisConstants.TOTAL_VISIT_COUNT_FIELD, 1);
    }
}
