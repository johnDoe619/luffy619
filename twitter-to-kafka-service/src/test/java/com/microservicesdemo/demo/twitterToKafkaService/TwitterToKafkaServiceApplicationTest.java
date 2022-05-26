package com.microservicesdemo.demo.twitterToKafkaService;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.security.RunAs;

// annotation
// this test class will run with Maven install command and load
// the application context by starting spring boot app
// Loading build it is helpful to see that application start succesfully
// Empty contextLoad() is a test to verify if the application is able to load Spring context successfully or not.
//@SpringBootTest(classes = TwitterToKafkaServiceApplicationTest.class) // if you enable this, starts nothing and tests nothing, doesnt blow up the application, but the main application doesnt work
@SpringBootTest
public class TwitterToKafkaServiceApplicationTest {
    @Test
    public void contextLoad() {

    }
}
