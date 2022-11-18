package postgresql;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class CopyData {
    public String azureTablesDataSync(long orgId, long ssv, Set<String> tableName, Connection conn, Connection connection) throws InterruptedException {
        tableName.remove("azure_hybrid_kubernetes_connected_cluster");
        tableName.forEach(s -> {
            System.out.println("Fetching data for table : " + s);
            String baseQuery = "SELECT distinct(column_name), data_type, ordinal_position from information_schema.columns where table_name=" + "'" + s + "' order by ordinal_position";
            Statement statement;
            try {
                statement = conn.createStatement();
                ResultSet rs = statement.executeQuery(baseQuery);
                String columnWithCd = "cd_id,cd_orgid,cd_snapshot_version,cd_snapshot_timestamp,";
                String columnWithoutCd = "";
                Map<String, String> data1 = new LinkedHashMap<>();
                while (rs.next()) {
                    data1.put(rs.getString(1), rs.getString(2));
                    //System.out.println(rs.getString(1) + "-->" + rs.getString(2));
                    columnWithoutCd = columnWithoutCd + "," + rs.getString(1);
                }
                columnWithoutCd = columnWithoutCd.substring(1);
                String query = "select " + columnWithoutCd + " from "+ s;
                List<String> data = Arrays.asList(columnWithoutCd.split(","));
                ResultSet resultSet = statement.executeQuery(query);
                Statement stmt = connection.createStatement();
                int count = 1;
                while (resultSet.next()) {
                    String info = "";
                    for (int i = 0; i < data1.size(); i++) {
                        if ((data1.get(data.get(i)).equals("text") || data1.get(data.get(i)).equals("jsonb")
                                || data1.get(data.get(i)).equals("inet") || data1.get(data.get(i)).equals("cidr")
                                || data1.get(data.get(i)).equals("timestamp with time zone") || data1.get(data.get(i)).equals("uuid"))
                                && resultSet.getString(data.get(i)) != null) {
                            info = info + "'" + resultSet.getString(data.get(i)).replaceAll("'", "`") + "',";
                        } else {
                            if (data1.get(data.get(i)).equals("boolean")) {
                                if (resultSet.getString(data.get(i)) == null)
                                    info = info + "FALSE,";
                                else if (resultSet.getString(data.get(i)).equals("t"))
                                    info = info + "TRUE,";
                                else if (resultSet.getString(data.get(i)).equals("f"))
                                    info = info + "FALSE,";
                            } else
                                info = info + resultSet.getString(data.get(i)) + ",";
                        }
                    }
                    info = info.substring(0, info.length() - 1);

                    String insertQuery = "INSERT INTO aws." + s + "(" + columnWithCd + columnWithoutCd.replaceAll("ARRAY", "text[]").replaceAll("order", "orders")
                            .replaceAll("primary", "primary_").replaceAll("user", "users")
                            .replaceAll("group", "groups").replaceAll("current_role", "current_roles") + ") VALUES ('"
                            + UUID.randomUUID() + "'," + orgId + "," + ssv + ",'" + LocalDateTime.now() + "'," + info + ")";
                    System.out.println(insertQuery);
                    stmt.execute(insertQuery);
                    System.out.println("Row " + count + " inserted");
                    count++;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            int a = 0;
            try {
                connection.commit();
                a = 1;
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
            System.out.println("Data Fetched for table : " + s);
            System.out.println("_________________________________________________________________________");
        });
        return "Success";
    }

    public static void main(String[] args) {
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("Opened Database Successfully !!");
            Connection copyDemo = DriverManager.getConnection("jdbc:postgresql://localhost:5432/copydemo", "admin", "ioanyt@123");
            Connection c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/cloud_query", "admin", "ioanyt@123");
            Statement statement = c.createStatement();
            ResultSet rs = statement.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'");
            Set<String> tableName = new HashSet<>();
            while (rs.next()) {
                tableName.add(rs.getString("table_name"));
            }
            CopyData copyData = new CopyData();
            copyData.azureTablesDataSync(1,1,tableName,c,copyDemo);
        } catch (SQLException | ClassNotFoundException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
