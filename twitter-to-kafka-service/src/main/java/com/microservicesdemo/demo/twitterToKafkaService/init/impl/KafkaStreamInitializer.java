package com.microservicesdemo.demo.twitterToKafkaService.init.impl;

import com.microservicesdemo.demo.config.KafkaConfigData;
import com.microservicesdemo.demo.kafka.admin.client.KafkaAdminClient;
import com.microservicesdemo.demo.twitterToKafkaService.init.StreamInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class KafkaStreamInitializer implements StreamInitializer {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaStreamInitializer.class);

    // now inject depencies
    private final KafkaConfigData kafkaConfigData;

    private final KafkaAdminClient kafkaAdminClient;

    public KafkaStreamInitializer(KafkaConfigData kafkaConfigData, KafkaAdminClient kafkaAdminClient) {
        this.kafkaConfigData = kafkaConfigData;
        this.kafkaAdminClient = kafkaAdminClient;
    }

    @Override
    public void init() {
        // gotta check if kafka topics are created and if schema registry is up and running, before streaming data from twitter
        // cuz we wanna send data from twitter to kafka server
        // create kafka topics
        kafkaAdminClient.createTopics();
        // check to see if schema registry is up and running prior to startung our service
        kafkaAdminClient.checkSchemaRegistry();
        LOG.info("Topics with name {} is ready for operations!", kafkaConfigData.getTopicNamesToCreate().toArray());
    }
}
