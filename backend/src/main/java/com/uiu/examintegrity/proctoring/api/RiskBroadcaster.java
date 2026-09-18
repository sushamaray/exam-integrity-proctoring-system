package com.uiu.examintegrity.proctoring.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.uiu.examintegrity.proctoring.application.MonitorService;
import com.uiu.examintegrity.proctoring.application.RiskUpdates;
import com.uiu.examintegrity.proctoring.application.view.MonitorView.Tile;

@Component
public class RiskBroadcaster {
    private static final Logger log = LoggerFactory.getLogger(RiskBroadcaster.class);

    private final RiskUpdates updates;
    private final MonitorService monitorService;
    private final SimpMessagingTemplate messaging;

    public RiskBroadcaster(RiskUpdates updates, MonitorService monitorService, SimpMessagingTemplate messaging) {
        this.updates = updates;
        this.monitorService = monitorService;
        this.messaging = messaging;
    }

    public static String topicFor(Long examId) {
        return "/topic/exams/" + examId + "/risk";
    }

    @Scheduled(fixedRateString = "${proctoring.broadcast.interval-ms:1500}")
    public void flush() {
        Map<Long, Set<Long>> dirty = updates.drain();
        dirty.forEach((examId, sessionIds) -> {
            try {
                List<Tile> tiles = monitorService.tiles(examId, sessionIds);
                messaging.convertAndSend(topicFor(examId), tiles);
            } catch (RuntimeException e) {
                log.warn("Could not push risk updates for exam {}", examId, e);
            }
        });
    }
}
