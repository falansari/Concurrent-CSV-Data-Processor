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
    /**
     * 8 thread fixed thread pool for asynchronous service execution.
     */
    private final ExecutorService executorService = Executors.newFixedThreadPool(8);

    /**
     * Upload employee data with a CSV or , separated text file.
     * @param file MultipartFile .csv, .txt [id,name,salary,joinDate,role,projectCompletionPercentage
     * @return ArrayList of Employee objects
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CompletableFuture<ArrayList<Employee>> uploadEmployeeData(@RequestParam("file") MultipartFile file) {
        return CompletableFuture.supplyAsync(() -> csvProcessor.loadEmployees(file), executorService);
    }

    /**
     * Get list of employees. Supports asynchronous multithreading.
     * @return ArrayList of Employee objects
     */
    @GetMapping("/employees")
    public CompletableFuture<ArrayList<Employee>> getEmployees() {
        return CompletableFuture.supplyAsync(() -> csvProcessor.getEmployees(), executorService);
    }

    /**
     * Calculate and return employee's new salary after raise. Supports asynchronous multithreading.
     * @param employee Employee object
     * @return double Raised salary.
     */
    @PostMapping("/raise")
    public CompletableFuture<Double> calculateSalaryWithRaise(@RequestBody Employee employee) {
        return CompletableFuture.supplyAsync(() -> csvProcessor.calculateSalaryWithRaise(employee), executorService);
    }

    /**
     * Download employee.csv file with new salaries after raise. Takes original before-raise file as data input.
     * @return boolean True if successfully downloaded.
     */
    @GetMapping("/download")
    public CompletableFuture<Boolean> downloadEmployeesFileWithRaise() {
        return CompletableFuture.supplyAsync(() -> csvProcessor.downloadEmployeesWithRaise(), executorService);
    }
}
