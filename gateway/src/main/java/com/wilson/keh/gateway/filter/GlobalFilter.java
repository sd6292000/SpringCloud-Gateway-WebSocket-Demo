package com.wilson.keh.gateway.filter;

import java.io.IOException;
import java.util.List;

import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class GlobalFilter implements GatewayFilter{
	   @Autowired
	    ObjectMapper objectMapper;
	 
	    private static Joiner joiner = Joiner.on("");

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpResponseDecorator responseDecorator = new ServerHttpResponseDecorator(exchange.getResponse()) {
			@Override
			public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
				if (body instanceof Flux) {
					 Flux<? extends DataBuffer> fluxBody = Flux.from(body);
	                    return super.writeWith(fluxBody.buffer().map(dataBuffers -> {
	                        List<String> list = Lists.newArrayList();
	 
	                        dataBuffers.forEach(dataBuffer -> {
	                            byte[] content = new byte[dataBuffer.readableByteCount()];
	                            dataBuffer.read(content);
	                            DataBufferUtils.release(dataBuffer);
	 
	                            try {
	                                list.add(new String(content, "utf-8"));
	                            } catch (IOException e) {
	                                e.printStackTrace();
	                            }
	                        });
	                        String s = joiner.join(list);
	                        s=s+"<end></end>";
	                        return bufferFactory().wrap(s.getBytes());
	                    }));
	 
	 
	                }
	                return super.writeWith(body);
	            }
	        };


		return chain.filter(exchange.mutate().response(responseDecorator).build());
	}

}
