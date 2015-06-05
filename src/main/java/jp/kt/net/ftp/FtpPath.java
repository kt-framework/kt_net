package jp.kt.net.ftp;

import jp.kt.exception.KtException;
import jp.kt.tool.StringUtil;
import jp.kt.tool.Validator;

/**
 * FTPサーバパス管理クラス.
 *
 * @author tatsuya.kumon
 */
class FtpPath {
	/** パスの区切り文字 */
	private static final String DELIM = "/";

	/** パスを分割して配列にしたもの */
	private String[] pathArray;

	/** 現在のパスの配列数 */
	private int currentCount;

	/**
	 * コンストラクタ.
	 *
	 * @param path
	 *            ディレクトリパス
	 */
	FtpPath(String path) {
		if (Validator.isEmpty(path) || !path.startsWith(DELIM)) {
			// パスが空、もしくは絶対パスではない場合はエラー
			throw new KtException("A047", "パスが空もしくは絶対パスになっていません [" + path + "]");
		}
		// 分割
		this.pathArray = StringUtil.split(path, DELIM);
		this.currentCount = pathArray.length;
	}

	/**
	 * 1つ上のパスへ移動.
	 */
	void moveParent() {
		if (currentCount > 0) {
			currentCount--;
		}
	}

	/**
	 * 1つ下のパスへ移動.
	 */
	void moveChild() {
		if (currentCount < pathArray.length) {
			currentCount++;
		}
	}

	/**
	 * 現在のパスを返す.
	 *
	 * @return 現在のパス
	 */
	String getCurrentPath() {
		if (currentCount == 0) {
			return DELIM;
		}
		StringBuilder path = new StringBuilder();
		for (int i = 0; i < currentCount; i++) {
			path.append(DELIM);
			path.append(pathArray[i]);
		}
		return path.toString();
	}

	/**
	 * 現在のパスの配列数を返す.
	 *
	 * @return 現在のパスの配列数
	 */
	int getCurrentCount() {
		return currentCount;
	}
}
