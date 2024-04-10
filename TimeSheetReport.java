// You are free to use these classes, but don't
// add your own imports.
import java.util.Arrays;
import java.io.IOException;
import static java.lang.System.out;
import static java.lang.System.err;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class TimeSheetReport {

  // These global variables can be used in your code.
  // Feel free to add your own.
  public static final int BORDER_LENGTH = 70;
  public static final String THIN_BORDER = "-".repeat(BORDER_LENGTH);
  public static final String THICK_BORDER = "=".repeat(BORDER_LENGTH);
  public static final String PIPE = "|";
  public static final String DELIMITER = " " + PIPE + " ";
  public static final int MINUTES_PER_HOUR = 60;
  public static final String TIME_SHEET_EXTENSION = ".txt";
  public static final String NOT_AVAILABLE = "n/a";
  public static final String CLARIFICATION_NEEDED = "clarification needed";
  public static final String PRESENT = "fully present";
  public static final String ABSENT = "absent";
  public static final int TIME_FIELD_WIDTH = 5;
  public static final int DURATION_FIELD_WIDTH = 8;
  public static final int DATE_FIELD_WIDTH = 10;

  // This scanner is used to open and close a time sheet.
  // It's also used to read the next record in the file.
  public static Scanner fileScanner;

  // The program's execution starts here.
  // The name of the time sheet file is provided as
  // a command line argument. You must append the
  // file extension txt and locate the file in the
  // sub directory time-sheets.
  // If no argument is provided by the user, a help
  // message should be printed.
  public static void main(String[] args) {
    // 1. Print a help message, if no arguments are provided.
    //    and exit.
    if (args.length == 0) {
      out.println("Input file name is missing.");
      return;
    }

    // 2. Extract year, month and employee from the filename.
    String filename = args[0];
    int year, month;
    String employeeId;
    try {
      String[] parts = filename.split("-");
      year = Integer.parseInt(parts[0]);
      month = Integer.parseInt(parts[1]);
      employeeId = parts[2];
    } catch (Exception e) {
      out.println("Invalid file name format.");
      return;
    }
    // 3. Build the path to the time sheet file and open it.
    // 4. Print the full report.
    try {
      Path path = Path.of("time-sheets", filename + TIME_SHEET_EXTENSION);
      out.println("Path: " + path);
      openTimeSheet(path);
      printReport(year, month, employeeId);
      closeTimeSheet();
    } catch (IOException e) {
      out.println("Error opening time sheet file.");
      closeTimeSheet();
    }
    // If an IOException occurs, inform the user with a message,
    // close the time sheet and exit gracefully.
  }

  // Return the number of days in a month.
  // Leap years are taken into account.
  public static int daysOfMonth(int year, int month) {
    switch (month) {
      case 2:
        return (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) ? 29 : 28;
      case 4: case 6: case 9: case 11:
        return 30;
      default:
        return (month < 1 || month > 12) ? 0 : 31;
    }
  }

  // Return a string in the format hh:mm.
  public static String formatTime(int hours, int minutes) {
    return (hours < 0 || minutes < 0) ? NOT_AVAILABLE : String.format("%02d:%02d", hours, minutes);
  }

  // Returns a formatted date of the form: dd.mm.yyyy.
  public static String formatDate(int year, int month, int day) {
    return String.format("%02d.%02d.%d", day, month, year);
  }

  // Format given minutes in hours and seconds
  // and return these values as a string in the format
  // xxh yym. Example: 02h 15m
  public static String formatDuration(int totalMinutes) {
    int hours = totalMinutes / MINUTES_PER_HOUR;
    int minutes = totalMinutes % MINUTES_PER_HOUR;
    return String.format("%02dh %02dm", hours, minutes);
  }

  // Add spaces to string until it contains
  // at least minLength characters. If prepend is true,
  // spaces are added to the start of the string, otherwise they
  // are appended to the end.
  public static String pad(String s, int minLength, boolean prepend) {
    if (s.length() >= minLength) return s;
    StringBuilder padded = new StringBuilder(s);
    while (padded.length() < minLength) {
      if (prepend) {
        padded.insert(0, ' ');
      } else {
        padded.append(' ');
      }
    }
    return padded.toString();
  }

  // Add spaces to the start of the string until it has
  // at least minLength characters. Avoid code redundancy
  // by using pad method.
  public static String padStart(String s, int minLength) {
    return pad(s, minLength, true);
  }

  // Add spaces to the end of the string until it has
  // at least minLength characters. Avoid code redundancy
  // by using pad method.
  public static String padEnd(String s, int minLength) {
    return pad(s, minLength, false);
  }

  // Calculate the duration between the two given times, in minutes.
  // If the second time is before the first time, assume that the second time
  // refers to the next day.
  public static int durationInMinutes(int startHours, int startMinutes, int endHours, int endMinutes) {
    int start = startHours * MINUTES_PER_HOUR + startMinutes;
    int end = endHours * MINUTES_PER_HOUR + endMinutes;
    if (end < start) {
      end += 24 * MINUTES_PER_HOUR;
    }
    return end - start;
  }

  public static void formatLine(String date, String startTime, String endTime, String duration, String remarks) {
    out.println(
            padEnd(date, DATE_FIELD_WIDTH) + DELIMITER +
                    padEnd(startTime, TIME_FIELD_WIDTH) + DELIMITER +
                    padEnd(endTime, TIME_FIELD_WIDTH) + DELIMITER +
                    padEnd(duration, DURATION_FIELD_WIDTH) + DELIMITER +
                    padEnd(remarks, (BORDER_LENGTH - 2 * TIME_FIELD_WIDTH - DATE_FIELD_WIDTH - DURATION_FIELD_WIDTH))
    );
  }

  // Print a line for the given date.
  // A line has the format: <date> <start-time> <end-time> <duration> <remarks>.
  // Date has the format: dd.mm.yyyy.
  // Time and duration have the format: hh:mm.
  // Missing values are printed as: n/a.
  // If startHours or startMinutes is negative, the start time is considered missing.
  // If endHours or endMinutes is negative, the end time is considered missing.
  // If start and end time are missing, remarks is "absent".
  // If both start and end time are available, remarks is "fully present".
  // If exactly one time value is missing, remarks is "clarification needed".
  public static void printLine(String date, int startHours, int startMinutes, int endHours, int endMinutes) {
    String startTime = formatTime(startHours, startMinutes);
    String endTime = formatTime(endHours, endMinutes);
    String duration, remarks;
    if (startHours < 0 || startMinutes < 0 || endHours < 0 || endMinutes < 0) {
      remarks = ABSENT;
      duration = NOT_AVAILABLE;
    } else if (startTime.equals(NOT_AVAILABLE) || endTime.equals(NOT_AVAILABLE)) {
      remarks = CLARIFICATION_NEEDED;
      duration = NOT_AVAILABLE;
    } else {
      remarks = PRESENT;
      duration = formatDuration(durationInMinutes(startHours, startMinutes, endHours, endMinutes));
    }
    formatLine(date, startTime, endTime, duration, remarks);
  }

  // Prints the report's header.
  // The header contains borders, year, month and employee id.
  // It also prints the column names for the day entries.
  public static void printHeader(int year, int month, String employeeId) {
    out.println(THICK_BORDER);
    out.printf("YEAR: %d / MONTH: %02d / ID: %s\n", year, month, employeeId);
    out.println(THICK_BORDER);
    formatLine("DATE", "START", "END", "DURATION", "REMARKS");
    out.println(THIN_BORDER);
  }

  // Prints the report's footer.
  // totalRecords   - number of records read from time sheet file
  // incompleteDays - number of days where either start or end time is missing
  // absentDays     - number of days with no records at all
  // presentDays    - number of days with start and end time
  // workingTime    - number of total minutes worked in this month
  public static void printFooter(int totalRecords, int incompleteDays, int absentDays, int presentDays, int workingTime) {
    throw new RuntimeException("Not implemented yet");
  }

  // Opens the time sheet file using the fileScanner.
  // Assume that the file is encoded in UTF8.
  // If the time sheet is already open, do nothing.
  // An IOException must be handled in main.
  public static void openTimeSheet(Path path) throws IOException {
    if (fileScanner == null) {
      fileScanner = new Scanner(path, StandardCharsets.UTF_8);
    }
  }

  // Closes the time sheet by closing the fileScanner.
  // If the time sheet isn't open, do nothing.
  public static void closeTimeSheet() {
    if (fileScanner != null) {
      fileScanner.close();
      fileScanner = null;
    }
  }

  // Reads the next line from the time sheet and converts
  // its values to integers (day, hours, minutes).
  // If there is no more line, null is returned.
  // Blank lines must be skipped automatically.
  public static int[] nextRecord() {
    throw new RuntimeException("Not implemented yet");
  }

  // Prints the full report: header, day entries, footer.
  // This method makes use of nextRecord and calculates the
  // statistics which are then printed in the footer.
  public static void printReport(int year, int month, String employeeId) {
    printHeader(year, month, employeeId);
    int days = daysOfMonth(year, month);
    for (int i = 1; i <= days; i++) {
      printLine(formatDate(year, month, i), 18, 0, 10, 0);
    }
  }

}
