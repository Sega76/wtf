package components;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import org.springframework.beans.factory.annotation.Value;

public class MyTrustManager implements X509TrustManager {

//    @Value("${server.ssl.trust-store:#{null}}")
    private String trustStoreProp="Users/s.ryzhov/work/ForTests/src/main/resources/certs/trust-one.jks";

//    @Value("${server.ssl.trust-store-password:#{null}}")
    private String trustStorePassProp= "123456";

    private X509TrustManager trustManager;

    List<X509Certificate> certificateList = new ArrayList<>();

    public MyTrustManager() throws Exception {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        KeyStore trustStore = loadKeyStore("jks",
            "/Users/s.ryzhov/work/ForTests/src/main/resources/certs/trust-one.jks", "123456".toCharArray());

        trustManagerFactory.init(trustStore);

        Enumeration<String> aliases = trustStore.aliases();
        while (aliases.hasMoreElements()) {
            Certificate certificate = trustStore.getCertificate(aliases.nextElement());
            if (certificate instanceof X509Certificate)
                certificateList.add((X509Certificate)certificate);

        }

        for (TrustManager tm : trustManagerFactory.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
                trustManager = (X509TrustManager)tm;
                break;
            }
        }
    }

    @Override
    public void checkClientTrusted(
        final X509Certificate[] chain, final String authType) throws CertificateException {
        System.err.println("checkClientTrusted");

        Arrays.asList(Thread.currentThread().getStackTrace()).forEach(System.err::println);

        if (!certificateList.contains(chain[0]))
            Arrays.asList(chain).forEach(c ->
                System.err.println("Client=" + c.getSubjectDN()));

//        this.trustManager.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(
        final X509Certificate[] chain, final String authType) throws CertificateException {
        System.err.println("checkServerTrusted");
        if (!certificateList.contains(chain[0]))
            Arrays.asList(chain).forEach(c ->
                System.err.println("Server=" + c.getSubjectDN()));

//        this.trustManager.checkServerTrusted(chain, authType);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return this.trustManager.getAcceptedIssuers();
    }

    public static KeyStore loadKeyStore(String keyStoreType, String storeFilePath,
        char[] keyStorePwd) throws SSLException {
        try (InputStream input = Files.newInputStream(Paths.get(storeFilePath), StandardOpenOption.READ)) {

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(input, keyStorePwd);

            return keyStore;
        }
        catch (GeneralSecurityException e) {
            throw new SSLException("Failed to initialize key store (security exception occurred) [type=" +
                keyStoreType + ", keyStorePath=" + storeFilePath + ']', e);
        }
        catch (FileNotFoundException e) {
            throw new SSLException("Failed to initialize key store (key store file was not found): [path=" +
                storeFilePath + ", msg=" + e.getMessage() + ']', e);
        }
        catch (IOException e) {
            throw new SSLException("Failed to initialize key store (I/O error occurred): " + storeFilePath, e);
        }
    }
}
