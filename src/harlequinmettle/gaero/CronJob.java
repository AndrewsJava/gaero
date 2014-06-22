package harlequinmettle.gaero;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class CronJob extends HttpServlet {

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		doGet(req, resp);
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		HttpURLConnection conn;
		String url = "http://b" + Backend.BACKEND + Backend.BACKEND_EXTRA + "."
				+ Backend.ApplicationID + ".appspot.com/backend";

		try {
			// ONE MINUTE LIMIT
			for (int i = 0; i < Backend.THREAD_COUNT; i++) {
				conn = (HttpURLConnection) new URL(url).openConnection();
				conn.setReadTimeout(60 * 1000);
				conn.setConnectTimeout(60 * 1000);

				InputStream response = conn.getInputStream();
				if (i == 0)
					Thread.sleep(5000);
				else
					Thread.sleep(1500);
			}
		} catch (Exception e) {

		}

	}

}
