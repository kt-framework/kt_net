package jp.kt.net.ftp;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import jp.kt.exception.KtException;
import jp.kt.logger.ApplicationLogger;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

/**
 * FTP接続ならびに操作メインクラス.
 *
 * @author tatsuya.kumon
 */
abstract class FtpBaseAction {
	/** FTP接続 */
	private FTPClient ftpClient;

	/** FTPサーバ名 */
	private String host;

	/** FTPポート番号 */
	private int port;

	/** ログインユーザ名 */
	private String user;

	/** ログインパスワード */
	private String password;

	/** PASVモードフラグ */
	private boolean isPassiveMode = true;

	/** Logger */
	private ApplicationLogger logger;

	/** デフォルトタイムアウト（ミリ秒） */
	private static final int TIMEOUT_MILLIS = 10000;

	/**
	 * コンストラクタ.
	 * <p>
	 * デフォルトポート番号（21）での接続
	 * </p>
	 *
	 * @param host
	 *            FTPサーバ名
	 * @param user
	 *            ログインユーザ名
	 * @param password
	 *            ログインパスワード
	 * @param logger
	 *            {@link ApplicationLogger}オブジェクト
	 */
	FtpBaseAction(String host, String user, String password,
			ApplicationLogger logger) {
		this(host, FTP.DEFAULT_PORT, user, password, logger);
	}

	/**
	 * コンストラクタ.
	 *
	 * @param host
	 *            FTPサーバ名
	 * @param port
	 *            FTPポート番号
	 * @param user
	 *            ログインユーザ名
	 * @param password
	 *            ログインパスワード
	 * @param logger
	 *            {@link ApplicationLogger}オブジェクト
	 */
	FtpBaseAction(String host, int port, String user, String password,
			ApplicationLogger logger) {
		this.host = host;
		this.port = port;
		this.user = user;
		this.password = password;
		this.logger = logger;
	}

	/**
	 * プロキシ設定.
	 * <p>
	 * デフォルトではプロキシは使用しないので、プロキシ経由で接続する場合はこのメソッドを実行する.
	 * </p>
	 *
	 * @param proxyHost
	 *            プロキシホスト名
	 * @param proxyPort
	 *            プロキシポート番号
	 */
	public void setProxy(String proxyHost, int proxyPort) {
		this.user = this.user + "@" + this.host + ":" + this.port;
		this.host = proxyHost;
		this.port = proxyPort;
	}

	/**
	 * PASVモード未使用設定.
	 * <p>
	 * デフォルトではPASVモードを使用するため、PASVモードで接続しない場合はこのメソッドを実行する.
	 * </p>
	 */
	public void setNoPassiveMode() {
		this.isPassiveMode = false;
	}

	/**
	 * FTPサーバへ接続.
	 *
	 * @throws IOException
	 */
	void connectBase() throws IOException {
		ftpClient = new FTPClient();
		// タイムアウト設定１
		ftpClient.setConnectTimeout(TIMEOUT_MILLIS);
		ftpClient.setDefaultTimeout(TIMEOUT_MILLIS);
		// サーバ接続
		ftpClient.connect(host, port);
		// 応答
		int reply = ftpClient.getReplyCode();
		if (!FTPReply.isPositiveCompletion(reply)) {
			// コードが200番台以外ならエラー
			throw new KtException("A043", createMessage("FTPサーバへの接続に失敗しました"));
		}
		// ログイン
		if (!ftpClient.login(user, password)) {
			// ログイン失敗
			throw new KtException("A044", createMessage("FTPサーバへのログインに失敗しました"));
		}
		// タイムアウト設定２
		ftpClient.setSoTimeout(TIMEOUT_MILLIS);
		ftpClient.setDataTimeout(TIMEOUT_MILLIS);
		// PASVモードに設定
		if (isPassiveMode) {
			ftpClient.enterLocalPassiveMode();
		}
		// バイナリモード
		ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
		logger.infoLog("A045", createMessage("FTPサーバへ正常に接続/ログインしました"));
	}

	/**
	 * FTPサーバから切断.
	 * <p>
	 * 未接続状態でこのメソッドを呼んでもExceptionは発生しません.
	 * </p>
	 *
	 * @throws IOException
	 */
	void disconnectBase() throws IOException {
		if (ftpClient != null) {
			// ログアウト
			ftpClient.logout();
			// 切断
			if (ftpClient.isConnected()) {
				ftpClient.disconnect();
			}
			logger.infoLog("A046", createMessage("FTPサーバから正常にログアウト/切断されました"));
		}
	}

	/**
	 * 指定パスのファイルリストを取得.
	 *
	 * @param path
	 *            パス
	 * @return ファイルリスト
	 * @throws IOException
	 */
	FTPFile[] listFilesBase(String path) throws IOException {
		// 絶対パスであることをチェック
		new FtpPath(path);
		// ディレクトリであることをチェック
		if (!existDirectoryBase(path)) {
			throw new KtException("A048", "ディレクトリが存在しません [" + path + "]");
		}
		// リスト取得
		return ftpClient.listFiles(path);
	}

	/**
	 * ディレクトリを作成する.
	 * <p>
	 * サブディレクトリも強制的に作成します.<br>
	 * パーミッションが無かったり、既にディレクトリが存在するなどの要因で<br>
	 * ディレクトリ作成に失敗した場合は {@link KtException}がthrowされます.
	 * </p>
	 *
	 * @param path
	 *            ディレクトリの絶対パス
	 * @throws IOException
	 */
	void makeDirectoryBase(String path) throws IOException {
		FtpPath ftpPath = new FtpPath(path);
		int allPathCount = ftpPath.getCurrentCount();
		// まずひとつディレクトリ作成できるところを探して作成
		boolean isSuccess = false;
		for (int i = 0; i < allPathCount; i++) {
			isSuccess = ftpClient.makeDirectory(ftpPath.getCurrentPath());
			if (isSuccess) {
				// まずひとつディレクトリ作成に成功したらbreakする
				break;
			}
			// 失敗したら一つ上のディレクトリへ移動
			ftpPath.moveParent();
		}
		if (!isSuccess) {
			// ここに来るということは、rootまで行ってしまいディレクトリ作成できなかった
			// もしくは、既にディレクトリが存在していた
			throw new KtException("A048", "ディレクトリ作成に失敗しました [" + path + "]");
		}
		// サブディレクトリを作成する
		int pathCount = ftpPath.getCurrentCount();
		for (int i = 0; i < allPathCount - pathCount; i++) {
			// 一つ下のディレクトリへ移動
			ftpPath.moveChild();
			// ディレクトリ作成（これは成功するはず）
			ftpClient.makeDirectory(ftpPath.getCurrentPath());
		}
	}

	/**
	 * ディレクトリ存在チェック.
	 *
	 * @param path
	 *            ディレクトリの絶対パス
	 * @return 存在していればtrue
	 * @throws IOException
	 */
	boolean existDirectoryBase(String path) throws IOException {
		// 絶対パスであることをチェック
		new FtpPath(path);
		// workディレクトリを移動して成功したら存在するということ
		return ftpClient.changeWorkingDirectory(path);
	}

	/**
	 * ディレクトリを削除する.
	 * <p>
	 * 強制的にディレクトリ配下のサブディレクトリやファイルも全て削除します.
	 * </p>
	 *
	 * @param path
	 *            ディレクトリの絶対パス
	 * @throws IOException
	 */
	void deleteDirectoryBase(String path) throws IOException {
		// 絶対パスであることをチェック
		new FtpPath(path);
		// workディレクトリ移動
		if (!ftpClient.changeWorkingDirectory(path)) {
			throw new KtException("A048", "ディレクトリが存在しません [" + path + "]");
		}
		// ディレクトリ削除
		deleteDirectory();
	}

	/**
	 * ディレクトリを再帰的に削除する.
	 *
	 * @throws IOException
	 */
	private void deleteDirectory() throws IOException {
		// workディレクトリ内のリストを取得
		FTPFile[] files = ftpClient.listFiles();
		for (FTPFile file : files) {
			if (file.isDirectory()) {
				// ディレクトリの場合
				// ディレクトリ移動
				ftpClient.changeWorkingDirectory(file.getName());
				// 再帰呼び出し
				deleteDirectory();
				// 1つ上のディレクトリに戻る
				ftpClient.changeToParentDirectory();
			} else {
				// ディレクトリ以外の場合
				// ファイル削除
				if (!ftpClient.deleteFile(file.getName())) {
					throw new KtException("A048", "ファイル削除に失敗しました ["
							+ ftpClient.printWorkingDirectory() + "/"
							+ file.getName() + "]");
				}
			}
		}
		// workディレクトリ内が空になっているはずなので削除
		if (!ftpClient.removeDirectory(ftpClient.printWorkingDirectory())) {
			throw new KtException("A048", "ディレクトリ削除に失敗しました ["
					+ ftpClient.printWorkingDirectory() + "]");
		}
	}

	/**
	 * ファイルを削除する.
	 *
	 * @param path
	 *            ファイルの絶対パス
	 * @throws IOException
	 */
	void deleteFileBase(String path) throws IOException {
		// 絶対パスであることをチェック
		new FtpPath(path);
		// workディレクトリ移動
		if (!ftpClient.deleteFile(path)) {
			throw new KtException("A048", "ファイル削除に失敗しました [" + path + "]");
		}
	}

	/**
	 * 1ファイルのPUT.
	 * <p>
	 * 既に存在する場合も上書きします.<br>
	 * 失敗した場合でもExceptionはthrowされず {@link PutData}オブジェクトに結果ならびにエラーメッセージがセットされます.
	 * </p>
	 *
	 * @param putData
	 *            PUT処理情報
	 * @throws IOException
	 */
	void put(PutData putData) throws IOException {
		InputStream is = null;
		try {
			is = new BufferedInputStream(new FileInputStream(
					putData.getLocalFilePath()));
			// workディレクトリを移動
			boolean result = ftpClient.changeWorkingDirectory(putData
					.getRemoteDirPath());
			if (!result) {
				// FTPサーバ上にディレクトリが存在しない
				putData.fail("リモートディレクトリが存在しませんでした");
			} else {
				// PUT実行
				result = ftpClient.storeFile(putData.getFileName(), is);
				if (!result) {
					putData.fail("PUT処理が失敗しました");
				} else {
					// PUT成功
					putData.success();
				}
			}
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}

	/**
	 * 接続情報を付加したログ用メッセージ生成.
	 *
	 * @param message
	 *            メッセージ
	 * @return 接続情報を付加したメッセージ
	 */
	private String createMessage(String message) {
		StringBuilder msg = new StringBuilder();
		msg.append(message);
		msg.append(" [host=");
		msg.append(host);
		msg.append("][port=");
		msg.append(port);
		msg.append("][user=");
		msg.append(user);
		msg.append("]");
		return msg.toString();
	}
}
