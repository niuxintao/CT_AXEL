package Service;

import java.sql.*;

public class JDBConnection {
    private Connection connection = null;
    String url;
    String userName;
    String password;

    /**
     * 通过构造方法加载数据库驱动
     *
     * @param url      数据库地址
     * @param userName 用户名
     * @param password 密码
     */
    public JDBConnection(String url, String userName, String password) {
        try {
            this.url = url;
            this.userName = userName;
            this.password = password;
            String dbDrive = "com.mysql.cj.jdbc.Driver";
            Class.forName(dbDrive).newInstance();
        } catch (Exception ex) {
            System.out.println("数据库加载失败");
        }
    }

    /**
     * 创建数据库连接
     *
     * @return 是否创建成功
     */
    public boolean creatConnection() {
        try {
            connection = DriverManager.getConnection(url, userName, password);
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            System.out.println("creatConnectionError!");
        }
        return true;
    }

    /**
     * 对数据库的增加、修改和删除的操作
     *
     * @param sql sql语句
     * @return 返回是否执行成功
     */
    public boolean executeUpdate(String sql, String[] params) {
        if (connection == null) {
            creatConnection();
        }
        try {
            PreparedStatement preState = connection.prepareStatement(sql);
            for (int i = 0; params != null && i < params.length; i++) {
                // 参数替换，防止 sql 注入
                preState.setString(i + 1, params[i]);
            }
            int iCount = preState.executeUpdate();
            System.out.println("操作成功，output 记录增" + iCount);
            return true;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    /**
     * 对数据库的查询操作
     *
     * @param sql sql语句
     * @param params 参数替换
     * @return 返回结果
     */
    public ResultSet executeQuery(String sql, String[] params) {
        ResultSet rs;
        try {
            if (connection == null) {
                // 创建连接
                creatConnection();
            }
            PreparedStatement preState =
                    connection.prepareStatement(sql,ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY );
            for (int i = 0; params != null && i < params.length; i++) {
                // 参数替换，防止 sql 注入
                preState.setString(i + 1, params[i]);
            }
            try {
                rs = preState.executeQuery();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
                return null;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            System.out.println("executeQueryError!");
            return null;
        }
        return rs;
    }

    /**
     * 关闭数据库
     */
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
