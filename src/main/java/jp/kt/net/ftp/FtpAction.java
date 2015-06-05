package jp.kt.net.ftp;

import java.io.IOException;
import java.util.List;

import jp.kt.exception.KtException;
import jp.kt.logger.ApplicationLogger;

import org.apache.commons.net.ftp.FTPFile;

/**
 * FTP処理.
 * <p>
 * アクション毎にFTPサーバへの接続ならびに切断を行うクラスです.<br>
 * FTPサーバへの接続ならびに切断は自動的に行われます.
 * </p>
 * <hr>
 *
 * <pre>
 * 【共通コード】FTPサーバへの接続
 * 	// FTPサーバ情報ならびに認証情報をセットしてFtpActionオブジェクトを生成する
 * 	FtpAction ftp = new FtpAction(FTP_HOST, FTP_USER, FTP_PW, logger);
 * 	// PASVモードにしたくない場合は下記メソッドを実行する
 * 	ftp.setNoPassiveMode();
 * 	// プロキシ経由で接続する場合は下記メソッドを実行する
 * 	ftp.setProxy(PROXY_HOST, PROXY_PORT);
 * </pre>
 * <hr>
 *
 * <pre>
 * 【サンプル1】指定ディレクトリ内のファイルやディレクトリリストを取得する
 * 	FTPFile[] fileList = ftp.listFiles(remoteDirectory);
 * 	for (FTPFile file : fileList) {
 * 		System.out.println(file.toString());
 * 	}
 * </pre>
 * <hr>
 *
 * <pre>
 * 【サンプル2】FTPサーバ上にディレクトリを作成する
 * 	// ディレクトリが存在しなければディレクトリを作成する
 * 	if (!ftp.existDirectory(dir)) {
 * 		ftp.makeDirectory(dir);
 * 	}
 * </pre>
 * <hr>
 *
 * <pre>
 * 【サンプル3】ファイルのPUT
 * 	// PUT処理情報リストを生成
 * 	List<PutData> list = new ArrayList<PutData>();
 * 	list.add(new PutData(localFilePath1, remoteDirPath1));
 * 	list.add(new PutData(localFilePath2, remoteDirPath2));
 * 	list.add(new PutData(localFilePath3, remoteDirPath3));
 * 	ftp.put(list);
 * 	// PUT結果を出力
 * 	for (PutData putData : list) {
 * 		System.out.println(putData.getLocalFilePath());
 * 		System.out.println(putData.getRemoteDirPath());
 * 		System.out.println(putData.isSuccess());
 * 		System.out.println(putData.getErrorMessage());
 * 	}
 * </pre>
 *
 * @author tatsuya.kumon
 */
public class FtpAction extends FtpBaseAction {
	/**
	 * コンストラクタ.
	 * <p>
	 * デフォルトポート番号（21）での接続
	 * </p>
	 *
	 * @param hostname
	 *            FTPサーバ名
	 * @param port
	 *            ポート番号
	 * @param username
	 *            ログインユーザ名
	 * @param password
	 *            ログインパスワード
	 * @param logger
	 *            {@link ApplicationLogger}オブジェクト
	 */
	public FtpAction(String hostname, int port, String username,
			String password, ApplicationLogger logger) {
		super(hostname, port, username, password, logger);
	}

	/**
	 * コンストラクタ.
	 *
	 * @param hostname
	 *            FTPサーバ名
	 * @param username
	 *            ログインユーザ名
	 * @param password
	 *            ログインパスワード
	 * @param logger
	 *            {@link ApplicationLogger}オブジェクト
	 */
	public FtpAction(String hostname, String username, String password,
			ApplicationLogger logger) {
		super(hostname, username, password, logger);
	}

	/**
	 * 指定パスのファイルリストを取得.
	 *
	 * @param path
	 *            パス
	 * @return ファイルリスト
	 * @throws IOException
	 *             入出力エラーが発生した場合
	 */
	public FTPFile[] listFiles(String path) throws IOException {
		try {
			// 接続
			super.connectBase();
			// リスト取得
			return super.listFilesBase(path);
		} finally {
			// 切断
			super.disconnectBase();
		}
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
	 *             入出力エラーが発生した場合
	 */
	public void makeDirectory(String path) throws IOException {
		try {
			// 接続
			super.connectBase();
			// ディレクトリ作成
			super.makeDirectoryBase(path);
		} finally {
			// 切断
			super.disconnectBase();
		}
	}

	/**
	 * ディレクトリ存在チェック.
	 *
	 * @param path
	 *            ディレクトリの絶対パス
	 * @return 存在していればtrue
	 * @throws IOException
	 *             入出力エラーが発生した場合
	 */
	public boolean existDirectory(String path) throws IOException {
		try {
			// 接続
			super.connectBase();
			// ディレクトリ存在チェック
			return super.existDirectoryBase(path);
		} finally {
			// 切断
			super.disconnectBase();
		}
	}

	/**
	 * ディレクトリの削除.
	 * <p>
	 * 強制的にディレクトリ配下のサブディレクトリやファイルも全て削除します.
	 * </p>
	 *
	 * @param path
	 *            ディレクトリの絶対パス
	 * @throws IOException
	 *             入出力エラーが発生した場合
	 */
	public void deleteDirectory(String path) throws IOException {
		try {
			// 接続
			super.connectBase();
			// ディレクトリ削除
			super.deleteDirectoryBase(path);
		} finally {
			// 切断
			super.disconnectBase();
		}
	}

	/**
	 * ファイルの削除.
	 *
	 * @param path
	 *            ディレクトリの絶対パス
	 * @throws IOException
	 *             入出力エラーが発生した場合
	 */
	public void deleteFile(String path) throws IOException {
		try {
			// 接続
			super.connectBase();
			// ファイル削除
			super.deleteFileBase(path);
		} finally {
			// 切断
			super.disconnectBase();
		}
	}

	/**
	 * ファイルをFTPサーバへPUTする.
	 * <p>
	 * 複数ファイルのPUTを1回の接続で行います.<br>
	 * 既に存在する場合も上書きします.<br>
	 * 失敗した場合でもExceptionはthrowされず {@link PutData}オブジェクトに結果ならびにエラーメッセージがセットされます.
	 * </p>
	 *
	 * @param putList
	 *            PUT処理情報のリスト
	 * @throws IOException
	 *             入出力エラーが発生した場合
	 */
	public void put(List<PutData> putList) throws IOException {
		try {
			// 接続
			super.connectBase();
			// ファイルのPUT
			for (PutData putData : putList) {
				super.put(putData);
			}
		} finally {
			// 切断
			super.disconnectBase();
		}
	}
}
