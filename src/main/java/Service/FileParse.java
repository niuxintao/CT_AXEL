package Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;

public interface FileParse {
    /**
     * This interface is aim to parse parameters in input.json
     *
     * @return Parameter[]
     * @throws IOException file exception
     */
    static Parameter[] parasParse(JSONObject jsonObject) throws Exception {
        JSONArray paras = jsonObject.getJSONArray("Paras");
        Parameter[] parameters = new Parameter[paras.size()];
        for (int i = 0; i < paras.size(); i++) {
            parameters[i] = getParameter(paras.getJSONObject(i));
        }
        return parameters;
    }

    static Parameter getParameter(JSONObject jsObject) throws Exception {
        Parameter parameter = new Parameter();
        parameter.setName(jsObject.getString("Name"));
        if (jsObject.containsKey("Empty")) {
            parameter.setEmpty(jsObject.getBoolean("Empty"));
        }
        if (jsObject.containsKey("Lost")) {
            parameter.setLost(jsObject.getBoolean("Lost"));
        }
        if (jsObject.containsKey("Contains")) {
            JSONArray jsonContains = jsObject.getJSONArray("Contains");
            String[] c = new String[jsonContains.size()];
            for (int j = 0; j < jsonContains.size(); j++) {
                c[j] = jsonContains.get(j).toString();
            }
            parameter.setContains(c);
        }
        if (jsObject.containsKey("Location")) {
            parameter.setLocation(jsObject.getString("Location"));
        }
        JSONObject jsonDataType = jsObject.getJSONObject("DataType");
        DataType dataType = new DataType();
        // must have the Type key
        switch (jsonDataType.getString("Type")) {
            case "boolean":
            case "Boolean":
            case "bool":
                dataType.setDatatype(Data.BOOL);
                dataType.setType("bool");
                break;
            case "int":
                dataType.setDatatype(Data.NUMBER);
                dataType.setType(jsonDataType.getString("Type"));
                break;
            case "enum":
                dataType.setDatatype(Data.ENUM);
                dataType.setType("enum");
                String[] c = new String[jsonDataType.getJSONArray("Values").size()];
                for (int k = 0; k < jsonDataType.getJSONArray("Values").size(); k++) {
                    c[k] = jsonDataType.getJSONArray("Values").get(k).toString();
                }
                dataType.setInclude(c);
                break;
            default:
                throw new Exception("Type" + jsonDataType.getString("Type") + "is not defined!");
        }

        if (jsonDataType.containsKey("gte")) {
            dataType.setGte(jsonDataType.getString("gte"));
        }
        if (jsonDataType.containsKey("lte")) {
            dataType.setLte(jsonDataType.getString("lte"));
        }

        parameter.setDataTypes(dataType);
        return parameter;
    }


    /**
     * This interface is aim to parse parameters in input.json
     *
     * @return Parameter[]
     * @throws IOException file exception
     */
    static Cons[] consParse(JSONObject jsonObject) throws IOException {
        // path = "/User/ligang/model.json"
//        File filename = new File(path);
//        String file = FileUtils.readFileToString(filename, "utf-8");
//        JSONObject jsonObject = JSON.parseObject(file);
        JSONArray consJson = jsonObject.getJSONArray("Cons");
        Cons[] cons = new Cons[consJson.size()];

        for (int i = 0; i < consJson.size(); i++) {
            cons[i] = getConstraint(consJson.getJSONObject(i));
        }
        return cons;
    }

    static Cons[] coverageParse(JSONArray jsonCoverages) throws Exception {
        if (jsonCoverages.size() == 0) {
            Cons[] covs = new Cons[1];
            covs[0] = new Cons();
            covs[0].setInfo("random solution");
            covs[0].setRules(null);
            return covs;
        }
        Cons[] covs = new Cons[jsonCoverages.size()];
        for (int i = 0; i < jsonCoverages.size(); i++) {
            covs[i] = getConstraint(jsonCoverages.getJSONObject(i));
        }
        return covs;
    }

    static Cons getConstraint(JSONObject jsObject) {
        Cons con = new Cons();
        if (jsObject.containsKey("Rules")) {
            JSONArray rulesJson = jsObject.getJSONArray("Rules");
            // Rules 可能为 null
            if (rulesJson != null) {
                String[] rs = new String[rulesJson.size()];
                for (int j = 0; j < rulesJson.size(); j++) {
                    rs[j] = rulesJson.get(j).toString();
                }
                con.setRules(rs);
            }
        }
        if (jsObject.containsKey("Info")) {
            con.setInfo(jsObject.getString("Info"));
        }
        return con;
    }

    static Parameter[] outputParasParse(JSONObject jsonObject) throws Exception {
        JSONArray parasArray = jsonObject.getJSONArray("Paras");
        Parameter[] paras = new Parameter[parasArray.size()];
        int index = 0;
        for (int i = 0; i < parasArray.size(); i++) {
            if (parasArray.getJSONObject(i).getString("Name").equals("statusCode"))
                paras[index++] = getParameter(parasArray.getJSONObject(i));
        }
        for (int i = 0; i < parasArray.size(); i++) {
            if (!parasArray.getJSONObject(i).getString("Name").equals("statusCode"))
                paras[index++] = getParameter(parasArray.getJSONObject(i));
        }
        return paras;
    }

    default Cons[] outputConsParse(JSONObject jsonObject) throws Exception {
        JSONArray assertArray = jsonObject.getJSONArray("Assertion");
        if(assertArray.size()==0){
            Cons[] covs = new Cons[1];
            covs[0] = new Cons();
            covs[0].setInfo("assertion");
            covs[0].setRules(null);
            return covs;
        }
        Cons[] cons = new Cons[assertArray.size()];
        for (int i = 0; i < cons.length; i++) {
            cons[i] = getConstraint(assertArray.getJSONObject(i));
        }
        return cons;
    }

    default Cons[] metamorphicsParse(JSONObject jsonObject) throws Exception {
        JSONArray metaArray = jsonObject.getJSONArray("Metamorphic");
        if(metaArray.size()==0){
            Cons[] covs = new Cons[1];
            covs[0] = new Cons();
            covs[0].setInfo("Metamorphic");
            covs[0].setRules(null);
            return covs;
        }
        Cons[] cons = new Cons[metaArray.size()];
        for (int i = 0; i < cons.length; i++) {
            cons[i] = getConstraint(metaArray.getJSONObject(i));
        }
        return cons;
    }
}
