package eu.nimble.service.catalogue.util;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by suat on 05-Jul-19.
 */
public class DBIntegrityUtil {
    private static final String firstDbUrl = "jdbc:postgresql://localhost:5432/ubldb_test";
    private static final String firstDbUsername = "postgres";
    private static final String firstDbPassword = "nimble";
    private static final String secondDbUrl = "jdbc:postgresql://localhost:5432/ubldb_test2";
    private static final String secondDbUsername = "postgres";
    private static final String secondDbPassword = "nimble";
    private static boolean dropResources = false;

    public static void main(String[] args) throws Exception {
        // second db is the neat db containing tables only for the latest model
        List<Connection> connections = getConnections();
        try {
            List<List<String>> tableNames = getTableNames(connections);
            removeUnusedTables(tableNames, connections);
            removeUnusedColumns(tableNames, connections);
            removeUnusedForeignKeyConstraints(tableNames, connections);

        } finally {
            closeConnections(connections);
        }
    }

    private static List<List<String>> getTableNames(List<Connection> connections) throws Exception {
        String query = "SELECT table_name FROM information_schema.tables WHERE table_type='BASE TABLE' AND table_schema='public';";
        Statement stmt1 = connections.get(0).createStatement();
        Statement stmt2 = connections.get(1).createStatement();

        List<List<String>> tableNames = new ArrayList<>();
        ResultSet rs1 = stmt1.executeQuery(query);
        List<String> namesList = new ArrayList<>();
        while (rs1.next()) {
            namesList.add(rs1.getString(1));
        }
        tableNames.add(namesList);
        rs1.close();
        stmt1.close();

        ResultSet rs2 = stmt2.executeQuery(query);
        namesList = new ArrayList<>();
        while (rs2.next()) {
            namesList.add(rs2.getString(1));
        }
        tableNames.add(namesList);
        rs2.close();
        stmt2.close();

        return tableNames;
    }

    private static void removeUnusedTables(List<List<String>> tableNames, List<Connection> connections) throws Exception {
        List<String> unusedTables = new ArrayList<>();
        List<String> existingTables = tableNames.get(0);
        List<String> neatTables = tableNames.get(1);

        String queryTemplate = "DROP TABLE %s CASCADE;";
        Statement stmt = connections.get(0).createStatement();
        for (String tableName : existingTables) {
            if (!neatTables.contains(tableName)) {
                unusedTables.add(tableName);
                String query = String.format(queryTemplate, tableName);
                System.out.println(tableName + " does not exist");
                if(dropResources) {
                    stmt.executeUpdate(query);
                    System.out.println(tableName + " dropped");
                }
            }
        }

        // check whether all the required tables still exist
        List<List<String>> newTableNames = getTableNames(connections);
        for (String tableName : neatTables) {
            if(!newTableNames.get(0).contains(tableName)) {
                System.out.println(tableName + " has been removed");
            }
        }
        stmt.close();
    }

    private static void removeUnusedColumns(List<List<String>> tableNames, List<Connection> connections) throws SQLException {
        for (String table : tableNames.get(1)) {
            String query = "select column_name, data_type from information_schema.columns where table_name = '" + table + "'";
            Statement s1 = connections.get(0).createStatement();
            ResultSet rs1 = s1.executeQuery(query);

            Map<String, String> map1 = new HashMap<>();
            Map<String, String> map2 = new HashMap<>();
            while (rs1.next()) {
                map1.put(rs1.getString(1), rs1.getString(2));
            }
            rs1.close();
            s1.close();

            Statement s2 = connections.get(1).createStatement();
            ResultSet rs2 = s2.executeQuery(query);
            while (rs2.next()) {
                map2.put(rs2.getString(1), rs2.getString(2));
            }
            rs2.close();
            s2.close();

            // check columns
            boolean firstPrint = true;
            // check whether the types of matching columns are the same
            // and check whether there are some redundant columns among the existing columns
            String queryTemplate = "ALTER TABLE %s DROP COLUMN %s";
            Statement stmt = connections.get(0).createStatement();
            for (Map.Entry<String, String> e : map1.entrySet()) {
                if (!map2.containsKey(e.getKey())) {
                    if (firstPrint) {
                        firstPrint = false;
                        System.out.println("\nTABLE: " + table);
                    }
                    System.out.println(e.getKey() + " is redundant");
                    query = String.format(queryTemplate, table, e.getKey());
                    if(dropResources) {
                        stmt.executeUpdate(query);
                        System.out.println(e.getKey() + " dropped");
                    }
                }
            }

            // check whether a required column does not exist among the existing columns
            for (Map.Entry<String, String> e : map2.entrySet()) {
                if (!map1.containsKey(e.getKey())) {
                    if (firstPrint) {
                        firstPrint = false;
                        System.out.println("\nTABLE: " + table);
                    }
                    System.out.println(e.getKey() + " is missing");
                }
            }

            // check the data types of the columns
            for (Map.Entry<String, String> e : map1.entrySet()) {
                if (map2.containsKey(e.getKey())) {
                    if(!e.getValue().contentEquals(map2.get(e.getKey()))) {
                        if (firstPrint) {
                            firstPrint = false;
                            System.out.println("\nTABLE: " + table);
                        }
                        System.out.println(e.getKey() + " has different types");
                    }
                }
            }
        }
    }

    private static void removeUnusedForeignKeyConstraints(List<List<String>> tableNames, List<Connection> connections) throws Exception {
        for(String table : tableNames.get(1)) {
            Map<String, List<String>> foreignKeys1 = new HashMap<>();
            Map<String, List<String>> foreignKeys2 = new HashMap<>();
            String query = "SELECT" +
                    " kcu.column_name," +
                    " ccu.table_name AS foreign_table_name," +
                    " ccu.column_name AS foreign_column_name" +
                    " FROM" +
                    " information_schema.table_constraints AS tc" +
                    " JOIN information_schema.key_column_usage AS kcu" +
                    " ON tc.constraint_name = kcu.constraint_name" +
                    " JOIN information_schema.constraint_column_usage AS ccu" +
                    " ON ccu.constraint_name = tc.constraint_name" +
                    " WHERE constraint_type = 'FOREIGN KEY' AND tc.table_name='" + table + "'";

            Statement s = connections.get(0).createStatement();
            ResultSet rs = s.executeQuery(query);
            while (rs.next()) {
                List<String> fkInfo = new ArrayList<>();
                fkInfo.add(rs.getString(2));
                fkInfo.add(rs.getString(3));
                foreignKeys1.put(table + "-" + rs.getString(1), fkInfo);
            }
            rs.close();
            s.close();

            s = connections.get(1).createStatement();
            rs = s.executeQuery(query);
            while (rs.next()) {
                List<String> fkInfo = new ArrayList<>();
                fkInfo.add(rs.getString(2));
                fkInfo.add(rs.getString(3));
                foreignKeys2.put(table + "-" + rs.getString(1), fkInfo);
            }
            rs.close();
            s.close();

            boolean firstPrint = false;
            for (Map.Entry<String, List<String>> e : foreignKeys1.entrySet()) {
                if (!foreignKeys2.containsKey(e.getKey())) {
                    if(firstPrint) {
                        firstPrint = false;
                        System.out.println("\nTABLE: " + table);
                    }
                    System.out.println(e.getKey() + " is redundant");
                } else {
                    String newReferencedEntity = e.getValue().get(0);
                    String newReferencedColumn = e.getValue().get(1);
                    String oldReferencedEntity = foreignKeys2.get(e.getKey()).get(0);
                    String oldReferencedColumn = foreignKeys2.get(e.getKey()).get(1);

                    if (!(newReferencedEntity.equals(oldReferencedEntity) && newReferencedColumn.equals(oldReferencedColumn))) {
                        if(firstPrint) {
                            firstPrint = false;
                            System.out.println("\nTABLE: " + table);
                        }
                        System.out.println(e.getKey() + " has different referenced entities: " + newReferencedEntity + ", " + newReferencedColumn);
                        System.out.println(e.getKey() + " has different referenced entities: " + oldReferencedEntity + ", " + oldReferencedColumn);
                    }
                }
            }
        }
    }

    private static List<Connection> getConnections() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        List<Connection> connections = new ArrayList<>();
        try {
            connections.add(DriverManager.getConnection(firstDbUrl, firstDbUsername, firstDbPassword));
            connections.add(DriverManager.getConnection(secondDbUrl, secondDbUsername, secondDbPassword));
            return connections;

        } catch (Exception e) {
            closeConnections(connections);
            throw e;
        }
    }

    private static void closeConnections(List<Connection> connections) {
        if (connections.get(0) != null) {
            try {
                connections.get(0).close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (connections.get(1) != null) {
            try {
                connections.get(1).close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
