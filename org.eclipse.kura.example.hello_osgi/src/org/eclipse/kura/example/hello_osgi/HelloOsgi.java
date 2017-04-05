package org.eclipse.kura.example.hello_osgi;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.gpio.GPIOService;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.kura.example.hello_osgi.DemoFrame;

public class HelloOsgi implements ConfigurableComponent,CloudClientListener{
	
	//Worker thread
    
		private final ScheduledExecutorService m_worker;
		private ScheduledFuture<?> m_handle;
	
	//GPIO service
	
		private  GPIOService m_GPIOService;  //static modifier removed
		
	//Cloud service 
		private CloudService m_cloudService;
		private CloudClient  m_cloudClient;
		
		private Map<String, Object> m_properties;

//      publishing properties
		// Publishing Property Names
		 private static final String PUBLISH_RATE_PROP_NAME = "publish.rate";
		    private static final String PUBLISH_TOPIC_PROP_NAME = "publish.semanticTopic";
		    private static final String PUBLISH_QOS_PROP_NAME = "publish.qos";
		    private static final String PUBLISH_RETAIN_PROP_NAME = "publish.retain";

	private static final Logger s_logger = LoggerFactory.getLogger(HelloOsgi.class);

	private static final String APP_ID = "org.eclipse.kura.example.hello_osgi";

	
	//constructor

    public HelloOsgi() {
        super();
        this.m_worker = Executors.newSingleThreadScheduledExecutor();
    }

	
	/*
	 * protected void activate(ComponentContext componentContext) {

		s_logger.info("Bundle " + APP_ID + " has started!");

		s_logger.debug(APP_ID + ": This is a debug message.");
		//readLED();
		
	}
	*/
	
	protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
       try{
		s_logger.info("Bundle " + APP_ID + " has started with config!");
		
		 this.m_properties = properties;
	        for (String s : properties.keySet()) {
	            s_logger.info("Activate - " + s + ": " + properties.get(s));
	        }
        
        // Acquire a Cloud Application Client for this Application
        s_logger.info("Getting CloudClient for {}...", APP_ID);
        this.m_cloudClient = this.m_cloudService.newCloudClient(APP_ID);
        this.m_cloudClient.addCloudClientListener(this);
        
        
        doUpdate();
        
      //perform Display
        createAndShow() ;		
       
    
    }
       catch(Exception e)
       {
    	   s_logger.info("activation issue...", e.toString());
       }
       
       s_logger.info("Activating Hello osgi... Done.");
	}
	
	public void updated(Map<String, Object> properties) {
		// TODO Auto-generated method stub
		 s_logger.info("Updated properties...");
		this.m_properties = properties;
		
		for (String s : properties.keySet()) {
            s_logger.info("Update - " + s + ": " + properties.get(s));
        }

        
        s_logger.info("Updated ExamplePublisher... Done.");
		if(properties != null && !properties.isEmpty()) {
			Iterator<Entry<String, Object>> it = properties.entrySet().iterator();
            while(it.hasNext()) {
                Entry<String, Object> entry = it.next();
                s_logger.info("New property - " + entry.getKey() + " = " +
                entry.getValue() + " of type " + entry.getValue().getClass().toString());
            }
		}
		

        // try to kick off a new job
       doUpdate(); 
		
	}

	protected void deactivate(ComponentContext componentContext) {
		
		// shutting down the worker and cleaning up the properties
        this.m_worker.shutdown();

        // Releasing the CloudApplicationClient
        s_logger.info("Releasing CloudApplicationClient for {}...", APP_ID);
        this.m_cloudClient.release();

        s_logger.debug("Deactivating ExamplePublisher... Done.");

		s_logger.info("Bundle " + APP_ID + " has stopped!");

	}
	protected void setGPIOService(GPIOService gpioService){
		
		this.m_GPIOService = gpioService;
	}

	protected void unsetGPIOService(GPIOService gpioService) {
	    this.m_GPIOService = null;
	  }

//cloud publisher service
	 public void setCloudService(CloudService cloudService) {
		   m_cloudService = cloudService;
		  }
		  public void unsetCloudService(CloudService cloudService) {
		   m_cloudService = null;
		  }
		  
	
	public void readLED(){
		
		try{
			int counter =0; // for max number of times
			//get LED  pin
			KuraGPIOPin led=m_GPIOService.getPinByTerminal(17);
			if(!led.isOpen())
				led.open();
			if(led !=null)
			{
				s_logger.info("Direction " + led.getDirection());
		        s_logger.info("Triger " + led.getTrigger());
		        s_logger.info("Mode " + led.getMode());
		        s_logger.info("Value " + led.getValue());
		        led.setValue(false); // start LED as off
			}
			
			while(true)
			{
				
				 s_logger.info("Trying to read & control LED as stated by Amar");
				
				//read LED pin
				
				if(led.getValue()==true) led.setValue(false);
		        
		        Thread.sleep(2000);
		        if(led.getValue()==false) led.setValue(true);
		       // s_logger.info("");
		        Thread.sleep(2000);
		        boolean currentstate= led.getValue() ;
		      
		        s_logger.info("LED state is:"+currentstate);
				if(counter>=60) 
					break;
				counter++;
			}
			}catch(Exception e) 
			{
				 s_logger.info("problem in reading led " + e.toString());
			}

		
	}
	
	
	 private void doUpdate() {
	   
		// cancel a current worker handle if one if active
	        if (this.m_handle != null) {
	            this.m_handle.cancel(true);
	        }
	        // schedule a new worker based on the properties of the service
	       int pubrate = (Integer) this.m_properties.get(PUBLISH_RATE_PROP_NAME);
	       this.m_handle = this.m_worker.scheduleAtFixedRate(new Runnable() {

	       //     @Override
	          public void run() {
	                doPublish();
	            }
	       }, 0, pubrate, TimeUnit.MINUTES);
	    }
	
	public void doPublish()
	  {
	   
		 // fetch the publishing configuration from the publishing properties
        String topic = (String) this.m_properties.get(PUBLISH_TOPIC_PROP_NAME);
        Integer qos = (Integer) this.m_properties.get(PUBLISH_QOS_PROP_NAME);
        Boolean retain = (Boolean) this.m_properties.get(PUBLISH_RETAIN_PROP_NAME);
        
		
	   // Allocate a new payload
	   KuraPayload payload = new KuraPayload();
	   
	   // Timestamp the message
	   payload.setTimestamp(new Date());
	   
	   // Add the temperature as a metric to the payload
	   payload.addMetric("temperature", 27.8);
	   
	   
	   
	   // Publish the message
	   try {
		   s_logger.info("trying to Publish to Topic:", topic, payload);
	    m_cloudClient.publish(topic, payload,qos, retain);
	    s_logger.info("Published to {} message: {}", topic, payload);
	   } 
	   catch (Exception e) {
	    s_logger.error("Cannot publish topic: "+topic, e);
	   }
	   
	   
	  }

	@Override
	public void onConnectionEstablished() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectionLost() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onControlMessageArrived(String arg0, String arg1, KuraPayload arg2, int arg3, boolean arg4) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessageArrived(String arg0, String arg1, KuraPayload arg2, int arg3, boolean arg4) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessageConfirmed(int arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessagePublished(int arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}
	
/* test swing method */
	public void createAndShow() {
		 s_logger.info("Entered createAndShow method");
		 
		
	        /* Test Swing 
	        
	        SwingUtilities.invokeLater(new Runnable() {
	            @Override
	            public void run() {
	        
	       JPanel jpanel = new JPanel();
	        jpanel.setSize(400, 400);
	        jpanel.setLayout(new GridLayout(3, 1));
	  
	       Label headerLabel = new Label();
	        headerLabel.setAlignment(Label.CENTER);
	       Label statusLabel = new Label();    
	        statusLabel.setAlignment(Label.CENTER);
	        statusLabel.setSize(350,100);

	       Panel controlPanel = new Panel();
	        controlPanel.setLayout(new FlowLayout());

	        jpanel.add(headerLabel);
	        jpanel.add(controlPanel);
	        jpanel.add(statusLabel);
	        jpanel.setVisible(true);  
	        
	        controlPanel.add(new DemoFrame());
	        controlPanel.setVisible(true);
	        jpanel.setVisible(true);
	        
	            }
	        });
	        */
	        
	        //option-2
	       
	        Thread dummyappThread = new Thread() {
	           
	        	public void run() {
	        		try {
	                	
	                	SwingUtilities.invokeLater(new Runnable() {
	                        @Override
	                        public void run() {
	                        //	private  final Logger s_logger1 = LoggerFactory.getLogger(Runnable.class);
	                        	s_logger.info("inside thred run code...");
	                        	 JFrame frame = new JFrame("Hello World");
	                     		frame.setSize(640,480);
	                     		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	                     		frame.setVisible(true);
	                        }
	                      });
	                   
	                }
	                catch (Exception e) {
	                	//s_logger1.info("exception thred run code..."+e);
	                }
	               
	            }
	        };
	        dummyappThread.start(); 
		}

}