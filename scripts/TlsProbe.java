import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;

public class TlsProbe {
    public static void main(String[] args) throws Exception {
        var certificate = CertificateFactory.getInstance("X.509")
                .generateCertificate(new FileInputStream(args[0]));
        var store = KeyStore.getInstance(KeyStore.getDefaultType());
        store.load(null);
        store.setCertificateEntry("academic-ca", certificate);
        var managers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        managers.init(store);
        var context = SSLContext.getInstance("TLSv1.2");
        context.init(null, managers.getTrustManagers(), null);

        var request = HttpRequest.newBuilder(URI.create("https://localhost:8443/actuator/health")).GET().build();
        var accepted = HttpClient.newBuilder().sslContext(context).build()
                .send(request, HttpResponse.BodyHandlers.ofString());
        if (accepted.statusCode() != 200) throw new AssertionError("CA válida rechazada: " + accepted.statusCode());

        boolean untrustedRejected = false;
        try {
            HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding());
        } catch (javax.net.ssl.SSLHandshakeException expected) {
            untrustedRejected = true;
        }
        if (!untrustedRejected) throw new AssertionError("Certificado no confiable fue aceptado");

        boolean hostnameRejected = false;
        try {
            var wrongHost = HttpRequest.newBuilder(URI.create("https://[::1]:8443/actuator/health")).GET().build();
            HttpClient.newBuilder().sslContext(context).build().send(wrongHost, HttpResponse.BodyHandlers.discarding());
        } catch (javax.net.ssl.SSLHandshakeException expected) {
            hostnameRejected = true;
        }
        if (!hostnameRejected) throw new AssertionError("Hostname incorrecto fue aceptado");
        System.out.println("TLS_CA_ACCEPTED=true");
        System.out.println("TLS_UNTRUSTED_REJECTED=true");
        System.out.println("TLS_HOSTNAME_REJECTED=true");
    }
}
