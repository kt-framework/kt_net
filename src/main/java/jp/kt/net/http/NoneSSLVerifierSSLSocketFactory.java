package jp.kt.net.http;

import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.conn.scheme.HostNameResolver;
import org.apache.http.conn.ssl.SSLSocketFactory;

/**
 * 無効なSSL証明書でも通信できるようにするためのSSLSocketFactory.
 * <p>
 * {@link HttpConnection}クラスからのみ呼び出される.
 * </p>
 *
 * @author tatsuya.kumon
 */
class NoneSSLVerifierSSLSocketFactory extends SSLSocketFactory {
	private final SSLContext sslcontext;

	public NoneSSLVerifierSSLSocketFactory(final SSLContext sslContext,
			final HostNameResolver nameResolver) {
		super(sslContext, nameResolver);
		sslcontext = sslContext;
	}

	public static NoneSSLVerifierSSLSocketFactory create()
			throws NoSuchAlgorithmException, KeyManagementException {
		SSLContext sslContext = SSLContext.getInstance("SSL");
		sslContext.init(null, new TrustManager[] { new X509TrustManager() {
			public void checkClientTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {
			}

			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		} }, null);
		return new NoneSSLVerifierSSLSocketFactory(sslContext, null);
	}

	@Override
	public Socket createSocket() throws IOException {
		return sslcontext.getSocketFactory().createSocket();
	}
}
