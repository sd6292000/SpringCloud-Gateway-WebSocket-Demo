package com.wilson.keh.gateway;

import com.wilson.keh.gateway.filter.SessionCreateFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseCookie;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionIdResolver;
import org.springframework.web.server.session.WebSessionManager;

import java.time.Duration;
import java.util.function.Consumer;

@SpringBootApplication
public class GatewayApplication {

	@Bean
	@LoadBalanced
	RestTemplate restTemplate() {
		return new RestTemplate();
	}

	/**
	 * The bean for changing cookie session name & its max age.
	 * @return
	 */
	@Bean
	DefaultWebSessionManager webSessionManager(){
		DefaultWebSessionManager webSessionManager =new DefaultWebSessionManager();
		CookieWebSessionIdResolver cookieWebSessionIdResolver=new CookieWebSessionIdResolver();
		cookieWebSessionIdResolver.setCookieName("WilsonSession");
		cookieWebSessionIdResolver.setCookieMaxAge(Duration.ofMinutes(5));
		webSessionManager.setSessionIdResolver(cookieWebSessionIdResolver);
		return webSessionManager;
	}
	@Bean
	GlobalFilter sessionCreateFilter(){
		return new SessionCreateFilter();
	}
	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}


}
