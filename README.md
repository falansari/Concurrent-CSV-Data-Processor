# Concurrent CSV Data Processor
General Assembly unit 3 concurrency mini project.  A CSV data processor for processing an employees data sheet to give raises to whoever deserves it.

## 📌 Synopsis
The CSV Processor is a Spring Boot application that manages employee data stored in CSV files. 
It supports uploading employee records, calculating salary raises based on service years, role, 
and project completion percentage, and downloading updated employee data with new salaries. 
The system leverages **multithreading** and **concurrency controls** (`ReadWriteLock`, `AtomicInteger`, `ConcurrentLinkedQueue`) 
to ensure efficient and thread-safe operations.

---

## 🚀 API Endpoints

| Method | Endpoint               | Input Type            | Parameters / Body                                                                     | Returns               | Description                                                                 |
|--------|------------------------|-----------------------|---------------------------------------------------------------------------------------|-----------------------|-----------------------------------------------------------------------------|
| POST   | `/processor/upload`    | `multipart/form-data` | `file`: CSV or text file (`id,name,salary,joinDate,role,projectCompletionPercentage`) | `ArrayList<Employee>` | Uploads employee data from a CSV file into memory.                          |
| GET    | `/processor/employees` | None                  | None                                                                                  | `ArrayList<Employee>` | Retrieves the current list of employees in memory.                          |
| POST   | `/processor/raise`     | `application/json`    | `Employee` object                                                                     | `double`              | Calculates and returns the new salary for an employee after applying raise. |
| GET    | `/processor/download`  | None                  | None                                                                                  | `boolean`             | Generates `data/employees.csv` with updated salaries after raise.           |

---

## 📂 Class & Method Documentation

### `CSVProcessorController`
Handles REST API endpoints and delegates logic to the `CSVProcessor` service.
- `uploadEmployeeData(MultipartFile file)` → Uploads CSV file asynchronously.
- `getEmployees()` → Retrieves employees asynchronously.
- `calculateSalaryWithRaise(Employee employee)` → Calculates raise asynchronously.
- `downloadEmployeesFileWithRaise()` → Generates updated CSV asynchronously.

### `CSVProcessor`
Core service class for CSV operations.

- **Fields:**
    - `ArrayList<Employee> employees` → In-memory employee list.
    - `ReadWriteLock lock` → Ensures thread-safe access.

- **Methods:**
    - `loadEmployees(MultipartFile employeeFile)`  
      Loads employees from CSV into memory. Write-locked to prevent concurrent modification.
    - `calculateSalaryWithRaise(Employee employee)`  
      Applies raise formula:
        - 2% per service year
        - Role-based raise (Director 5%, Manager 2%, Employee 1%)
        - Project completion < 60% → no raise
        - Project completion > 80% → 1.5× role raise
    - `downloadEmployeesWithRaise()`  
      Multithreaded salary update and CSV generation. Uses `AtomicInteger` for index distribution and `ConcurrentLinkedQueue` for safe row collection.
    - `getEmployees()`  
      Returns current employee list (read-locked).
    - `writeDataRowsToFile(ConcurrentLinkedQueue<String> queue)`  
      Writes sorted rows to `data/employees.csv`.
    - `sortDataQueue(ConcurrentLinkedQueue<String> queue)`  
      Sorts rows by employee ID before writing.
    - `formatDataRow(Employee employee)`  
      Converts an `Employee` object into a CSV-formatted string.

---

## 📊 Project Report

### Implementation Details
- **Concurrency:**
    - `ReadWriteLock` ensures safe concurrent access to employee list.
    - `AtomicInteger` distributes work across threads.
    - `ConcurrentLinkedQueue` collects rows safely before writing.
- **Multithreading:**
    - Thread pool sized to available CPU cores.
    - Workers process employees in parallel, calculating raises and preparing rows.
    - Main thread writes rows sequentially after sorting by ID.
- **File I/O:**
    - Uses `BufferedWriter` for efficient writing.
    - Ensures clean CSV formatting with proper newlines.
- **Spring Boot Integration:**
    - REST endpoints exposed via `CSVProcessorController`.
    - Asynchronous execution with `CompletableFuture` and `ExecutorService`.

### Outcomes
- **Performance:** Multithreaded processing significantly reduces time for large CSV files.
- **Reliability:** Concurrency controls prevent race conditions and ensure data consistency.
- **Scalability:** Design supports large datasets and can be extended with additional endpoints.
- **Maintainability:** Clear separation of concerns between controller and service classes.

### Technologies Used
- [JAVA 17 Spring Boot v4.0.2](https://start.spring.io/) application with dependencies:
    - Starter WebMVC
    - DevTools
- [Lombok](https://projectlombok.org/) for boilerplate reduction.
- [Maven builder](https://maven.apache.org/).