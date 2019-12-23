package other;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.tomcat.util.net.SSLContext;
import org.springframework.util.ResourceUtils;

public class MyJSSESSLContext implements SSLContext {

    private javax.net.ssl.SSLContext context2=SSLContextBuilder
        .create()
        .loadKeyMaterial(ResourceUtils.getFile("/Users/s.ryzhov/work/ForTests/src/main/resources/certs/validUser.jks"),
            "123456".toCharArray(), "123456".toCharArray())
        .loadTrustMaterial((certificate, authType) -> true)
        .build();


    private javax.net.ssl.SSLContext context;
    private KeyManager[] kms;
    private TrustManager[] tms;
    private TrustStrategy trustStrategy;

    public MyJSSESSLContext(javax.net.ssl.SSLContext context) throws NoSuchAlgorithmException, UnrecoverableKeyException, CertificateException, IOException, KeyStoreException, KeyManagementException {
        this.context = context;
    }

    public MyJSSESSLContext(String protocol) throws NoSuchAlgorithmException, UnrecoverableKeyException, CertificateException, IOException, KeyStoreException, KeyManagementException {
        context=javax.net.ssl.SSLContext.getInstance(protocol)   ;
    }

    @Override
    public void init(KeyManager[] kms, TrustManager[] tms, SecureRandom sr)
        throws KeyManagementException {
        this.kms = kms;
        this.tms = tms;
        context.init(kms, tms, sr);
    }

    @Override
    public void destroy() {
    }

    @Override
    public SSLSessionContext getServerSessionContext() {
        return context.getServerSessionContext();
    }

    @Override
    public SSLEngine createSSLEngine() {
        return context2.createSSLEngine();
    }

    @Override
    public SSLServerSocketFactory getServerSocketFactory() {
        return context.getServerSocketFactory();
    }

    @Override
    public SSLParameters getSupportedSSLParameters() {
        return context.getSupportedSSLParameters();
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        X509Certificate[] result = null;
        if (kms != null) {
            for (int i = 0; i < kms.length && result == null; i++) {
                if (kms[i] instanceof X509KeyManager) {
                    result = ((X509KeyManager) kms[i]).getCertificateChain(alias);
                }
            }
        }
        return result;
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        Set<X509Certificate> certs = new HashSet<>();
        if (tms != null) {
            for (TrustManager tm : tms) {
                if (tm instanceof X509TrustManager) {
                    X509Certificate[] accepted = ((X509TrustManager) tm).getAcceptedIssuers();
                    if (accepted != null) {
                        for (X509Certificate c : accepted) {
                            certs.add(c);
                        }
                    }
                }
            }
        }
        return certs.toArray(new X509Certificate[certs.size()]);
    }

    public void setTrustStrategy(TrustStrategy trustStrategy) {
        this.trustStrategy = trustStrategy;
    }
}
