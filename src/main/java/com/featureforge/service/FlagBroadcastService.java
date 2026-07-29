package com.featureforge.service;

import com.featureforge.event.FlagChangeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Thin wrapper over SimpMessagingTemplate so FeatureFlagService doesn't need
 * to know the topic naming convention or construct destinations itself —
 * same reasoning as AccessControlService/CachedFlagLookupService being a
 * single chokepoint for their respective concerns.
 */
@Service
@RequiredArgsConstructor
public class FlagBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcast(UUID projectId, FlagChangeEvent event) {
        messagingTemplate.convertAndSend("/topic/projects/" + projectId + "/flags", event);
    }
}
