package components;

import java.awt.Event;
import java.util.Arrays;
import javax.net.ssl.SSLContext;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import other.MyJSSESSLContext;

import static components.MyTomcatWebServerCustomizer.STORE_PASS;

//@Component
public class WebServerFactory extends TomcatServletWebServerFactory {
//    @Bean
    public TomcatWebServer tomcatWebServer(ServletContextInitializer... initializers) throws Exception {
        Tomcat tomcat = new Tomcat();
        tomcat.setConnector(createSslConnector());

        prepareContext(tomcat.getHost(), initializers);


        TomcatWebServer server = new TomcatWebServer(tomcat, true);

        return server;
    }

    private Connector createSslConnector() throws Exception {
        Connector connector = new Connector();
        Http11NioProtocol protocol = (Http11NioProtocol)connector.getProtocolHandler();
        try {
            connector.setScheme("https");
            connector.setSecure(true);
            connector.setPort(8990);

            SSLHostConfig hostCfg = new SSLHostConfig();
            hostCfg.setSslProtocol("TLS");
            connector.addSslHostConfig(hostCfg);

            protocol.setSSLEnabled(true);
            protocol.setClientAuth("true");
            protocol.setKeystoreFile("/Users/s.ryzhov/work/ForTests/src/main/resources/certs/userAdmin.jks");
            protocol.setKeystorePass(STORE_PASS);
            protocol.setTrustManagerClassName(MyTrustManager.class.getName());
            protocol.setTruststoreFile("/Users/s.ryzhov/work/ForTests/src/main/resources/certs/trust-one.jks");
            protocol.setTruststorePass(STORE_PASS);

//            System.err.println(protocol.getClass().getName());
//
//            AbstractJsseEndpoint<NioChannel, ?> endpoint = protocol.getEndpoint();
//            System.err.println(endpoint.getClass().getName());
//            endpoint.setHandler(new MyHttp11NioProtocol.MyHandler<>(protocol));
//
//            AbstractEndpoint.Handler<NioChannel> handler = endpoint.getHandler();
//            System.err.println(handler.getClass().getName());

            protocol.setKeyAlias("useradmin");
            MyJSSESSLContext myJSSELCtx = new MyJSSESSLContext(getSslContext());

            Arrays.asList(connector.findSslHostConfigs())
                .forEach(s ->
                    s.getCertificates(true)
                        .forEach(c -> {
                            System.err.println("bla bla bla");
                            c.setSslContext(myJSSELCtx);
                        })
                );

            return connector;
        }
        catch (Exception ex) {
            throw new IllegalStateException("can't access keystore: [" + "keystore"
                + "] or truststore: [" + "keystore" + "]", ex);
        }
    }

    private SSLContext getSslContext() throws Exception {
        return SSLContextBuilder
            .create()
            .loadKeyMaterial(ResourceUtils.getFile("/Users/s.ryzhov/work/ForTests/src/main/resources/certs/validUser.jks"),
                STORE_PASS.toCharArray(), STORE_PASS.toCharArray())
            .loadTrustMaterial((certificate, authType) -> {
                System.err.println("authType=" + authType);
                Arrays.asList(certificate).forEach(c -> System.err.println(c.getSubjectDN().getName()));
//                    if (Arrays.stream(certificate)
//                        .anyMatch(c -> c.getSubjectDN().getName().contains("validUser")))
                return true;
            })
            .build();
    }
}
