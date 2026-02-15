package com.ga.csv_processor.services;

import com.ga.csv_processor.enums.ROLES;
import com.ga.csv_processor.models.Employee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.ga.csv_processor.enums.ROLES.*;

@Service
public class CSVProcessor {
    ArrayList<Employee> employees;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

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
        lock.writeLock().lock();

        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(employeeFile.getInputStream()));
            String line;
            ArrayList<Employee> employees = new ArrayList<>();

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
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Calculate and return employee's raise amount based on formula.
     * - Each completed service year is 2%
     * - % based on job role: Director 5%, Manager 2%, Employee 1%
     * - Project completion % below 60% get no raise at all
     * - Project completion % above 80% get 1.5x job role raise
     * FORMULA: (years worked * 2%) + role%
     * @param employee Employee object
     * @return double New increased salary
     */
    public double calculateSalaryWithRaise(Employee employee) {
        double currentSalary = employee.getSalary();
        LocalDate joinDate = employee.getJoinDate();
        ROLES role = employee.getRole();
        double projectCompletionPercentage = employee.getProjectCompletionPercentage();

        if (projectCompletionPercentage < 0.6) return currentSalary; // Weed out low-achievers

        double rolePercentage = 0.0;

        if (role.equals(DIRECTOR)) { // Determine role-based raise %
            rolePercentage = 5.0;
        } else if (role.equals(MANAGER)) {
            rolePercentage = 2.0;
        } else if (role.equals(EMPLOYEE)) {
            rolePercentage = 1.0;
        }

        if (projectCompletionPercentage > 0.8) rolePercentage = rolePercentage * 1.5; // Reward high-achievers

        // Determine number of years worked
        LocalDate today = LocalDate.now();
        Period period = Period.between(joinDate, today);
        int yearsWorked = period.getYears();

        double serviceReward = yearsWorked * 2.0;

        return currentSalary + (currentSalary * (serviceReward / 100)) + (currentSalary * (rolePercentage / 100));
    }

    /**
     * Returns an employees.csv file that contains the employees with their new raised salaries. Write locked.
     * @return boolean true if successfully saved file.
     */
    public boolean downloadEmployeesWithRaise() {
        lock.writeLock().lock();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("data/employees.csv"));

            int total = this.employees.size();
            for (int i = 0; i < total; i++) { // Write each employee as a data row
                Employee employee = this.employees.get(i);
                employee.setSalary(calculateSalaryWithRaise(employee));

                String row = employee.getId() + "," +
                        employee.getName() + "," +
                        employee.getSalary() + "," +
                        employee.getJoinDate().format(formatter) + "," +
                        employee.getRole() + "," +
                        employee.getProjectCompletionPercentage();

                writer.write(row);
                if (i != (total - 1)) writer.newLine(); // avoid adding a new empty line at last row
            }

            writer.close();

            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get list of employees data objects.
     * @return ArrayList of Employee
     */
    public ArrayList<Employee> getEmployees() {
        lock.readLock().lock();
        try {
            return employees;
        } finally {
            lock.readLock().unlock();
        }
    }
}

/*
    - Atomic operations: Java provides atomic classes such as AtomicInteger, AtomicLong, and AtomicReference in the java.util.concurrent.atomic package.
    These classes provide atomic operations that are guaranteed to be executed without interruption,
    making them useful for implementing thread-safe operations on primitive types and object references.
    */