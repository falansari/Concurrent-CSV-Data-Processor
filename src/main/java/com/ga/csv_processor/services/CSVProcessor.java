package com.ga.csv_processor.services;

import com.ga.csv_processor.enums.ROLES;
import com.ga.csv_processor.models.Employee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;

import static com.ga.csv_processor.enums.ROLES.*;

@Service
public class CSVProcessor {
    ArrayList<Employee> employees;

    @Autowired
    public CSVProcessor(ArrayList<Employee> employees) {
        this.employees = employees;
    }

    /**
     * Load employee data from CSV file.
     * @param employeeFile MultipartFile CSV or , separated text file.
     * @return ArrayList of Employee.
     */
    public ArrayList<Employee> loadEmployees(MultipartFile employeeFile) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(employeeFile.getInputStream()));
            String line;
            ArrayList<Employee> employees = new ArrayList<>();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

            while ((line = bufferedReader.readLine()) != null) { // Load up data rows
                String[] employeeData = line.split(",");
                int id = Integer.parseInt(employeeData[0]);
                String name = employeeData[1];
                double salary = Double.parseDouble(employeeData[2]);
                LocalDate joinDate = LocalDate.parse(employeeData[3]);
                ROLES role = ROLES.valueOf(employeeData[4].toUpperCase());
                double projectCompletionPercentage = Double.parseDouble(employeeData[5]);

                // Create employee object
                employees.add(new Employee(id, name, salary, joinDate, role, projectCompletionPercentage));
            }

            this.employees = employees;

            return this.employees;
        } catch (IOException e) {
            throw new RuntimeException("File upload error: " + e.getMessage());
        }
    }
}

/*
    - use thread pool

    - Locks: The java.util.concurrent.locks.Lock interface provides a more flexible synchronization mechanism than intrinsic locks.
    It allows for more advanced locking strategies such as reentrant locks, fair locks, and condition variables.

    - Atomic operations: Java provides atomic classes such as AtomicInteger, AtomicLong, and AtomicReference in the java.util.concurrent.atomic package.
    These classes provide atomic operations that are guaranteed to be executed without interruption,
    making them useful for implementing thread-safe operations on primitive types and object references.
    */