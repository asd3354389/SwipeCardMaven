package com.swipecard;

import java.io.Reader;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.swing.JOptionPane;

import org.apache.ibatis.exceptions.ExceptionFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.log4j.Logger;

import com.swipecard.util.DESUtils;

public class CheckCurrentVersion implements Runnable {
	private static Logger logger = Logger.getLogger(CheckCurrentVersion.class);
	private boolean active;
	private static SqlSessionFactory sqlSessionFactory;
	private static Reader reader;
	private String localSwipeCardVersion;
	private Timestamp CurrentDBTimeStamp;
	static Properties pps = new Properties();
	static Reader pr = null;
	static {
		try {
			pr = Resources.getResourceAsReader("db.properties");
			pps.load(pr);
			pps.setProperty("username", DESUtils.getDecryptString(pps.getProperty("username")));
			pps.setProperty("password", DESUtils.getDecryptString(pps.getProperty("password")));
			reader = Resources.getResourceAsReader("Configuration.xml");
			/*
			 * String filePath = System.getProperty("user.dir") +
			 * "/Configuration.xml"; FileReader reader=new FileReader(filePath);
			 */
			sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader,pps);
		} catch (Exception e) {
			logger.error("版本檢查時 Error building SqlSession，原因:"+e);
			e.printStackTrace();
		}
	}

	public CheckCurrentVersion(String currentVersion) {
		active = true;
		localSwipeCardVersion = currentVersion;
	}

	private boolean CheckCurrentVersionIsLatest(String localVersion) {
		SqlSession session = null;
		HashMap<String, Object> versionFromDB = null;
		boolean IsLatest = false;
		try {
			session = sqlSessionFactory.openSession();
			List<HashMap<String, Object>> VersionsFromDB = session.selectList("getCurrentVersionFromDB");
			Iterator<HashMap<String, Object>> iterator = VersionsFromDB.iterator();
			while (iterator.hasNext()) {
				versionFromDB = iterator.next();
			}

			CurrentDBTimeStamp = (Timestamp) versionFromDB.get("db_time");

			if (versionFromDB.get("version").equals(localVersion))
				IsLatest = true;
			else
				IsLatest = false;
		} catch (Exception ex) {
			logger.error("版本檢查時 Error building SqlSession，原因:"+ex);
			SwipeCardNoDB d = new SwipeCardNoDB(null);
			throw ExceptionFactory.wrapException("Error opening session.  Cause: " + ex, ex);
		} finally {
			ErrorContext.instance().reset();
			if (session != null) {
				session.close();
			}
		}

		return IsLatest;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		while (isActive()) {
			try {
				boolean isLatest = this.CheckCurrentVersionIsLatest(localSwipeCardVersion);
				int currentHour = CurrentDBTimeStamp.getHours();

				if (isLatest) {
					System.out.println("程式為最新版本");
					Thread.currentThread().sleep(60*60*1000);
				} else {
					if ((currentHour >= 9 && currentHour < 12) || (currentHour >= 14 && currentHour < 17)
							|| (currentHour >= 21 && currentHour < 24) || (currentHour >= 1 && currentHour < 7)) {
						int dialogResult = JOptionPane.showConfirmDialog(null,
								"本地端程序為舊版本，點選「確認」關閉\n關閉後請重新開啟刷卡端程式\n", "程序版本警告",
								JOptionPane.DEFAULT_OPTION);
						System.exit(0);
					}

				}

			} catch (InterruptedException ex) {
				logger.error("版本檢查時 Error building SqlSession，原因:"+ex);
				// logger.error(ex.toString());
				ex.printStackTrace();
			}
		}
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	private boolean isActive() {
		// TODO Auto-generated method stub
		return active;
	}

	public static SqlSessionFactory getSession() {
		return sqlSessionFactory;
	}

}
