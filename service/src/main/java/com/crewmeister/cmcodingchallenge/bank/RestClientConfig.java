package com.crewmeister.cmcodingchallenge.bank;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    private static final String BASE_URL = "https://api.statistiken.bundesbank.de";
    private static final int CONNECT_TIMEOUT = 5;
    private static final int READ_TIMEOUT = 10;

    /**
     * Builds the shared RestClient for Bundesbank API requests.
     *
     * @return configured RestClient instance
     */
    @Bean
    public RestClient restClient(){
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings
                .defaults()
                .withConnectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT))
                .withReadTimeout(Duration.ofSeconds(READ_TIMEOUT));
        ClientHttpRequestFactory requestFactory =
                ClientHttpRequestFactoryBuilder.detect().build(settings);
        return RestClient.builder().baseUrl(BASE_URL).requestFactory(requestFactory).build();
    }
}
