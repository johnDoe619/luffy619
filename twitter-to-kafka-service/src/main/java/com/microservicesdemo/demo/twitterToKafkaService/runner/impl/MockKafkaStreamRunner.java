package com.microservicesdemo.demo.twitterToKafkaService.runner.impl;

import com.microservicesdemo.demo.config.TwitterToKafkaServiceConfigData;
import com.microservicesdemo.demo.twitterToKafkaService.runner.StreamRunner;
import com.microservicesdemo.demo.twitterToKafkaService.exception.TwitterToKafkaServiceException;
import com.microservicesdemo.demo.twitterToKafkaService.listener.TwitterKafkaStatusListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

@Component
// since we program to interface instead of concrete class, we can easily swich two implmentations
// and set havingValue to true for this mock implemetation of Stream Runner
// now if we set the configuration value, enable-mock-tweets in application.yml as true, the mock implementation will be loaded at runtime, while twitter implemantation will be ignored
// and vice versa
// this way we decide the implentation class at compile time by just setting a configuration variable and Spring will load the correct bean at runtime
@ConditionalOnProperty(name = "twitter-to-kafka-service.enable-mock-tweets", havingValue = "true")
public class MockKafkaStreamRunner implements StreamRunner {
    // Logger and Loggerfactory from sl4j -- spring provides it
    private static final Logger LOG = LoggerFactory.getLogger(MockKafkaStreamRunner.class);

    private final TwitterToKafkaServiceConfigData twitterToKafkaServiceConfigData;
    private final TwitterKafkaStatusListener twitterKafkaStatusListener;

    private static final Random RANDOM = new Random();
    private static final String[] WORDS = new String[]{
            "Lorem",
            "ipsum",
            "dolor",
            "sit",
            "amet",
            "consectetuer",
            "adipiscing",
            "elit",
            "Maecenas",
            "porttitor",
            "congue",
            "massa",
            "Fusce",
            "posuere",
            "magna",
            "sed",
            "pulvinar",
            "ultricies",
            "purus",
            "lectus",
            "malesuada",
            "libero"
    };

    private static final String tweetAsRawJson = "{" +
            "\"created_at\":\"{0}\"," +
            "\"id\":\"{1}\"," +
            "\"text\":\"{2}\"," +
            "\"user\":{\"id\":\"{3}\"}" +
            "}";

    private static final String TWITTER_STATUS_DATE_FORMAT = "EEE MMM dd HH:mm:ss zzz yyyy";

    // constructor inkectstion instead of field injection
    public MockKafkaStreamRunner(TwitterToKafkaServiceConfigData twitterToKafkaServiceConfigData, TwitterKafkaStatusListener twitterKafkaStatusListener) {
        this.twitterToKafkaServiceConfigData = twitterToKafkaServiceConfigData;
        this.twitterKafkaStatusListener = twitterKafkaStatusListener;
    }
    @Override
    public void start() throws TwitterException {
        String[] keywords = twitterToKafkaServiceConfigData.getTwitterKeywords().toArray(new String[0]);
        int minTweetLength = twitterToKafkaServiceConfigData.getMockMinTweetLength();
        int maxTweetLength = twitterToKafkaServiceConfigData.getMockMaxTweetLength();
        long sleepTimeMs = twitterToKafkaServiceConfigData.getMockSleepMs();
        LOG.info("Started mock filtering twitter stream for keywords {}");
        // by using infine loop to simulate streaming data continuosly
        // extracting this piece of infinte loop code  to a method and run it in a different thread, instead of blocking main thread
        simulateTwitterStream(keywords, minTweetLength, maxTweetLength, sleepTimeMs);
    }

    // by using infine loop to simulate streaming data continuosly
    // extracting this piece of infinte loop code  to a method and run it in a different thread, instead of blocking main thread
    private void simulateTwitterStream(String[] keywords, int minTweetLength, int maxTweetLength, long sleepTimeMs) {
//        Runnable runnable = () -> {
//            while (true) {
//                String formattedTweetAsRawJson = getFormattedTweets(keywords, minTweetLength, maxTweetLength);
//                // lets call the twitter object factor createStatus method and assign the result to local status
//                Status status = null;
//                try {
//                    status = TwitterObjectFactory.createStatus(formattedTweetAsRawJson);
//                } catch (TwitterException e) {
//                    e.printStackTrace();
//                }
//                twitterKafkaStatusListener.onStatus(status);
//                sleep(sleepTimeMs);
//            }
//        };
        // runnable interface is a functional interface cuz it has only one abstract method and you can use functional interface
        // if you look into runnable interface it has one method void run() which returns nothing and takes no parmater
        // here submit method takes a functional interfface either Runnable or callable
        // and i will send a Runnable interface implementation in the a lambda experession here cuz its a functional interface
        // lambda expressin - () -> {code to run}
        // get a new single thread execuotr object and call submit method on this object
        // this submit method will run on a new thread and run the code in this thread instead of running it on the main thread
        // Note: when we write this lamda inside submit mehtod we actually implement the Runable interface
        Executors.newSingleThreadExecutor().submit(() -> {
            while (true) {
                String formattedTweetAsRawJson = getFormattedTweets(keywords, minTweetLength, maxTweetLength);
                // lets call the twitter object factor createStatus method and assign the result to local status
                Status status = null;
                try {
                    status = TwitterObjectFactory.createStatus(formattedTweetAsRawJson);
                } catch (TwitterException e) {
                    LOG.error("Error creating twitter status", e);
                }
                twitterKafkaStatusListener.onStatus(status);
                sleep(sleepTimeMs);
            }
        });
    }

    private void sleep(long sleepTimeMs) {
        try {
            Thread.sleep(sleepTimeMs);
        } catch (InterruptedException e) {
            throw new TwitterToKafkaServiceException("Error while sleeping for waiting new status to create!!");
        }
    }

    // First item in string array params to get currendt data, using zonedate now and formatting date
    // second item we will use ThreadLocalRandom to get a random long number, to set the id fiekld of the tweet
    // there is no nextLong with an upper bound in standard Random class
    // so we use this thread safe ThreadLocalRandom class to create a random Long number
    private String getFormattedTweets(String[] keywords, int minTweetLength, int maxTweetLength) {
        String[] params = new String[] {
                // final note it would be better to use English Locale for this date fromatter method
                // cuz the format of the tweet is in english locale and you will get a Date parse error if your computer is in timezone if your computer doesnt use english locale
                ZonedDateTime.now().format(DateTimeFormatter.ofPattern(TWITTER_STATUS_DATE_FORMAT, Locale.ENGLISH)),
                String.valueOf(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE)),
                getRandomTweetContent(keywords, minTweetLength, maxTweetLength),
                String.valueOf(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE))
        };

        return formatTweetAsJsonWithParams(params);
    }

    private String formatTweetAsJsonWithParams(String[] params) {
        String tweet = tweetAsRawJson;
        for (int i = 0; i < params.length; i++) {
            // String.replace does not change the string content but returns the result of the replacement.
            // Strings are immutable which means you cant change them, but you can reassign lile str = str.replace...
            // but you cant do str.replace -- you have to reassign
            tweet = tweet.replace("{" + i + "}", params[i]);
        }
        return tweet;
    }

    private String getRandomTweetContent(String[] keywords, int minTweetLength, int maxTweetLength) {
        StringBuilder tweet = new StringBuilder();
        // i wanna get a random twet length between minimum and maximum tweet length
        // random integer between min and max tweetlength
        // we do +1 cuz the last value is exclusve with nextInt
        int tweetLength = RANDOM.nextInt(maxTweetLength - minTweetLength + 1) + minTweetLength;
        return constructRandomTweet(keywords, tweet, tweetLength);
    }

    private String constructRandomTweet(String[] keywords, StringBuilder tweet, int tweetLength) {
        for (int i = 0; i < tweetLength; i++) {
            // this is just an imainiation, you can use any
            tweet.append(WORDS[RANDOM.nextInt(WORDS.length)]).append(" ");
            if (i == tweetLength / 2) {
                tweet.append(keywords[RANDOM.nextInt(keywords.length)]).append(" ");
            }
        }
        // cast stringbuilder to string cuz the return type is String
        return tweet.toString().trim();
    }
}
