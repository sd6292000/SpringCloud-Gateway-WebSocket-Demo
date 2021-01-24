package com.wilson.keh.gateway;

import com.wilson.keh.gateway.filter.SessionCreateFilter;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseCookie;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionIdResolver;
import org.springframework.web.server.session.WebSessionManager;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.tcp.ProxyProvider;

import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.function.Consumer;

@SpringBootApplication
@Slf4j
public class GatewayApplication {

    @Bean
    @LoadBalanced
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * The bean for changing cookie session name & its max age.
     *
     * @return
     */
    @Bean
    DefaultWebSessionManager webSessionManager() {
        DefaultWebSessionManager webSessionManager = new DefaultWebSessionManager();
        CookieWebSessionIdResolver cookieWebSessionIdResolver = new CookieWebSessionIdResolver();
        cookieWebSessionIdResolver.setCookieName("WilsonSession");
        cookieWebSessionIdResolver.setCookieMaxAge(Duration.ofMinutes(5));
        webSessionManager.setSessionIdResolver(cookieWebSessionIdResolver);
        return webSessionManager;
    }

    /**
     * Netty connection settings
     *
     * @param properties
     */
    @Bean
    public HttpClient gatewayHttpClient(HttpClientProperties properties) {

        // configure pool resources
        HttpClientProperties.Pool pool = properties.getPool();

        ConnectionProvider
                connectionProvider = ConnectionProvider.elastic(pool.getName(),
                Duration.ofSeconds(120));


        HttpClient httpClient = HttpClient.create(connectionProvider)
                .tcpConfiguration(tcpClient -> {

                    if (properties.getConnectTimeout() != null) {
                        tcpClient = tcpClient.option(
                                ChannelOption.CONNECT_TIMEOUT_MILLIS,
                                properties.getConnectTimeout());
                    }

                    // configure proxy if proxy host is set.
                    HttpClientProperties.Proxy proxy = properties.getProxy();

                    if (StringUtils.hasText(proxy.getHost())) {

                        tcpClient = tcpClient.proxy(proxySpec -> {
                            ProxyProvider.Builder builder = proxySpec
                                    .type(ProxyProvider.Proxy.HTTP)
                                    .host(proxy.getHost());

                            PropertyMapper map = PropertyMapper.get();

                            map.from(proxy::getPort).whenNonNull().to(builder::port);
                            map.from(proxy::getUsername).whenHasText()
                                    .to(builder::username);
                            map.from(proxy::getPassword).whenHasText()
                                    .to(password -> builder.password(s -> password));
                            map.from(proxy::getNonProxyHostsPattern).whenHasText()
                                    .to(builder::nonProxyHosts);
                        });
                    }
                    return tcpClient;
                });

        HttpClientProperties.Ssl ssl = properties.getSsl();
        if ((ssl.getKeyStore() != null && ssl.getKeyStore().length() > 0)
                || ssl.getTrustedX509CertificatesForTrustManager().length > 0
                || ssl.isUseInsecureTrustManager()) {
            httpClient = httpClient.secure(sslContextSpec -> {
                // configure ssl
                SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();

                X509Certificate[] trustedX509Certificates = ssl
                        .getTrustedX509CertificatesForTrustManager();
                if (trustedX509Certificates.length > 0) {
                    sslContextBuilder = sslContextBuilder
                            .trustManager(trustedX509Certificates);
                } else if (ssl.isUseInsecureTrustManager()) {
                    sslContextBuilder = sslContextBuilder
                            .trustManager(InsecureTrustManagerFactory.INSTANCE);
                }

                try {
                    sslContextBuilder = sslContextBuilder
                            .keyManager(ssl.getKeyManagerFactory());
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }

                sslContextSpec.sslContext(sslContextBuilder)
                        .defaultConfiguration(ssl.getDefaultConfigurationType())
                        .handshakeTimeout(ssl.getHandshakeTimeout())
                        .closeNotifyFlushTimeout(ssl.getCloseNotifyFlushTimeout())
                        .closeNotifyReadTimeout(ssl.getCloseNotifyReadTimeout());
            });
        }
        //metrics method plays similar logic as access log on recording.
        return httpClient.wiretap(false);
    }

    public static void main(String[] args) {
        //Enable the netty access log, which print the whole process flow on netty & same function with accessLog Global Filter
        System.setProperty("reactor.netty.http.server.accessLogEnabled", "true");
        SpringApplication.run(GatewayApplication.class, args);
    }


}
