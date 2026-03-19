package com.icecandylovers.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ChatRateLimitInterceptor chatRateLimitInterceptor;

    public WebMvcConfig(ChatRateLimitInterceptor chatRateLimitInterceptor) {
        this.chatRateLimitInterceptor = chatRateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(chatRateLimitInterceptor)
                .addPathPatterns("/api/chat/**");
    }
}
