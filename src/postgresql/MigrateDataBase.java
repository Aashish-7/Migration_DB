package postgresql;

import java.sql.*;

public class MigrateDataBase {
    public static void main(String[] args) throws SQLException {

        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("Opened Database Successfully !!");
            Connection copyDemo = DriverManager.getConnection("jdbc:postgresql://localhost:5432/copydemo", "admin", "ioanyt@123");

            Connection c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/cloud_query", "admin", "ioanyt@123");

              Statement statement = c.createStatement();
              Statement statement1 = copyDemo.createStatement();
              Statement statement2 = c.createStatement();
              String columnWithCd = "cd_id uuid,cd_orgid bigint,cd_snapshot_version bigint,cd_snapshot_timestamp timestamp with time zone,";


              ResultSet rs = statement.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'");
                  while (rs.next()){
                      String tableName = rs.getString("table_name");
                      ResultSet resultSet = statement2.executeQuery("SELECT distinct(column_name), data_type, ordinal_position from information_schema.columns where table_name = "+"'"+tableName +"'"+"order by ordinal_position");
                     String columns ="";
                      while (resultSet.next()) {
                         columns = columns + resultSet.getString("column_name")+" "+resultSet.getString("data_type")+",";
                      }
                      String allColumns = columnWithCd+columns.substring(0,columns.length()-1);

                      String createQuery = "CREATE TABLE IF NOT EXISTS aws."+ tableName+"("+allColumns.replaceAll("ARRAY", "text[]").replaceAll("order", "orders")
                              .replaceAll("primary", "primary_").replaceAll("user", "users")
                              .replaceAll("group", "groups").replaceAll("current_role", "current_roles")+")";
                      statement1.execute(createQuery);
                      System.out.println("Table name : " + tableName);


                  }
                  c.close();
                  copyDemo.close();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName()+ ": " + e.getMessage());
        }
    }
}

