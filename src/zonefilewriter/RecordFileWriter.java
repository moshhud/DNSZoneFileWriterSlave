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

import common.ApplicationConstants;
import common.EmailValidator;
import common.SmsMailLogDAO;
import dnshostinginfo.DNSHostingConstant;
import dnshostinginfo.DnsHostingInfoDTO;
import dnshostinginfo.DnsHostingZoneRecordDTO;


public class RecordFileWriter {
	static Logger logger = Logger.getLogger(RecordFileWriter.class);
	String winDir = "D:/root";	//test dir for windows
	String[] dotBDSlds = {"com", "co", "edu", "gov", "info", "net", "org", "ac", "mil" ,"ws", "tv"};
		
	private  final String NAMED_FILE_CONTENT ="zone \"VAR_DOMAIN\" IN {\n"
			+ "  type slave;\n"
			+ "  file \""+ZoneFileWriter.zoneFileLocation+"/VAR_ZONE_FILE_NAME\";\n"
			+ "  masters {VAR_ALLOWED_UPDATE_IP;};\n"
			+ "};";	
	
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
					createReverseDNSFile(mxData,aData);
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
			String namedFile = winDir+ZoneFileWriter.namedFilePath+"/"+ZoneFileWriter.namedFileName;
			
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
	
	public  void createReverseDNSFile(LinkedHashMap<String, String> mxData,LinkedHashMap<String, String> aData) {
		
		try {	
			for(Entry<String, String> entry : aData.entrySet()) {				
				String[] arr = entry.getValue().split(Pattern.quote("."));
				String reverse = arr[2]+"."+arr[1]+"."+arr[0];
				logger.debug("reverse: "+reverse);			
				String reverseFileName = "db."+reverse;
				String fqdn = reverse+".in-addr.arpa";
				String fileName = winDir+ZoneFileWriter.zoneFileLocation+"/"+ZoneFileWriter.reverseDIR+"/"+reverseFileName;
				File file = new File(fileName);
				
				boolean writeReverseNamedFile = false;
				if(!file.exists()) {
					writeReverseNamedFile = true;					
				}
				
				if(writeReverseNamedFile) {
					writeIntoNamedFile(fqdn,ZoneFileWriter.reverseDIR+"/"+reverseFileName,new FileWriter(new File(winDir+ZoneFileWriter.namedFilePath+"/"+ZoneFileWriter.namedFileName),true));
				}
			}
		}catch(Exception e) {
			logger.fatal(e.toString());
		}
		
	}
		
	
	public  boolean processData(LinkedHashMap<Long, DnsHostingInfoDTO> data,String ids) {
		boolean status = false;
		for(DnsHostingInfoDTO dto:data.values() ) {
			
			try {
				
				dto.setDomainName(java.net.IDN.toASCII(dto.getDomainName()));
				String fileName = "db."+dto.getDomainName();
				String fileDIR = "";
				
				if(dto.getDomainName().endsWith(".bd")) {
					for(String sld: dotBDSlds){
						sld = sld+".bd";
						if(dto.getDomainName().contains(sld)) {
							fileDIR = sld;
							break;
						}else {
							fileDIR = "bd";							
						}
						
					}					
				}else if(dto.getDomainName().endsWith(".বাংলা")){
					fileDIR = ZoneFileWriter.zoneFileDIRDOTBANGLA;
				}else {
					fileDIR = ZoneFileWriter.zoneFileDIROTHER;
				}
				
				String filePath = winDir+ZoneFileWriter.zoneFileLocation+"/"+fileDIR+"/"+fileName;
                if(dto.getZoneFileForSlave()==1) {
                	createZoneFile(dto);    				
    				String namedFile = winDir+ZoneFileWriter.namedFilePath+"/"+ZoneFileWriter.namedFileName;
    				deleteContent(namedFile,dto.getDomainName());
    				writeIntoNamedFile(dto.getDomainName(),fileDIR+"/"+fileName,new FileWriter(new File(namedFile),true));
    					    				
				}
				else if(dto.getZoneFileForSlave()==2){
					deleteZoneFileEntry(dto,filePath);
				}
				
				
				status = true;
				Thread.sleep(200);
				
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
