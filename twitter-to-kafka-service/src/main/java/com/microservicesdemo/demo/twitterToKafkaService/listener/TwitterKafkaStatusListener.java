package com.microservicesdemo.demo.twitterToKafkaService.listener;

import com.microservicesdemo.demo.kafka.avro.model.TwitterAvroModel;
import com.microservicesdemo.demo.config.KafkaConfigData;
import com.microservicesdemo.demo.kafka.producer.config.service.KafkaProducer;
import com.microservicesdemo.demo.twitterToKafkaService.transformer.TwitterStatusToAvroTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import twitter4j.Status;
import twitter4j.StatusAdapter;

@Component // spring managed bean -- will be scanned by spring at runtime
public class TwitterKafkaStatusListener extends StatusAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(TwitterKafkaStatusListener.class);

    // inject depencies/consturcior injection
    private final KafkaConfigData kafkaConfigData;

    private final KafkaProducer<Long, TwitterAvroModel> kafkaProducer;

    private final TwitterStatusToAvroTransformer twitterStatusToAvroTransformer;

    public TwitterKafkaStatusListener(KafkaConfigData kafkaConfigData, KafkaProducer<Long, TwitterAvroModel> kafkaProducer, TwitterStatusToAvroTransformer twitterStatusToAvroTransformer) {
        this.kafkaConfigData = kafkaConfigData;
        this.kafkaProducer = kafkaProducer;
        this.twitterStatusToAvroTransformer = twitterStatusToAvroTransformer;
    }

    // here Status class is from twitter4j and represents twitter object
    @Override
    public void onStatus(Status status) {
        LOG.info("Received status text {} sending to kafka topic {}", status.getText(), kafkaConfigData.getTopicName());
        // convert status object to twitterAvroModel, cuz we need to send it to kafka
        TwitterAvroModel twitterAvroModel = twitterStatusToAvroTransformer.getTwitterAvroModelFromStatus(status);
        // for message we just send the twitterAvroModel object
        // we use getUserId as key, cuz we want to partition the data using a userId field of TwitterAvroModel object
        kafkaProducer.send(kafkaConfigData.getTopicName(), twitterAvroModel.getUserId(), twitterAvroModel);

    }
}
