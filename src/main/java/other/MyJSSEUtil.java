package other;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;
import org.apache.tomcat.util.net.SSLContext;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.apache.tomcat.util.net.jsse.JSSEUtil;

public class MyJSSEUtil extends JSSEUtil {
    public MyJSSEUtil(SSLHostConfigCertificate certificate) {
        super(certificate);
    }

    public MyJSSEUtil(SSLHostConfigCertificate certificate, boolean warnOnSkip) {
        super(certificate, warnOnSkip);
    }

//    /** {@inheritDoc} */
//    @Override
//    public SSLContext createSSLContext(List<String> negotiableProtocols) throws NoSuchAlgorithmException {
//        SSLContext sslContext = createSSLContextInternal(negotiableProtocols);
//        sslContext.init(getKeyManagers(), getTrustManagers(), null);
//
//        SSLSessionContext sessionContext = sslContext.getServerSessionContext();
//        if (sessionContext != null) {
//            configureSessionContext(sessionContext);
//        }
//
//        return sslContext;
//    }
//
    @Override
    public SSLContext createSSLContextInternal(List<String> negotiableProtocols)
        throws NoSuchAlgorithmException {
        try {
            return new MyJSSESSLContext(sslHostConfig.getSslProtocol());
        }
        catch (UnrecoverableKeyException | IOException | KeyStoreException | KeyManagementException | CertificateException e) {
            e.printStackTrace();
        }
        return null;
    }


}
