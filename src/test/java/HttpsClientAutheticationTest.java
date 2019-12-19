/*
 * Copyright 2019 JSC SberTech
 */

import app.App;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ResourceUtils;

import static app.App.VALID_ARGS_WITH_SSL;

@RunWith(SpringJUnit4ClassRunner.class)
public class HttpsClientAutheticationTest {
    /**
     * ApplicationContext.
     */
    protected AnnotationConfigServletWebServerApplicationContext ctx;
    /**
     * All password.
     */
    private String STORE_PASS = "123456";

    private String CERT_PATH="/Users/s.ryzhov/work/ForTests/src/main/resources/certs/validUser.jks";
    /**
     * Url for authentication.
     */
    private final String URL_TEST = "https://localhost:8990/test";
    /**
     * Client.
     */
    protected boolean client;

    /**
     * ObjectMapper.
     */
    private ObjectMapper om = new ObjectMapper();

    @Test
    public void testSameCert() throws Exception {

        SSLContext ctx = getDefaultSSLContext();

        String rsp = getAuthenticationResponse(ctx);

        System.out.println(rsp);

//        assertNull(rsp.getError());
//        assertNull(rsp.getErrMsg());
    }

    private String getAuthenticationResponse(SSLContext sslCtx)
        throws Exception {

        final CloseableHttpClient client = HttpClients.custom()
            .setSSLContext(sslCtx)
            .setSSLHostnameVerifier(new NoopHostnameVerifier())
            .build();

        HttpUriRequest request = RequestBuilder.get()
            .setUri(URL_TEST)
            .setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .setCharset(StandardCharsets.UTF_8)
            .build();

        HttpResponse res = client.execute(request);
        Assert.assertEquals(200, res.getStatusLine().getStatusCode());

        try (BufferedReader br = new BufferedReader(new InputStreamReader(res.getEntity().getContent()))) {

            return br.readLine();
        }
    }

    private SSLContext getDefaultSSLContext() throws Exception {

        KeyStore keyStore = KeyStore.getInstance("jks");
        keyStore.load(null);

        return SSLContextBuilder
            .create()
            .loadKeyMaterial(ResourceUtils.getFile(
//                "/Users/s.ryzhov/work/ForTests/src/main/resources/certs/other.jks"),
                CERT_PATH),
                STORE_PASS.toCharArray(), STORE_PASS.toCharArray())
//            .loadTrustMaterial(ResourceUtils.getFile(
//                "/Users/s.ryzhov/work/ForTests/src/main/resources/certs/trust-one.jks"),
//                STORE_PASS.toCharArray())
            .loadTrustMaterial((certificate, authType) -> {
////                Arrays.asList(certificate).forEach(c -> System.err.println(c.getSubjectDN().getName()));
////                if (!Arrays.stream(certificate)
//////                    .anyMatch(c -> c.getSubjectDN().getName().contains("validUser")))
////                    .anyMatch(c -> c.getSubjectDN().getName().contains("userAdmin")))
////                    return false;
                return true;
            })
            .build();
    }

    @After
    public void after() {
        if (ctx != null)
            SpringApplication.exit(ctx);
    }

    @Before
    public void before() throws Exception {
        startApplication(VALID_ARGS_WITH_SSL);
    }

    protected void startApplication(String[] args) {
        ctx = (AnnotationConfigServletWebServerApplicationContext)SpringApplication.run(App.class, args);
    }
}
