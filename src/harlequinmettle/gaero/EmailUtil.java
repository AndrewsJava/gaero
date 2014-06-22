package harlequinmettle.gaero;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.PriorityBlockingQueue;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.mail.MailService;
import com.google.appengine.api.mail.MailServiceFactory;

public class EmailUtil {

	public static void sendScanResultsByEmail(String title, String to) {
		// /////////////////////////////////////

		// ///////////////////////////////
		try {
			MailService service = MailServiceFactory.getMailService();
			MailService.Message msg = new MailService.Message();
			msg.setSender("harlequinmettle@gmail.com");
			msg.setTo(to);
			msg.setSubject(Backend.ApplicationID + "  : " + title);
			TreeMap<Float, TreeMap<Integer, TwoNumbers>> savedResults = DSBlobs
					.restoreAnalysisResults(DSBlobs.E_DS_RESULTS,
							DSBlobs.P_RESULTS);
			StringBuilder bodyText = new StringBuilder();
			int i = 0;
			for (Entry<Float, TreeMap<Integer, TwoNumbers>> ent : savedResults
					.entrySet()) {
				String topResults = "" + (-ent.getKey()) + "  ---> "
						+ ent.getValue() + "\n";
				bodyText.append(topResults);
				if (i++ == 100)
					break;
			}
			i = 0;
			for (Entry<Float, TreeMap<Integer, TwoNumbers>> ent : savedResults
					.descendingMap().entrySet()) {
				String topResults = "" + (-ent.getKey()) + "  ---> "
						+ ent.getValue() + "\n";
				bodyText.append(topResults);
				if (i++ == 100)
					break;
			}

			byte[] attachmentData = bodyText.toString().getBytes(); // ...

			MailService.Attachment attachment = new MailService.Attachment(
					title, attachmentData);
			msg.setAttachments(attachment);

			service.send(msg);

		} catch (IOException e) {
		}

	}

	public static void sendDataByEmail(String[] data, String title, String to) {
		// /////////////////////////////////////

		// ///////////////////////////////
		try {
			MailService service = MailServiceFactory.getMailService();
			MailService.Message msg = new MailService.Message();
			msg.setSender("harlequinmettle@gmail.com");
			msg.setTo(to);
			msg.setSubject(Backend.ApplicationID + "  : " + title);

			StringBuilder bodyText = new StringBuilder();
			for (String s : data) {

				bodyText.append(Backend.completeResults.get(s));
			}

			byte[] attachmentData = bodyText.toString().getBytes(); // ...

			MailService.Attachment attachment = new MailService.Attachment(
					title, attachmentData);
			msg.setAttachments(attachment);

			service.send(msg);

		} catch (IOException e) {
		}

	}

	public static void sendDataByEmail(String data, String title) {
		try {
			MailService service = MailServiceFactory.getMailService();
			MailService.Message msg = new MailService.Message();
			msg.setSender("harlequinmettle@gmail.com");
			msg.setTo("harlequinmettle@gmail.com");
			msg.setSubject(title);

			byte[] attachmentData = data.toString().getBytes(); // ...

			MailService.Attachment attachment = new MailService.Attachment(
					title, attachmentData);
			msg.setAttachments(attachment);

			service.send(msg);

		} catch (IOException e) {
		}

	}

	public static void sendDataByEmail2(PriorityBlockingQueue<String> data,
			String title) {

		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);

		String msgBody = "...EMAIL BODY";

		try {
			Message msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress("harlequinmettle@gmail.com"));

			msg.addRecipient(Message.RecipientType.TO, new InternetAddress(
					"harlequinmettle@gmail.com"));

			StringBuilder bodyText = new StringBuilder(title + "\n");
			for (String s : data) {
				bodyText.append(s);
			}

			msg.setSubject("sending data attachment tests");

			byte[] attachmentData = bodyText.toString().getBytes(); // ...

			Multipart mp = new MimeMultipart();

			MimeBodyPart attachment = new MimeBodyPart();
			attachment.setFileName("data.txt");
			attachment.setContent(attachmentData, "text/html");
			mp.addBodyPart(attachment);

			msg.setContent(mp);

			msg.setText(bodyText.toString());
			Transport.send(msg);

		} catch (AddressException e) {
			// ...
		} catch (MessagingException e) {
			// ...
		}

	}

	public static void sendDataByEmail() {

		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);

		String msgBody = "...EMAIL BODY";

		try {
			Message msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress("harlequinmettle@gmail.com"));

			msg.addRecipient(Message.RecipientType.TO, new InternetAddress(
					"harlequinmettle@gmail.com"));

			StringBuilder bodyText = new StringBuilder();
			for (int i = 0; i < 10000; i++) {
				if (i % 200 == 0)
					bodyText.append("\n");
				bodyText.append("data data ");
			}

			msg.setSubject("sending data attachment tests");

			byte[] attachmentData = "this should be in attachment.".getBytes(); // ...

			Multipart mp = new MimeMultipart();

			MimeBodyPart attachment = new MimeBodyPart();
			attachment.setFileName("data.txt");
			attachment.setContent(attachmentData, "text/html");
			mp.addBodyPart(attachment);

			msg.setContent(mp);

			msg.setText(bodyText.toString());
			Transport.send(msg);

		} catch (AddressException e) {
			// ...
		} catch (MessagingException e) {
			// ...
		}

	}

	public static void sendDataViaEmailAttachment(String index, String property) {
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);

		String msgBody = "...";

		try {
			// //////////////////////////////

			String htmlBody = index; // ...
			byte[] attachmentData; // ...
			String converToBytes = property; // getDataFromDatastore(index,
												// property);
			attachmentData = converToBytes.getBytes();

			Multipart mp = new MimeMultipart();

			MimeBodyPart htmlPart = new MimeBodyPart();
			htmlPart.setContent(index + "   " + property, "text/html");
			mp.addBodyPart(htmlPart);

			MimeBodyPart attachment = new MimeBodyPart();
			attachment.setFileName("data.txt");
			attachment.setContent(attachmentData, "txt");
			mp.addBodyPart(attachment);

			// ///////////////////////////
			Message msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress("harlequinmettle@gmail.com"));
			msg.addRecipient(Message.RecipientType.TO, new InternetAddress(
					"harlequinmettle@gmail.com"));
			msg.setSubject("app engine:" + index + "  " + property);
			msg.setText("  ");
			msg.setContent(mp);

			Transport.send(msg);

		} catch (AddressException e) {
		} catch (MessagingException e) {

		}
	}

	public static void sendDataViaEmail(String index, String property) {
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);

		String msgBody = DSUtil.getDataFromDatastore(index, property);

		try {
			// //////////////////////////////

			// ///////////////////////////
			Message msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress("harlequinmettle@gmail.com"));
			msg.addRecipient(Message.RecipientType.TO, new InternetAddress(
					"harlequinmettle@gmail.com"));
			msg.setSubject("app engine:" + index + "  " + property);
			msg.setText(msgBody);
			Transport.send(msg);

		} catch (AddressException e) {
		} catch (MessagingException e) {

		}
	}

	public static HttpServletResponse sendMessage(HttpServletRequest req,
			HttpServletResponse resp, String index, String property)
			throws IOException {

		resp.getWriter().println("sending mail...<br/>");

		// ...
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);

		String msgBody = DSUtil.getDataFromDatastore(index, property);

		try {
			// //////////////////////////////
			MailService service = MailServiceFactory.getMailService();

			// ///////////////////////////
			resp.getWriter().println("entered try block...<br/>");
			Message msg = new MimeMessage(session);
			resp.getWriter().println("new MimeMessage...<br/>");
			msg.setFrom(new InternetAddress("harlequinmettle@gmail.com"));
			resp.getWriter().println(
					"new InternetAddress(harlequinmettle@gmail.com...<br/>");
			msg.addRecipient(Message.RecipientType.TO, new InternetAddress(
					"harlequinmettle@gmail.com"));
			resp.getWriter().println(
					"add recipient harlequinmettel@gmail.com...<br/>");
			msg.setSubject(index);
			resp.getWriter().println("added subject...<br/>");
			msg.setText(msgBody);
			resp.getWriter().println("added text body...<br/>");
			Transport.send(msg);
			resp.getWriter().println("mail transported...<br/>");

		} catch (AddressException e) {
			resp.getWriter().println("address exception...<br/>");
		} catch (MessagingException e) {
			resp.getWriter().println(
					"messaging exception...<br/>" + e.getMessage() + "<br/>"
							+ e.toString() + "<br/>");

		}
		return resp;
	}
}
