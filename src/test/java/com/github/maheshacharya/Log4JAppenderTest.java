package com.github.maheshacharya;

import junit.framework.TestCase;
import org.slf4j.Logger;


public class Log4JAppenderTest
        extends TestCase {

    private Logger logger = org.slf4j.LoggerFactory.getLogger(Log4JAppenderTest.class);


    public void testApp() {
        logger.info("This is a test log message from Log4JElasticAppender");
    }
}
