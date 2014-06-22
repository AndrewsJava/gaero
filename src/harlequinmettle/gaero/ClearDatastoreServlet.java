package harlequinmettle.gaero;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ClearDatastoreServlet  extends HttpServlet {

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		doGet(req, resp);
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		Backend.clearDataFor1Minute();
	}
}
