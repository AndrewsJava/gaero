package harlequinmettle.gaero;

 
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
 
 

public class BackendServlet extends HttpServlet {
 
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
	 { 

		if (req.getRequestURL().toString()
				.indexOf("b1."+Backend.ApplicationID  ) > 0) {
			Backend.runCollectionInBackend();
		}

		if (req.getRequestURL().toString()
				.indexOf("b2."+Backend.ApplicationID  ) > 0) {
			Backend.runCollectionInBackend( );
		}

		if (req.getRequestURL().toString()
				.indexOf("b4."+Backend.ApplicationID  ) > 0) {
			Backend.runCollectionInBackend( );
		}

		if (req.getRequestURL().toString()
				.indexOf("b41gig."+Backend.ApplicationID  ) > 0) {
			Backend.runCollectionInBackend( );
		}
		
}
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
	 {
		doGet(req,resp);
}
}
