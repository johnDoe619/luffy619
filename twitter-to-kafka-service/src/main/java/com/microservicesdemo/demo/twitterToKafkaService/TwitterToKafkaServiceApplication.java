package com.microservicesdemo.demo.twitterToKafkaService;

import com.microservicesdemo.demo.config.TwitterToKafkaServiceConfigData;
import com.microservicesdemo.demo.twitterToKafkaService.init.StreamInitializer;
import com.microservicesdemo.demo.twitterToKafkaService.runner.StreamRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;

import java.util.Arrays;

@SpringBootApplication
// this basepackage component scan is required to allow finding the spring beans in other modules
// when a spring boot modules starts by default it scans the packages starting from the package dir that the spring boot main application resides
// so when we work with multiple modles there will be some spring beans that resides in differnet packages
// so if we look at twittertoconfigdataclass that resides in different packages that the defualt one we have right now, this doesnt fit to the default scan definiton
// in this case we can use component scan annotation @ComponentScan with base packages and mention com.microservicesdemo as the base package
// since updating to use com.micriserrvices as the starting package for all packages in all modules, spring will scan and find all the beans that resides in all modules
// that starts with "com.microservicesdemo"
@ComponentScan(basePackages = "com.microservicesdemo")
// @Scope("request") -- not good for intialization cuz it creates bean for each request to come by
public class TwitterToKafkaServiceApplication implements CommandLineRunner {

    // we set the getLogger as a class name so that this will natch during logging
    private static final Logger LOG = LoggerFactory.getLogger(TwitterToKafkaServiceApplication.class);

    // private final TwitterKafkaStreamRunner twitterKafkaStreamRunner;

    // this interface is implemented by two class twitter and mock stream data impleenttion
    // so we can directly call this and it wil find a class that has implmented start method down below dt compile time and use that
    // depending upon the enable-mock-tweeets config value from application.yml file
    private final StreamRunner streamRunner;

    private final StreamInitializer streamInitializer;

    // injecting objects using @AutoWired,
    // you dont need to inialize or initalzniate, spring will do for us
    // two injectiosn -- field injections and constructore inkection
//    @Autowired
//    private  TwitterToKafkaServiceConfigData twitterToKafkaServiceConfigData;

    // here constructor injection is preferred
    // becasue you can make objects immutable using final keyword, only iniatlize once inside constucotr
    // immuatbale(read only) objects are robust and thread safe(Two threads will not be creating the same object,) application
    // also spring deosnt using reflection with construcotr apprach unlike field injection
    // reflection makes the application code to run slower since its results types that are dynmaic result at runtime
//    private final TwitterToKafkaServiceConfigData twitterToKafkaServiceConfigData;
    public TwitterToKafkaServiceApplication(StreamRunner streamRunner, StreamInitializer streamInitializer) {
        this.streamRunner = streamRunner;
        this.streamInitializer = streamInitializer;
    }

    public static void main(String[] args) {
        SpringApplication.run(TwitterToKafkaServiceApplication.class, args);
    }
    // this will have the initialization logic in run method
    @Override
    public void run(String... args) throws Exception {
        LOG.info("App starts...");
//        LOG.info(Arrays.toString(twitterToKafkaServiceConfigData.getTwitterKeywords().toArray(new String[] {})));
//        LOG.info(twitterToKafkaServiceConfigData.getWelcomeMessage());
        streamInitializer.init();
        streamRunner.start();
    }

//    @PostConstruct
//    public void init() {
//    }
}

