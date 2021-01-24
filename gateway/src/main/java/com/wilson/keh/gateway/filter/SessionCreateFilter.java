package com.wilson.keh.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author Wilson Ge
 * @since 1.0
 **/
@Slf4j
public class SessionCreateFilter implements GlobalFilter , Ordered {

    public SessionCreateFilter(){
        log.info("Session Create Filter inited");
    }
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getSession().flatMap(session->{
          boolean hasCreated=  session.getAttributeOrDefault("hasCreated",false);
          if(!hasCreated){
              //fengzhailong
              //zbrb365
              log.info("Save attribute into session");
              session.getAttributes().put("hasCreated",true);
              session.save();
          }
          return chain.filter(exchange);
        });
    }
    @Override
    public int getOrder() {
        return 0;
    }

}
