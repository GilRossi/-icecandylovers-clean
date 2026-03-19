package com.icecandylovers.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatRateLimitInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ChatRateLimitInterceptor.class);

    private static final int MAX_REQUESTS = 20;
    private static final long WINDOW_MILLIS = 60_000L;

    private final ConcurrentHashMap<String, Deque<Long>> requestTimestamps = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        String clientIp = extractClientIp(request);
        long now = System.currentTimeMillis();

        Deque<Long> timestamps = requestTimestamps.computeIfAbsent(clientIp, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            // Remove timestamps outside the sliding window
            long windowStart = now - WINDOW_MILLIS;
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= MAX_REQUESTS) {
                logger.warn("Rate limit excedido para IP: {}", clientIp);
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"erro\":\"Limite de mensagens atingido. Aguarde um momento.\"}");
                return false;
            }

            timestamps.addLast(now);
        }

        return true;
    }

    @Scheduled(fixedDelay = 300_000)
    public void limparEntradasAntigas() {
        long windowStart = System.currentTimeMillis() - WINDOW_MILLIS;
        Iterator<Map.Entry<String, Deque<Long>>> iterator = requestTimestamps.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Deque<Long>> entry = iterator.next();
            Deque<Long> timestamps = entry.getValue();
            synchronized (timestamps) {
                while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart) {
                    timestamps.pollFirst();
                }
                if (timestamps.isEmpty()) {
                    iterator.remove();
                }
            }
        }
        logger.debug("Limpeza de entradas antigas do rate limiter concluída.");
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            // X-Forwarded-For may contain a comma-separated list; the first is the real client IP
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
