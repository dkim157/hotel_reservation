import java.sql.ResultSet;
import java.sql.Statement;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InnReservations {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);

        int option = -1;
        while (option != 7) {
            System.out.println("""
                    Select an option [1-7]:
                    Room Rates [1]
                    Reservations [2]
                    Reservation Change [3]
                    Cancellation [4]
                    Detailed Reservation Info [5]
                    Revenue [6]
                    Quit [7]""");
            option = in.nextInt();

            try {
                InnReservations ir = new InnReservations();
                switch (option) {
                    case 1:
                        ir.getRoomRate();
                        break; // FR1 GOES HERE
                    case 2:
                        ir.reservations();
                        break; // FR2 GOES HERE
                    case 3:
                        ir.changeReservation();
                        break; // FR3 GOES HERE
                    case 4:
                        ir.cancellation();
                        break; // FR4 GOES HERE
                    case 5:
                        ir.detailedReservationInfo();
                        break; // FR5 GOES HERE
                    case 6:
                        ir.getRevenue();
                    case 7:
                        break;
                }
            } catch (SQLException e) {
                System.err.println("SQLException: " + e.getMessage());
            } catch (Exception e2) {
                System.err.println("Exception: " + e2.getMessage());
            }
            System.out
                    .print("--------------------------------------------------------------------------------------\n");
        }
    }

    // update reservation (FR3)
    private void changeReservation() throws SQLException {
        Scanner in = new Scanner(System.in);
        String resCode = getExistingResCode(in);
        System.out.println("\nEnter new value or 'no change'");
        String fname = newFName(in);
        String lname = newLName(in);
        String startDate = newStartDate(in);
        String endDate = newEndDate(in);
        String numKids = newNumKids(in);
        String numAdults = newNumAdults(in);

        updateReservation(resCode, fname, lname, startDate, endDate, numKids, numAdults);
    }

    private void updateReservation(String resCode, String fname, String lname, String startDate, String endDate,
            String numKids, String numAdults) throws SQLException {
        try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                System.getenv("HP_JDBC_USER"),
                System.getenv("HP_JDBC_PW"))) {
            if (newDatesConflict(resCode, startDate, endDate)) {
                System.out.println("\nNew dates conflict with existing reservations");
                return;
            }
            String sql = getUpdateSql(resCode, fname, lname, startDate, endDate, numKids, numAdults);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                System.out.println("\nSuccessfully updated");
            }
        }
    }

    private boolean newDatesConflict(String resCode, String startDate, String endDate) throws SQLException {
        try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                System.getenv("HP_JDBC_USER"),
                System.getenv("HP_JDBC_PW"))) {
            String sql = conflictCheckSql(resCode, startDate, endDate);
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                return rs.first();
            }
        }
    }

    private String conflictCheckSql(String resCode, String startDate, String endDate) {
        return String.format("""
                with rc as (
                    select Room
                    from lab7_reservations r
                    where CODE='%s'
                ), table_without_existing_res as (
                    select *
                    from lab7_reservations r
                    where CODE!='%s'
                )
                select *
                from (select * from table_without_existing_res) r
                where Room=(select Room from rc) and ('%s' < r.Checkout and '%s' > r.CheckIn);""",
                resCode, resCode, startDate, endDate);
    }

    private String getUpdateSql(String resCode, String fname, String lname, String startDate, String endDate,
            String numKids, String numAdults) {
        if (fname.equals("no change")) {
            fname = "";
        } else {
            fname = String.format(", FirstName='%s'", fname);
        }
        if (lname.equals("no change")) {
            lname = "";
        } else {
            lname = String.format(", LastName='%s'", lname);
        }
        if (startDate.equals("no change")) {
            startDate = "";
        } else {
            startDate = String.format(", CheckIn='%s'", startDate);
        }
        if (endDate.equals("no change")) {
            endDate = "";
        } else {
            endDate = String.format(", Checkout='%s'", endDate);
        }
        if (numKids.equals("no change")) {
            numKids = "";
        } else {
            numKids = String.format(", Kids=%s", numKids);
        }
        if (numAdults.equals("no change")) {
            numAdults = "";
        } else {
            numAdults = String.format(", Adults=%s", numAdults);
        }

        return String.format("""
                update lab7_reservations
                set CODE=%s %s %s %s %s %s %s
                where CODE=%s;""", resCode, fname, lname, startDate, endDate, numAdults, numKids, resCode);
    }

    private String newNumAdults(Scanner in) {
        while (true) {
            System.out.print("Enter new number of adults: ");
            String str = in.nextLine();
            if (str.matches("[0-9]+") || str.equals("no change")) {
                return str;
            } else {
                System.out.println("Use digits only");
            }
        }
    }

    private String newNumKids(Scanner in) {
        while (true) {
            System.out.print("Enter new number of kids: ");
            String str = in.nextLine();
            if (str.matches("[0-9]+") || str.equals("no change")) {
                return str;
            } else {
                System.out.println("Use digits only");
            }
        }
    }

    private String newEndDate(Scanner in) {
        Pattern p = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
        while (true) {
            System.out.print("Enter new end date: ");
            String date = in.nextLine();
            if ((p.matcher(date).matches() && validDate(date)) || date.equals("no change")) {
                return date;
            } else {
                System.out.println("Invalid date format");
            }
        }
    }

    private String newStartDate(Scanner in) {
        Pattern p = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
        while (true) {
            System.out.print("Enter new start date: ");
            String date = in.nextLine();
            if ((p.matcher(date).matches() && validDate(date)) || date.equals("no change")) {
                return date;
            } else {
                System.out.println("Invalid date format");
            }
        }
    }

    private String newLName(Scanner in) {
        while (true) {
            System.out.print("Enter new last name: ");
            String str = in.nextLine();
            if (str.matches("[a-zA-z]+") || str.equals("no change")) {
                return str;
            } else {
                System.out.println("Use alphabet characters only");
            }
        }
    }

    private String newFName(Scanner in) {
        while (true) {
            System.out.print("Enter new first name: ");
            String str = in.nextLine();
            if (str.matches("[a-zA-z]+") || str.equals("no change")) {
                return str;
            } else {
                System.out.println("Use alphabet characters only");
            }
        }
    }

    private String getExistingResCode(Scanner in) throws SQLException {
        ArrayList<String> codes = queryRow("select CODE from lab7_reservations;", "CODE");
        while (true) {
            System.out.print("Enter reservation code: ");
            String userCode = in.nextLine();
            if (codes.contains(userCode)) {
                return userCode;
            } else {
                System.out.println("Invalid reservation code");
            }
        }
    }

    // detailed reservation info (FR5)
    private void detailedReservationInfo() throws SQLException {
        System.out.println("\nEnter search information (blank for any)");
        Scanner in = new Scanner(System.in);
        String fname = getFName(in);
        String lname = getLName(in);
        String startDate = getStartDate(in);
        String endDate = getEndDate(in);
        String rCode = getRCode(in);
        String resCode = getResCode(in);

        searchReservations(fname, lname, startDate, endDate, rCode, resCode);
    }

    private void searchReservations(String fname, String lname, String startDate, String endDate, String rCode,
            String resCode) throws SQLException {
        // Establish connection to RDBMS
        try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                System.getenv("HP_JDBC_USER"),
                System.getenv("HP_JDBC_PW"))) {
            // Construct SQL statement
            String sql = getReservationSearchString(fname, lname, startDate, endDate, rCode, resCode);
            // Send SQL statement to DBMS
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                displayReservations(rs);
            }
        }
    }

    private void displayReservations(ResultSet rs) throws SQLException {
        System.out.println();
        while (rs.next()) {
            String fname = rs.getString("FirstName");
            String lname = rs.getString("LastName");
            String startDate = rs.getString("CheckIn");
            String endDate = rs.getString("Checkout");
            String rCode = rs.getString("Room");
            String resCode = rs.getString("CODE");
            String rName = rs.getString("RoomName");
            System.out.format("%s %s %s %s %s %s %s\n", resCode, rCode, startDate, endDate, fname, lname, rName);
        }
    }

    private String getReservationSearchString(String fname, String lname, String startDate, String endDate,
            String rCode, String resCode) {
        // set first name
        if (fname.contains("%")) {
            fname = String.format("and FirstName like '%s'", fname);
        } else if (fname.length() > 0) {
            fname = String.format("and FirstName='%s'", fname);
        }
        // set last name
        if (lname.contains("%")) {
            lname = String.format("and LastName like '%s'", lname);
        } else if (lname.length() > 0) {
            lname = String.format("and LastName='%s'", lname);
        }
        // set start date
        if (startDate.length() > 0) {
            startDate = String.format("and CheckIn='%s'", startDate);
        }
        // set end date
        if (endDate.length() > 0) {
            endDate = String.format("and Checkout='%s'", endDate);
        }
        // set room code
        if (rCode.contains("%")) {
            rCode = String.format("and Room like '%s'", rCode);
        } else if (rCode.length() > 0) {
            rCode = String.format("and Room='%s'", rCode);
        }
        // set reservation code
        if (resCode.contains("%")) {
            resCode = String.format("and CODE like '%s'", resCode);
        } else if (resCode.length() > 0) {
            resCode = String.format("and CODE='%s'", resCode);
        }

        return String.format("""
                select lab7_reservations.*, RoomName
                from lab7_reservations\s
                    join lab7_rooms on Room=RoomCode
                where CODE>1 %s %s %s %s %s %s
                ;""", fname, lname, startDate, endDate, rCode, resCode);
    }

    private String getResCode(Scanner in) {
        System.out.print("Enter reservation code: ");
        String str = in.nextLine();
        if (str.matches("[0-9%]*")) {
            return str;
        } else {
            return "";
        }
    }

    private String getRCode(Scanner in) {
        System.out.print("Enter room code: ");
        String str = in.nextLine();
        if (str.matches("[a-zA-Z%]*")) {
            return str;
        } else {
            return "";
        }
    }

    boolean validDate(String s) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);
        return sdf.parse(s, new ParsePosition(0)) != null;
    }

    private String getEndDate(Scanner in) {
        Pattern p = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
        System.out.print("Enter end date: ");
        String date = in.nextLine();
        if (p.matcher(date).matches() && validDate(date)) {
            return date;
        } else {
            return "";
        }
    }

    private String getStartDate(Scanner in) {
        Pattern p = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
        System.out.print("Enter start date: ");
        String date = in.nextLine();
        if (p.matcher(date).matches() && validDate(date)) {
            return date;
        } else {
            return "";
        }
    }

    private String getLName(Scanner in) {
        System.out.print("Enter last name: ");
        String str = in.nextLine();
        if (str.matches("[a-zA-z%]*")) {
            return str;
        } else {
            return "";
        }
    }

    private String getFName(Scanner in) {
        System.out.print("Enter first name: ");
        String str = in.nextLine();
        if (str.matches("[a-zA-z%]*")) {
            return str;
        } else {
            return "";
        }
    }

    // cancellations (FR4)
    private void cancellation() throws SQLException {
        Scanner in = new Scanner(System.in);
        System.out.print("Enter reservation code: ");
        int code = in.nextInt();
        try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                System.getenv("HP_JDBC_USER"),
                System.getenv("HP_JDBC_PW"))) {
            // Construct SQL statement
            String sql = String.format("select * from lab7_reservations where CODE=%d", code);
            // Send SQL statement to DBMS
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                if (!rs.next()) {
                    System.out.println("Enter valid reservation number");
                } else {
                    int result = printConfirmation(rs);
                    if (result == 1) {
                        cancel(rs);
                        System.out.println("cancellation successful");
                    } else {
                        System.out.println("no cancellation");
                    }
                }
            }
        }
    }

    private void cancel(ResultSet rs) throws SQLException {
        try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                System.getenv("HP_JDBC_USER"),
                System.getenv("HP_JDBC_PW"))) {
            // Step 2: Construct SQL statement
            String sql = String.format("delete\n" +
                    "from lab7_reservations \n" +
                    "where CODE=%d;", rs.getInt("CODE"));
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        }
    }

    private int printConfirmation(ResultSet rs) throws SQLException {
        Scanner in = new Scanner(System.in);
        System.out.format("Confirm cancellation of reservation %d?\n\n", rs.getInt("CODE"));
        System.out.format("Name: %s %s\n", rs.getString("FirstName"), rs.getString("LastName"));
        System.out.format("Start date: %s, End date: %s\n", rs.getString("CheckIn"), rs.getString("Checkout"));
        System.out.format("Number children: %d, Number adults: %d", rs.getInt("Kids"), rs.getInt("Adults"));
        System.out.print("\n\n1 to cancel reservation, 0 to go back: ");
        return in.nextInt();
    }

    // Reservations (FR2)
    private void reservations() throws SQLException {
        Map<String, String> info = new HashMap<>();
        Map<Integer, List<String>> availableRooms = new HashMap<>();

        getReservationInfo(info);
        availableRooms = queryAvailable(info);
        if (availableRooms != null) {
            bookRoom(availableRooms, info);
        }
    }

    private void bookRoom(Map<Integer, List<String>> availableRooms, Map<String, String> info) throws SQLException {
        displayAvailable(availableRooms);
        while (true) {
            // prompt choice
            System.out.println("\nChoose an option number to book that room or type '-1' to cancel");
            Scanner in = new Scanner(System.in);
            int selectedRoom = in.nextInt();
            // confirm choice
            if (availableRooms.containsKey(selectedRoom)) {
                createReservation(availableRooms.get(selectedRoom), info);
                break;
            } else if (selectedRoom == -1) {
                break;
            } else {
                System.out.println("Choose an option number to book that room or type '-1' to cancel");
            }
        }
    }

    private void displayAvailable(Map<Integer, List<String>> availableRooms) {
        for (Integer key : availableRooms.keySet()) {
            String roomCode = availableRooms.get(key).get(0);
            String roomName = availableRooms.get(key).get(1);
            String beds = availableRooms.get(key).get(2);
            String bedType = availableRooms.get(key).get(3);
            String maxOcc = availableRooms.get(key).get(4);
            String basePrice = availableRooms.get(key).get(5);
            String decor = availableRooms.get(key).get(6);
            String checkIn = availableRooms.get(key).get(7);
            String checkout = availableRooms.get(key).get(8);
            System.out.format("%d %s %s %s %s %s %s %s %s %s %n", key, roomCode, roomName, beds, bedType, maxOcc,
                    basePrice,
                    decor, checkIn, checkout);
        }
    }

    private void createReservation(List<String> selectedRoom, Map<String, String> info) throws SQLException {
        double cost = getCost(selectedRoom);
        if (confirmReservation(selectedRoom, cost, info)) {
            String newCode = addReservation(selectedRoom, info);
            System.out.format("\nConfirmation code: %s\n", newCode);
        }
    }

    private String addReservation(List<String> selectedRoom, Map<String, String> info) throws SQLException {
        ArrayList<String> existingCodes = queryRow("select CODE from lab7_reservations;", "CODE");
        String newCode = newRoomCode();
        while (existingCodes.contains(newCode)) {
            newCode = newRoomCode();
        }
        // Establish connection to RDBMS
        try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                System.getenv("HP_JDBC_USER"),
                System.getenv("HP_JDBC_PW"))) {
            // Construct SQL statement
            String sql = getNewReservationInfo(selectedRoom, info, newCode);
            // Send SQL statement to DBMS
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                return newCode;
            }
        }
    }

    private String getNewReservationInfo(List<String> selectedRoom, Map<String, String> info, String newCode) {
        String roomCode = selectedRoom.get(0);
        String checkIn = info.get("CheckIn");
        String checkout = info.get("Checkout");
        String rate = selectedRoom.get(5);
        String lastName = info.get("LastName");
        String firstName = info.get("FirstName");
        String adults = info.get("Adults");
        String kids = info.get("Kids");

        return String.format("INSERT INTO lab7_reservations (CODE, Room, CheckIn, Checkout, Rate, LastName, " +
                "FirstName, Adults, Kids) VALUES \n" +
                "    ('%s', '%s', '%s', '%s', %s, '%s', '%s', %s, %s)",
                newCode, roomCode, checkIn, checkout, rate, lastName, firstName, adults, kids);
    }

    private void printReservationInfo(List<String> selectedRoom, Map<String, String> info) {
        List<String> labels = new ArrayList<String>(
                Arrays.asList("RoomCode", "RoomName", "BedType", "Checkin", "Checkout", "numAdult", "numChild"));

        System.out.format("%s | %s\n", "First Name", info.get("FirstName"));
        System.out.format("%s | %s\n", "Last Name", info.get("LastName"));

        System.out.format("%s | %s\n", labels.get(0), selectedRoom.get(0));
        System.out.format("%s | %s\n", labels.get(1), selectedRoom.get(1));
        System.out.format("%s | %s\n", labels.get(2), selectedRoom.get(3));
        System.out.format("%s | %s\n", labels.get(3), selectedRoom.get(7));
        System.out.format("%s | %s\n", labels.get(4), selectedRoom.get(8));
        System.out.format("%s | %s\n", labels.get(5), info.get("Adults"));
        System.out.format("%s | %s\n", labels.get(6), info.get("Kids"));
    }

    private boolean confirmReservation(List<String> selectedRoom, double cost, Map<String, String> info) {
        // print confirmation information and prompt, return true if confirmed
        Scanner in = new Scanner(System.in);

        System.out.println("Confirm reservation?:\n");
        printReservationInfo(selectedRoom, info);
        System.out.format("\nfor $%.2f? [Yes/No] %n", cost);

        String result = in.nextLine();
        if (!result.equals("Yes") && !result.equals("No")) {
            System.out.println("invalid input");
            return confirmReservation(selectedRoom, cost, info);
        } else {
            return result.equals("Yes");
        }
    }

    private double getCost(List<String> selectedRoom) {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        double basePrice = Double.parseDouble(selectedRoom.get(5));
        double weekendPrice = basePrice * 1.1;
        try {
            Date checkInDate = formatter.parse(selectedRoom.get(7));
            Date checkoutDate = formatter.parse(selectedRoom.get(8));
            int weekdays = getWorkingDaysBetweenTwoDates(checkInDate, checkoutDate);
            int days = getDaysBetweenTwoDates(checkInDate, checkoutDate);
            int weekendDays = days - weekdays;
            return weekdays * basePrice + weekendDays * weekendPrice;
        } catch (ParseException e) {
            System.out.println("invalid date");
        }
        return -1;
    }

    // return table of select options to list of room information (everything needed
    // to book that room)
    private Map<Integer, List<String>> queryAvailable(Map<String, String> info) throws SQLException {
        // Establish connection to RDBMS
        try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                System.getenv("HP_JDBC_USER"),
                System.getenv("HP_JDBC_PW"))) {
            // Construct SQL statement
            String sql = getReservationQuery(info);
            // Send SQL statement to DBMS
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                return getAvailable(rs, info);
            }
        }
    }

    private String getReservationQuery(Map<String, String> info) {
        String roomString = "";
        if (!info.get("RoomCode").equals("Any")) {
            roomString = "and Room = '" + info.get("RoomCode") + "'";
        }
        String bedTypeString = "";
        if (!info.get("bedType").equals("Any")) {
            bedTypeString = "and bedType = '" + info.get("bedType") + "'";
        }
        int occupants = Integer.parseInt(info.get("Kids")) + Integer.parseInt(info.get("Adults"));
        return getReservationString(info, roomString, bedTypeString, occupants);
    }

    private Map<Integer, List<String>> getAvailable(ResultSet rs, Map<String, String> info) throws SQLException {
        int maxCapacity = Integer.parseInt(queryRow("select max(maxOcc) mo from lab7_rooms;", "mo").get(0));
        // too many guests for one room
        if ((Integer.parseInt(info.get("Adults")) + Integer.parseInt(info.get("Kids"))) > maxCapacity) {
            System.out.format(
                    "No suitable rooms available. Book multiple rooms to accommodate greater than %d people\n",
                    maxCapacity);
            return null;
        } else if (!rs.first()) { // result set is empty
            return getSimilarRooms(info);
        } else { // available rooms with all requirements met
            System.out.println("Available Rooms on Your Selected Dates:");
            return getMatchingRooms(rs, info);
        }
    }

    private Map<Integer, List<String>> getMatchingRooms(ResultSet rs, Map<String, String> info) throws SQLException {
        Map<Integer, List<String>> availabilities = new HashMap<>();
        // convert ResultSet to table
        int i = 0;
        while (rs.next()) {
            i++; // increments the output
            String roomCode = rs.getString("RoomCode");
            String roomName = rs.getString("RoomName");
            String beds = rs.getString("Beds");
            String bedType = rs.getString("bedType");
            String maxOcc = rs.getString("maxOcc");
            String basePrice = rs.getString("basePrice");
            String decor = rs.getString("decor");
            String checkIn = info.get("CheckIn");
            String checkout = info.get("Checkout");
            List<String> roomInfo = Arrays.asList(roomCode, roomName, beds, bedType, maxOcc, basePrice, decor, checkIn,
                    checkout);
            availabilities.put(i, roomInfo);
        }
        return availabilities;
    }

    private Map<Integer, List<String>> getSimilarRooms(Map<String, String> info) throws SQLException {
        // query for same occupancy, closest dates
        // Establish connection to RDBMS
        try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                System.getenv("HP_JDBC_USER"),
                System.getenv("HP_JDBC_PW"))) {
            // Construct SQL statement
            info.put("RoomCode", "Any");
            info.put("bedType", "Any");
            String sql = getReservationQuery(info);
            // sql = sql.substring(0, sql.length()-1) + "LIMIT 5;";
            // Send SQL statement to DBMS
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                if (!rs.next()) {
                    System.out.println("No rooms found on selected dates with required capacity");
                    return null;
                }
                System.out.println("No exact matches found - here are the most similar bookings");
                return getAvailable(rs, info);
            }
        }
    }

    private String getReservationString(Map<String, String> info, String roomString, String bedTypeString,
            int occupants) {
        return String.format(
                "with conflicts as (\n" +
                        "    select distinct RoomCode\n" +
                        "    from lab7_reservations\n" +
                        "        join lab7_rooms on RoomCode=Room\n" +
                        "    where ('%s' < Checkout and '%s' > CheckIn)\n" +
                        ")\n" +
                        "select distinct RoomCode, RoomName, Beds, bedType, maxOcc, basePrice, decor\n" +
                        "from lab7_reservations\n" +
                        "    join lab7_rooms on RoomCode=Room\n" +
                        "where RoomCode not in (select * from conflicts)\n" +
                        "    %s %s and maxOcc>=%d;",
                info.get("CheckIn"), info.get("Checkout"), roomString, bedTypeString, occupants);
    }

    // puts info into hashmap, cleans hashmap input
    private void getReservationInfo(Map<String, String> info) throws SQLException {
        Scanner in = new Scanner(System.in);

        getName(info, in, "First Name: ", "FirstName");
        getName(info, in, "Last Name: ", "LastName");
        getRoomCode(info, in);
        getBedType(info, in);
        getDate(info, in, "Check-in date (as 'YYYY-MM-DD'): ", "CheckIn");
        getDate(info, in, "Checkout date (as 'YYYY-MM-DD'): ", "Checkout");
        getNumber(info, in, "Number of children: ", "Kids");
        getNumber(info, in, "Number of adults: ", "Adults");
        System.out.println();
    }

    // ensures a number is put into the info table
    private void getNumber(Map<String, String> info, Scanner in, String prompt, String key) {
        while (true) {
            System.out.print(prompt);
            String number = in.nextLine();
            if (isNumber(number)) {
                info.put(key, number);
                return;
            } else {
                System.out.println("Use numbers only");
            }
        }
    }

    private boolean isNumber(String number) {
        try {
            Integer.parseInt(number);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // ensures a date with proper format is put into the info table (does not
    // account for invalid dates like 2022-50-50)
    private void getDate(Map<String, String> info, Scanner in, String prompt, String key) {
        // used to validate date format
        Pattern p = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
        while (true) {
            System.out.print(prompt);
            String date = in.nextLine();
            // ensures dates follow the proper format
            if (p.matcher(date).matches() && validDate(date)) {
                info.put(key, date);
                return;
            } else {
                System.out.println("Invalid date");
            }
        }
    }

    // ensures an existing bed type or "Any" is put into the info table
    private void getBedType(Map<String, String> info, Scanner in) throws SQLException {
        // list of valid bed types
        ArrayList<String> roomList = queryRow("select distinct bedType from lab7_rooms;", "bedType");
        while (true) {
            System.out.print("Desired bed type (or “Any” to indicate no preference): ");
            String bedType = in.nextLine();
            // ensures input is valid
            if (roomList.contains(bedType) || bedType.equals("Any")) {
                info.put("bedType", bedType);
                return;
            } else {
                System.out.println("Invalid bed type");
            }
        }
    }

    // ensures an existing room code or "Any" is put into the info table
    private void getRoomCode(Map<String, String> info, Scanner in) throws SQLException {
        // list of valid room codes
        ArrayList<String> roomList = queryRow("select distinct RoomCode from lab7_rooms;", "RoomCode");
        while (true) {
            System.out.print("Room code (or “Any” to indicate no preference): ");
            String roomCode = in.nextLine();
            // ensures input is valid
            if (roomList.contains(roomCode) || roomCode.equals("Any")) {
                info.put("RoomCode", roomCode);
                return;
            } else {
                System.out.println("Invalid code");
            }
        }
    }

    // ensures names put into the info table use letters only
    private void getName(Map<String, String> info, Scanner in, String prompt, String key) {
        while (true) {
            System.out.print(prompt);
            String name = in.nextLine();
            // ensures names use letters only
            if (name.matches("[a-zA-Z]+")) {
                info.put(key, name);
                return;
            } else {
                System.out.println("Use letters only");
            }
        }
    }

    // ONLY USE THIS FOR TRUSTED IN-CODE QUERIES
    private ArrayList<String> queryRow(String query, String rowName) throws SQLException {
        ArrayList<String> row = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                System.getenv("HP_JDBC_USER"),
                System.getenv("HP_JDBC_PW"))) {
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    row.add(rs.getString(rowName));
                }
            }
        }
        return row;
    }

    private Map<Integer, Float> makeMonthsMap() {
        Map<Integer, Float> months = new HashMap<>();
        for (int i = 1; i < 13; i++) {
            months.put(i, 0f);
        }
        return months;
    }

    private Map<String, Map<Integer, Float>> makeRoomMap() throws SQLException {
        Map<String, Map<Integer, Float>> rooms = new HashMap<>();

        try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                System.getenv("HP_JDBC_USER"),
                System.getenv("HP_JDBC_PW"))) {

            String sql = "SELECT RoomCode FROM lab7_rooms";

            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sql)) {

                // Receive results
                while (rs.next()) {
                    String RoomCode = rs.getString("RoomCode");
                    Map<String, Float> months = new HashMap<>();
                    rooms.put(RoomCode, makeMonthsMap());
                }
            }
        }
        // System.out.print(rooms);
        return rooms;
    }

    private void roomMonthRev(Map<String, Map<Integer, Float>> roomRev, int month) throws SQLException {

        // Establish connection to RDBMS
        try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                System.getenv("HP_JDBC_USER"),
                System.getenv("HP_JDBC_PW"))) {
            // Construct SQL statement
            String sql = String.format("""
                    with daysInMonth as(
                        SELECT
                            CODE,
                            (case
                                -- checkin and out both in valid month
                                when
                                    MONTH(CheckIn) = %d and MONTH(Checkout) = %d
                                    and YEAR(rsv.CheckIn) = YEAR(CURRENT_DATE)
                                    and YEAR(rsv.Checkout) = YEAR(CURRENT_DATE)
                                    then DATEDIFF(Checkout, CheckIn)
                                -- checkin in valid month but checkout after current month
                                when MONTH(CheckIn) = %d and YEAR(rsv.CheckIn) = YEAR(CURRENT_DATE)
                                    then
                                        (case
                                            -- count 1 night if checkin on last of month
                                            when LAST_DAY(CheckIn) = CheckIn then 1
                                            else  DATEDIFF(LAST_DAY(CheckIn), CheckIn) + 1
                                        end)
                                -- checkout in valid month but checkin before current month
                                when
                                    MONTH(Checkout) = %d
                                    then DAY(Checkout) - 1
                                -- checkin and checkout encompass the current month
                                when
                                    CheckIn < '2022-%d-01'
                                    and Checkout > LAST_DAY('2022-%d-01')
                                    then DATEDIFF(LAST_DAY('2022-%d-01'), '2022-%d-01')
                                -- current month not in reservation
                                else 0
                            end) as numDays,
                            CheckIn, Checkout
                        FROM lab7_reservations rsv
                        WHERE
                            YEAR(rsv.Checkout) >= YEAR(CURRENT_DATE)
                    ), stayRevenue as(
                        SELECT rsv.CODE, dim.numDays * rate as stayRev
                        FROM daysInMonth dim
                            join lab7_reservations rsv on dim.CODE = rsv.CODE
                    )
                    SELECT rsv.Room, SUM(rev.stayRev) as monthlyRev
                    FROM stayRevenue rev
                        join lab7_reservations rsv on rev.CODE = rsv.CODE
                    GROUP BY rsv.Room
                                    """, month, month, month, month, month, month, month, month);

            // Send SQL statement to DBMS
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sql)) {

                // Receive results
                while (rs.next()) {
                    String Room = rs.getString("Room");
                    float monthRev = rs.getFloat("monthlyRev");
                    if (Room == "SAY") {
                        System.out.println(Room);
                        System.out.println(monthRev);
                    }
                    roomRev.get(Room).put(month, monthRev);
                }
            }
        }
    }

    private void printRoomRevenue(Map<String, Map<Integer, Float>> roomRev, String room) {
        Map<Integer, Float> revenue = roomRev.get(room);
        System.out.print(room);
        float revSum = 0;
        for (int i = 1; i < 13; i++) {
            System.out.print(" | ");
            float monthRev = revenue.get(i);
            System.out.format("%-7.2f", monthRev);
            revSum = revSum + monthRev;
        }
        int roundedSum = Math.round(revSum);
        System.out.format(" | %-8d", roundedSum);
        System.out.println("");
    }

    private void printAllRevenue(Map<String, Map<Integer, Float>> roomRev) {
        System.out.format(
                "Room| %-7s | %-7s | %-7s | %-7s | %-7s | %-7s | %-7s | %-7s | %-7s | %-7s | %-7s | %-7s | TOTAL%n",
                "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC");
        roomRev.forEach((k, v) -> printRoomRevenue(roomRev, k));
    }

    // FR6
    private void getRevenue() throws SQLException {

        try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                System.getenv("HP_JDBC_USER"),
                System.getenv("HP_JDBC_PW"))) {

            Map<String, Map<Integer, Float>> roomRev = makeRoomMap();
            System.out.println("Fetching revenue...\n");
            for (int i = 1; i < 13; i++) {
                roomMonthRev(roomRev, i);
            }
            printAllRevenue(roomRev);
        }
    }

    private void getRoomRate() throws SQLException {

        System.out.println("getting popular rooms and rates");

        // establish connection
        try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                System.getenv("HP_JDBC_USER"),
                System.getenv("HP_JDBC_PW"))) {

            // construct query statement
            String sql = """
                        with daysPerRsv as(
                            SELECT CODE,
                                (case
                                    when CheckIn < DATE_SUB(CURRENT_DATE, INTERVAL 180 DAY)
                                    then DATEDIFF(Checkout, DATE_SUB(CURRENT_DATE, INTERVAL 180 DAY))
                                                        else DATEDIFF(Checkout, CheckIn)
                                end) as numDays
                            FROM lab7_reservations
                            WHERE
                                DATEDIFF(CURRENT_DATE, Checkout) <= 180
                        ), daysPerRm as(
                            SELECT Room, SUM(numDays) as numDays
                            FROM daysPerRsv dprsv
                                join lab7_reservations rsv on dprsv.CODE = rsv.CODE
                            GROUP BY Room
                        ), popularity as(
                            SELECT rm.*, ROUND(numDays / 180, 2) as popularity
                            FROM daysPerRm dprm
                                join lab7_rooms rm on dprm.Room = rm.RoomCode
                        ), latestOut as(
                            SELECT Room, MAX(Checkout) as lastOut
                            FROM lab7_reservations
                            GROUP BY Room
                        ), recentStay as (
                            SELECT rsv.CODE
                            FROM lab7_reservations rsv
                                join latestOut lo on rsv.Room = lo.Room
                            WHERE rsv.Checkout = lo.lastOut
                        )
                        SELECT
                            p.*,
                            DATE_ADD(rsv.Checkout, INTERVAL 1 DAY) as nextAvail,
                            DATEDIFF(rsv.Checkout, rsv.CheckIn) as recentStayLength
                        FROM lab7_reservations rsv
                            join popularity p on rsv.Room = p.RoomCode
                            join recentStay rs on rsv.CODE = rs.CODE
                        WHERE
                            rsv.Checkout <> CURRENT_DATE
                        ORDER BY p.popularity desc
                    """;
            // Step 4: Send SQL statement to DBMS
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sql)) {

                // Step 5: Receive results
                String headerSpace = "| %-3s | %-25s | %-1s | %-7s | %-1s | %-3s | %-12s | %-10s | %-10s | %-12s | %n";
                System.out.format(headerSpace, "RoomCode", "RoomName", "Beds", "bedType", "maxOcc", "basePrice",
                        "Decor",
                        "popularity", "nextAvail", "recentLength");
                System.out.println("-".repeat(134));
                // System.out.format("| %-3s | %-25s | %-1s | %-7s | %-1s | %-3s | %-12s | %-10s
                // | %-10s | %-12s | %n",
                // "RoomCode", "RoomName", "Beds", "bedType", "maxOcc", "basePrice", "Decor",
                // "popularity", "nextAvail", "recentLength");
                while (rs.next()) {
                    String RoomCode = rs.getString("RoomCode");
                    String RoomName = rs.getString("RoomName");
                    int Beds = rs.getInt("Beds");
                    String bedType = rs.getString("bedType");
                    int maxOcc = rs.getInt("maxOcc");
                    int basePrice = rs.getInt("basePrice");
                    String Decor = rs.getString("Decor");
                    float popularity = rs.getFloat("popularity");
                    String nextAvail = rs.getString("nextAvail");
                    int recentStayLength = rs.getInt("recentStayLength");

                    System.out.format(
                            "| %-8s | %-25s | %-4d | %-7s | %-6d | %-9d | %-12s | %-10.2f | %-10s | %-12d | %n",
                            RoomCode, RoomName, Beds, bedType, maxOcc, basePrice, Decor,
                            popularity, nextAvail, recentStayLength);
                }
            }

        }

    }

    // get weekdays between 2 dates
    // taken from
    // https://stackoverflow.com/questions/4600034/calculate-number-of-weekdays-between-two-dates-in-java
    public static int getWorkingDaysBetweenTwoDates(Date startDate, Date endDate) {
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(startDate);
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(endDate);
        int workDays = 0;
        // Return 0 if start and end are the same
        if (startCal.getTimeInMillis() == endCal.getTimeInMillis()) {
            return 0;
        }
        if (startCal.getTimeInMillis() > endCal.getTimeInMillis()) {
            startCal.setTime(endDate);
            endCal.setTime(startDate);
        }
        do {
            // excluding start date
            startCal.add(Calendar.DAY_OF_MONTH, 1);
            if (startCal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY
                    && startCal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                ++workDays;
            }
        } while (startCal.getTimeInMillis() < endCal.getTimeInMillis()); // excluding end date
        return workDays;
    }

    // modified the above to get all days between dates
    public static int getDaysBetweenTwoDates(Date startDate, Date endDate) {
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(startDate);
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(endDate);
        int days = 0;
        // Return 0 if start and end are the same
        if (startCal.getTimeInMillis() == endCal.getTimeInMillis()) {
            return 0;
        }
        if (startCal.getTimeInMillis() > endCal.getTimeInMillis()) {
            startCal.setTime(endDate);
            endCal.setTime(startDate);
        }
        do {
            // excluding start date
            startCal.add(Calendar.DAY_OF_MONTH, 1);
            ++days;
        } while (startCal.getTimeInMillis() < endCal.getTimeInMillis()); // excluding end date
        return days;
    }

    public String newRoomCode() {
        String chars = "0123456789";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(5);
        for (int i = 0; i < 5; i++)
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }
}
