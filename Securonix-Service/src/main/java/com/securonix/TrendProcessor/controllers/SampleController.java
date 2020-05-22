package com.securonix.TrendProcessor.controllers;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.securonix.TrendProcessor.service.SecuronixProcessor;

@CrossOrigin
@RestController
@RequestMapping(path = "/securonix")
public class SampleController {

	@Autowired
	private SecuronixProcessor processor;
	
	@GetMapping
    public String healthCheck(){
        return "Welcome to Securonix";
    }
    
	@CrossOrigin
    @PostMapping("/process-securonix-data")
    public ResponseEntity uploadToLocalFileSystem(@RequestParam("file") MultipartFile file) throws IOException {
    	Map<String, Object> responseBody = processor.extractSecuronixRawData(file);
    	return ResponseEntity.ok(responseBody);
    }

}
