package com.microservicesdemo.demo.twitterToKafkaService.runner;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import twitter4j.TwitterException;

public interface StreamRunner {
    void start() throws TwitterException;
}
