package com.microservicesdemo.demo.kafka.admin.client;

import com.microservicesdemo.demo.config.KafkaConfigData;
import com.microservicesdemo.demo.config.RetryConfigData;
import com.microservicesdemo.demo.kafka.admin.config.WebClientConfig;
import com.microservicesdemo.demo.kafka.admin.exception.KafkaClientException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicListing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Component
public class KafkaAdminClient {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaAdminClient.class);

    // construcotr injecting all depenccies
    private final KafkaConfigData kafkaConfigData;

    private final RetryConfigData retryConfigData;

    // this is via admin client depency
    private final AdminClient adminClient;

    // this is via retry template depency spring retry
    private final RetryTemplate retryTemplate;

    // inject webclient as well
    private final WebClient webClient;

    public KafkaAdminClient(KafkaConfigData kafkaConfigData, RetryConfigData retryConfigData, AdminClient adminClient, RetryTemplate retryTemplate, WebClient webClient) {
        this.kafkaConfigData = kafkaConfigData;
        this.retryConfigData = retryConfigData;
        this.adminClient = adminClient;
        this.retryTemplate = retryTemplate;
        // spring will inject weblcient implementation bean at runtime,
        this.webClient = webClient;
    }

    public void createTopics() {
        // this is coming from admin client depencdy that we added
        CreateTopicsResult createTopicsResult;
        try {
            createTopicsResult = retryTemplate.execute(this::doCreateTopics);
        } catch (Throwable t) {
            throw new KafkaClientException("Reached max number of retry for creating kafka topics!", t);
        }
        // double check to make sure topics are created
        checkTopicsCreated();
    }

    private CreateTopicsResult doCreateTopics(RetryContext retryContext) {
        List<String> topicNames = kafkaConfigData.getTopicNamesToCreate();
        LOG.info("Creating {} topics, attempt {}", topicNames.size(), retryContext.getRetryCount());

        // convert list of topic names to stream
        List<NewTopic> kafkaTopics = topicNames.stream().map(topic -> new NewTopic(
                topic.trim(),
                kafkaConfigData.getNumOfPartitions(),
                kafkaConfigData.getReplicationFactor()
        )).collect(Collectors.toList());

        // pass created topics into adminclient creeatetopic
        return adminClient.createTopics(kafkaTopics);
    }

    public void checkTopicsCreated() {
        Collection<TopicListing> topics = getTopics();
        int retryCount = 1;
        Integer maxRetry = retryConfigData.getMaxAttempts();
        Integer multiplier = retryConfigData.getMultiplier().intValue();
        Long sleepTimeMs = retryConfigData.getSleepTimeMs();
        for (String topic: kafkaConfigData.getTopicNamesToCreate()) {
            // create topics is an asynchronous logic, thats why we added custom retry logic in top of retry template
            // custom retry login, wait until topics created or max retry reached
            while (!isTopicCreated(topics, topic)) {
                // check until max retry time
                checkMaxRetry(retryCount++, maxRetry);
                sleep(sleepTimeMs);
                // exponentially increase the sleep time until we get to the topics using getTopics method
                sleepTimeMs *= multiplier;
                topics = getTopics();
            }
        }
    }

    // check to see if schema registry is up and running
    // because we will run everything schema regisry, kafka and services in same compose file
    // we dont want to fail at startup because schema registry is unreachable
    // to do that we need to make a REST call to schema regisry end point
    public void checkSchemaRegistry() {
        int retryCount = 1;
        Integer maxRetry = retryConfigData.getMaxAttempts();
        // getMultipplier is a type double but we get intValue of int
        int multiplier = retryConfigData.getMultiplier().intValue();
        Long sleepTimeMs = retryConfigData.getSleepTimeMs();
        // check to see if we have successful schema registry http status
        // we will try check and wait until schemaregistry is up and running and wait until max retry
        while (!getSchemaRegistryStatus().is2xxSuccessful()) {
            // check until max retry time
            checkMaxRetry(retryCount++, maxRetry);
            sleep(sleepTimeMs);
            sleepTimeMs *= multiplier;
        }
    }

    // will return a httpstatus object
    private HttpStatus getSchemaRegistryStatus() {
        // we will make a rest call here and return the http status, to check the status of the schema registry
        try {
            return webClient
                    .method(HttpMethod.GET)
                    .uri(kafkaConfigData.getSchemaRegistryUrl())
                    .exchange()
                    .map(ClientResponse::statusCode) // here we will map the response to ClientResponse statusCode object
                    .block(); // block the operations to ge the result synchronosly from schema registry
        } catch (Exception e) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
    }

    private void sleep(Long sleepTimeMs) {
        // we will just sleep for the configure amount of time
        try {
            Thread.sleep(sleepTimeMs);
        } catch (InterruptedException e) {
            throw new KafkaClientException("Error while sleeping for waitingnew created topics ");
        }
    }

    private void checkMaxRetry(int retry, Integer maxRetry) {
        // check if the current retry number if greater than maxretry
        // if it is greater then we throw kafka client exception
        if(retry > maxRetry) {
            throw new KafkaClientException("Reached max number of retry for reading kafka topics");
        }
    }

    private boolean isTopicCreated(Collection<TopicListing> topics, String topicName) {
        // if topic collection is null we return false
        if(topics == null) {
            return false;
        }

        // otherwise we will convert topics to string and search topicName we passed
        return topics.stream().anyMatch(topic -> topic.name().equals(topicName));
    }

    // read topics that was created
    // TopicListing is also from apache admin client dependncy
    public Collection<TopicListing> getTopics() {
        Collection<TopicListing> topics;
        try {
            topics = retryTemplate.execute(this::doGetTopics);
        } catch (Throwable t) {
            throw new KafkaClientException("Reached max number of retry for creating kafka topics!", t);
        }
        return topics;
    }

    private Collection<TopicListing> doGetTopics(RetryContext retryContext)
            throws ExecutionException, InterruptedException {
        LOG.info(
                "Reading kafka topic {}, attempt {}",
                kafkaConfigData.getTopicNamesToCreate().toArray(), retryContext.getRetryCount()
        );

        // collection of topic listings
        Collection<TopicListing> topics = adminClient.listTopics().listings().get();
        if(topics != null) {
            topics.forEach(topic -> LOG.debug("Topic with name {}", topic.name()));
        }

        return topics;
    }
}
