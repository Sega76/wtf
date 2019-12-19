package app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan({"controller", "components"})
@SpringBootApplication
public class App {

    public static String[] VALID_ARGS_WITH_SSL = {
        "--server.port=8081",
        "--server.ssl.client-auth=need",
        "--server.ssl.key-store-type=JKS",
        "--server.ssl.key-store=/Users/s.ryzhov/work/ForTests/src/main/resources/certs/validUser.jks",
        "--server.ssl.key-store-password=123456",
        "--server.ssl.trust-store=/Users/s.ryzhov/work/ForTests/src/main/resources/certs/trust-one.jks",
        "--server.ssl.trust-store-password=123456"
    };

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        SpringApplication.run(App.class, VALID_ARGS_WITH_SSL);
    }
}
