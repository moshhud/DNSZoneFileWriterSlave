package zonefilewriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import dnshostinginfo.DnsHostingInfoDTO;
import dnshostinginfo.DnsHostingPTRDTO;
import dnshostinginfo.DnsHostingZoneRecordDTO;


public class RecordFileWriter {
	static Logger logger = Logger.getLogger(RecordFileWriter.class);
	
	String[] dotBDSlds = {"com", "co", "edu", "gov", "info", "net", "org", "ac", "mil" ,"ws", "tv"};
		
	private  final String NAMED_FILE_CONTENT ="zone \"VAR_DOMAIN\" IN {\n"
			+ "  type slave;\n"
			+ "  file \""+ZoneFileWriter.zoneFileLocation+"/VAR_ZONE_FILE_NAME\";\n"
			+ "  masters {VAR_ALLOWED_UPDATE_IP;};\n"
			+ "};";	
	private  final String REVERSE_ZONE_FILE_CONSTANT_PORTION = "$TTL 86400\n"+
			"VAR_FQDN.  900  IN SOA 	VAR_DNS_1. VAR_DNS_2.  (\n"+
			"   VAR_SERIAL_NUMBER	; Serial\n"+
			"   1200	; Refresh \n"+
			"   3600	; Retry\n"+
			"   604800	; Expire\n"+
			"   86400 )	; Minimum ttl\n"+
			" \t\t\t\tIN	NS	VAR_DEFAULT_DNS_1. \n"+
			" \t\t\t\tIN	NS	VAR_DEFAULT_DNS_2. \n";
	public static String getSerialNumber(long currentTime){

		Calendar cal = new GregorianCalendar();
		int year = cal.get(cal.YEAR);		
		int month = cal.get(cal.MONTH)+1;
		String monthString = (month<10?"0"+month:""+month);
		int day = cal.get(cal.DAY_OF_MONTH);
		String dayString = (day<10?"0"+day:""+day);
		int hour = cal.get(cal.HOUR_OF_DAY);
		int min = cal.get(cal.MINUTE);
		
		int countOfHalfHour = hour*2+(min>=30?1:0);
		String countOfHalfHourString = ""+countOfHalfHour;
		while(countOfHalfHourString.length()<2){
			countOfHalfHourString="0"+countOfHalfHour;
		}
		return ""+year+monthString+dayString+countOfHalfHourString;
	}
	public void writeIntoNamedFile(String domain, String fileName,Writer writer) {
		try {
			
			String content = NAMED_FILE_CONTENT.replace("VAR_DOMAIN", domain);
			content = content.replaceAll("VAR_ZONE_FILE_NAME", fileName);
			content = content.replaceAll("VAR_ALLOWED_UPDATE_IP", ZoneFileWriter.master_server_ip);
			writer.write(content);
			writer.write("\n");
			writer.flush();
			writer.close();
		}
		catch(Exception e) {
			logger.fatal(e.toString());			
		}
	}
	
	public  void createZoneFile(DnsHostingInfoDTO dto) {		
		LinkedHashMap<String, String> mxData = new LinkedHashMap<String, String>();
		LinkedHashMap<String, String> aData = new LinkedHashMap<String, String>();
		try {		
			String primaryDNS = dto.getPrimaryDNS().length()>3? dto.getPrimaryDNS():ZoneFileWriter.primaryDNS;
			String secondaryDNS = dto.getSecondaryDNS().length()>3? dto.getSecondaryDNS():ZoneFileWriter.secondaryDNS;	
			
			if(dto.getZoneRecordDTOMap().values()!=null && dto.getZoneRecordDTOMap().values().size()>0) {
				for (DnsHostingZoneRecordDTO zoneDTO : dto.getZoneRecordDTOMap().values()) {
					
					if(zoneDTO!=null) {			
						
						if(zoneDTO.getRecordType().equals("MX")) {							
							String[] arr = zoneDTO.getRecordValue().split(Pattern.quote("."));
							mxData.put(arr[0], zoneDTO.getRecordValue());							
						}						
						if(zoneDTO.getRecordType().equals("A")) {
							if(mxData.containsKey(zoneDTO.getRecordName())) {
								aData.put(zoneDTO.getRecordName(), zoneDTO.getRecordValue());
							}
						}
						
					}
				}				
				
				if(aData!=null&&aData.size()>0) {
					createReverseDNSFile(mxData,aData,primaryDNS,secondaryDNS);
				}
			}else {
				logger.debug("No zone Record found for "+dto.getDomainName());
			}
			
			
		}catch(Exception e) {
			logger.fatal(e.toString());
		}
		
	}
	
	
	public void deleteContent(String filePath, String matchString) {
	   try {
		    //logger.debug("Deleting if any duplicate for : "+matchString+" in named.conf");
		    BufferedReader file = new BufferedReader(new FileReader(filePath));
	        String line;
	        StringBuffer inputBuffer = new StringBuffer();
	        int count=1;
	        boolean ignore = false;
	        
	        while ((line = file.readLine()) != null) {
	        	if(line.contains(matchString)){	
	        		  ignore=true; 
	        	} 
	        	if(ignore && count<=5){
	        			count++;
	        			continue;
	        	}
	        	if(count>5){
	        		ignore = false;
	        		count=1;
	        	}
	        	
	            inputBuffer.append(line);
	            inputBuffer.append("\n");
	        }
	        String inputStr = inputBuffer.toString();	
	        file.close();	       
	        FileOutputStream fileOut = new FileOutputStream(filePath);
	        fileOut.write(inputStr.getBytes());
	        fileOut.flush();
	        fileOut.close();
	   }catch(Exception e) {
			logger.fatal(e.toString());
		}
	}
	
	public void deleteZoneFileEntry(DnsHostingInfoDTO dto, String filePath) {
		try {
			String namedFile = ZoneFileWriter.winDir+ZoneFileWriter.namedFilePath+"/"+ZoneFileWriter.namedFileName;
			
			deleteContent(namedFile,dto.getDomainName());
	        
	        	        
	        if(dto.getZoneRecordDTOMap().values()!=null && dto.getZoneRecordDTOMap().values().size()>0) {
	        	LinkedHashMap<String, String> mxData = new LinkedHashMap<String, String>();
				LinkedHashMap<String, String> aData = new LinkedHashMap<String, String>();
				
	        	for (DnsHostingZoneRecordDTO zoneDTO : dto.getZoneRecordDTOMap().values()) {
	        		if(zoneDTO!=null) {
	        			if(zoneDTO.getRecordType().equals("MX")) {							
							String[] arr = zoneDTO.getRecordValue().split(Pattern.quote("."));
							mxData.put(arr[0], zoneDTO.getRecordValue());							
						}
	        			if(zoneDTO.getRecordType().equals("A")) {
							if(mxData.containsKey(zoneDTO.getRecordName())) {
								aData.put(zoneDTO.getRecordName(), zoneDTO.getRecordValue());
							}
						}
	        		}
	        	}
	        	
	        	if(aData!=null&&aData.size()>0) {
	        		deleteReverseDNSInfo(mxData,aData,namedFile);
				}
	        }
	        
		}
		catch(Exception e) {
			logger.fatal(e.toString());
		}
	}
	
	public void deleteReverseDNSInfo(LinkedHashMap<String, String> mxData,LinkedHashMap<String, String> aData, String namedFile) {
		try {
			for(Entry<String, String> entry : aData.entrySet()) {
				String[] arr = entry.getValue().split(Pattern.quote("."));
				String reverse = arr[2]+"."+arr[1]+"."+arr[0];
				String fqdn = reverse+".in-addr.arpa";
				logger.debug("Reverse: "+fqdn);				
				deleteContent(namedFile,fqdn);
				
			}
		}catch(Exception e){
    		logger.fatal(e.toString());
    	}
	}	
	public void removeEntry(String str,String fileName) {
		try {	       
	        BufferedReader file = new BufferedReader(new FileReader(fileName));
	        String line;
	        StringBuffer inputBuffer = new StringBuffer();
	
	        while ((line = file.readLine()) != null) {
	        	if(line.contains(str)) continue;
	            inputBuffer.append(line);
	            inputBuffer.append('\n');
	        }
	        String inputStr = inputBuffer.toString();	
	        file.close();
	        
	        FileOutputStream fileOut = new FileOutputStream(fileName);
	        fileOut.write(inputStr.getBytes());
	        fileOut.close();
	
	    } catch (Exception e) {
	        logger.fatal("Error: "+e.toString());
	    }
	}
		
	public  void createReverseDNSFile(LinkedHashMap<String, String> mxData,LinkedHashMap<String, String> aData,String primaryDNS, String secondaryDNS) {
		
		try {	
			for(Entry<String, String> entry : aData.entrySet()) {				
				String[] arr = entry.getValue().split(Pattern.quote("."));
				String reverse = arr[2]+"."+arr[1]+"."+arr[0];
				logger.debug("reverse: "+reverse);			
				String forward = arr[0]+"."+arr[1]+"."+arr[2];
				String reverseFileName = "rev."+forward;
				String fqdn = reverse+".in-addr.arpa";
				String value = mxData.get(entry.getKey());;
				String content = arr[3]+" PTR "+value;
				String fileName = ZoneFileWriter.winDir+ZoneFileWriter.zoneFileLocation+"/"+ZoneFileWriter.reverseDIR+"/"+reverseFileName;
				File file = new File(fileName);
				FileWriter fw = null;
				boolean writeReverseNamedFile = false;
				if(file.exists()) {
					removeEntry(value,fileName);
					fw = new FileWriter(file,true);					
					fw.write(content);
					fw.write("\n");
					
				}else {
					fw = new FileWriter(file,false);
					String header = REVERSE_ZONE_FILE_CONSTANT_PORTION.replace("VAR_FQDN", fqdn);
					header = header.replaceAll("VAR_DNS_1", primaryDNS);
					header = header.replaceAll("VAR_DNS_2", secondaryDNS);					
					header = header.replaceAll("VAR_SERIAL_NUMBER", getSerialNumber(System.currentTimeMillis()));
					header = header.replaceAll("VAR_DEFAULT_DNS_1", primaryDNS);
					header = header.replaceAll("VAR_DEFAULT_DNS_2", secondaryDNS);
					header +="$ORIGIN  "+fqdn+".";
					fw.write(header);
					fw.write("\n");
					fw.write(content);
					fw.write("\n");
					writeReverseNamedFile = true;
				}
				fw.flush();
				fw.close();
				
				if(writeReverseNamedFile) {
					writeIntoNamedFile(fqdn,ZoneFileWriter.reverseDIR+"/"+reverseFileName,new FileWriter(new File(ZoneFileWriter.winDir+ZoneFileWriter.namedFilePath+"/"+ZoneFileWriter.namedFileName),true));
				}
			}
		}catch(Exception e) {
			logger.fatal(e.toString());
		}
		
	}
	
	public  void createReverseDNSFile(DnsHostingPTRDTO dto, String primaryDNS, String secondaryDNS) {
		try {
			
			primaryDNS= primaryDNS.length()>3? primaryDNS:ZoneFileWriter.primaryDNS;
			secondaryDNS = secondaryDNS.length()>3? secondaryDNS:ZoneFileWriter.secondaryDNS;
			
			String[] arr = dto.getIpAddress().split(Pattern.quote("."));
			String reverse = arr[2]+"."+arr[1]+"."+arr[0];
			String forward = arr[0]+"."+arr[1]+"."+arr[2];
			logger.debug("reverse: "+reverse);
			String value =dto.getEmailServerDomain();
			String content = arr[3]+" PTR "+value;
			String reverseFileName = "rev."+forward;
			String fqdn = reverse+".in-addr.arpa";
			String fileName = ZoneFileWriter.winDir+ZoneFileWriter.zoneFileLocation+"/"+ZoneFileWriter.reverseDIR+"/"+reverseFileName;
			File file = new File(fileName);
			FileWriter fw = null;
			boolean writeReverseNamedFile = false;
			
			if(file.exists()) {
				//remove duplicate
				removeEntry(value,fileName);
				fw = new FileWriter(file,true);					
				fw.write(content);
				fw.write("\n");				
			}
			else {
				fw = new FileWriter(file,false);
				String header = REVERSE_ZONE_FILE_CONSTANT_PORTION.replace("VAR_FQDN", fqdn);
				header = header.replaceAll("VAR_DNS_1", primaryDNS);
				header = header.replaceAll("VAR_DNS_2", secondaryDNS);					
				header = header.replaceAll("VAR_SERIAL_NUMBER", getSerialNumber(System.currentTimeMillis()));
				header = header.replaceAll("VAR_DEFAULT_DNS_1", primaryDNS);
				header = header.replaceAll("VAR_DEFAULT_DNS_2", secondaryDNS);
				header +="$ORIGIN  "+fqdn+".";
				fw.write(header);
				fw.write("\n");
				fw.write(content);
				fw.write("\n");
				writeReverseNamedFile = true;
			}
			fw.flush();
			fw.close();
			
			if(writeReverseNamedFile) {
				writeIntoNamedFile(fqdn,ZoneFileWriter.reverseDIR+"/"+reverseFileName,new FileWriter(new File(ZoneFileWriter.winDir+ZoneFileWriter.namedFilePath+"/"+ZoneFileWriter.namedFileName),true));
			}
			
		}
		catch(Exception e) {
			logger.fatal(e.toString());
		}
	}
	
	public  boolean processPTRData(LinkedHashMap<Long, DnsHostingPTRDTO> data,String ids) {
		boolean status = false;
		for(DnsHostingPTRDTO dto:data.values() ) {
			try {
				if(!dto.getEmailServerDomain().endsWith(".")) {
					dto.setEmailServerDomain(dto.getEmailServerDomain()+".");
				}
				//1=add,2=edit,3=delete
				if(dto.getZoneFileForSlave()==1 || dto.getZoneFileForSlave()==2) {
					createReverseDNSFile(dto,"","");
				}
				else if(dto.getZoneFileForSlave()==3) {
					String[] arr = dto.getIpAddress().split(Pattern.quote("."));
					String reverse = arr[2]+"."+arr[1]+"."+arr[0];
					String forward = arr[0]+"."+arr[1]+"."+arr[2];
					logger.debug("reverse: "+reverse);
					String value =dto.getEmailServerDomain();					
					String reverseFileName = "rev."+forward;					
					String fileName = ZoneFileWriter.winDir+ZoneFileWriter.zoneFileLocation+"/"+ZoneFileWriter.reverseDIR+"/"+reverseFileName;
					 
					removeEntry(value,fileName);
				}
				
				status = true;
				Thread.sleep(100);
			}
			catch(Exception e) {
				logger.fatal(e.toString());
			}
		}
		
		return status;
	}
		
	public  boolean processData(LinkedHashMap<Long, DnsHostingInfoDTO> data,String ids) {
		boolean status = false;
		for(DnsHostingInfoDTO dto:data.values() ) {
			
			try {				
				dto.setDomainName(java.net.IDN.toASCII(dto.getDomainName()));
				String domainName = dto.getDomainName();
				String fileName = domainName;
				String fileDIR = "";
				
				if(domainName.endsWith(".bd")) {
					for(String sld: dotBDSlds){
						sld = sld+".bd";
						if(dto.getDomainName().contains(sld)) {
							fileDIR = sld;
							break;
						}else {
							fileDIR = "bd";							
						}
						
					}					
				}else if(domainName.endsWith(".বাংলা")){
					fileDIR = ZoneFileWriter.zoneFileDIRDOTBANGLA;
				}else {
					fileDIR = ZoneFileWriter.zoneFileDIROTHER;
				}
				
				String filePath = ZoneFileWriter.winDir+ZoneFileWriter.zoneFileLocation+"/"+fileDIR+"/"+fileName;
                if(dto.getZoneFileForSlave()==1) {
                	createZoneFile(dto); 
                	Thread.sleep(100);
    				String namedFile = ZoneFileWriter.winDir+ZoneFileWriter.namedFilePath+"/"+ZoneFileWriter.namedFileName;
    				//deleteContent(namedFile,dto.getDomainName());
    				writeIntoNamedFile(domainName,fileDIR+"/"+fileName,new FileWriter(new File(namedFile),true));    					    				
				}
				else if(dto.getZoneFileForSlave()==2){
					deleteZoneFileEntry(dto,filePath);
				}
				
				
				status = true;
				Thread.sleep(100);
				
			}catch(Exception e) {
				logger.fatal(e.toString());
			}
			
		}		
		logger.debug("Zone File written successfully.");
		
		return status;
	}
	
	
	public void runUnixCommand(String... command){
		
		try{
			Process p = Runtime.getRuntime().exec(command);
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String termialOutputLine;
			while ((termialOutputLine = stdInput.readLine()) != null) {
				logger.debug(termialOutputLine);
			}
		}catch(Exception ex){
			logger.fatal("fatal",ex);
		}
	}

}
