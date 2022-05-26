package com.microservicesdemo.demo.kafka.producer.config.service.impl;

import com.microservicesdemo.demo.kafka.avro.model.TwitterAvroModel;
import com.microservicesdemo.demo.kafka.producer.config.service.KafkaProducer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import javax.annotation.PreDestroy;

// for generic variables replacement we will use Long and TwitterAvroModel
// TwitterAvroModel is a generated class file that we generated via schema
@Service
public class TwitterKafkaProducer implements KafkaProducer<Long, TwitterAvroModel> {
    private static final Logger LOG = LoggerFactory.getLogger(TwitterKafkaProducer.class);

    // inject kafka template to this spring bean
    private final KafkaTemplate<Long, TwitterAvroModel> kafkaTemplate;

    public TwitterKafkaProducer(KafkaTemplate<Long, TwitterAvroModel> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }


    // destory kafkaTemplote before application shut down
    @PreDestroy
    public void close() {
        if (kafkaTemplate != null) {
            LOG.info("Closing kafka producer");
            kafkaTemplate.destroy();
        }
    }
    @Override
    public void send(String topicName, Long key, TwitterAvroModel message) {
        LOG.info("Sending Message='{}' to topic='{}", message, topicName);
        // this will return Listenable Furutre and add a generic parmater
        // it will include SendResult with Long and TwitterAvroModel with geenric variable
        // Listenabe Future: Register callback methods for handling events when the reposne return
        // here since the send method of kafkaTemplate is asyncrohnous kafkaTemplate.send, it returns a listenale future,
        // and to get a resoonse later asynchrnously, we simply added a callback method below and override its onSuccess and onFailure method
        ListenableFuture<SendResult<Long, TwitterAvroModel>> kafkaResultFuture = kafkaTemplate.send(topicName, key, message);
        // add a callback method to this listenable future object
        addCallBack(topicName, message, kafkaResultFuture);
    }

    private void addCallBack(String topicName, TwitterAvroModel message, ListenableFuture<SendResult<Long, TwitterAvroModel>> kafkaResultFuture) {
        kafkaResultFuture.addCallback(new ListenableFutureCallback<SendResult<Long, TwitterAvroModel>>() {
            @Override
            public void onFailure(Throwable throwable) {
                LOG.error("Error while sending message {} to topic {}", message.toString(), topicName, throwable);
            }

            @Override
            public void onSuccess(SendResult<Long, TwitterAvroModel> result) {
                // onSuccess method we will get metadata from result
                RecordMetadata metadata = result.getRecordMetadata();
                LOG.debug("Reeived new metadata. Topic: {}; Partition {}; Offset {}; Timestamp {}, at time {}",
                        metadata.topic(),
                        metadata.partition(),
                        metadata.offset(),
                        metadata.timestamp(),
                        System.nanoTime()
                );
            }
        });
    }
}
