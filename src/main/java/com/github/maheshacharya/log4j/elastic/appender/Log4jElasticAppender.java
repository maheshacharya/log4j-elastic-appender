package com.github.maheshacharya.log4j.elastic.appender;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.ClientConfig;
import io.searchbox.core.Index;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.slf4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Log4jElasticAppender extends AppenderSkeleton {

    private ExecutorService threadPool = Executors.newSingleThreadExecutor();
    private JestClient client;
    private String applicationName;
    private String hostName;
    private String elasticIndex;
    private String elasticType;
    private String elasticHost;
    private String datePattern = "MM/dd/yyyy HH:mm:ss";
    private SimpleDateFormat sdf = new SimpleDateFormat(datePattern);

    private Logger logger = org.slf4j.LoggerFactory.getLogger(Log4jElasticAppender.class);

    /**
     * @param event
     */
    @Override
    protected void append(LoggingEvent event) {
        if (isAsSevereAsThreshold(event.getLevel())) {
            threadPool.submit(new AppenderTask(event));

        }
    }

    /**
     * activeOptions
     */
    @Override
    public void activateOptions() {
        ClientConfig clientConfig = new ClientConfig.Builder(elasticHost).multiThreaded(true).build();
        JestClientFactory factory = new JestClientFactory();
        factory.setClientConfig(clientConfig);
        client = factory.getObject();
        super.activateOptions();
    }

    /**
     * close
     */
    public void close() {
        if (client != null) {
            client.shutdownClient();
        }
    }

    /**
     * @return boolean
     */
    public boolean requiresLayout() {
        return false;
    }

    /**
     * @return String
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * @param applicationName
     */
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * @return String
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * @param hostName
     */
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    /**
     * @return
     */
    public String getElasticIndex() {
        return elasticIndex;
    }

    /**
     * @param elasticIndex
     */
    public void setElasticIndex(String elasticIndex) {
        this.elasticIndex = elasticIndex;
    }

    /**
     * @return String
     */
    public String getElasticType() {
        return elasticType;
    }

    /**
     * @param elasticType
     */
    public void setElasticType(String elasticType) {
        this.elasticType = elasticType;
    }

    /**
     * @return String
     */
    public String getElasticHost() {
        return elasticHost;
    }

    /**
     * @param elasticHost
     */
    public void setElasticHost(String elasticHost) {
        this.elasticHost = elasticHost;
    }

    /**
     * @return String
     */
    public String getDatePattern() {
        return datePattern;
    }

    /**
     * @param datePattern
     */
    public void setDatePattern(String datePattern) {
        this.datePattern = datePattern;
        if (StringUtils.isNotEmpty(datePattern)) {
            sdf = new SimpleDateFormat(datePattern);
        }
    }


    class AppenderTask implements Callable<LoggingEvent> {


        LoggingEvent logEvent;

        AppenderTask(LoggingEvent logEvent) {
            this.logEvent = logEvent;

        }

        /**
         * @param log Map<String, Object>
         * @param e   LoggingEvent
         */
        protected void addLogProperties(Map<String, Object> log, LoggingEvent e) {
            log.put("hostName", getHostName());
            log.put("applicationName", getApplicationName());
            log.put("timestamp", sdf.format(new Date(e.getTimeStamp())));
            log.put("logger", e.getLoggerName());
            log.put("level", e.getLevel().toString());
            log.put("message", e.getMessage());
        }

        /**
         * @param log
         * @param e
         */
        protected void addStackTrace(Map<String, Object> log, LoggingEvent e) {
            ThrowableInformation ti = e.getThrowableInformation();
            if (ti != null) {
                Throwable t = ti.getThrowable();
                log.put("className", t.getClass().getCanonicalName());
                log.put("stackTrace", getStackTrace(t));
            }
        }

        /**
         * @param t
         * @return
         */
        protected String getStackTrace(Throwable t) {
            final Writer result = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(result);
            t.printStackTrace(printWriter);
            return result.toString();
        }

        /**
         * @return
         * @throws Exception
         */
        public LoggingEvent call() throws Exception {

            try {
                if (client != null) {
                    String uuid = UUID.randomUUID().toString();
                    Map<String, Object> data = new HashMap<String, Object>();

                    addLogProperties(data, logEvent);
                    addStackTrace(data, logEvent);
                    Index index = new Index.Builder(data).index(getElasticIndex()).type(getElasticType()).id(uuid).build();
                    client.execute(index);
                }
            } catch (Exception ex) {

                // logger.info("Error Occurred", ex);
            }
            return logEvent;
        }

    }
}
