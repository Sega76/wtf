package other;

import java.awt.event.InputMethodListener;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.util.ResourceUtils;

public class Main {
    public static void main(String[] args) throws Exception {

        char[] pass = "123456".toCharArray();

        SSLContext sslContext = SSLContextBuilder
            .create()
            .loadKeyMaterial(ResourceUtils.getFile("/Users/s.ryzhov/work/ForTests/src/main/resources/certs/validUser.jks"),
                pass, pass)
            .loadTrustMaterial((certificate, authType) -> true)
            .build();

        SSLEngine engine = sslContext.createSSLEngine();
        engine.setUseClientMode(false);
        engine.setNeedClientAuth(true);

    }



}
