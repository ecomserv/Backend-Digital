package yea.ecomservapi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        log.info("========== INCOMING REQUEST ==========");
        log.info("Method: {} | URI: {} | Query: {}",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString());
        log.info("Content-Type: {}", request.getContentType());
        log.info("Remote Address: {}", request.getRemoteAddr());

        // Log headers
        Collections.list(request.getHeaderNames()).forEach(headerName ->
            log.debug("Header: {} = {}", headerName, request.getHeader(headerName))
        );

        try {
            filterChain.doFilter(request, response);
        } finally {
            log.info("========== RESPONSE STATUS: {} ==========", response.getStatus());
        }
    }
}
