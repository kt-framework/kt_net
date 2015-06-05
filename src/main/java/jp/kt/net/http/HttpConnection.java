package jp.kt.net.http;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.kt.exception.KtException;
import jp.kt.fileio.FileUtil;
import jp.kt.logger.ApplicationLogger;
import jp.kt.prop.KtProperties;
import jp.kt.tool.Validator;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.AllClientPNames;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

/**
 * HTTP通信を行い、結果を取得します.
 * <p>
 * 使い方:<br/>
 * １、コンストラクタ実行（URLはパラメータがないものをセット）<br/>
 * ２、パラメータがあれば、addParameter(String, String)メソッドでセット<br/>
 * ３、各種設定を行う（詳細は下記参照）<br/>
 * ４、通信実行（executeXxxxMethod()メソッド）<br/>
 * </p>
 * <p>
 * <b>■プロキシ設定</b><br/>
 * プロキシ設定のデフォルトは、kt.propertiesの下記値を参照します.<br/>
 * <ul>
 * <li>プロキシアドレス：kt.net.httpclient.proxy.address</li>
 * <li>プロキシポート：kt.net.httpclient.proxy.port</li>
 * </ul>
 * <b>■BASIC認証設定</b><br/>
 * BASIC認証が必要な通信の場合は、setBasicAuth(String, String)メソッドを実行してください.<br/>
 * <br/>
 * <b>■リクエスト文字コード設定</b><br/>
 * リクエスト時の文字コードのデフォルトは、Application.propertiesのデフォルト文字コードを参照します.<br/>
 * 別の文字コードを指定したい場合は、setRequestEncode(String)メソッドを実行してください.<br/>
 * <br/>
 * <b>■レスポンス文字コード設定</b><br/>
 * レスポンス時の文字コードは、HTTPヘッダに指定されている文字コードを使用しますが、<br/>
 * 未指定の場合は、setResponseEncode(String)メソッドにて明示的に指定することが可能です.<br/>
 * ただし、HTTPヘッダにて指定されている場合は、setResponseEncode(String)メソッドを実行していてもヘッダ値が優先されます. <br/>
 * <br/>
 * <b>■タイムアウト設定</b><br/>
 * 下記3項目に対して同じ値が設定されます.<br/>
 * デフォルトは10秒です.変更したい場合は、setTimeoutSecond(int)メソッドを実行してください.<br/>
 * <ul>
 * <li>接続タイムアウト</li>
 * <li>ソケットタイムアウト</li>
 * <li>処理タイムアウト</li>
 * </ul>
 * <b>■ユーザエージェント設定</b><br/>
 * デフォルトはHttpClientのユーザエージェントですが、明示的に指定したい場合は、setUserAgent(String)メソッドを実行してください.
 * <br/>
 * <br/>
 * <b>■リトライ設定</b><br/>
 * HTTP接続時にException発生、もしくはレスポンスコードが正常でない場合にリトライを実施する設定が可能です.<br/>
 * デフォルトはリトライ無しです.<br/>
 * リトライを設定したい場合は、setRetrySetting(int, int)メソッドにて指定します.<br/>
 * 第一引数が最大リトライ回数、第二引数がリトライ間隔（ミリ秒）です. <br/>
 * 最大リトライ回数に4を指定した場合は、最大で5回実行されます.<br/>
 * 最大回数リトライを行ってもエラーの場合は、Exceptionをthrowもしくは最後に実施した接続の{@link ResponseData}
 * オブジェクトを返します.<br/>
 * <br/>
 * <b>■SSL証明書チェック可否設定</b><br/>
 * SSL証明書の有効性チェックを行うか否かの設定です.<br/>
 * デフォルトは、チェックする設定となっています.<br/>
 * チェックする設定だと、不正なSSL証明書の場合はExceptionが発生します.<br/>
 * チェックしない設定にすると、不正なSSL証明書でも通信が可能となります.<br/>
 * チェックしない設定にしたい場合は、setSslVerify(boolean)メソッドを実行してください.
 * </p>
 *
 * @author tatsuya.kumon
 */
public class HttpConnection {
	/** ログ出力オブジェクト */
	private ApplicationLogger logger;

	/**
	 * 接続タイムアウト（ミリ秒）.<br>
	 * デフォルト10秒.
	 */
	private int timeoutMillis = 10 * 1000;

	/** BASIC認証用ID */
	private String basicAuthId;

	/** BASIC認証用パスワード */
	private String basicAuthPassword;

	/** プロキシアドレス */
	private String proxyAddress;

	/** プロキシポート番号 */
	private int proxyPort;

	/** ユーザエージェント */
	private String userAgent;

	/** リクエストURL */
	private String url;

	/** リクエストパラメーター群 */
	private List<NameValuePair> paramList;

	/** リクエストパラメーター群（URLエンコードしないパラメータ群） */
	private List<NameValuePair> noEncodingParamList;

	/** リクエスト送信時の文字コード */
	private String requestEncode;

	/** レスポンスの文字コード */
	private String responseEncode;

	/** 任意でセットするリクエストヘッダ */
	private Map<String, String> requestHeaderMap;

	/**
	 * SSL証明書チェックを行うかどうかのフラグ.<br>
	 * 行う場合はtrue.<br>
	 * デフォルトはtrue.
	 */
	private boolean isSslVerify = true;

	/** 最大リトライ回数 */
	private int retryTimes = 0;

	/** リトライ時の間隔（ミリ秒） */
	private int intervalMillis = 0;

	/** Expect: 100-Continue の設定有無 */
	private boolean useExpectContinue = true;

	/** プロキシアドレスのプロパティファイルのキー */
	private static final String PROXY_ADDRESS_KEY = "kt.net.httpclient.proxy.address";

	/** プロキシポートのプロパティファイルのキー */
	private static final String PROXY_PORT_KEY = "kt.net.httpclient.proxy.port";

	/**
	 * コンストラクタ.
	 *
	 * @param url
	 *            リクエストURL（パラメータは除外したURLであること）
	 * @param logger
	 *            ApplicationLoggerオブジェクト
	 */
	public HttpConnection(String url, ApplicationLogger logger) {
		this.logger = logger;
		this.url = url;
		KtProperties prop = KtProperties.getInstance();
		// リクエスト時、レスポンス時の文字コードのデフォルトをセットする
		requestEncode = prop.getDefaultCharset();
		// プロキシ設定
		// プロパティファイルの情報をデフォルト設定する
		if (prop.existKey(PROXY_ADDRESS_KEY) && prop.existKey(PROXY_PORT_KEY)) {
			// プロキシ情報がプロパティファイルに設定されている場合は取得
			proxyAddress = prop.getString(PROXY_ADDRESS_KEY);
			String proxyPortStr = prop.getString(PROXY_PORT_KEY);
			if (!Validator.isEmpty(proxyPortStr)) {
				// キーはあるが値が空の場合にNumberFormatExceptionが発生しないようにするための対応
				proxyPort = Integer.parseInt(proxyPortStr);
			}
		}
	}

	/**
	 * 設定項目のチェックを行います.
	 *
	 * @throws KtException
	 */
	private void checkConfiguration() throws KtException {
		// -----------------------------------
		// 各種設定チェック
		// -----------------------------------
		// 接続先URL設定チェック
		if (Validator.isEmpty(url)) {
			throw new KtException("B003", "接続先URLの設定がされていません。");
		}
	}

	/**
	 * 設定情報を元にHttpClientオブジェクトを生成する.
	 */
	private DefaultHttpClient createHttpClient() {
		// HttpClient生成
		DefaultHttpClient httpClient;
		if (isSslVerify) {
			// SSL証明書チェックを行う場合（不正証明書の場合はExceptionが発生する）
			httpClient = new DefaultHttpClient();
		} else {
			// SSL証明書チェックを行わない場合
			httpClient = new NoneSSLVerifierHttpClient();
		}
		// タイムアウト設定
		setTimeoutSetting(httpClient);
		// ユーザエージェント設定
		setUserAgentSetting(httpClient);
		// プロキシ設定
		setProxySetting(httpClient);
		// BASIC認証設定
		setBasicAuthSetting(httpClient);
		// リダイレクト設定
		setRedirectSetting(httpClient);
		// リクエストヘッダに「Expect: 100-Continue」を付加するかしないかの設定
		httpClient.getParams().setParameter(
				AllClientPNames.USE_EXPECT_CONTINUE, useExpectContinue);
		return httpClient;
	}

	/**
	 * プロキシ設定.
	 *
	 * @param httpClient
	 */
	private void setProxySetting(DefaultHttpClient httpClient) {
		// HttpClientオブジェクトにプロキシ情報をセット
		if (!Validator.isEmpty(proxyAddress) && proxyPort > 0) {
			HttpHost proxy = new HttpHost(proxyAddress, proxyPort);
			httpClient.getParams().setParameter(AllClientPNames.DEFAULT_PROXY,
					proxy);
		}
	}

	/**
	 * タイムアウト設定.
	 *
	 * @param httpClient
	 */
	private void setTimeoutSetting(DefaultHttpClient httpClient) {
		// コネクションタイムアウト設定
		httpClient.getParams().setIntParameter(
				AllClientPNames.CONNECTION_TIMEOUT, timeoutMillis);
		// ソケットタイムアウト設定
		httpClient.getParams().setIntParameter(AllClientPNames.SO_TIMEOUT,
				timeoutMillis);
		// 処理タイムアウト設定
		httpClient.getParams().setLongParameter(AllClientPNames.TIMEOUT,
				timeoutMillis);
	}

	/**
	 * ユーザエージェント設定.
	 *
	 * @param httpClient
	 */
	private void setUserAgentSetting(DefaultHttpClient httpClient) {
		if (!Validator.isEmpty(userAgent)) {
			// ユーザエージェント設定
			httpClient.getParams().setParameter(AllClientPNames.USER_AGENT,
					userAgent);
		}
	}

	/**
	 * BASIC認証設定.
	 *
	 * @param httpClient
	 */
	private void setBasicAuthSetting(DefaultHttpClient httpClient) {
		if (!Validator.isEmpty(basicAuthId)
				&& !Validator.isEmpty(basicAuthPassword)) {
			// 認証情報
			Credentials credentials = new UsernamePasswordCredentials(
					basicAuthId, basicAuthPassword);
			// 認証情報をセット（スコープはANY）
			httpClient.getCredentialsProvider().setCredentials(AuthScope.ANY,
					credentials);
		}
	}

	/**
	 * リダイレクト設定.
	 *
	 * @param httpClient
	 */
	private void setRedirectSetting(DefaultHttpClient httpClient) {
		httpClient.getParams().setParameter(AllClientPNames.HANDLE_REDIRECTS,
				false);
	}

	/**
	 * 通信を実行し、レスポンスデータをセットする.<br>
	 * 返り値として{@link ResponseData}オブジェクトを返す.
	 *
	 * @param requestBase
	 *            HttpHead or HttpGet or HttpPost オブジェクト
	 * @param savePath
	 *            レスポンスを保存するファイルパス
	 * @return ResponseData
	 * @throws Exception
	 */
	private ResponseData connect(HttpRequestBase requestBase, String savePath)
			throws Exception {
		// カスタムリクエストヘッダ追加
		if (this.requestHeaderMap != null) {
			for (String name : requestHeaderMap.keySet()) {
				requestBase.setHeader(name, requestHeaderMap.get(name));
			}
		}
		ResponseData resData = new ResponseData();
		for (int i = 0; i < this.retryTimes + 1; i++) {
			// 各種設定情報を元にHttpClientを生成する
			DefaultHttpClient httpClient = createHttpClient();
			// 通信直前のログ出力
			outputConnectLogMessage(requestBase.getMethod(), httpClient);
			FileOutputStream fos = null;
			try {
				// 通信する
				HttpResponse response = httpClient.execute(requestBase);
				// レスポンスのステータス情報を取得
				StatusLine statusLine = response.getStatusLine();
				logger.debugLog("responseCode :" + statusLine.getStatusCode());
				// レスポンスデータにセット
				resData.setStatusLine(statusLine);
				/*
				 * HEADリクエスト以外の場合のみ、レスポンス本体取得
				 */
				if (!(requestBase instanceof HttpHead)) {
					if (!Validator.isEmpty(savePath) && resData.isOkResponse()) {
						// ファイル保存が200で返ってきた場合は、レスポンスをファイルへ出力
						// 親ディレクトリの存在チェック
						FileUtil f = new FileUtil(savePath);
						f.setParentPath();
						if (!f.isDirectory()) {
							throw new KtException("A015", "親ディレクトリが存在しません:"
									+ savePath);
						}
						// ファイル保存処理
						fos = new FileOutputStream(savePath);
						response.getEntity().writeTo(fos);
					} else {
						// 保存先パスが指定されていない場合、もしくはファイル保存が200以外だった場合は、レスポンスをResponseDataへセット
						resData.setResponseData(EntityUtils
								.toByteArray(response.getEntity()));
					}
				}
				/*
				 * レスポンスの文字コード取得
				 */
				String responseEncodeTemp = EntityUtils
						.getContentCharSet(response.getEntity());
				if (!Validator.isEmpty(responseEncodeTemp)) {
					// レスポンスヘッダに文字コードがセットされている場合は、クラス変数にセットする
					this.responseEncode = responseEncodeTemp;
				}
				/*
				 * レスポンスヘッダを取得
				 */
				Header[] headers = response.getAllHeaders();
				for (Header header : headers) {
					resData.addResponseHeader(header.getName(),
							header.getValue());
				}
				/*
				 * 終了もしくはリトライ判定
				 */
				if (resData.isOkResponse()) {
					// レスポンスコードが正常の場合はbreak
					break;
				} else {
					// レスポンスコードが異常の場合
					if (i == this.retryTimes) {
						// リトライ最後の場合はbreak
						break;
					}
					// 指定間隔をあけてリトライする
					retry(i + 1,
							resData.getStatusCode() + " "
									+ resData.getStatusText());
					continue;
				}
			} catch (Exception e) {
				// Exceptionが発生した場合
				if (i == this.retryTimes) {
					// リトライ最後の場合はExceptionをthrow
					throw e;
				}
				// 指定間隔をあけてリトライする
				retry(i + 1, e.getClass().getName());
				continue;
			} finally {
				// FileOutputStreamのclose
				if (fos != null)
					fos.close();
				// 接続を解放
				httpClient.getConnectionManager().shutdown();
				logger.debugLog("HTTP通信:close処理完了");
			}
		}
		return resData;
	}

	/**
	 * リトライ処理.
	 *
	 * @param time
	 *            何回目か
	 * @param causeText
	 *            エラーの原因
	 * @throws InterruptedException
	 */
	private void retry(int time, String causeText) throws InterruptedException {
		logger.warnLog("A023", "HTTP通信にてエラー発生のためリトライ開始 [回数]" + time + "/"
				+ this.retryTimes + " [url]" + url + " [原因]" + causeText);
		Thread.sleep(this.intervalMillis);
	}

	/**
	 * HEADでHTTP(HTTPS)通信を行い、結果を取得します.
	 *
	 * @return ResponseData
	 * @throws Exception
	 *             接続中に例外発生した場合
	 */
	public ResponseData executeHeadMethod() throws Exception {
		// 設定情報の内容をチェック
		checkConfiguration();
		// パラメータセット
		// HttpHeadを生成
		HttpHead head = new HttpHead(this.createUrl());
		// 通信実行
		return connect(head, null);
	}

	/**
	 * GETでHTTP(HTTPS)通信を行い、結果を取得します.
	 * <p>
	 * レスポンス本体を{@link ResponseData}にセットします.
	 * </p>
	 *
	 * @return ResponseData
	 * @throws Exception
	 *             接続中に例外発生した場合
	 */
	public ResponseData executeGetMethod() throws Exception {
		return executeGetMethod(null);
	}

	/**
	 * GETでHTTP(HTTPS)通信を行い、結果を取得します.
	 *
	 * @param savePath
	 *            レスポンスを保存するファイルパス
	 * @return ResponseData
	 * @throws Exception
	 *             接続中に例外発生した場合
	 */
	public ResponseData executeGetMethod(String savePath) throws Exception {
		// 設定情報の内容をチェック
		checkConfiguration();
		// HttpGetを生成
		HttpGet get = new HttpGet(this.createUrl());
		// 通信実行
		return connect(get, savePath);
	}

	/**
	 * GETならびにHEADリクエスト用のURLを生成する.
	 *
	 * @return 生成したURL
	 */
	private String createUrl() {
		// パラメータセット
		StringBuilder queryString = new StringBuilder();
		if (paramList != null && paramList.size() > 0) {
			queryString
					.append(URLEncodedUtils.format(paramList, requestEncode));
		}
		// パラメータ（URLエンコードなし）セット
		for (int i = 0; noEncodingParamList != null
				&& i < noEncodingParamList.size(); i++) {
			if (queryString.length() > 0) {
				queryString.append('&');
			}
			NameValuePair param = noEncodingParamList.get(i);
			queryString.append(param.getName());
			queryString.append('=');
			queryString.append(param.getValue());
		}
		StringBuilder fullUrl = new StringBuilder();
		fullUrl.append(url);
		if (queryString.length() > 0) {
			fullUrl.append('?');
			fullUrl.append(queryString);
		}
		return fullUrl.toString();
	}

	/**
	 * POSTでHTTP(HTTPS)通信を行い、結果を取得します.
	 * <p>
	 * レスポンス本体を{@link ResponseData}にセットします.
	 * </p>
	 *
	 * @return ResponseData
	 * @throws Exception
	 *             接続中に例外発生した場合
	 */
	public ResponseData executePostMethod() throws Exception {
		return executePostMethod(null);
	}

	/**
	 * POSTでHTTP(HTTPS)通信を行い、結果を取得します.
	 * <p>
	 * レスポンス本体を指定したファイルに保存します.<br>
	 * テキストでもバイナリでも可能です.<br>
	 * 正常なレスポンスの場合はレスポンス本体はファイル保存されるのみで、{@link ResponseData}にはセットされません.<br>
	 * NotFoundなど、
	 * </p>
	 *
	 * @param savePath
	 *            レスポンスを保存するファイルパス
	 * @return ResponseData
	 * @throws Exception
	 *             接続中に例外発生した場合
	 */
	public ResponseData executePostMethod(String savePath) throws Exception {
		// 設定情報の内容をチェック
		checkConfiguration();
		// HttpPostを生成
		HttpPost post = new HttpPost(url);
		// パラメータセット
		if (paramList != null && paramList.size() > 0) {
			post.setEntity(new UrlEncodedFormEntity(paramList, requestEncode));
		}
		// 通信実行
		return connect(post, savePath);
	}

	/**
	 * 通信時のログ出力メッセージ作成.
	 *
	 * @param method
	 *            接続メソッド
	 * @param httpClient
	 *            DefaultHttpClientオブジェクト
	 */
	private void outputConnectLogMessage(String method,
			DefaultHttpClient httpClient) {
		StringBuilder msg = new StringBuilder();
		msg.append("HttpConnection");
		msg.append(" [method]");
		msg.append(method);
		msg.append(" [url]");
		msg.append(url);
		msg.append(" [proxy]");
		if (Validator.isEmpty(proxyAddress)) {
			// プロキシ設定なし
			msg.append("(none)");
		} else {
			// プロキシ設定あり
			msg.append(proxyAddress);
			msg.append(",");
			msg.append(proxyPort);
		}
		msg.append(" [basic-auth]");
		if (Validator.isEmpty(basicAuthId)) {
			// BASIC認証なし
			msg.append("(none)");
		} else {
			// BASIC認証あり
			msg.append(basicAuthId);
		}
		msg.append(" [useragent]");
		if (Validator.isEmpty(userAgent)) {
			msg.append(httpClient.getParams().getParameter(
					AllClientPNames.USER_AGENT));
		}
		// ログ出力
		logger.infoLog("A022", msg.toString());
	}

	/**
	 * HTTPパラメータをセット.
	 *
	 * @param name
	 *            パラメータ名
	 * @param value
	 *            パラメータ値
	 */
	public void addParameter(String name, String value) {
		// 値がnullの場合は空文字をセット
		if (value == null) {
			value = "";
		}
		// paramListがnullの場合はインスタンス化
		if (paramList == null) {
			paramList = new ArrayList<NameValuePair>();
		}
		// パラメータを追加
		paramList.add(new BasicNameValuePair(name, value));
	}

	/**
	 * URLエンコードしないHTTPパラメータをセット.
	 * <p>
	 * POSTメソッドの場合はURLエンコードは関係ありませんが、このメソッドで追加されたパラメータはセットされます.
	 * </p>
	 *
	 * @param name
	 *            パラメータ名
	 * @param value
	 *            パラメータ値
	 */
	public void addNoEncodingParameter(String name, String value) {
		// 値がnullの場合は空文字をセット
		if (value == null) {
			value = "";
		}
		// paramListがnullの場合はインスタンス化
		if (noEncodingParamList == null) {
			noEncodingParamList = new ArrayList<NameValuePair>();
		}
		// パラメータを追加
		noEncodingParamList.add(new BasicNameValuePair(name, value));
	}

	/**
	 * BASIC認証設定を行う.
	 *
	 * @param basicAuthId
	 *            ID
	 * @param basicAuthPassword
	 *            パスワード
	 */
	public void setBasicAuth(String basicAuthId, String basicAuthPassword) {
		this.basicAuthId = basicAuthId;
		this.basicAuthPassword = basicAuthPassword;
	}

	/**
	 * タイムアウト秒数を設定する.<br>
	 * 接続タイムアウト、ソケットタイムアウト、処理タイムアウト全てに同じ値が設定されます.
	 *
	 * @param timeoutSecond
	 *            タイムアウトする秒数.
	 */
	public void setTimeoutSecond(int timeoutSecond) {
		this.timeoutMillis = timeoutSecond * 1000;
	}

	/**
	 * プロキシ設定.<br>
	 * デフォルトでよい場合は実行不要です.<br>
	 * デフォルトは設定有りだが、プロキシ無しにしたい場合はproxyAddressにnullを指定してください.
	 *
	 * @param proxyAddress
	 *            プロキシアドレス
	 * @param proxyPort
	 *            プロキシポート
	 */
	public void setProxy(String proxyAddress, int proxyPort) {
		// プロキシ設定チェック
		if (!Validator.isEmpty(proxyAddress)) {
			// アドレスをチェック
			if (!proxyAddress.matches("[a-zA-Z0-9._~-]+")) {
				throw new KtException("B004",
						"プロキシアドレスの設定が不正な値です。[proxyAddress:" + proxyAddress
								+ "]");
			}
			// ポートをチェック
			if (proxyPort <= 0) {
				throw new KtException("B004", "プロキシポートの設定が不正な値です。[proxyPort:"
						+ proxyPort + "]");
			}
		}
		// 設定
		this.proxyAddress = proxyAddress;
		this.proxyPort = proxyPort;
	}

	/**
	 * ユーザエージェントを指定する.<br>
	 * このメソッドを実行しない場合は、Apache-HttpClient標準のユーザエージェントとなります.
	 *
	 * @param userAgent
	 *            ユーザエージェント.
	 */
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	/**
	 * リクエスト送信時の文字コードを設定.
	 *
	 * @param requestEncode
	 *            リクエスト送信時の文字コード
	 */
	public void setRequestEncode(String requestEncode) {
		this.requestEncode = requestEncode;
	}

	/**
	 * レスポンスの文字コードを設定.
	 * <p>
	 * HTTPレスポンスのヘッダContent-Typeで文字コードが指定されている場合は、<br>
	 * このメソッドを実行しても反映されません.<br>
	 * </p>
	 *
	 * @param responseEncode
	 *            レスポンスの文字コード
	 */
	public void setResponseEncode(String responseEncode) {
		this.responseEncode = responseEncode;
	}

	/**
	 * SSL証明書チェックを実施したくない場合に実行する.
	 * <p>
	 * SSL証明書が不正（自己証明書や有効期限切れの証明書など）であっても通信可能となります.
	 * </p>
	 */
	public void setNoSslVerify() {
		this.isSslVerify = false;
	}

	/**
	 * リトライを行う場合は実行する.
	 * <p>
	 * デフォルトはリトライ無し.
	 * </p>
	 *
	 * @param retryTimes
	 *            最大リトライ回数
	 * @param intervalMillis
	 *            リトライ時の間隔（ミリ秒）
	 */
	public void setRetrySetting(int retryTimes, int intervalMillis) {
		this.retryTimes = retryTimes;
		this.intervalMillis = intervalMillis;
	}

	/**
	 * ヘッダに Expect: 100-Continue を付加したくない場合に実行する.
	 * <p>
	 * 相手先サーバによっては未対応の場合417エラーを返してくるので、その場合は実行すると回避できる.
	 * </p>
	 */
	public void setNotUseExpectContinue() {
		this.useExpectContinue = false;
	}

	/**
	 * リクエストヘッダに任意の値をセットしたい場合に実行する.
	 *
	 * @param name
	 *            リクエストヘッダ名
	 * @param value
	 *            リクエストヘッダ値
	 */
	public void setRequestHeader(String name, String value) {
		if (this.requestHeaderMap == null) {
			this.requestHeaderMap = new HashMap<String, String>();
		}
		this.requestHeaderMap.put(name, value);
	}

	/**
	 * レスポンス情報格納クラス.
	 *
	 * @author tatsuya.kumon
	 */
	public class ResponseData {
		/** レスポンスのステータス情報 */
		private StatusLine statusLine;

		/** レスポンス本体 */
		private byte[] responseData;

		/** レスポンスヘッダ群 */
		private Map<String, String> responseHeaderMap;

		private void setStatusLine(StatusLine statusLine) {
			this.statusLine = statusLine;
		}

		private void setResponseData(byte[] responseData) {
			this.responseData = responseData;
		}

		private void addResponseHeader(String name, String value) {
			if (this.responseHeaderMap == null) {
				this.responseHeaderMap = new HashMap<String, String>();
			}
			this.responseHeaderMap.put(name, value);
		}

		/**
		 * レスポンスのステータスコードを取得する.
		 *
		 * @return ステータスコード
		 */
		public int getStatusCode() {
			return statusLine.getStatusCode();
		}

		/**
		 * レスポンスのステータステキストを取得する.
		 *
		 * @return ステータステキスト
		 */
		public String getStatusText() {
			return statusLine.getReasonPhrase();
		}

		/**
		 * レスポンスをテキスト形式で取得する.
		 *
		 * @return レスポンステキスト
		 * @throws UnsupportedEncodingException
		 *             指定されたエンコードが不正な場合
		 * @throws IOException
		 *             入出力エラーが発生した場合
		 */
		public String getResponseText() throws UnsupportedEncodingException,
				IOException {
			String responseText = "";
			if (this.responseData != null) {
				if (Validator.isEmpty(responseEncode)) {
					responseEncode = KtProperties.getInstance()
							.getDefaultCharset();
				}
				responseText = new String(responseData, responseEncode);
			}
			return responseText;
		}

		/**
		 * HTTPレスポンスコードが200であればtrueを返す.
		 *
		 * @return レスポンスコードが200ならtrue
		 */
		public boolean isOkResponse() {
			return (getStatusCode() == HttpStatus.SC_OK);
		}

		/**
		 * レスポンスヘッダ値を取得する.
		 * <p>
		 * ヘッダ名は大文字小文字を区別せずマッチングを行います.
		 * </p>
		 *
		 * @param name
		 *            ヘッダ名
		 * @return ヘッダ値
		 */
		public String getHeader(String name) {
			if (this.responseHeaderMap == null || Validator.isEmpty(name)) {
				return null;
			}
			Set<String> keySet = this.responseHeaderMap.keySet();
			for (String key : keySet) {
				// 小文字変換してマッチングをかける
				if (key.toLowerCase().equals(name.toLowerCase())) {
					return this.responseHeaderMap.get(key);
				}
			}
			// ここまで来たらマッチした項目がなかったということ
			return null;
		}
	}
}
