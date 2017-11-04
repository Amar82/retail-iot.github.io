package org.superus.kura.alertservcie.email;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailAlertService implements ConfigurableComponent {
	
	
	//Email server details 
	Boolean alertOption;
	String emailservertype = "";
	 String hostname="";
	 String from_user="";//change accordingly
	 String fromUser_password="";//change accordingly
	 
	 String to_user="";//change accordingly

	private Map<String, Object> m_properties;
	
	// mail Property Names
	    private static final String EnableAlert ="EnableAlert" ;
		private static final String EmailServerType = "EmailServerType";
		private static final String MailServerHostName = "MailServerHostName";
		private static final String MailTo = "MailTo";
		private static final String MailFrom = "MailFrom";
		private static final String FromMailPassword = "FromMailPassword";

	
	private static final Logger s_logger = LoggerFactory.getLogger(EmailAlertService.class);

    private static final String APP_ID = "org.superus.kura.alertservcie.email";
    
    public EmailAlertService() {
		// TODO Auto-generated constructor stub
	}


    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
		try {
			s_logger.info("Bundle " + APP_ID + " has started with config!");
			

			this.m_properties = properties;
			for (String s : properties.keySet()) {
				s_logger.info("Activate - " + s + ": " + properties.get(s)) ;
				} 
		    }catch (Exception e) {
				s_logger.info("activation issue...", e.toString());
			}

		doupdate(); //update the propertied in the class itself
			s_logger.info("Activating EmailAlertService... Done.");
		}
			
			
    public void updated(Map<String, Object> properties) {
		// TODO Auto-generated method stub
		s_logger.info("Updated properties...");
		this.m_properties = properties;

		for (String s : properties.keySet()) {
			s_logger.info("Update - " + s + ": " + properties.get(s));
		}

		s_logger.info("Updated Properties... Done.");
		if (properties != null && !properties.isEmpty()) {
			Iterator<Entry<String, Object>> it = properties.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, Object> entry = it.next();
				s_logger.info("New property - " + entry.getKey() + " = " + entry.getValue() + " of type "
						+ entry.getValue().getClass().toString());
			}
		}
		
		doupdate();  // update the properties details in the class
    }
    
    protected void deactivate(ComponentContext componentContext) {

	    s_logger.debug("Deactivating EmailAlertService... Done.");

		s_logger.info("Bundle " + APP_ID + " has stopped!");

	}
    
    protected void doupdate(){
    	
    	//update the mail server & user details
    	this.alertOption = (Boolean)this.m_properties.get(EnableAlert);
    	 this.emailservertype = (String) this.m_properties.get(EmailServerType);
    	 this.hostname = (String) this.m_properties.get(MailServerHostName);
    	 this.from_user = (String) this.m_properties.get(MailFrom);
    	 this.fromUser_password = (String) this.m_properties.get(FromMailPassword);
    	 this.to_user = (String) this.m_properties.get(MailTo);
    }
    
    
public void sendXChangeMail(String msgSubject, String msgBody){
 
 String host= this.hostname ;
 final String user=this.from_user;
 final String password=this.fromUser_password;
 
 String to=this.to_user;
 
//Get the session object
  Properties props = new Properties();
  
  //for exchange servers
 props.put("mail.smtp.host",host);
 props.put("mail.smtp.auth", "true");
  
  Session session = Session.getDefaultInstance(props,
   new javax.mail.Authenticator() {
   protected PasswordAuthentication getPasswordAuthentication() {
return new PasswordAuthentication(user,password);
     }
   });

//Compose the message
   try {
    MimeMessage message = new MimeMessage(session);
    message.setFrom(new InternetAddress(user));
    message.addRecipient(Message.RecipientType.TO,new InternetAddress(to));
    message.setSubject(msgSubject);
    message.setText(msgBody);
    
   //send the message
    Transport.send(message);

    s_logger.info("message sent successfully...");
 
    } catch (MessagingException e) {e.printStackTrace();}
  
}

public void sendGmailTSL(String msgSubject, String msgBody)
{

	
	String host= this.hostname ;  // for Gmail  the value is "smtp.gmail.com"
	 final String user=this.from_user;
	 final String password=this.fromUser_password;
	 
	 String to=this.to_user;
	 
Properties props = new Properties();
props.put("mail.smtp.auth", "true");
props.put("mail.smtp.starttls.enable", "true");
props.put("mail.smtp.host", host);
props.put("mail.smtp.port", "587");

Session session = Session.getInstance(props,
 new javax.mail.Authenticator() {
protected PasswordAuthentication getPasswordAuthentication() {
return new PasswordAuthentication(user, password);
}
 });

try {

Message message = new MimeMessage(session);
message.setFrom(new InternetAddress(user));
message.setRecipients(Message.RecipientType.TO,
InternetAddress.parse(to));
message.setSubject(msgSubject);
message.setText(msgBody);

Transport.send(message);

s_logger.info("message sent from Gmail TLS server successfully...");
} catch (MessagingException e) {
throw new RuntimeException(e);
}
}

public void sendGmailSSL(String msgSubject, String msgBody)
{
	String host= this.hostname ;  // for Gmail  the value is "smtp.gmail.com"
	 final String user=this.from_user;
	 final String password=this.fromUser_password;
	 
	 String to=this.to_user;
	 
Properties props = new Properties();
props.put("mail.smtp.host", host);
props.put("mail.smtp.socketFactory.port", "465");
props.put("mail.smtp.socketFactory.class",
"javax.net.ssl.SSLSocketFactory");
props.put("mail.smtp.auth", "true");
props.put("mail.smtp.port", "465");

Session session = Session.getDefaultInstance(props,
new javax.mail.Authenticator() {
protected PasswordAuthentication getPasswordAuthentication() {
return new PasswordAuthentication(user,password);
}
});

try {

Message message = new MimeMessage(session);
message.setFrom(new InternetAddress(user));
message.setRecipients(Message.RecipientType.TO,
InternetAddress.parse(to));
message.setSubject(msgSubject);
message.setText(msgBody);

Transport.send(message);

s_logger.info("message sent from Gmail  SSL server successfully...");

} catch (MessagingException e) {
throw new RuntimeException(e);
}
}



/*
	public static void main(String[] args) {
// TODO Auto-generated method stub
//sendXChangeMail();
new SendAlertMail().sendGmailTSL();

}
*/
	
}
