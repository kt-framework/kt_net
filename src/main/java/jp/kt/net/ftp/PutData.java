package jp.kt.net.ftp;

import java.io.Serializable;

import jp.kt.exception.KtException;
import jp.kt.fileio.FileUtil;

/**
 * PUT処理情報クラス.
 *
 * @author tatsuya.kumon
 */
public class PutData implements Serializable {
	private static final long serialVersionUID = 1L;

	/** PUTするローカルファイル */
	private FileUtil localFile;

	/** FTP先のディレクトリ */
	private String remoteDirPath;

	/** PUT処理結果（成功ならtrue） */
	private Boolean result;

	/** エラーメッセージ */
	private String errorMessage;

	/**
	 * コンストラクタ.
	 *
	 * @param localFilePath
	 *            PUTするローカルパス
	 * @param remoteDirPath
	 *            FTP先のディレクトリ
	 */
	public PutData(String localFilePath, String remoteDirPath) {
		// ローカルファイル
		this.localFile = new FileUtil(localFilePath);
		if (!localFile.isFile()) {
			throw new KtException("A049", "PUTするファイルが存在しません [" + localFilePath
					+ "]");
		}
		// サーバディレクトリ
		// この時点で絶対パスであることをチェック
		new FtpPath(remoteDirPath);
		this.remoteDirPath = remoteDirPath;
	}

	/**
	 * 処理成功.
	 */
	void success() {
		this.result = true;
	}

	/**
	 * 処理失敗.
	 *
	 * @param errorMessage
	 *            エラーメッセージ
	 */
	void fail(String errorMessage) {
		this.result = false;
		this.errorMessage = errorMessage;
	}

	/**
	 * エラーメッセージを取得.
	 *
	 * @return エラーメッセージ
	 */
	public String getErrorMessage() {
		return this.errorMessage;
	}

	/**
	 * PUT結果を取得する.
	 * <p>
	 * PUT処理前の場合は {@link KtException} がthrowされます.
	 * </p>
	 *
	 * @return trueなら成功
	 */
	public boolean isSuccess() {
		if (this.result == null) {
			throw new KtException("A050", "PUT処理前です [" + localFile.getPath()
					+ "] [" + remoteDirPath + "]");
		}
		return this.result;
	}

	/**
	 * ローカルファイルのパスを返す.
	 *
	 * @return ローカルファイルのパス
	 */
	public String getLocalFilePath() {
		return this.localFile.getPath();
	}

	/**
	 * PUTするファイル名のみを返す.
	 *
	 * @return ファイル名
	 */
	String getFileName() {
		return this.localFile.getName();
	}

	/**
	 * FTP先のディレクトリパスを返す
	 *
	 * @return FTP先のディレクトリパス
	 */
	public String getRemoteDirPath() {
		return this.remoteDirPath;
	}

	/*
	 * (非 Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[localFilePath=");
		sb.append(this.localFile.getPath());
		sb.append("] ");
		sb.append("[remoteDirPath=");
		sb.append(this.remoteDirPath);
		sb.append("] ");
		sb.append("[result=");
		sb.append(this.result);
		if (!this.result) {
			sb.append(",");
			sb.append(this.errorMessage);
		}
		sb.append("]");
		return sb.toString();
	}
}
