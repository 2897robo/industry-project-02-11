package com.team11.auth.authservice.commons.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.http.HttpHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class FeignConfig {

    @Bean
    RequestInterceptor requestInterceptor() {
        return requestTemplate -> {

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if(attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String accessToken = request.getHeader("Authorization");

                if(accessToken != null) {
                    requestTemplate.header(HttpHeaders.AUTHORIZATION, accessToken);
                }
            }
        };
    }
}
