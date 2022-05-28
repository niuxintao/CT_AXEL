package Service;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.lang3.RandomStringUtils;

public class Parameter {
    /**
     * Number/Boolean 这两种类型可直接用来建 choco 的参数（intVar 和 boolVar）；
     * enum 型也被看成 int 型，范围为 [0， length - 1]
     */
    private String name;
    private String[] contains;
    private DataType type;
    private Boolean empty;
    private Boolean lost;
    private String Location;
    private int size;
    private final HashMap<Integer, String> cons;
    private final HashMap<String, Integer> reverseCons;

    public final String LOST = "lost";
    public final String EMPTY = "null";

    Parameter() {
        cons = new HashMap<>();
        reverseCons = new HashMap<>();
    }

    public HashMap<Integer, String> getCons() {
        return cons;
    }

    public HashMap<String, Integer> getReverseCons() {
        return reverseCons;
    }

    public ArrayList<String> getBoundaryValue(Boolean valid) throws Xeger.FailedRandomWalkException {
        ArrayList<String> values = new ArrayList<>();
        //System.out.println("There contains no boundary value in type " + getType().getDataType() + "!");
        if (getType().getDataType() == Data.NUMBER) {
                // 生成长度一定的字符串
                if (getType().getLte() != null) {
                    int len = Integer.parseInt(getType().getLte());
                    String result = valid ? String.valueOf(len) : String.valueOf(len + 1);
                    values.add(result);
                }
                // 生成长度一定的字符串
                if (getType().getGte() != null) {
                    int len = Integer.parseInt(getType().getGte());
                    String result = valid ? String.valueOf(len) : String.valueOf(len - 1);
                    values.add(result);
            }
        }
        return values;
    }


    public String getLocation() {
        return Location;
    }

    public String getName() {
        return this.name;
    }

    public Boolean getEmpty() {
        return empty;
    }

    public Boolean getLost() {
        return lost;
    }

    public DataType getType() {
        return type;
    }

    public String[] getContains() {
        return contains;
    }

    public int getSize() {
        switch (getType().getDataType()) {
            case ENUM:
                return getType().getInclude().length;
            case NUMBER:
                return getType().getUb().intValue() - getType().getLb().intValue() + 1;
            case BOOL:
                return 2;
            default:
                return 0;
        }
    }

    public void addCons(int key, String cons) {
        this.cons.put(key, cons);
    }

    public void addRcons(String key, int value) {
        this.reverseCons.put(key, value);
    }

    public void setLocation(String location) {
        Location = location;
    }

    public void setType(DataType type) {
        this.type = type;
    }

    public void setEmpty(Boolean empty) {
        this.empty = empty;
    }

    public void setLost(Boolean lost) {
        this.lost = lost;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setContains(String[] contains) {
        this.contains = contains;
    }

    public void setDataTypes(DataType dataType) {
        this.type = dataType;
    }
}
