package timesheet.model;

import java.util.Objects;


public class Employee {

    private final String personnelNumber;
    private final String fullName;
    private final long hourlyRate;
    private final WorkNorm norm;


    public Employee(String personnelNumber, String fullName, long hourlyRate, WorkNorm norm) {
        if (personnelNumber == null || personnelNumber.isBlank()) {
            throw new IllegalArgumentException("Табельный номер не может быть пустым");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("ФИО не может быть пустым");
        }
        if (hourlyRate <= 0) {
            throw new IllegalArgumentException("Ставка должна быть больше нуля");
        }
        this.personnelNumber = personnelNumber;
        this.fullName = fullName;
        this.hourlyRate = hourlyRate;
        this.norm = Objects.requireNonNull(norm);
    }


    public String getPersonnelNumber() {
        return personnelNumber;
    }


    public String getFullName() {
        return fullName;
    }


    public long getHourlyRate() {
        return hourlyRate;
    }


    public WorkNorm getNorm() {
        return norm;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Employee other)) {
            return false;
        }
        return personnelNumber.equals(other.personnelNumber);
    }


    @Override
    public int hashCode() {
        return personnelNumber.hashCode();
    }

    @Override
    public String toString() {
        return personnelNumber + " " + fullName;
    }
}
