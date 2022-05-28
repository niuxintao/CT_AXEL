package Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;

import org.apache.commons.lang.RandomStringUtils;

public class DataType {
    /**
     * 数据类型：typeFormat定义优先
     */
    Data datatype;
    String type;
    String typeFormat;
    String gte;
    String lte;
    /**
     * lb: 下界
     * ub: 上界
     */
    Number lb;
    Number ub;


    /**
     * 枚举型
     */
    String[] include;

    DataType() {
    }

    public Data getDataType() {
        return datatype;
    }

    public Number getLb() {
        return lb;
    }

    public Number getUb() {
        return ub;
    }

    public String[] getInclude() {
        return include;
    }

    public String getGte() {
        return gte;
    }

    public String getLte() {
        return lte;
    }

    public String getType() {
        return type;
    }


    public void setType(String type) {
        this.type = type;
    }

    public void setTypeFormat(String typeFormat) {
        this.typeFormat = typeFormat;
    }

    public void setGte(String gte) {
        this.gte = gte;
        if (datatype == Data.NUMBER) {
            setLb(gte);
        }
    }

    public void setLte(String lte) {
        this.lte = lte;
        if (datatype == Data.NUMBER ) {
            setUb(lte);
        }
    }


    public void setDatatype(Data datatype) {
        this.datatype = datatype;
    }

    public void setInclude(String[] include) {
        this.include = include;
    }

    public void setLb(String lb) {
        Number ub = null;
        if ("int".equals(type)) {
            this.lb = Integer.parseInt(lb);
            ub = Integer.parseInt(lb) + 1000;
        } else {
            System.err.println("There is not " + type + " type exists !!!");
        }
        if (this.ub == null) {
            this.ub = ub;
        }
    }

    public void setUb(String ub) {
        Number lb = null;
        if ("int".equals(type)) {
            this.ub = Integer.parseInt(ub);
            lb = Integer.parseInt(ub) - 1000;
        } else {
            this.ub = 0;
            System.err.println("There is not " + type + " type exists !!!");
        }
        if (this.lb == null) {
            this.lb = lb;
        }
    }
}