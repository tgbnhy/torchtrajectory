package au.edu.rmit.bdm.Torch.base.db;

import au.edu.rmit.bdm.Torch.base.FileSetting;
import au.edu.rmit.bdm.Torch.base.helper.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.sql.*;

public class DBManager {

    private static Logger logger = LoggerFactory.getLogger(DBManager.class);
    private Connection conn;
    private FileSetting setting;

    public DBManager(FileSetting setting) {
        this.setting = setting;
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            logger.error("cannot find jdbc-sqlite driver!");
        }
        connect();
    }

    public static void main(String[] args) throws IOException {
        FileSetting setting = new FileSetting("Torch_nantong");
        DBManager db = new DBManager(setting);
        db.buildFromFile(setting.TRAJECTORY_EDGE_TABLE, setting.TRAJECTORY_EDGE_REPRESENTATION_PATH_PARTIAL, true);
        db.buildFromFile(setting.TRAJECTORY_VERTEX_TABLE, setting.TRAJECTORY_VERTEX_REPRESENTATION_PATH_PARTIAL, true);
    }

    public void buildTable(String tableName, boolean override){
        // SQL statement for creating a new table
        String sql;
        if (override) {

            sql = "DROP TABLE IF EXISTS '" + tableName+"';";
            try {
                Statement stmt = conn.createStatement();
                stmt.executeUpdate(sql);
            } catch (SQLException e) {
                logger.error(e.getMessage());
                logger.error("cannot delete table");
                System.exit(-1);
            }
        }

        sql = "CREATE TABLE " + tableName + " (\n"
                + "	id text PRIMARY KEY,\n"
                + "	content text NOT NULL\n"
                + ");";

        // create a new table
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            logger.error(e.getMessage());
            logger.error("cannot create table");
            System.exit(-1);
        }
    }

    public DBManager buildFromFile(String tableName, String path2file, boolean override) {

        connect();
        buildTable(tableName, override);

        //insert all records
        try(FileReader fr = new FileReader(path2file);
            BufferedReader reader = new BufferedReader(fr)){

            String line;
            int counter = 0;
            while((line = reader.readLine())!=null) {
                if (counter++ %2000 == 0)
                    logger.info("has insert "+counter+" records into db");
                String[] tokens = line.split("\t");
                String id = tokens[0];
                String content = tokens[1];

                insert(tableName, Integer.parseInt(id), content);
            }

        }catch (IOException e){
            logger.error(e.getMessage());
            logger.error("cannot find "+path2file);
            System.exit(-1);
        }

        return this;
    }

    public void insert(String tableName, Integer id, String content){
        connect();

        String sql = "INSERT INTO " + tableName + "(id,content) VALUES("+id+",'"+content+"')";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeUpdate();

        } catch (SQLException e) {
            logger.error(e.getMessage());
            logger.error("cannot insert record: id={}, content:{}"+id, content);
            System.exit(-1);
        }
    }

    public void insert(String tableName, String name, String content){
        connect();

        String sql = "INSERT INTO " + tableName + "(name,content) VALUES(?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setString(2, content);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error(e.getMessage());
            logger.error("cannot insert record: id={}, content:{}"+name, content);
        }
    }

    public DBManager connect(){
        if (conn != null) return this;
        FileUtil.ensureExistence(setting.DB_URL.split(":")[2]);
        try {
            String url = setting.DB_URL;
            conn = DriverManager.getConnection(url);
            logger.info("connection to sqlite succeeds");
        } catch (SQLException e) {
            logger.error(e.getMessage());
            logger.error("cannot init connection for sqlite, system on exit");
            System.exit(-1);
        }
        return this;
    }

    public void closeConn(){

        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException ex) {
            logger.error(ex.getMessage());
            System.exit(-1);
        }
    }

//    public int[] getEdgeRepresentation(String trajId){
//        return get(FileSetting.fileSetting.TRAJECTORY_EDGE_TABLE, trajId);
//    }
//
//    public int[] getVertexRepresentation(String trajId){
//        return get(FileSetting.fileSetting.TRAJECTORY_VERTEX_TABLE, trajId);
//    }

    public String get(String table, int key) {
        return get(table,String.valueOf(key));
    }

    public String get(String table, String val) {
        if (conn == null) throw new IllegalStateException("do not have sqlite connection");
        String attr;
        if (table.equals(setting.EDGENAME_ID_TABLE))
            attr = "name";
        else
            attr = "id";

        String sql = "SELECT content from " + table + " WHERE "+attr+ " = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            //pstmt.setString(1, attr);
            pstmt.setString(1, val);
            ResultSet rs = pstmt.executeQuery();
            if (rs.isClosed()) {
                System.out.println("no result is found!");
                return null;
            }
            String ret = rs.getString(1);
            rs.close();
            return ret;
        } catch (SQLException e) {
            throw new IllegalStateException(e.getMessage());

        }
    }
}
