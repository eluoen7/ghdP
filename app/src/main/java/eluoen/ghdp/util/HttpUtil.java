package eluoen.ghdp.util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import com.google.gson.Gson;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import static java.net.Proxy.Type.HTTP;

public class HttpUtil {
	
	//public static String IP="http://120.77.49.216/gdsalt_dataup_pda/";
	public static String IP="http://120.77.49.216/gdsalt_dataup_pda/";
	
	public static VersionMessage vm = null;

	/**
	 * 检测网络状态是否正常
	 * 
	 * @param context
	 *            Context对象或Activity对象
	 * @return 如果正常返回true,不正常则返回false
	 */
	public static boolean isNetWorkLink(Context context) {
		ConnectivityManager cwManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cwManager.getActiveNetworkInfo();
		if (null != info && info.isAvailable()) {
			return true;
		}
		return false;
	}



	public static String httpClient(String params, String url,String requestMethod) {

		System.out.println(url);

		StringBuffer bufferRes = new StringBuffer();

		try {

			URL realUrl = new URL(url);

			HttpURLConnection conn = (HttpURLConnection) realUrl.openConnection();

			// 连接超时
			conn.setConnectTimeout(10000);

			// 读取超时 --服务器响应比较慢,增大时间
			conn.setReadTimeout(10000);

			// 请求方式
			conn.setRequestMethod(requestMethod);

			// 发送POST请求必须设置如下两行
			//以后就可以使用conn.getOutputStream().write()
			conn.setDoOutput(true);
			//以后就可以使用conn.getInputStream().read();
			conn.setDoInput(true);

			//设置字符编码
			conn.setRequestProperty("Accept-Charset", "UTF-8");
			//
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");

			conn.connect();

			// 获取URLConnection对象对应的输出流
			if(params!=null&&!params.isEmpty()){
				OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());

				// 发送请求参数
				out.write(params);
				out.flush();
				out.close();
			}


			InputStream in = conn.getInputStream();

			BufferedReader read = new BufferedReader(new InputStreamReader(in,"UTF-8"));

			String valueString = null;

			while ((valueString = read.readLine()) != null) {
				bufferRes.append(valueString);
			}

			System.out.println(url+":"+bufferRes.toString());

			in.close();

			if (conn != null) {

				// 关闭连接

				conn.disconnect();

			}

		} catch (Exception e) {

			e.printStackTrace();

		}
		return bufferRes.toString();
	}
	
	
	
	
	/**
	 * 得到当前程序版本号
	 * 
	 * @param context
	 * @return 版本号
	 */
	public static int getVersionCode(Context context) {
		int versionCode = -1;
		try {
			versionCode = context.getPackageManager().getPackageInfo(
					"eluoen.ghdp", 0).versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return versionCode;
	}

	/**
	 * 得到当前程序版本名称
	 * 
	 * @param context
	 * @return 版本名称
	 */
	public static String getVersionName(Context context) {
		String versionName = "";
		try {
			versionName = context.getPackageManager().getPackageInfo(
					"eluoen.ghdp", 0).versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return versionName;
	}

	/**
	 * 得到服务端程序版本信息
	 * 
	 * @return 版本信息对象
	 */
	public static VersionMessage getServerVersionCode() {
		VersionMessage vm = null;
		BufferedReader buffer;
		// 创建一个HTTP连接
		HttpURLConnection urlConn;

		URL url;
		try {
			url = new URL(IP+"version.json");
			urlConn = (HttpURLConnection) url.openConnection();
			System.out.println("url:"+url);

			urlConn.connect();
			// 使用IO进行流数据的读取

			buffer = new BufferedReader(new InputStreamReader(urlConn.getInputStream(),"GBK"), 1000);


			String str = buffer.readLine();

			System.out.println("str:"+str);

			urlConn.disconnect();
			Gson gson = new Gson();
			vm = gson.fromJson(str, VersionMessage.class);
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		return vm;
	}
}
