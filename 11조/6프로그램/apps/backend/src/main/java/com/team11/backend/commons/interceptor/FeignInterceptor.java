package com.team11.backend.commons.interceptor;

import feign.RequestInterceptor;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignInterceptor {

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

                Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        if ("refresh_token".equals(cookie.getName())) {
                            requestTemplate.header(HttpHeaders.COOKIE, "refresh_token=" + cookie.getValue());
                            break;
                        }
                    }
                }
            }
        };
    }
}
