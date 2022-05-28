package Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import javax.sound.sampled.Line;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class DdFormat {
    String url, user, password;
    static String[] dataTypes = new String[]{"Type", "gte", "lte", "Values"};
    String id;
    JDBConnection jdbConnection;

    public static void main(String[] args) throws SQLException {
        String url = "jdbc:mysql://localhost:3306/tsgen?serverTimezone=UTC&useUnicode=true&zeroDateTimeBehavior=convertToNull&autoReconnect=true&characterEncoding=utf-8";
        String user = "root";
        String password = "password";
        DdFormat df = new DdFormat("2", url, user, password);
        JSONObject data = df.projectJson();
        System.out.println(data.toJSONString());
        df.jdbConnection.closeConnection();
    }

    public DdFormat(String id, String url, String user, String password) {
        this.id = id;
        this.url = url;
        this.user = user;
        this.password = password;
        jdbConnection = new JDBConnection(url, user, password);
    }
    public String projectName() throws SQLException {
        JSONObject rsObject = new JSONObject(true);
        ResultSet rs = jdbConnection.executeQuery("select Name from model where id = ?", new String[]{id});
        String projectName = new String();
        if (rs.next()) {
            projectName = rs.getString("Name");
        }
        return projectName;
    }

    public JSONObject projectJson() throws SQLException {
        JSONObject rsObject = new JSONObject(true);
        ResultSet rs = jdbConnection.executeQuery("select Name,classname,tWay from model where id = ?", new String[]{id});
        if (rs.next()) {
            String projectName = rs.getString("Name");
            String classname = rs.getString("classname");
            String tWay = rs.getString("tWay");
            JSONObject inputJson = getInputJson(classname,projectName);
            JSONArray coverJson = getConsJson(projectName, "coverages");
            JSONObject outputJson = getOutputJson(classname,projectName);
            rsObject.put("tWay", tWay);
            rsObject.put("Input", inputJson);//输入参数
            rsObject.put("Coverage", coverJson);
            rsObject.put("Output", outputJson);
        }
        return rsObject;
    }

    private JSONObject getInputJson(String classname,String projectName) throws SQLException {
        JSONObject inputJson = new JSONObject(true);
        // 项目名称
        inputJson.put("Name", projectName);
        // 输入参数
        JSONArray inputParas = new JSONArray();
        String inputParaSQL = "select Name, DataType, `Empty`, Lost from input_paras where projectName = ?";
        ResultSet inputRs = jdbConnection.executeQuery(inputParaSQL, new String[]{projectName});
        while (inputRs.next()) {
            JSONObject paraJson = new JSONObject(true);
            paraJson.put("Name", inputRs.getString("Name"));
            paraJson.put("DataType", getDftJson(classname,inputRs.getString("DataType")));
            paraJson.put("Empty", inputRs.getBoolean("Empty"));
            paraJson.put("Lost", inputRs.getBoolean("Lost"));
            inputParas.add(paraJson);
        }
        // 将参数插入输入参数
        inputJson.put("Paras", inputParas);
        // 约束
        JSONArray consJson = getConsJson(projectName, "constraints");
        // 将约束放入输入中
        inputJson.put("Cons", consJson);
        return inputJson;
    }

    private JSONArray getConsJson(String projectName, String tableName) throws SQLException {
        // 约束
        JSONArray consJson = new JSONArray();
        String commonConSQL = "select Rules,ruleGroup from " + tableName + " where projectName = ?";
        ResultSet conRs = jdbConnection.executeQuery(commonConSQL, new String[]{projectName});
        LinkedHashMap<Integer, ArrayList<String>> rulesMap = new LinkedHashMap<>();
        HashMap<Integer, String> Infos = new HashMap<>();
        while (conRs.next()) {
            int classId = conRs.getInt("ruleGroup");
            ResultSet InfoRs = jdbConnection.executeQuery("select ruleGroup, Info from Infos where ruleGroup = ?", new String[]{String.valueOf(classId)});
            String info = "";
            int index=0;
            if (InfoRs.next()) {
                info = InfoRs.getString("Info");
                Infos.put(classId, InfoRs.getString("Info"));
            }
            String value = conRs.getString("Rules");
            ArrayList<String> ru;
            // 分别加入一般类约束 Rules 和规则类约束 DbRules
            ru = rulesMap.containsKey(classId) ? rulesMap.get(classId) : new ArrayList<>();
            ru.add(value);
            rulesMap.put(classId, ru);
        }
        putCons2Json(rulesMap, Infos, consJson, "Rules");
        return consJson;
    }

    private void putCons2Json(LinkedHashMap<Integer, ArrayList<String>> rulesMap, HashMap<Integer, String> Infos, JSONArray consJson, String name) {
        for (int classId : rulesMap.keySet()) {
            JSONObject rulesJson = new JSONObject(true);
            JSONArray rules = new JSONArray();
            rules.addAll(rulesMap.get(classId));
            if (!rules.isEmpty()) {
                rulesJson.put(name, rules);
                if (Infos.containsKey(classId)) {
                    rulesJson.put("Info", Infos.get(classId));
                }
                consJson.add(rulesJson);
            }
        }
    }

    JSONObject getOutputJson(String classname,String projectName) throws SQLException {
        JSONObject outputJson = new JSONObject(true);
        // 输出参数数组
        JSONArray outputParas = new JSONArray();
        String inputParaSQL = "select Name, DataType, `Empty`, Lost from output_paras where projectName = ?";
        ResultSet inputRs = jdbConnection.executeQuery(inputParaSQL, new String[]{projectName});
        while (inputRs.next()) {
            JSONObject paraJson = new JSONObject(true);
            paraJson.put("Name", inputRs.getString("Name"));
            paraJson.put("DataType", getDftJson(classname,inputRs.getString("DataType")));
            boolean empty = "1".equals(inputRs.getString("Empty"));
            paraJson.put("Empty", empty);
            boolean lost = "1".equals(inputRs.getString("Lost"));
            paraJson.put("Lost", lost);
            outputParas.add(paraJson);
        }
        // 将参数插入输出的 Json
        outputJson.put("Paras", outputParas);
        JSONArray assertionJson = getConsJson(projectName, "assertions");
        outputJson.put("Assertion", assertionJson);
        JSONArray metamorphicJson = getConsJson(projectName, "metamorphics");
        outputJson.put("Metamorphic", metamorphicJson);
        return outputJson;
    }

    private JSONObject getDftJson(String classname,String typeName) throws SQLException {
        String sql = "select ";
        for (int i = 0; i < dataTypes.length - 1; i++) {
            sql += "`" + dataTypes[i] + "`, ";
        }
        sql += "`" + dataTypes[dataTypes.length - 1] + "` from dataType where classname = ? and typeName = ?";
        JSONObject dataTypeJson = new JSONObject(true);
        ResultSet resultSet = jdbConnection.executeQuery(sql, new String[]{classname,typeName});
        if (resultSet.next()) {
            for (String property : dataTypes) {
                // 拿到 dataType 表对应的参数值
                String value = resultSet.getString(property);
                if (value == null) {
                    continue;
                }
                // 如果是枚举型，Values 的值不为空
                if ("Values".equals(property)) {
                    ResultSet rs = jdbConnection.executeQuery("select value from `values` where classname =? and name = ?", new String[]{classname,value});
                    ArrayList<String> values = new ArrayList<>();
                    while (rs.next()) {
                        values.add(rs.getString("value"));
                    }
                    JSONArray valuesJson = new JSONArray();
                    valuesJson.addAll(values);
                    dataTypeJson.put("Values", valuesJson);
                } else {
                    dataTypeJson.put(property, value);
                }
            }
        }
        return dataTypeJson;
    }

    public JDBConnection getJdbConnection() {
        return jdbConnection;
    }
}
