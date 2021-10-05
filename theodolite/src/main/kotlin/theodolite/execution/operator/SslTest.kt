package theodolite.execution.operator

import java.net.URL
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.*


object SSLTest {
    fun test() {
        // configure the SSLContext with a TrustManager
        val ctx: SSLContext = SSLContext.getInstance("TLS")
        ctx.init(arrayOfNulls<KeyManager>(0), arrayOf<TrustManager>(DefaultTrustManager()), SecureRandom())
        SSLContext.setDefault(ctx)
        val url = URL("https://192.168.48.151:6443/apis/theodolite.com/v1/namespaces/titan-she/executions")
        val conn = url.openConnection() as HttpsURLConnection
        conn.setHostnameVerifier(DefaultHostnameVerifier())
        System.out.println(conn.getResponseCode())
        val certs: Array<Certificate> = conn.getServerCertificates()
        for (cert in certs) {
            System.out.println(cert.getType())
            System.out.println(cert)
        }
        conn.disconnect()
    }

    private class DefaultTrustManager : X509TrustManager {
        @Throws(CertificateException::class)
        override fun checkClientTrusted(arg0: Array<X509Certificate?>?, arg1: String?) {
        }

        @Throws(CertificateException::class)
        override fun checkServerTrusted(arg0: Array<X509Certificate?>?, arg1: String?) {
        }

        override fun getAcceptedIssuers(): Array<X509Certificate>? {
            return null
        }

    }

    private class DefaultHostnameVerifier(): HostnameVerifier {
        override fun verify(arg0: String?, arg1: SSLSession?): Boolean {
            return true
        }
    }
}