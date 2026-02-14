package com.ga.csv_processor.controllers;

import com.ga.csv_processor.models.Employee;
import com.ga.csv_processor.services.CSVProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;

@RestController
@RequestMapping("processor")
public class CSVProcessorController {
    @Autowired
    private CSVProcessor csvProcessor;

    /**
     * Upload employee data with a CSV or , separated text file.
     * @param file MultipartFile .csv, .txt [id,name,salary,joinDate,role,projectCompletionPercentage
     * @return ArrayList of Employee objects
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ArrayList<Employee> uploadEmployeeData(@RequestParam("file") MultipartFile file) {
        return csvProcessor.loadEmployees(file);
    }
}
