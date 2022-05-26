package com.microservicesdemo.demo.kafka.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    // this is coming from the depency we aded call sping webflux
    // so wehn you create a method with @Bean annotiaon, spring will create a bean for this method at run time
    // and this bean will help you the type of the return object with this method
    // since return type is WebLCient, so spring will create a webclient object and i can inject it and use it anywhere  to make a rest call
    @Bean // managed by spring contianer
    WebClient webClient() {
        return WebClient.builder().build();
    }
}
