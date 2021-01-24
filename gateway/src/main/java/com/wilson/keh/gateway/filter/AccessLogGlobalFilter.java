package com.wilson.keh.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * @author Wilson Ge
 * @since 1.0
 **/
@Component
@Slf4j
public class AccessLogGlobalFilter implements GlobalFilter, Ordered {
    static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.US);
    static final String COMMON_LOG_FORMAT =
            "{} - {} [{}] \"{} {} {}\" {} {} {} {} ms";

    static final String MISSING = "-";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String startTime = ZonedDateTime.now().format(DATE_TIME_FORMATTER);
        long startTimeMillis = System.currentTimeMillis();

        return chain.filter(exchange).doFinally(s -> {
            log.info(COMMON_LOG_FORMAT,
                    MISSING, MISSING, startTime, exchange.getRequest().getMethod(), exchange.getRequest().getURI().toString(),
                    exchange.getResponse().getRawStatusCode(), MISSING, MISSING, exchange.getResponse().getHeaders().getContentLength(),
                    (System.currentTimeMillis() - startTimeMillis));
        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
