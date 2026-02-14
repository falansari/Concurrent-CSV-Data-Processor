package com.ga.csv_processor.models;

import com.ga.csv_processor.enums.ROLES;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Employee object model class
 * Fields:
 * id int
 * name String
 * salary double
 * joinDate Date
 * role Enum ROLES [Director, Manager, Employee]
 * projectCompletionPercentage double
 */
@Getter @Setter
public class Employee {
    private int id;
    private String name;
    private double salary;
    private LocalDate joinDate;
    /**
     * Director, Manager, or Employee
     */
    private Enum<ROLES> role;
    /**
     * Value range from 0.0 (0%) to 1.0 (100%)
     */
    private double projectCompletionPercentage;

    public Employee(int id, String name, double salary, LocalDate joinDate, Enum<ROLES> role, double projectCompletionPercentage) {
        this.id = id;
        this.name = name;
        this.salary = salary;
        this.joinDate = joinDate;
        this.role = role;
        this.projectCompletionPercentage = projectCompletionPercentage;
    }
}
