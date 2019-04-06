package com.naah.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

@Controller
public class HelloController {

	private static final Logger logger = LoggerFactory.getLogger(HelloController.class);

	@Autowired
	private RestTemplate restTemplate;

	@GetMapping("/gatewayHello")
	@ResponseBody
	@Scheduled(cron = "0 0/2 * * * ?")
	public String say() {
		logger.info("Gateway hello");
		return restTemplate.getForEntity("http://bullet/hello", String.class).getBody();

	}

}