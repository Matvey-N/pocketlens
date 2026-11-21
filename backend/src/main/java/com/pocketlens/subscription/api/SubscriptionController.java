package com.pocketlens.subscription.api;

import com.pocketlens.common.security.CurrentUserId;
import com.pocketlens.subscription.service.Subscription;
import com.pocketlens.subscription.service.SubscriptionService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    public List<Subscription> list(@CurrentUserId UUID userId) {
        return subscriptionService.detect(userId);
    }
}
