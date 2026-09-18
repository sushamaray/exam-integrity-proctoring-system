package com.uiu.examintegrity.config;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.uiu.examintegrity.course.CourseAccess;
import com.uiu.examintegrity.exam.persistence.ExamRepository;
import com.uiu.examintegrity.user.AppUserDetails;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    private static final Pattern EXAM_TOPIC = Pattern.compile("^/topic/exams/(\\d+)/risk$");

    private final String[] allowedOrigins;
    private final CourseAccess courseAccess;
    private final ExamRepository examRepository;

    public WebSocketConfig(@Value("${app.websocket.allowed-origins:http://localhost:5173}") String[] allowedOrigins,
            CourseAccess courseAccess, ExamRepository examRepository) {
        this.allowedOrigins = allowedOrigins;
        this.courseAccess = courseAccess;
        this.examRepository = examRepository;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOrigins);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    authorizeSubscription(accessor.getDestination(), accessor.getUser());
                }
                return message;
            }
        });
    }

    private void authorizeSubscription(String destination, Principal user) {
        Matcher topic = destination == null ? null : EXAM_TOPIC.matcher(destination);
        if (topic == null || !topic.matches()) {
            throw refused(destination, "unknown topic");
        }
        if (!(user instanceof Authentication auth) || !(auth.getPrincipal() instanceof AppUserDetails staff)) {
            throw refused(destination, "no signed-in principal (" + (user == null ? "null" : user.getClass().getSimpleName()) + ")");
        }
        Long examId = Long.valueOf(topic.group(1));
        boolean allowed = examRepository.findById(examId)
                .map(exam -> courseAccess.isStaff(exam.getCourse().getId(), staff.getId()))
                .orElse(false);
        if (!allowed) {
            throw refused(destination, "not staff on this exam's course");
        }
    }

    private static AccessDeniedException refused(String destination, String reason) {
        log.debug("Refused subscription to {}: {}", destination, reason);
        return new AccessDeniedException(reason);
    }
}
