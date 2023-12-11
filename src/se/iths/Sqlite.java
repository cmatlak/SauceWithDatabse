package se.iths;

import java.sql.*;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

public class Sqlite {

    private static final Scanner scanner = new Scanner(System.in);

    private static Connection connect() {

        String url = "jdbc:sqlite:C:/Users/Admin/sqlite-tools-win-x64-3440200/lab3";
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    private static String showMenu() {
        return """
                Make a choice
                ========
                1. Sauce List
                2. Add Sauce
                3. Update Sauce
                4. Average sauce Price
                5. Number of Sauces
                6. Update Sauce Category
                7. Delete Sauce
                8. Select Favorite
                9. Favorite Sauces
                e. Exit
                """;

    }


    private static void selectAll() {
        String sql = "SELECT sauce.sauceId, sauce.sauceTitle, sauce.sauceManufacturer, category.categoryName, sauce.saucePrice, sauce.sauceDescription " +
                "FROM sauce " +
                "INNER JOIN sauceCategory ON sauce.sauceId = sauceCategory.sauceId " +
                "INNER JOIN category ON sauceCategory.categoryId = category.categoryId";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet res = stmt.executeQuery(sql)) {


            System.out.println("+--------+----------------------+-----------------+------------+------------*----------------------+");
            System.out.printf("| %-6s | %-20s | %-15s | %-10s | %-10s | %-20s |%n", "ID", "Title", "Manufacturer", "Category", "Price", "Description");
            System.out.println("+--------+----------------------+-----------------+------------+------------*----------------------+");


            while (res.next()) {
                int id = res.getInt("sauceId");
                String title = res.getString("sauceTitle");
                String manufacturer = res.getString("sauceManufacturer");
                String category = res.getString("categoryName");
                Double price = res.getDouble("saucePrice");
                String description = res.getString("sauceDescription");
                System.out.printf("| %-6d | %-20s | %-15s |%-12s|%-12.2f| %-20s |%n", id, title, manufacturer, category, price, description);
            }


            System.out.println("+--------+----------------------+-----------------+------------+------------*----------------------+");

        } catch (SQLException e) {
            System.out.println("Something went wrong: " + e.getMessage());
        }
    }

    private static void countRowsInTable() {
        String sql = "SELECT COUNT(*) as count FROM " + "sauce";
        int rowCount;

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // Hämta antalet rader från resultatet av frågan
            if (rs.next()) {
                rowCount = rs.getInt("count");

                System.out.println("Total amount of Sauces : " + rowCount);
            }
        } catch (SQLException e) {
            System.out.println("Error counting rows: " + e.getMessage());
        }

    }


    private static void insertNewSauce() {
        String insertSauceSQL = "INSERT INTO sauce(sauceTitle, sauceManufacturer, sauceDescription, saucePrice) VALUES (?, ?, ?, ?)";
        String insertCategorySQL = "INSERT INTO category(categoryName) VALUES (?)";
        String insertSauceCategorySQL = "INSERT INTO sauceCategory(sauceId, categoryId) VALUES (?, ?)";

        try (Connection conn = connect();
             PreparedStatement preparedSauce = conn.prepareStatement(insertSauceSQL);
             PreparedStatement preparedCategory = conn.prepareStatement(insertCategorySQL);
             PreparedStatement preparedSauceCategory = conn.prepareStatement(insertSauceCategorySQL)) {

            List<Object> inputValues = input(); // Användarinput för såsen och kategorin

            // Hämta värden för såsen och kategorin från input
            String sauceTitle = (String) inputValues.get(0);
            String sauceManufacturer = (String) inputValues.get(1);
            String sauceDescription = (String) inputValues.get(2);
            Integer saucePrice = (Integer) inputValues.get(3);
            String categoryName = (String) inputValues.get(4);

            // Starta en transaktion
            conn.setAutoCommit(false);

            try {
                // Lägg till kategorin i category-tabellen
                preparedCategory.setString(1, categoryName);
                preparedCategory.executeUpdate();

                // Hämta kategorins ID
                int categoryId = getLastInsertedId(conn, "category");

                // Lägg till såsen i sauce-tabellen
                preparedSauce.setString(1, sauceTitle);
                preparedSauce.setString(2, sauceManufacturer);
                preparedSauce.setString(3, sauceDescription);
                preparedSauce.setDouble(4, saucePrice);
                preparedSauce.executeUpdate();

                // Hämta såsens ID
                int sauceId = getLastInsertedId(conn, "sauce");

                // Koppla kategorin till såsen i sauceCategory-tabellen
                preparedSauceCategory.setInt(1, sauceId);
                preparedSauceCategory.setInt(2, categoryId);
                preparedSauceCategory.executeUpdate();

                // Commit om allt gick bra
                conn.commit();
                System.out.println("You successfully added a new sauce with the associated category.");

            } catch (SQLException e) {
                // Om något går fel, gör en rollback
                conn.rollback();
                System.out.println("Something went wrong, please try again: " + e.getMessage());
            } finally {
                // Återställ auto-commit till true för att undvika att påverka andra operationer
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.out.println("Error connecting to the database: " + e.getMessage());
        }
    }

    private static int getLastInsertedId(Connection conn, String tableName) {
        String query = "SELECT last_insert_rowid() FROM " + tableName;
        try (PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("Error fetching last inserted ID " + e.getMessage());
            return -1;
        }
        }


    private static List<Object> input() {
        Connection conn = null;
        List<Object> values = new ArrayList<>();

        try {
            conn = connect();
            conn.setAutoCommit(false);

            while (true) {
                System.out.println("Enter sauce title: ");
                String title = scanner.nextLine();

                System.out.println("Enter sauce manufacturer: ");
                String manufacturer = scanner.nextLine();

                System.out.println("Enter sauce description: ");
                String description = scanner.nextLine();

                System.out.println("Enter sauce price: ");
                int price = scanner.nextInt();
                scanner.nextLine(); // För att rensa bufferten

                System.out.println("Enter sauce category: ");
                String category = scanner.nextLine();

                values.add(title);
                values.add(manufacturer);
                values.add(description);
                values.add(price);
                values.add(category);


                break;
            }

            conn.commit();
        } catch (InputMismatchException e) {
            System.out.println("Invalid Value, Transaction rollback initiated.");
            if (conn != null) {
                try {
                    conn.rollback(); // Ångra eventuella ändringar i databasen
                } catch (SQLException rollbackException) {
                    System.out.println("Rollback failed: " + rollbackException.getMessage());
                }
            }
            scanner.nextLine(); // För att rensa bufferten
            values.clear(); // Rensa insamlade värden eftersom transaktionen rullades tillbaka
        } catch (SQLException e) {
            System.out.println("Something went wrong: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeException) {
                    System.out.println("Error while closing connection: " + closeException.getMessage());
                }
            }
        }

        return values;
    }


    private static void updateSauce() {
        System.out.println("Enter the ID of the sauce you want to update: ");
        int sauceId = scanner.nextInt();
        scanner.nextLine();

        boolean running = true;

        do {
            System.out.println(getUpdateChoice());
            String input = scanner.nextLine();

            switch (input) {

                case "1" -> updateSauceField("sauceTitle", sauceId);

                case "2" -> updateSauceField("sauceManufacturer", sauceId);

                case "3" -> updateSauceField("sauceDescription", sauceId);

                case "4" -> updateSauceField("saucePrice", sauceId);

                case "e", "E" -> running = false;

            }
        } while (running);
        System.out.println("Exit to main menu");
    }

    private static String getUpdateChoice() {

        return """
                 Make a choice
                 ========
                 1. Title
                 2. Manufacturer
                 3. Description
                 4. Price
                 e. Main menu
                """;


    }

    private static void updateSauceField(String field, int sauceId) {
        System.out.println("Enter the new value for " + field + ": ");
        String newValue = scanner.nextLine();

        // Skriv ut det nya värdet för att kontrollera att det är korrekt
        System.out.println("New value entered: " + newValue);

        String sql = "UPDATE sauce SET " + field + " = ? WHERE sauceId = ?";

        Connection conn = null;
        try {
            conn = connect();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, newValue);
                pstmt.setInt(2, sauceId);

                int rowsUpdated = pstmt.executeUpdate();
                if (rowsUpdated > 0) {
                    System.out.println("Sauce " + field + " updated successfully for sauce ID: " + sauceId);
                } else {
                    System.out.println("Could not update sauce " + field + " for sauce ID: " + sauceId);
                }
                conn.commit();
            } catch (SQLException innerException) {
                System.out.println("Error updating sauce " + field + ": " + innerException.getMessage());
                if (conn != null) {
                    try {
                        conn.rollback(); // Ångra eventuella ändringar om det blir ett problem
                    } catch (SQLException rollbackException) {
                        System.out.println("Rollback failed: " + rollbackException.getMessage());
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error connecting to database: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeException) {
                    System.out.println("Error while closing connection: " + closeException.getMessage());
                }
            }
        }
    }


    private static void deleteSauce() {
        System.out.println("Enter the Sauce ID you wish to remove: ");
        int sauceId = scanner.nextInt();

        String sql = "DELETE FROM sauce WHERE sauceId = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, sauceId);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Sauce removed successfully");
                updateOtherSaucesCategory(conn, sauceId);
                conn.commit();
            } else {
                System.out.println("No sauce found with the given ID");
            }
        } catch (SQLException e) {
            System.out.println("Error deleting sauce: " + e.getMessage());
        }
    }


    private static void updateOtherSaucesCategory(Connection conn, int sauceId) {
        String sql = "UPDATE sauceCategory SET categoryId = NULL WHERE sauceId = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sauceId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error updating sauce category for other sauces: " + e.getMessage());
        }
    }


    private static void updateSauceCategory() {


        System.out.println("Enter the ID of the sauce you want to update: ");
        int sauceId = scanner.nextInt();
        scanner.nextLine(); // Consume newline character

        System.out.println("Enter the new category name for the sauce: ");
        String categoryName = scanner.nextLine();

        // Kontrollera om kategorin redan finns
        int categoryId = getCategoryId(categoryName);

        if (categoryId == -1) {
            // Om kategorin inte finns, lägg till den i category-tabellen
            addCategory(categoryName);
            // Hämta det nya kategorins ID
            categoryId = getCategoryId(categoryName);
        }

        if (categoryId != -1) {
            String sql = "UPDATE sauceCategory " +
                    "SET categoryId = ? " +
                    "WHERE sauceId = ?";

            try (Connection conn = connect();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, categoryId);
                pstmt.setInt(2, sauceId);

                int rowsUpdated = pstmt.executeUpdate();
                if (rowsUpdated > 0) {
                    System.out.println("Sauce category updated successfully for sauce ID: " + sauceId);
                    conn.commit();
                } else {
                    System.out.println("Could not update sauce category for sauce ID: " + sauceId);
                }
            } catch (SQLException e) {
                System.out.println("Error updating sauce category: " + e.getMessage());
            }
        } else {
            System.out.println("Category does not exist and could not be added.");
        }

    }

    // Hjälpmetod för att få kategorins ID
    private static int getCategoryId(String categoryName) {
        // SQL-fråga för att hämta kategorins ID baserat på namnet
        String sql = "SELECT categoryId FROM category WHERE categoryName = ?";
        int categoryId = -1;

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, categoryName);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                categoryId = rs.getInt("categoryId");
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving category ID: " + e.getMessage());
        }
        return categoryId;
    }

    // Hjälpmetod för att lägga till en ny kategori
    private static void addCategory(String categoryName) {
        String insertCategorySQL = "INSERT INTO category(categoryName) VALUES (?)";

        try (Connection conn = connect();
             PreparedStatement preparedCategory = conn.prepareStatement(insertCategorySQL)) {
            preparedCategory.setString(1, categoryName);
            preparedCategory.executeUpdate();
            conn.commit();
            System.out.println("New category added: " + categoryName);
        } catch (SQLException e) {
            System.out.println("Error adding category: " + e.getMessage());
        }
    }






    private static void showAveragePrice() {
        String sql = "SELECT AVG(saucePrice) AS avgPrice FROM sauce";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet res = stmt.executeQuery(sql)) {

            if (res.next()) {
                double averagePrice = res.getDouble("avgPrice");
                System.out.println("Average price of all sauces: " + averagePrice);
            } else {
                System.out.println("No sauces found.");
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void toggleFavorite() {
        System.out.println("Enter the Sauce ID you want to mark/unmark as favorite: ");
        int favoriteSauceId = scanner.nextInt();
        scanner.nextLine();

        System.out.println("Do you want to mark (Y) or unmark (N) the sauce as favorite? (Y/N)");
        String markAsFavorite = scanner.nextLine().trim().toUpperCase();

        boolean isFavorite;
        if (markAsFavorite.equals("Y")) {
            isFavorite = true;
        } else if (markAsFavorite.equals("N")) {
            isFavorite = false;
        } else {
            System.out.println("Invalid input. Please enter Y or N.");
            return; // Avslutar metoden om inputen är ogiltig
        }

        String sql = "UPDATE sauce SET isFavorite = ? WHERE sauceId = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBoolean(1, isFavorite);
            pstmt.setInt(2, favoriteSauceId);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                if (isFavorite) {
                    System.out.println("Sauce marked as favorite.");
                } else {
                    System.out.println("Favorite mark removed from sauce.");
                }
                conn.commit();
            } else {
                System.out.println("No sauce found with the given ID");
            }
        } catch (SQLException e) {
            System.out.println("Error updating sauce favorite status: " + e.getMessage());
        }
    }

    private static void showFavorite() {
        String sql = "SELECT sauce.sauceId, sauce.sauceTitle, sauce.sauceManufacturer, category.categoryName, sauce.saucePrice, sauce.sauceDescription " +
                "FROM sauce " +
                "INNER JOIN sauceCategory ON sauce.sauceId = sauceCategory.sauceId " +
                "INNER JOIN category ON sauceCategory.categoryId = category.categoryId " +
                "WHERE sauce.isFavorite = 1";


        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet res = stmt.executeQuery(sql)) {

            System.out.println("Favorite Sauces:");
            System.out.println("+--------+----------------------+-----------------+------------+------------+----------------------+");
            System.out.printf("| %-6s | %-20s | %-15s | %-10s | %-10s | %-20s |%n", "ID", "Title", "Manufacturer", "Category", "Price", "Description");
            System.out.println("+--------+----------------------+-----------------+------------+------------+----------------------+");

            while (res.next()) {
                int id = res.getInt("sauceId");
                String title = res.getString("sauceTitle");
                String manufacturer = res.getString("sauceManufacturer");
                String category = res.getString("categoryName");
                double price = res.getDouble("saucePrice");
                String description = res.getString("sauceDescription");
                System.out.printf("| %-6d | %-20s | %-15s | %-10s | %-10.2f | %-20s |%n", id, title, manufacturer, category, price, description);
            }

            System.out.println("+--------+----------------------+-----------------+------------+------------+----------------------+");

        } catch (SQLException e) {
            System.out.println("Error fetching favorite sauces: " + e.getMessage());
        }
    }


    public static void main(String[] args) {

        boolean running = true;

        do {
            System.out.println(showMenu());
            String action = scanner.nextLine();

            switch (action) {

                case "1" -> selectAll();

                case "2" -> insertNewSauce();

                case "3" -> updateSauce();

                case "4" -> showAveragePrice();

                case "5" -> countRowsInTable();

                case "6" -> updateSauceCategory();

                case "7" -> deleteSauce();

                case "8" -> toggleFavorite();

                case "9" -> showFavorite();

                case "e", "E" -> running = false;

            }
        } while (running);
        System.out.println("Program Shutting Down");
    }


}





