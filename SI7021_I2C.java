package org.superus.kura.sensors.temperature;



import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.data.DataTransportService;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraPosition;
import org.eclipse.kura.position.NmeaPosition;
import org.eclipse.kura.position.PositionService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jdk.dio.DeviceManager;
import jdk.dio.i2cbus.I2CDevice;
import jdk.dio.i2cbus.I2CDeviceConfig;

public class SI7021_I2C implements ConfigurableComponent,CloudClientListener {
 
 
 private static final Logger s_logger = LoggerFactory.getLogger(SI7021_I2C.class);
 
 private static final String APP_ID = "org.superus.kura.sensors.temperature.SI7021_I2C";
 
	//Worker thread to publish at predefined rate
 
	private final ScheduledExecutorService m_worker;
	private ScheduledFuture<?> m_handle;

	//Cloud service & clients to publish to cloud
			private CloudService m_cloudService;
			private CloudClient  m_cloudClient;
			
	//data trasport service
			private DataTransportService m_dataTrasportservice;
			
	//Position Service
			private PositionService m_positionService;
			private double longitude;
			private double latitude;
			private double altitude;
			
	//Propetries handler		
			private Map<String, Object> m_properties;
			
	//Asset monitoring type
			private static final String MONITORED_FACILITY = "Monitoredfacility";
			private static final String MONITORING_TYPE = "MonitoringType";
			
	//temperature properties
			private static final String PERMISSIBLE_TEMP_MAX = "PermissibleTemperatureRange_MAX";
			private static final String PERMISSIBLE_TEMP_MIN = "PermissibleTemperatureRange_MIN";
			
	//humidity properties
			private static final String PERMISSIBLE_HUM_MAX = "PermissibleHumidityRange_MAX";
			private static final String PERMISSIBLE_HUM_MIN = "PermissibleHumidityRange_MIN";
			
	// Publish properties
		
			 private static final String PUBLISH_RATE_PROP_NAME = "publish.rate";
			 private static final String PUBLISH_TOPIC_PROP_NAME = "publish.semanticTopic";
			 private static final String PUBLISH_QOS_PROP_NAME = "publish.qos";
			 private static final String PUBLISH_RETAIN_PROP_NAME = "publish.retain";		
			 
	//Alert Mail Properties
			//these are for config XMLs
			    private static final String EnableAlert ="EnableAlert" ;
				private static final String EmailServerType = "EmailServerType";
				private static final String MailServerHostName = "MailServerHostName";
				private static final String MailTo = "MailTo";
				private static final String MailFrom = "MailFrom";
				private static final String FromMailPassword = "FromMailPassword";
				
				//these are for  class usage
				//Email server details 
				Boolean alertOption;
				String emailservertype = "";
				 String hostname="";
				 String from_user="";//change accordingly
				 String fromUser_password="";//change accordingly
				 String to_user="";//change accordingly	
	       
				 //local alert mail service ; this will be replaced with OSGI based alert mail service in next release
			private SendAlertMail emailService = new SendAlertMail() ;
	
 /*
  * Address defined in Jdk.dio.properties
  * #Standard I2C device configuration
  * 41 = deviceType: i2cbus.I2CDevice, address:0x29, addressSize:7, clockFrequency:400000 
  * jdk.dio.policy file should have entry
  * permission jdk.dio.i2cbus.I2CPermission "*:*", "open";
  */
 
 /*
  * @ Table 11. I2C Command Table
Command Description | Command Code
Measure Relative Humidity, Hold Master Mode |  0xE5
Measure Relative Humidity, No Hold Master Mode  | 0xF5
Measure Temperature, Hold Master Mode |  0xE3
Measure Temperature, No Hold Master Mode | 0xF3
Read Temperature Value from Previous RH Measurement |  0xE0
Reset |  0xFE
Write RH/T User Register 1  |  0xE6
Read RH/T User Register 1  |  0xE7
Write Heater Control Register  | 0x51
Read Heater Control Register  | 0x11
Read Electronic ID 1st Byte  |  0xFA 0x0F
Read Electronic ID 2nd Byte  |  0xFC 0xC9
Read Firmware Revision  |  0x84 0xB8
  */
 
 //define addresses for Si7021
  private final int CFG_ADDRESS = 0x40;  //Sensor configuration address from datasheet , int value of 0X40 = 2^6 =64
  private final  int CLK_FREQ=400000; // sensor clock frequency SCL
  private final int ADDRESS_SIZE = 7; //Address size from Datasheet
  
 
 // Registers addresses and definitions of configuration parameters of Si7021 from Datasheet 
  //for humidity
     private final int MEASURE_HUMIDITY = 0xE5; 
     private final int RT_ADDR_SIZE = 0x01;
     private final int READ_HUMIDITY_SIZE = 2;
     private  int HUMIDITY_VAL;
  
  //for temperature
     private final int MEASURE_TEMP = 0xE3;
     private final int TEMP_ADDR_SIZE = 0x01;
     private final int READ_TEMP_SIZE = 2;
     private  int TEMP_VAL;  
 
     //i2C device configurations
   private I2CDeviceConfig config ;  
   private I2CDevice si7021 ;
   
     //Constructor
    public SI7021_I2C(){
    	 super();
         this.m_worker = Executors.newSingleThreadScheduledExecutor();
         
     }
     
     //cloud publisher service
	 public void setCloudService(CloudService cloudService) {
		   this.m_cloudService = cloudService;
		  }
    public void unsetCloudService(CloudService cloudService) {
		   this.m_cloudService = null;
		  }
       	  

	//set position service
	public void setPositionService(PositionService positionService) {
	   this.m_positionService = positionService;
	}

	public void unsetPositionService(PositionService positionService) {
		m_positionService = null;
	}
	
	//set dataTransport service
	
	public void setDataTransportService(DataTransportService dataTrasportservice) {
	   this.m_dataTrasportservice = dataTrasportservice;
	}

	public void unsetDataTransportService (DataTransportService dataTrasportservice){
		
		this.m_dataTrasportservice=null;
	}
		
    
     
   protected void activate(ComponentContext componentContext,Map<String, Object> properties) {
   try{
   s_logger.info("Bundle " + APP_ID + " is starting !");
   
   this.m_properties = properties;
   for (String s : properties.keySet()) {
       s_logger.info("Activate - " + s + ": " + properties.get(s));
   }
    
// Create a I2C device configuration
	s_logger.info("Creating a I2C device configuration");
	 this.config = new I2CDeviceConfig(1, CFG_ADDRESS, ADDRESS_SIZE, CLK_FREQ);  //Bus index is 1 for I2C
   s_logger.info("Obtained I2C device-config: "+config.toString());
   
	// Open device using configuration
    this.si7021 = (I2CDevice) DeviceManager.open(I2CDevice.class,this.config);
   s_logger.info("Obtained I2C device: "+si7021.toString());  
	  
   
// Acquire a Cloud Application Client for this Application
s_logger.info("Getting CloudClient for {}...", APP_ID);
this.m_cloudClient = this.m_cloudService.newCloudClient(APP_ID);
this.m_cloudClient.addCloudClientListener(this);

//get position of Gateway
NmeaPosition position = this.m_positionService.getNmeaPosition();
this.latitude=position.getLatitude();
this.longitude=position.getLongitude();
this.altitude=position.getAltitude();
//print position
//s_logger.info("gateway is at lat: "+this.latitude +"long: "+this.longitude);


doUpdate();
   
  }
   catch(Exception e)
   {
    s_logger.debug("Activation issue :"+e.toString());
   }
  }

     
  public void updated(Map<String, Object> properties) {
 		// TODO Auto-generated method stub
 		 s_logger.info("Updatng  properties...");
 		this.m_properties = properties;
 		
 		for (String s : properties.keySet()) {
             s_logger.info("Update - " + s + ": " + properties.get(s));
         }
         
         s_logger.info("Updated SI7021... Done.");
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
     
  
  protected void deactivate(ComponentContext componentContext) throws IOException {
	  
	  s_logger.info("Trying to Close I2C device...");
	 if(si7021.isOpen())
		 si7021.close(); //Close the I2C device if it is open
	 s_logger.info(" I2C device is now (false means Closed)"+si7021.isOpen());
	 
	// shutting down the worker and cleaning up the properties
      this.m_worker.shutdown();
      // Releasing the CloudApplicationClient
      s_logger.info("Releasing CloudApplicationClient for {}...", APP_ID);
      this.m_cloudClient.release();
      
   s_logger.info("Bundle " + APP_ID + " has stopped!");
   
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
	  boolean deviation = false ; // track deviation
	  
	  try {
	 // fetch the publishing configuration from the publishing properties
    String topic = (String) this.m_properties.get(PUBLISH_TOPIC_PROP_NAME);
    Integer qos = (Integer) this.m_properties.get(PUBLISH_QOS_PROP_NAME);
    Boolean retain = (Boolean) this.m_properties.get(PUBLISH_RETAIN_PROP_NAME);
    
	
   // Allocate a new payload
   KuraPayload payload = new KuraPayload();
   
   // Timestamp the message
   payload.setTimestamp(new Date());
   
   //set the Kura Position
   KuraPosition gatewayPosition = new KuraPosition() ;
   gatewayPosition.setLatitude(this.latitude);
   gatewayPosition.setLongitude(this.longitude);
   gatewayPosition.setAltitude(this.altitude);
   payload.setPosition(gatewayPosition);
   
   //set gateway Client ID and Account name
   payload.addMetric("AccountName", this.m_dataTrasportservice.getAccountName());
   payload.addMetric("GatewayDeviceID", this.m_dataTrasportservice.getClientId());
   payload.addMetric("SensorDevice", "Temperature");
   
 /* Add the temperature as a metric to the Payload */
   
   //fetch the  sensor date
   double[] sensorvalues = this.readI2C() ;  
   
   //determine if there is deviation
   if((sensorvalues[0]< (Double)this.m_properties.get(PERMISSIBLE_HUM_MIN)) || (sensorvalues[0]> (Double)this.m_properties.get(PERMISSIBLE_HUM_MAX)) || 
			(sensorvalues[1]< (Double)this.m_properties.get(PERMISSIBLE_TEMP_MIN)) || (sensorvalues[1]> (Double)this.m_properties.get(PERMISSIBLE_TEMP_MAX))
			)
	    deviation = true ;
   
   //send alert mail if there is a deviation in  reading; reading is out of range
     this.alertOption = (Boolean)this.m_properties.get(EnableAlert);
     
     if(this.alertOption.booleanValue()) {
     sendAlertMailer("Reading Deviation alert !",
    		 "Humidity & Temp Readings are: " + String.valueOf(sensorvalues[0])+
    		 " % and " +
    		 String.valueOf(sensorvalues[1]) + "*C");
     }
     
     
  
 if(((Integer)this.m_properties.get(MONITORING_TYPE))==1) //monitor only deviations in readings
 {
	   if(deviation = true)
	   {
	   payload.addMetric("humidity", sensorvalues[0]);
	   payload.addMetric("temperature", sensorvalues[1]);
	   payload.addMetric("deviation", deviation);
	   
	   // Publish the message
		s_logger.info("trying to Publish to Topic:", topic, payload);
	    m_cloudClient.publish(topic, payload,qos, retain);
	    s_logger.info("Published to {} message: {}", topic, payload);
	   }
	   
   }
 else{ //if monitoring absolute values; the default value
   
   payload.addMetric("humidity", sensorvalues[0]);
   payload.addMetric("temperature", sensorvalues[1]);
   payload.addMetric("deviation", deviation);
   
   // Publish the message
	s_logger.info("trying to Publish to Topic:", topic, payload);
    m_cloudClient.publish(topic, payload,qos, retain);
    s_logger.info("Published to {} message: {}", topic, payload);
 }
   } 
   catch (Exception e) {
    s_logger.error("Cannot publish topic: ", e);
   }
   
   
  }
  
     
    //Sensor specific API 
    public double[]  readI2C() throws IOException
     {
    	
    	 double[]  values= new double[2];  // to store & return the temp & humidity
    	
        try  {
       
        	 s_logger.info("inside readI2c, just entered");
            //Start Relative Humidity reading
        	//this.si7021.begin();
            this.si7021.write(MEASURE_HUMIDITY);  //send  
            s_logger.info("write completed in humidity reading");
           // Thread.sleep(1000);
            //read humidity
            ByteBuffer humBuf = ByteBuffer.allocate(READ_HUMIDITY_SIZE);  // retuns 2 bytes of data
            s_logger.info("Allocated buffer"+humBuf);
            
            //this.si7021.read(RT_ADDR_SIZE,humBuf);   //most significant byte first
            this.si7021.read(humBuf);   //most significant byte first
            //si7021.read(0xE7,2,humBuf); //
            s_logger.info("read completd in humidity reading");
            
            s_logger.info("completed humidity reading");
            
          //Start temperature reading
            this.si7021.write(MEASURE_TEMP);  //send command 0XE3 to measure temp
                  
                 // Thread.sleep(1000);
                  // Read 2 bytes of temperature data, msb first
                  // Read temperature
                  ByteBuffer tempBuf = ByteBuffer.allocate(READ_TEMP_SIZE);  // retuns 2 bytes of data
                  //int noOfBytes= this.si7021.read(RT_ADDR_SIZE,tempBuf);   //most significant byte first
                 
                  int noOfBytes= this.si7021.read(tempBuf);   //most significant byte first
                  
                  s_logger.info("completed temperature reading with no of Bytes:"+noOfBytes);
             
         /*Interpret the result humidity
            
            %RH = (125*RH_Code)/ 65536 – 6
         */
          
            
             //retrieve 2 bytes of humidity; MSB at Byte(0) and LSB at Byte(1)
             /*
             HUMIDITY_VAL = humBuf.getShort(0);
            //put bytes in correct Order
             HUMIDITY_VAL=Short.reverseBytes(HUMIDITY_VAL);
             s_logger.info("Humidity code is :"+HUMIDITY_VAL);
             */
            /* Not using the first method of calculation as it gives erroneous results sometimes 
             HUMIDITY_VAL=0X00000000;
             HUMIDITY_VAL= HUMIDITY_VAL|humBuf.get(0);
             HUMIDITY_VAL=HUMIDITY_VAL<<8;
             HUMIDITY_VAL=HUMIDITY_VAL|humBuf.get(1);
             s_logger.info("Humidity code is :"+HUMIDITY_VAL);
             values[0] = ((125*HUMIDITY_VAL)/65536)-6 ;
             */
                  values[0] = (((((humBuf.get(0) & 0xFF) * 256) + (humBuf.get(1) & 0xFF)) * 125.0) / 65536.0) - 6;
             s_logger.info("Humidity value as per second reading :"+values[0]);
             
             s_logger.info("Humidity in % is :"+values[0]);
             humBuf.clear();
             
           
             
             /*Interpret the result temperature
                 
                Temperature 'C = (175.72*Temp_Code)/65536 - 46.85
             */
             //Interpret the result temperature  
             
             
            /*
              TEMP_VAL=tempBuf.getShort(0);
              //put bytes in correct order
              TEMP_VAL=Short.reverseBytes(TEMP_VAL);
              */
            /*
             TEMP_VAL=0x00000000;
             TEMP_VAL=TEMP_VAL|tempBuf.get(0);
             TEMP_VAL=TEMP_VAL<<8;
             TEMP_VAL=TEMP_VAL|tempBuf.get(1);
              s_logger.info("Temperature code is :"+TEMP_VAL);
              
            values[1]=((175.72*TEMP_VAL)/65536)-46.85 ;
              s_logger.info("temp in *C is :"+values[1]);
            */
            values[1] = (((((tempBuf.get(0) & 0xFF) * 256) + (tempBuf.get(1) & 0xFF)) * 175.72) / 65536.0) - 46.85;
            s_logger.info("temp in *C by second method :"+values[1]);
          
            tempBuf.clear();
            s_logger.info("completed interpreting values ");
           // this.si7021.end();
            
            return values;
             
     }catch (Exception e){
    	 s_logger.info("exception in i2c reading"+e.toString());
    	 // this.si7021.close(); // do not close this as this will throw closed device exception in next reading
      return null;
     }
     }

    
    public void sendAlertMailer(String msgSub, String msgBody){

    	//update the mail server & user details
    	// this.alertOption = (Boolean)this.m_properties.get(EnableAlert);
    	 this.emailservertype = (String) this.m_properties.get(EmailServerType);
    	 this.hostname = (String) this.m_properties.get(MailServerHostName);
    	 this.from_user = (String) this.m_properties.get(MailFrom);
    	 this.fromUser_password = (String) this.m_properties.get(FromMailPassword);
    	 this.to_user = (String) this.m_properties.get(MailTo);
    	 
    	 //update emailAlert mail service
    	 this.emailService.setHostname(this.hostname);
    	 this.emailService.setFromMailID(this.from_user);
    	 this.emailService.setFromMailPassword(this.fromUser_password);
    	 this.emailService.setToMailID(this.to_user);
    	 
    	 switch(this.emailservertype) {
    	 case "XChangeServer": 
    		 
    		 s_logger.info("Alert mail sent success? " + this.emailService.sendXChangeMail(msgSub,msgBody)); 
    		
    		 break;
    	 case "Gmail_TSL": 
    		 s_logger.info("Alert mail sent success? " + this.emailService.sendGmailTSL(msgSub, msgBody));
    		 break;
    	 case "Gmail_SSL":
    		 s_logger.info("Alert mail sent success? " + this.emailService.sendGmailSSL(msgSub, msgBody));
    		 break;
    	 default: 
    		 s_logger.info("Alert mail could not be sent");
    	 }
    }
    
    //Kura cloud client lister methods
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
     
 
}

