package dev.plotnikov.jobradar.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimiter {
    static final int MAX_FAILURES = 10;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean isBlocked(String ip) {
        Deque<Instant> list = failures.get(ip);
        if (list == null) {
            return false;
        }
        synchronized (list) {
            removeOld(list);
            return list.size() >= MAX_FAILURES;
        }
    }

    public void recordFailure(String ip) {
        Deque<Instant> list = failures.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (list) {
            removeOld(list);
            list.addLast(Instant.now(clock));
        }
    }

    public void reset(String ip) {
        failures.remove(ip);
    }

    private void removeOld(Deque<Instant> list) {
        Instant limit = Instant.now(clock).minus(WINDOW);
        while (!list.isEmpty() && list.peekFirst().isBefore(limit)) {
            list.pollFirst();
        }
    }
}
