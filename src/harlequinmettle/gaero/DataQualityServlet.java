package harlequinmettle.gaero;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;

@SuppressWarnings("serial")
public class DataQualityServlet extends HttpServlet {
	static final String PARAMETER = "THREAD_COUNT";
	static final String PARAMETER2 = "THREAD_TYPE";

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		doGet(req, resp);
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
	  
			Queue queue = QueueFactory.getQueue("dataquality");
			queue.add(TaskOptions.Builder.withUrl("/dataqualitytask") );// 
			  
	}

	
}