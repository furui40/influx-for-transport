package com.example.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "mail")
public class MailConfig {
    private String host;
    private int port;
    private String username;
    private String password;
    private String from;
    private Properties properties;

    @Data
    public static class Properties {
        private Smtp smtp;

        @Data
        public static class Smtp {
            private boolean auth;
            private Starttls starttls;
            private boolean ssl;
            private long timeout;

            @Data
            public static class Starttls {
                private boolean enable;
            }
        }
    }
}