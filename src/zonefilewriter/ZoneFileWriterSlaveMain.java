package zonefilewriter;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import shutdown.ShutDownListener;
import shutdown.ShutDownService;

public class ZoneFileWriterSlaveMain implements ShutDownListener{
	
	static Logger logger = Logger.getLogger(ZoneFileWriterSlaveMain.class);
	public static ZoneFileWriterSlaveMain obMain = null;
	public static   ZoneFileWriter obZoneFileWriter = null;
	public static void main(String[] args)	
	{
		PropertyConfigurator.configure("log4j.properties");
		obMain = new ZoneFileWriterSlaveMain();
		
		obZoneFileWriter = ZoneFileWriter.getInstance();
		obZoneFileWriter.start();
		
		ShutDownService.getInstance().addShutDownListener(obMain);
		logger.debug("Zone File Writer started successfully.");
	}

	@Override
	public void shutDown() {
		// TODO Auto-generated method stub
		obZoneFileWriter.shutdown();
		logger.debug("Shut down server successfully");
		System.exit(0);
	}

}
