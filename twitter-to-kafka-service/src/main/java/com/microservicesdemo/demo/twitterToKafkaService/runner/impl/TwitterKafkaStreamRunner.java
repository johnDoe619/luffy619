package com.microservicesdemo.demo.twitterToKafkaService.runner.impl;

import com.microservicesdemo.demo.config.TwitterToKafkaServiceConfigData;
import com.microservicesdemo.demo.twitterToKafkaService.runner.StreamRunner;
import com.microservicesdemo.demo.twitterToKafkaService.listener.TwitterKafkaStatusListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import twitter4j.FilterQuery;
import twitter4j.TwitterException;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;

import javax.annotation.PreDestroy;

@Component
// since we program to interface instead of concrete class, we can easily swich two implmentations
// and set havingValue to false for this mock implemetation of Stream Runner
// matchIfMissing to true for twitter implmentation, the original tweet implemation will be loaded and used so incase this enable-mock-tweets cannot be found in configuration
// now if we set the configuration value, enable-mock-tweets in application.yml as true, the mock implementation will be loaded at runtime, while twitter implemantation will be ignored
// and vice versa
// this way we decide the implentation class at compile time by just setting a configuration variable and Spring will load the correct bean at runtime
@ConditionalOnProperty(name = "twitter-to-kafka-service.enable-mock-tweets", havingValue = "false", matchIfMissing = true)
public class TwitterKafkaStreamRunner implements StreamRunner {

    private static final Logger LOG = LoggerFactory.getLogger(TwitterKafkaStreamRunner.class);

    private final TwitterToKafkaServiceConfigData twitterToKafkaServiceConfigData;
    private final TwitterKafkaStatusListener twitterKafkaStatusListener;

    // global twitterstream variable which is twitter4j librbary again
    private TwitterStream twitterStream;

    // consturcotr injection -- if its a final then you can initalize
    public TwitterKafkaStreamRunner(TwitterToKafkaServiceConfigData twitterToKafkaServiceConfigData, TwitterKafkaStatusListener twitterKafkaStatusListener) {
        this.twitterToKafkaServiceConfigData = twitterToKafkaServiceConfigData;
        this.twitterKafkaStatusListener = twitterKafkaStatusListener;
    }
    @Override
    public void start() throws TwitterException {
        // creating a twitter stream
        twitterStream = new TwitterStreamFactory().getInstance();
        twitterStream.addListener(twitterKafkaStatusListener);
        long[] bla = {1,2,3};
        FilterQuery filterQuery = new FilterQuery(1,bla);
        twitterStream.filter(filterQuery);
        LOG.info("Started filtering twitter stream for keywords {}");
    }

    // this method will be called before the bean is destoryed, means before the application is shut down
    // will make sure twitter stream is closed before the application cloase
    @PreDestroy
    public void shutDown() {
        if (twitterStream != null) {
            LOG.info("Closing twitter stream!");
            twitterStream.shutdown();
        }
    }

    private void addFilter() {
//        String[] keywords = twitterToKafkaServiceConfigData.getTwitterKeywords().toArray(new String[]{});
//        FilterQuery filterQuery = new FilterQuery(keywords);
//        twitterStream.filter(filterQuery);
        LOG.info("Started filtering twitter stream for keywords {}");
    }
}
