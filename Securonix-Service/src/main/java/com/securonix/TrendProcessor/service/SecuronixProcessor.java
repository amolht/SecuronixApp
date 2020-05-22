package com.securonix.TrendProcessor.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

@Service
public class SecuronixProcessor {
	
	private static final String UNZIP_DIRECTORY_PATH = "src/main/resources/unzip";
	private byte[] buffer = new byte[1024];
	
	public Map<String, Object> extractSecuronixRawData(MultipartFile file) throws IOException, FileNotFoundException {
		ZipInputStream zipIn = new ZipInputStream(file.getInputStream());
        ZipEntry entry;
        CsvParserSettings parserSettings = new CsvParserSettings();
     
        parserSettings.getFormat().setDelimiter('\t');
        
        CsvParser csv = new CsvParser(parserSettings);
        //use this if the files are small (less than 50mb each)
        parserSettings.setReadInputOnSeparateThread(false);
        
        File destDir = new File(UNZIP_DIRECTORY_PATH);
        final Map<String, Object> securonixTrendData = new ConcurrentSkipListMap<>();
        final Map<String, Object> securonixTopUsers = new ConcurrentSkipListMap<>();
        final Map<String, Object> securonixTopFiles = new ConcurrentSkipListMap<>();
        
        while ((entry = zipIn.getNextEntry()) != null) {
        	//InputStreamReader isr = new InputStreamReader(zipIn);
        	
        	if(!entry.isDirectory()) {
            	File newFile = newFile(destDir, entry);
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zipIn.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                
        		List<Record> records = csv.parseAllRecords(newFile);
        		newFile.deleteOnExit();
        		processSecuronixData(records, securonixTrendData, securonixTopUsers, securonixTopFiles);
        	}
        }
        
        zipIn.closeEntry();
        zipIn.close();
        
        final Map<String, Object> securonixResponseData = new HashMap<>();
        securonixResponseData.put("TrendData", securonixTrendData);
        securonixResponseData.put("TopUsers", securonixTopUsers);
        securonixResponseData.put("TopFiles", securonixTopFiles);

        
        return securonixResponseData;
	}
    
    public File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
    	String[] fileNameSplit = zipEntry.getName().split("/");
    	String fileName = fileNameSplit[fileNameSplit.length - 1];
        File destFile = new File(destinationDir, fileName);
         
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();
         
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
         
        return destFile;
    }
	
	private void processSecuronixData(final List<Record> records, final Map<String, Object> securonixTrendData, 
			final Map<String, Object> securonixTopUsers, final Map<String, Object> securonixTopFiles) {
		
		Map<Integer, Object> hourMap = new HashMap<>();
		Map<String, Object> ipAddressMap = new HashMap<>();
		Map<String, Integer> usersMap = new HashMap<>();
		Map<String, Integer> filesMap = new HashMap<>();
		
		Map<String, Integer> topUsersMap = new HashMap<>();
		Map<String, Integer> topFilesMap = new HashMap<>();
		String date = extractColumnValue(records.get(0), 2);
		if (StringUtils.isNotEmpty(date)) {
			securonixTrendData.put(date, hourMap);
			
			for (Record singleRec : records ) {
				
				String time = extractColumnValue(singleRec, 3).split(":")[0];
				if (StringUtils.isNumeric(time)) {
					int startHour = Integer.parseInt(time);
					String objectName = extractColumnValue(singleRec, 5);
					String userName = extractColumnValue(singleRec, 13);
					String ipAddress = extractColumnValue(singleRec, 8);
					
					if (hourMap.containsKey(startHour)) {
						ipAddressMap = (Map<String, Object>) hourMap.get(startHour);
						if (ipAddressMap.containsKey(ipAddress)) {
							int count = (int) ipAddressMap.get(ipAddress);
							ipAddressMap.put(ipAddress, count+1);
						} else {
							ipAddressMap.put(ipAddress, 1);
						}
					} else {
						ipAddressMap = new HashMap<>();
						ipAddressMap.put(ipAddress, 1);
						hourMap.put(startHour, ipAddressMap);
					}
					
					if (usersMap.containsKey(userName)) {
						int count = (int) usersMap.get(userName);
						usersMap.put(userName, count+1);
					} else {
						usersMap.put(userName, 1);
					}
					
					
					if (filesMap.containsKey(objectName)) {
						int count = (int) filesMap.get(objectName);
						filesMap.put(objectName, count+1);
					} else {
						filesMap.put(objectName, 1);
					}
				}
			}
		}
		
		TopsCollection topItems = new TopsCollection();
		
		topUsersMap = topItems.getTops(usersMap);
		topFilesMap = topItems.getTops(filesMap);
		
		securonixTopUsers.put(date, topUsersMap);
		securonixTopFiles.put(date, topFilesMap);
		
	}
	
	private class TopsCollection { 

		private Map<String, Integer> collectors = new HashMap<>();

		private void add(String playerName, int score) {
		    collectors.put(playerName, score);
		}

		private void clearCollectors() {
		    synchronized (collectors) {
		        collectors.clear();
		    }
		}

		public Map<String, Integer> getTops(Map<String, Integer> usersMap) {
			Map<String, Integer> topMap = new HashMap<>();
			
			clearCollectors();
			
			for (Entry<String, Integer> userEntry : usersMap.entrySet()) {
				add(userEntry.getKey(), userEntry.getValue());
			}
			
			List<Map.Entry<String, Integer>> topList = collectors.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue, Collections.reverseOrder())).limit(5).collect(Collectors.toList());
			
			for (Entry<String, Integer> entry : topList) {
				topMap.put(entry.getKey(), entry.getValue());
			}
			
		    return topMap;
		}
	}

	private String extractColumnValue(Record singleRec, int colIndex) {
		if (singleRec.getValues().length <= colIndex) {
			return "";
		}
		String[] strArr = singleRec.getValues()[colIndex].split("=");
		return strArr.length >= 2 ? strArr[1] : strArr[0];
	}

}
