package br.com.sintonia.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class ClientSessionRegistry {

    private static final Duration DEFAULT_GRACE_PERIOD = Duration.ofSeconds(3);

    private final ApplicationEventPublisher eventPublisher;
    private final Duration gracePeriod;
    private final ScheduledExecutorService scheduler;

    private final Map<String, String> sessionToClient = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> clientToSessions = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> pendingReleases = new ConcurrentHashMap<>();

    @Autowired
    public ClientSessionRegistry(ApplicationEventPublisher eventPublisher) {
        this(eventPublisher, DEFAULT_GRACE_PERIOD);
    }

    ClientSessionRegistry(ApplicationEventPublisher eventPublisher, Duration gracePeriod) {
        this.eventPublisher = eventPublisher;
        this.gracePeriod = gracePeriod;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "client-session-disconnect");
            thread.setDaemon(true);
            return thread;
        });
    }

    public synchronized void register(String sessionId, String clientSessionId) {
        sessionToClient.put(sessionId, clientSessionId);
        clientToSessions.computeIfAbsent(clientSessionId, key -> ConcurrentHashMap.newKeySet()).add(sessionId);
        cancelPendingRelease(clientSessionId);
    }

    public synchronized void unregister(String sessionId) {
        String clientSessionId = sessionToClient.remove(sessionId);
        if (clientSessionId == null) {
            return;
        }

        Set<String> sessions = clientToSessions.get(clientSessionId);
        if (sessions == null) {
            return;
        }

        sessions.remove(sessionId);
        if (sessions.isEmpty()) {
            clientToSessions.remove(clientSessionId);
            schedulePendingRelease(clientSessionId);
        }
    }

    private void cancelPendingRelease(String clientSessionId) {
        ScheduledFuture<?> future = pendingReleases.remove(clientSessionId);
        if (future != null) {
            future.cancel(false);
        }
    }

    private void schedulePendingRelease(String clientSessionId) {
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            pendingReleases.remove(clientSessionId);
            if (!hasLiveConnection(clientSessionId)) {
                eventPublisher.publishEvent(new ClientSessionLostEvent(clientSessionId));
            }
        }, gracePeriod.toMillis(), TimeUnit.MILLISECONDS);
        pendingReleases.put(clientSessionId, future);
    }

    private boolean hasLiveConnection(String clientSessionId) {
        Set<String> sessions = clientToSessions.get(clientSessionId);
        return sessions != null && !sessions.isEmpty();
    }

    void shutdown() {
        scheduler.shutdownNow();
    }
}
