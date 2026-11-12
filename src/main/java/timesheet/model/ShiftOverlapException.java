package timesheet.model;


public class ShiftOverlapException extends TimesheetException {

    private final Shift newShift;
    private final Shift existingShift;


    public ShiftOverlapException(Shift newShift, Shift existingShift) {
        super("Смена " + newShift.start().toLocalDateTime() + " - " + newShift.end().toLocalDateTime()
                + " пересекается со сменой " + existingShift.start().toLocalDateTime()
                + " - " + existingShift.end().toLocalDateTime());
        this.newShift = newShift;
        this.existingShift = existingShift;
    }


    public Shift getNewShift() {
        return newShift;
    }


    public Shift getExistingShift() {
        return existingShift;
    }
}
