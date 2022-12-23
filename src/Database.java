import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Scanner;

public class Database {

    public static void main(String[] args) {
        loadDriver();
        Connection connection = getConnection();
        userInterface(connection);
        closeConnection(connection);
    }

    public static void insertCustomer(Connection connection){
        PreparedStatement ps = null;
        Scanner scanner = new Scanner(System.in);

        try{

            System.out.println("Enter customer data:");
            System.out.println("Personal code:");
            String personal_code = scanner.next();
            System.out.println("First name:");
            String first_name = scanner.next();
            System.out.println("Last name:");
            String last_name = scanner.next();
            System.out.println("Date of birth:");
            String date_of_birth_value = scanner.next();
            Date date_of_birth = formatToDate(date_of_birth_value);

            if(date_of_birth != null){

                System.out.println("Address:");
                String address = scanner.next();
                System.out.println("Phone_number:");
                String phone_number = scanner.next();

                ps = connection.prepareStatement("INSERT INTO Customer" +
                        " (personal_code, first_name, last_name, date_of_birth, address, phone_number)" +
                        "VALUES (?, ?, ?, ?, ?, ?)");

                ps.setString(1, personal_code);
                ps.setString(2, first_name);
                ps.setString(3, last_name);
                ps.setDate(4, date_of_birth);
                ps.setString(5, address);
                ps.setString(6, phone_number);

                ps.executeUpdate();

                System.out.println("New customer successfully added");

            }else{
                System.out.println("Date input is invalid. Enter date in a format yyyy-MM-dd");
            }

        }catch (SQLException e) {
            System.out.println("Couldn't insert new customer");
            System.out.println(e.getMessage());
        }finally {
            closeStatement(ps);
        }
    }

    public static void findCustomer(Connection connection){
        PreparedStatement ps = null;
        ResultSet rs = null;
        Scanner scanner = new Scanner(System.in);

        try{

            System.out.println("Select by what data you want to find a customer:\n" +
                    "0 - personal_code\n" +
                    "1 - first name and last name");
            int input = scanner.nextInt();

            switch (input){
                case 0:
                    System.out.println("Personal code:");
                    String personal_code = scanner.next();

                    ps = connection.prepareStatement("SELECT * FROM customer WHERE personal_code = ?");
                    ps.setString(1, personal_code);
                    rs = ps.executeQuery();

                    findCustomerResults(rs);
                    break;
                case 1:
                    System.out.println("First name:");
                    String first_name = scanner.next();
                    System.out.println("Last name:");
                    String last_name = scanner.next();

                    ps = connection.prepareStatement("SELECT * FROM customer WHERE first_name = ? AND last_name = ?");
                    ps.setString(1, first_name);
                    ps.setString(2, last_name);
                    rs = ps.executeQuery();

                    findCustomerResults(rs);
                    break;
                default:
                    System.out.println("Wrong input number");
            }
        }catch (SQLException e){
            System.out.println("Failed to search for a customer");
            System.out.println(e.getMessage());
        }finally {
            closeStatement(ps);
            closeResultSet(rs);
        }
    }

    public static void findCustomerResults(ResultSet rs){
        try{
            if(rs.isBeforeFirst()){
                printCustomerTable(rs);
            }else{
                System.out.println("Customer not found");
            }
        }
        catch (SQLException e){
            e.printStackTrace();
        }


    }
    public static void deleteCustomer(Connection connection){
        PreparedStatement ps = null;
        Scanner scanner = new Scanner(System.in);

        try{
            displayCustomers(connection);

            System.out.println("Select a customer to delete by entering id:");
            int id = scanner.nextInt();

            ps = connection.prepareStatement("DELETE FROM Customer WHERE id = ?");
            ps.setInt(1, id);
            int deleted = ps.executeUpdate();

            if(deleted > 0) {
                System.out.println("Customer successfully deleted");
            }else{
                System.out.println("Customer with such id doesn't exist");
            }

        }catch (SQLException e){
            System.out.println("Customer couldn't be deleted");
            System.out.println(e.getMessage());
        }finally {
            closeStatement(ps);
        }
    }

    public static void displayCustomers(Connection connection){
        Statement s = null;
        ResultSet rs = null;
        try{
            s = connection.createStatement();
            rs = s.executeQuery("SELECT * FROM Customer");
            printCustomerTable(rs);

        }catch (SQLException e){
            System.out.println("Customers couldn't be selected");
        }finally {
            closeStatement(s);
            closeResultSet(rs);
        }
    }

    public static void makeReservation(Connection connection){
        PreparedStatement ps = null;
        Scanner scanner = new Scanner(System.in);
        Statement s = null;
        ResultSet rs = null;
        try{

            displayCustomers(connection);

            System.out.println("Select a customer by entering id:");
            int customer_id = scanner.nextInt();

            s = connection.createStatement();
            rs = s.executeQuery("SELECT Car.id, license_number, make, model, price, category, city, address\n" +
                    "FROM Car, Car_categories, Location\n" +
                    "WHERE status = 'available' AND  Car.id = Car_categories.id AND Car.location_id = Location.id\n;");

            printSelectCarTable(rs);

            System.out.println("Select a car by entering id:");
            int car_id = scanner.nextInt();

            System.out.println("Enter pick up date:");
            String pickup_value = scanner.next();
            System.out.println("Enter return date:");
            String return_value = scanner.next();

            Date pickup_date = formatToDate(pickup_value);
            Date return_date = formatToDate(return_value);

            if(pickup_date != null && return_date != null){

                ps = connection.prepareStatement("INSERT INTO Reservation" +
                        " (car_id, customer_id, pickup_date, return_date)" +
                        "values(?, ?, ?, ?)");

                ps.setInt(1, car_id);
                ps.setInt(2, customer_id);
                ps.setDate(3, pickup_date);
                ps.setDate(4, return_date);

                ps.executeUpdate();

                System.out.println("Reservation successfully made");

            }else{
                System.out.println("Date input is invalid. Enter date in a format yyyy-MM-dd");
            }

        }catch (SQLException e){
            System.out.println("Couldn't make a reservation");
            System.out.println(e.getMessage());
            rollBack(connection);
        }finally {
            closeStatement(s);
            closeStatement(ps);
            closeResultSet(rs);
        }
    }
    public static void deleteLocation(Connection connection){
        PreparedStatement ps = null;
        Scanner scanner = new Scanner(System.in);

        try{
            connection.setAutoCommit(false);

            displayLocations(connection);

            System.out.println("Enter location id to delete:");
            int id = scanner.nextInt();

            if(id == 1){
                System.out.println("Default location can't be deleted");
            }else{

                ps = connection.prepareStatement("UPDATE Car SET location_id = 1 WHERE location_id = ? ");
                ps.setInt(1, id);

                ps.executeUpdate();

                ps = connection.prepareStatement("DELETE FROM Location WHERE id = ?");
                ps.setInt(1, id);

                int deleted = ps.executeUpdate();

                connection.commit();
                connection.setAutoCommit(true);

                if(deleted > 0){
                    System.out.println("Location successfully deleted");
                }else{
                    System.out.println("Location with such id doesn't exist");
                }
            }

        }catch (SQLException e){
            System.out.println("Couldn't delete location");
            System.out.println(e.getMessage());
            rollBack(connection);

        }finally {
            closeStatement(ps);
        }
    }

    public static void updateLocation(Connection connection){
        PreparedStatement ps = null;
        Scanner scanner = new Scanner(System.in);

        try{
            displayLocations(connection);

            System.out.println("Enter location id to update:");
            int id = scanner.nextInt();

            System.out.println("Enter what you want to update:\n" +
                    "0 - city\n" +
                    "1 - address\n" +
                    "2 - phone number");
            int input  = scanner.nextInt();

            String colName = null;

            switch (input){
                case 0:
                    colName = "city";
                    break;
                case 1:
                    colName = "address";
                    break;
                case 2:
                    colName = "phone_number";
                    break;
                default:
                    System.out.println("Wrong input number");
            }

            if(colName != null){
                System.out.println("Enter new value:");
                String value = scanner.next();

                ps = connection.prepareStatement("UPDATE Location SET " + colName + " = ? WHERE id = ?");
                ps.setString(1, value);
                ps.setInt(2, id);

                int upadated = ps.executeUpdate();

                if(upadated > 0) {
                    System.out.println("Location successfully updated");
                }else{
                    System.out.println("Location with such id doesn't exist");
                }

            }
        }
        catch (SQLException e){
            System.out.println("Location couldn't be updated");
            System.out.println(e.getMessage());
        }finally {
            closeStatement(ps);
        }
    }

    public static void displayLocations(Connection connection){
        Statement s = null;
        ResultSet rs = null;
        try{
            s = connection.createStatement();
            rs = s.executeQuery("SELECT * FROM Location");
            printLocationTable(rs);

        }catch (SQLException e){
            System.out.println("Locations couldn't be selected");
            System.out.println(e.getMessage());
        }finally {
            closeStatement(s);
            closeResultSet(rs);
        }
    }

    public static Date formatToDate(String value){
        Date date = null;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        try{
            date = new Date((formatter.parse(value)).getTime());
            if(value.equals(formatter.format(date))) {
                return date;
            }
        }catch (ParseException e){
            System.out.println("Couldn't parse date value");
        }

        return null;
    }

    public static void printSelectCarTable(ResultSet rs) {

        try{
            System.out.println("car_id\t\tlicense_number\t\tmake\t\tmodel\t\tprice\t\tcategory\t\tcity\t\taddress");
            while (rs.next()) {
                System.out.println(rs.getInt(1) + "\t\t"
                        + rs.getString(2) + "\t\t"
                        + rs.getString(3) + "\t\t"
                        + rs.getString(4) + "\t\t"
                        + rs.getDouble(5) + "\t\t"
                        + rs.getString(6) + "\t\t"
                        + rs.getString(7) + "\t\t"
                        + rs.getString(8));
            }
        }catch (SQLException e){
            System.out.println("Couldn't print table content");
        }
    }

    public static void printCustomerTable(ResultSet rs) {
        try{
            System.out.println("id\t\tpersonal_code\t\tfirst_name\t\tlast_name\t\tdate_of_birth\t\taddress\t\tphone_number");
            while (rs.next()) {
                System.out.println(rs.getInt(1) + "\t\t"
                        + rs.getString(2) + "\t\t"
                        + rs.getString(3) + "\t\t"
                        + rs.getString(4) + "\t\t"
                        + rs.getDate(5) + "\t\t"
                        + rs.getString(6) + "\t\t"
                        + rs.getString(7));
            }
        }catch (SQLException e){
            System.out.println("Couldn't print table content");
        }
    }

    public static void printLocationTable(ResultSet rs){
        try {
            System.out.println("id\t\tcity\t\taddress\t\tphone_number");
            while (rs.next()) {
                System.out.println(rs.getInt(1) + "\t\t"
                        + rs.getString(2) + "\t\t"
                        + rs.getString(3) + "\t\t"
                        + rs.getString(4));
            }
        }catch (SQLException e){
            System.out.println("Couldn't print table content");
        }
    }

    public static void userInterface(Connection connection){
        Scanner scanner = new Scanner(System.in);
        displayOptions();
        int input = 1;
        while(input != 0){
            System.out.println("Select an action:");
            input = scanner.nextInt();

            switch (input){
                case 0:
                    System.out.println("Quitting the program...");
                    break;
                case 1:
                    displayOptions();
                    break;
                case 2:
                    insertCustomer(connection);
                    break;
                case 3:
                    findCustomer(connection);
                    break;
                case 4:
                    displayCustomers(connection);
                    break;
                case 5:
                    deleteCustomer(connection);
                    break;
                case 6:
                    makeReservation(connection);
                    break;
                case 7:
                    deleteLocation(connection);
                    break;
                case 8:
                    updateLocation(connection);
                    break;
                case 9:
                    displayLocations(connection);
                default:
                    System.out.println("There is no such action");
                    displayOptions();
            }
        }
    }

    public static void displayOptions(){
        System.out.println("Actions:\n" +
                "0 - quit the program\n" +
                "1 - display actions\n" +
                "2 - add a new customer\n" +
                "3 - find a customer\n" +
                "4 - display all customers\n" +
                "5 - delete a customer\n" +
                "6 - make a reservation\n" +
                "7 - delete location\n" +
                "8 - update location\n" +
                "9 - display all locations\n");
    }

    public static void loadDriver(){
        try{
            Class.forName("org.postgresql.Driver");
        }catch (ClassNotFoundException e){
            System.out.println("Can't find driver class");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static Connection getConnection(){
        Connection connection;
        try{
            connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/lab2", "postgres", "Geek.123");
        }catch (SQLException e){
            System.out.println("Couldn't connect to database");
            e.printStackTrace();
            return null;
        }

        System.out.println("Successfully connected to the database");
        return connection;
    }

    public static void rollBack(Connection connection){
        try {
            connection.rollback();
            System.out.println("Transaction was rolled back");
        }catch (SQLException e2){
            System.out.println("Transaction couldn't be rolled back");
        }
    }

    public static void closeConnection(Connection connection){
        if(connection != null){
            try{
                connection.close();
            }catch (SQLException e){
                System.out.println("Couldn't close connection");
                e.printStackTrace();
            }
        }
    }

    public static void closeStatement(Statement s){
        if(s != null){
            try{
                s.close();
            }catch (SQLException e){
                System.out.println("Couldn't close statement");
                e.printStackTrace();
            }
        }
    }

    public static void closeResultSet(ResultSet rs){
        if(rs != null){
            try{
                rs.close();
            }catch (SQLException e){
                System.out.println("Couldn't close result set");
                e.printStackTrace();
            }
        }
    }

}
