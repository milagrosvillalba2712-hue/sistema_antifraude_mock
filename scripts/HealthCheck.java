import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.time.Duration;

public class HealthCheck {
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
        var response = HttpClient.newBuilder().sslContext(context).connectTimeout(Duration.ofSeconds(2)).build()
                .send(HttpRequest.newBuilder(URI.create(args[1])).timeout(Duration.ofSeconds(3)).GET().build(),
                        HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() != 200) System.exit(1);
    }
}
