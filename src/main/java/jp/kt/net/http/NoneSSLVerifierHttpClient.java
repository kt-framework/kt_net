package jp.kt.net.http;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionManagerFactory;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.params.HttpParams;

/**
 * 無効なSSL証明書でも通信できるようにするためのDefaultHttpClient.
 * <p>
 * {@link HttpConnection}クラスからのみ呼び出される.
 * </p>
 *
 * @author tatsuya.kumon
 */
class NoneSSLVerifierHttpClient extends DefaultHttpClient {
	@Override
	protected ClientConnectionManager createClientConnectionManager() {
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", PlainSocketFactory
				.getSocketFactory(), 80));

		SSLSocketFactory sslSocketFactory = null;
		try {
			sslSocketFactory = NoneSSLVerifierSSLSocketFactory.create();
		} catch (KeyManagementException e) {
			throw new RuntimeException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		sslSocketFactory
				.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		registry.register(new Scheme("https", sslSocketFactory, 443));

		ClientConnectionManager connManager = null;
		HttpParams params = getParams();

		ClientConnectionManagerFactory factory = null;

		String className = (String) params
				.getParameter(ClientPNames.CONNECTION_MANAGER_FACTORY_CLASS_NAME);
		if (className != null) {
			try {
				Class<?> clazz = Class.forName(className);
				factory = (ClientConnectionManagerFactory) clazz.newInstance();
			} catch (ClassNotFoundException ex) {
				throw new IllegalStateException("Invalid class name: "
						+ className);
			} catch (IllegalAccessException ex) {
				throw new IllegalAccessError(ex.getMessage());
			} catch (InstantiationException ex) {
				throw new InstantiationError(ex.getMessage());
			}
		}
		if (factory != null) {
			connManager = factory.newInstance(params, registry);
		} else {
			connManager = new SingleClientConnManager(getParams(), registry);
		}

		return connManager;
	}
}
