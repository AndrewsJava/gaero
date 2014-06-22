package harlequinmettle.gaero;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class EmailTask  extends HttpServlet {
	public static final String PARAMETER = "option";
	public static final String SETQ = "10000";
	public static final String SETY = "21111";
	public static final int SETQi =  10000 ;
	public static final int SETYi =  21111 ;
	public static final int ANALYSIS_RESULTS = 7333311;
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws IOException {
//PrintWriter w = resp.getWriter();
   int CHOICE = Integer.parseInt(req.getParameter(PARAMETER));
   switch(CHOICE){
   case 0:
	   EmailUtil.sendDataByEmail();
	   break;
   case SETQi:///ab@backend-test-app
	 //  EmailUtil.sendDataByEmail(Qi.QQ,TheBackend.fileTitleQ,"harlequinmettle@gmail.com");
	   EmailUtil.sendDataByEmail(Qi.QQ,Backend.fileTitleQ,"parelius@uw.edu");
	   //EmailUtil.sendDataByEmail(Qi.QQ,TheBackend.fileTitleQ,"hm@"+"financialdatacollector"+".appspotmail.com");
	   //EmailUtil.sendDataByEmail(Qi.QQ,TheBackend.fileTitleQ,"hm@"+"cloudcomputer99"+".appspotmail.com");
	  // EmailUtil.sendDataByEmail(Qi.QQ,TheBackend.fileTitleQ,"hm@"+"backend-test-app"+".appspotmail.com");
	   //EmailUtil.sendDataByEmail(Qi.QQ,TheBackend.fileTitleQ,"hm@"+POSTUtil.ApplicationID+".appspotmail.com");
	   break;
   case SETYi:
	  // EmailUtil.sendDataByEmail(Yi.YY,TheBackend.fileTitleY,"harlequinmettle@gmail.com");
	   EmailUtil.sendDataByEmail(Yi.YY,Backend.fileTitleY,"parelius@uw.edu");
	  // EmailUtil.sendDataByEmail(Yi.YY,TheBackend.fileTitleY,"hm@"+"financialdatacollector"+".appspotmail.com");
	  // EmailUtil.sendDataByEmail(Yi.YY,TheBackend.fileTitleY,"hm@"+"cloudcomputer99"+".appspotmail.com");
	  // EmailUtil.sendDataByEmail(Yi.YY,TheBackend.fileTitleY,"hm@"+"backend-test-app"+".appspotmail.com");
	  // EmailUtil.sendDataByEmail(Yi.YY,TheBackend.fileTitleY,"hm@"+POSTUtil.ApplicationID+".appspotmail.com");
	   break;
   case ANALYSIS_RESULTS:
	   EmailUtil.sendScanResultsByEmail( Backend.fileTitleY,"harlequinmettle@gmail.com");
	  
	  // EmailUtil.sendDataByEmail(Yi.YY,TheBackend.fileTitleY,"hm@"+POSTUtil.ApplicationID+".appspotmail.com");
	   break;
   case 1:
	   
EmailUtil.sendDataViaEmailAttachment("sendDataViaEmailAttachment","--testing--"); 
break;
   case 2://OK both text in subject
EmailUtil.sendDataViaEmail("sendDataViaEmail", "test");
break;
   case 3://OK first in subject other nowhere?
EmailUtil.sendMessage(req, resp, "sendMessage", "tstststtsttsts");
break;
default:
	break;
}
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
	throws IOException {
		doGet(req,resp);
	} 
}
