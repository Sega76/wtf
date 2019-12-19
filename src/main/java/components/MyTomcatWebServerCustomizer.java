package components;

import java.util.Set;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.net.AbstractJsseEndpoint;
import org.apache.tomcat.util.net.NioChannel;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class MyTomcatWebServerCustomizer
    implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    private static String STORE_PASS = "123456";

    @Override
    public void customize(TomcatServletWebServerFactory factory) {

        try {
            factory.addAdditionalTomcatConnectors(createSslConnector());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        factory.setDisplayName("RyzhovSV");
        // customize the factory here
    }

//    @Bean
//    public ServletWebServerFactory servletContainer() throws Exception {
//        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
//        tomcat.addAdditionalTomcatConnectors(createSslConnector());
//        tomcat.setDisplayName("Ryzhov");
//        tomcat.addContextCustomizers();
//        tomcat.
//
//        return tomcat;
//    }

//    @Bean
//    public TomcatServletWebServerFactory tomcatServletWebServerFactory() {
//        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
//        factory.addAdditionalTomcatConnectors(httpConnector());
//        factory.addContextCustomizers(securityCustomizer());
//        return factory;
//    }

//    @Bean
//    public TomcatWebServer tomcatWebServer() throws Exception {
//        Tomcat tomcat = new Tomcat();
//        tomcat.setConnector(createSslConnector());
//        tomcat.setHost(new StandardHost());
//
//
//        TomcatWebServer server = new TomcatWebServer(tomcat, true);
//
//        return server;
//    }

    private Connector createSslConnector() throws Exception {
        Connector connector = new Connector(MyHttp11NioProtocol.class.getName());
        MyHttp11NioProtocol protocol = (MyHttp11NioProtocol)connector.getProtocolHandler();
        try {
            connector.setScheme("https");
            connector.setSecure(true);
            connector.setPort(8990);
            connector.addSslHostConfig( new SSLHostConfig());
            protocol.setSSLEnabled(true);
            protocol.setClientAuth("true");
            protocol.setKeystoreFile("/Users/s.ryzhov/work/ForTests/src/main/resources/certs/userAdmin.jks");
            protocol.setKeystorePass(STORE_PASS);
            protocol.setTrustManagerClassName(MyTrustManager.class.getName());
            protocol.setTruststoreFile("/Users/s.ryzhov/work/ForTests/src/main/resources/certs/trust-one.jks");
            protocol.setTruststorePass(STORE_PASS);

            System.err.println(protocol.getClass().getName());

            AbstractJsseEndpoint<NioChannel, ?> endpoint = protocol.getEndpoint();
            System.err.println(endpoint.getClass().getName());
            endpoint.setHandler(new MyHttp11NioProtocol.MyHandler<>(protocol));

            AbstractEndpoint.Handler<NioChannel> handler = endpoint.getHandler();
            System.err.println(handler.getClass().getName());


//            protocol.setKeyAlias("useradmin");

//            NioEndpoint nioEndpoint = new NioEndpoint();

//            MyJSSELContext myJSSELCtx = new MyJSSELContext(sslContext);
//
//            Arrays.asList(connector.findSslHostConfigs())
//                .forEach(s ->
//                    s.getCertificates(true)
//                        .forEach(c -> {
//                            System.err.println("bla bla bla");
////                            util.con(Arrays.asList("org.apache.coyote.http11.Http11NioProtocol"))
//                            c.setSslContext(myJSSELCtx);
//                        })
//                );
            return connector;
        }
        catch (Exception ex) {
            throw new IllegalStateException("can't access keystore: [" + "keystore"
                + "] or truststore: [" + "keystore" + "]", ex);
        }
    }

//    @Bean
//    public ServerProperties serverProperties() {
//        final ServerProperties serverProps = new ServerProperties();
//
//        final Ssl ssl = new Ssl();
//
//        ssl.setClientAuth(Ssl.ClientAuth.NEED);
//        ssl.setKeyStore("/Users/s.ryzhov/work/ForTests/src/main/resources/certs/userAdmin.jks");
//        ssl.setKeyPassword(STORE_PASS);
//        ssl.setTrustStore("/Users/s.ryzhov/work/ForTests/src/main/resources/certs/trust-one.jks");
//        ssl.setTrustStorePassword(STORE_PASS);
//
//        serverProps.setPort(8081);
//        serverProps.setSsl(ssl);
//
//        return serverProps;
//    }

    private Connector httpConnector() {
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme("http");
        connector.setPort(8082);
        connector.setSecure(false);
        connector.setRedirectPort(8990);
        return connector;
    }

    public TomcatContextCustomizer securityTomcatCustomizer() {
        return context -> {
            SecurityConstraint securityConstraint = new SecurityConstraint();
            securityConstraint.setUserConstraint("CONFIDENTIAL");

            SecurityCollection collection = new SecurityCollection();
            collection.addPattern("/*");
            securityConstraint.addCollection(collection);

            context.addConstraint(securityConstraint);
        };
    }

//    private SSLContext getSslContext() throws Exception {
//        return SSLContextBuilder
//            .create()
//            .loadKeyMaterial(ResourceUtils.getFile("/Users/s.ryzhov/work/ForTests/src/main/resources/certs/validUser.jks"),
//                STORE_PASS.toCharArray(), STORE_PASS.toCharArray())
//            .loadTrustMaterial((certificate, authType) -> {
//                System.err.println("authType=" + authType);
//                Arrays.asList(certificate).forEach(c -> System.err.println(c.getSubjectDN().getName()));
////                    if (Arrays.stream(certificate)
////                        .anyMatch(c -> c.getSubjectDN().getName().contains("validUser")))
//                return true;
//            })
//            .build();
//    }
}