package com.ga.csv_processor.controllers;

import com.ga.csv_processor.models.Employee;
import com.ga.csv_processor.services.CSVProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.concurrent.*;

@RestController
@RequestMapping("processor")
public class CSVProcessorController {
    @Autowired
    private CSVProcessor csvProcessor;
    private final ExecutorService executorService = Executors.newFixedThreadPool(8);

    /**
     * Upload employee data with a CSV or , separated text file.
     * @param file MultipartFile .csv, .txt [id,name,salary,joinDate,role,projectCompletionPercentage
     * @return ArrayList of Employee objects
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ArrayList<Employee> uploadEmployeeData(@RequestParam("file") MultipartFile file) throws ExecutionException, InterruptedException {
        Future<ArrayList<Employee>> future = executorService.submit(() -> csvProcessor.loadEmployees(file));
        return future.get();
    }
    }

    /**
     * Calculate and return employee's new salary after raise.
     * @param employee Employee
     * @return double Raised salary.
     */
    @PostMapping("/raise")
    public double calculateSalaryWithRaise(@RequestBody Employee employee) {
        return csvProcessor.calculateSalaryWithRaise(employee);
    }

    /**
     * Download employee.csv file with new salaries after raise. Takes original before-raise file as data input.
     * @param file MultipartFile .csv, or , separated .txt
     * @return boolean True if successfully downloaded.
     */
    @PostMapping(value = "/download", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public boolean downloadEmployeeFileWithRaise(@RequestParam("file") MultipartFile file) {
        return csvProcessor.downloadEmployeesWithRaiseFile(file);
    }
}
