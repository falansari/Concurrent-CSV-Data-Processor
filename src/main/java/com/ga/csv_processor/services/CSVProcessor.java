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
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
     * Supports pooled multithreading through the use of Runnable workers.
     * @return boolean true if successfully saved file data/employees.csv
     */
    public boolean downloadEmployeesWithRaise() {
        lock.writeLock().lock();

        try {
            int total = this.employees.size();
            AtomicInteger index = new AtomicInteger(0); // For the loop
            ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>(); // For data rows to add them sequentially while supporting multithreading

            ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()); // Thread pool

            Runnable worker = () -> { // Process data rows into a queue
                for (int i = index.getAndIncrement(); i < total; i = index.getAndIncrement()) { // Write each employee as a data row
                    Employee employee = this.employees.get(i);
                    employee.setSalary(calculateSalaryWithRaise(employee));

                    String row = formatDataRow(employee);

                    queue.add(row);
                }
            };

            // Launch workers
            int workers = Runtime.getRuntime().availableProcessors();
            for (int i = 0; i < workers; i++) {
                executorService.submit(worker);
            }

            executorService.shutdown();
            boolean isTerminated = executorService.awaitTermination(60, TimeUnit.SECONDS);

            if (isTerminated) { // Write only after all workers done adding rows to queue
                return writeDataRowsToFile(queue);
            }

            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e.getMessage());
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

    /**
     * Write data rows to data/employees.csv file.
     * @param queue ConcurrentLinkedQueue of String the data rows' queue.
     * @return boolean true if successfully written.
     */
    private boolean writeDataRowsToFile(ConcurrentLinkedQueue<String> queue) {
        try { // Write data rows into csv file
            List<String> rows = sortDataQueue(queue);
            BufferedWriter writer = new BufferedWriter(new FileWriter("data/employees.csv"));

            for (int i = 0; i < rows.size(); i++) {
                writer.write(rows.get(i));

                if (i != rows.size() - 1) { // Avoid adding new line after last row
                    writer.newLine();
                }
            }

            writer.close();

            return true;
        } catch (IOException e) {
            throw new RuntimeException("File write error: " + e.getMessage());
        }
    }

    /**
     * Sort data queue by employee id.
     * @param queue ConcurrentLinkedQueue of String
     * @return List of String sorted list.
     */
    private List<String> sortDataQueue(ConcurrentLinkedQueue<String> queue) {
        List<String> list = new ArrayList<>();
        while (!queue.isEmpty()) {
            list.add(queue.poll());
        }

        list.sort(Comparator.comparingInt(row -> Integer.parseInt(row.split(",")[0])));

        return list;
    }

    /**
     * Take Employee object and return it as a string data row for CSV saving.
     * @param employee Employee
     * @return String .csv formatted.
     */
    private String formatDataRow(Employee employee) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return employee.getId() + "," +
                employee.getName() + "," +
                employee.getSalary() + "," +
                employee.getJoinDate().format(formatter) + "," +
                employee.getRole() + "," +
                employee.getProjectCompletionPercentage();
    }
}