package Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import edu.uta.cse.fireeye.common.SUT;
import edu.uta.cse.fireeye.common.TestGenProfile;
import edu.uta.cse.fireeye.common.TestSet;
import edu.uta.cse.fireeye.common.TestSetWrapper;
import edu.uta.cse.fireeye.service.engine.IpoEngine;
import edu.uta.cse.fireeye.service.engine.SUTInfoReader;
import metaMorphic.MetaRelation;
import metaMorphic.TriMCT;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.expression.discrete.arithmetic.ArExpression;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.RealVar;
import org.chocosolver.solver.variables.Variable;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class uModel implements FileParse {
    public Model model;
    int redundancy = 0;
    int tWay = 2;
    StringBuilder queryInfo;
    int[][] combTs;
    ArrayList<ArrayList<Solution>> metamorphicSolution;
    ArrayList<ArrayList<String>> metamorphicSolutionInfo;
    ArrayList<Solution> normalSolutions;
    ArrayList<Solution> oracles;
    ArrayList<Solution> totalNormalSolutions;
    ArrayList<Solution> totalOracles;
    // 正例信息 <i, info> i为
    ArrayList<String> caseInfo;
    public final double precision = 1e-2;
    public Parameter[] inputParas;
    public Variable[] inputVars;
    public Variable[] outputVars;
    Parameter[] outputParas;
    public Cons[] cons;                        //constraints
    Cons[] coverages;
    Cons[] outputCons;                  //assertion
    Cons[] metamorphics;
    Cons[] setCons;                     //for combination set
    ArrayList<ArrayList<Cons>> metamorphicsConSet;             //for metamorphics result
    HashMap<String, Variable> variableHashMap;
    public HashMap<String, Parameter> parameterHashMap;
    HashMap<String, Parameter> stringParameter;
    /**
     * parasNum: 输入参数的个数
     * totalParasNum: 输入参数加输出参数的个数
     */
    int parasNum;
    int totalParasNum;
    /**
     * parasNumWithCons: 加入约束后参数的个数
     * conNum: 加入约束后，约束的个数
     */
    int parasNumWithCons;
    int consNum;
    /**
     * It's value determines what changeData function should do
     */
    Boolean switchCons;
    JSONObject  jsonObject;
    public void setCoverages(Cons[] coverages) {
        this.coverages = coverages;
    }

    public uModel(JSONObject jsonObject) {
        try {
            this.jsonObject = jsonObject;
            queryInfo = new StringBuilder();
            caseInfo = new ArrayList<>();
            tWay = Integer.parseInt(jsonObject.get("tWay").toString());
            JSONObject inputJson = jsonObject.getJSONObject("Input");//输入参数和constraints
            JSONArray coverageJson = jsonObject.getJSONArray("Coverage");//covarage
            JSONObject outputJson = jsonObject.getJSONObject("Output");  //输出参数和assertion
            switchCons = true;
            model = new Model(inputJson.get("Name").toString());
            inputParas = FileParse.parasParse(inputJson);
            outputParas = FileParse.outputParasParse(outputJson);
            cons = FileParse.consParse(inputJson);          //constraints
            coverages = FileParse.coverageParse(coverageJson);
            outputCons = outputConsParse(outputJson);       //assertion
            metamorphics = metamorphicsParse(outputJson);   //metamorphics
            variableHashMap = new HashMap<>();
            parameterHashMap = new HashMap<>();
            stringParameter = new HashMap<>();
            normalSolutions = new ArrayList<>();
            metamorphicSolution = new ArrayList<>();
            totalNormalSolutions = new ArrayList<>();
            totalOracles = new ArrayList<>();
            // 为 model 增加参数，初始化参数个数
            inputVars = variablesMatch(inputParas);
            parasNum = parameterHashMap.size();
            // 将输出参数放进模型中，初始化参数总数
            outputVars = variablesMatch(outputParas);
            totalParasNum = parameterHashMap.size();
            // 为 model 增加约束，用参数个数和约束个数记录 model 的初始化状态
            addConstraints();
            parasNumWithCons = model.getNbVars();
            consNum = model.getNbCstrs();
            // 用 Coverage 来决定需要哪些测试用例
            addCoverage(true);

            //在coverage的基础上实现 t 维覆盖
            addCombinationCoverage();

            // 添加MR
            addTriMCT();

            // 更新解的 Oracle 信息
            oracles = new ArrayList<>();
            updateOracles();
            if (!totalOracles.isEmpty()) {
                queryInfo.append("redundancy=").append(redundancy / totalOracles.size() - 1);
            } else {
                totalOracles.add(normalSolutions.get(0));
                oracles.add(normalSolutions.get(0));
                queryInfo.append("redundancy=0");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Variable[] variablesMatch(Parameter[] parameters) {
        Variable[] vars = new Variable[parameters.length];
        int varCount = 0;
        for (Parameter para : parameters) {
            String name = para.getName();
            DataType dataType = para.getType();
            if (!stringParameter.containsKey(para.getType().typeFormat)) {
                Parameter p = new Parameter();
                p.setName(para.getType().typeFormat);
                stringParameter.put(para.getType().typeFormat, new Parameter());
            }
            Variable var;
            switch (dataType.getDataType()) {
                case NUMBER:
                    if ("double".equals(dataType.getType()) ) {
                        var = model.realVar(name, (double) dataType.getLb(), (double) dataType.getUb(), precision);
//                        var = model.realVar(name, (double) dataType.getLb(), (double) dataType.getUb(), precision);
                    } else if ("float".equals(dataType.getType())) {
                        var = model.realVar(name, dataType.getLb().doubleValue(), dataType.getUb().doubleValue(), precision);
                    } else {
                        var = model.intVar(name, (Integer) dataType.getLb(), (Integer) dataType.getUb());
                    }
                    break;
                case ENUM:
                    var = model.intVar(name, 0, dataType.getInclude().length - 1);
                    break;
                case BOOL:
                    var = model.boolVar(name);
                    break;
                default:
                    var = null;
                    System.err.println("Error!");
                    System.exit(-1);
            }
            vars[varCount] = var;
            varCount++;
            // 存 choco 参数的映射
            variableHashMap.put(name, var);
            // 存 Parameter 参数的映射
            parameterHashMap.put(name, para);
        }
        return vars;
    }

    void addConstraints() {
        for (Cons c : cons) {
            putConsToModel(c);
        }
    }

    private ArExpression Arinit(Object a,  String o ,Object b) {
        if (b instanceof Variable) {
            switch (o) {
                case "+":
                    return ((IntVar) a).add((IntVar) b);
                case "-":
                    return ((IntVar) a).sub((IntVar) b);
                case "*":
                    return ((IntVar) a).mul((IntVar) b);
                case "/":
                    return ((IntVar) a).div((IntVar) b);
            }
        }else{
            switch (o) {
                case "+":
                    return ((IntVar) a).add((Integer)b);
                case "-":
                    return ((IntVar) a).sub((Integer)b);
                case "*":
                    return ((IntVar) a).mul((Integer)b);
                case "/":
                    return ((IntVar) a).div((Integer)b);
            }
        }
        return null;
    }

    private ArExpression ArExtention(ArExpression temp, String operation, Object value) {
        if (value instanceof Variable) {
            switch (operation){
                case "+":
                    return temp.add((IntVar)value);
                case "-":
                    return temp.sub((IntVar)value);
                case "*":
                    return temp.mul((IntVar)value);
                case "/":
                    return temp.div((IntVar)value);
            }
        }else{
            switch (operation){
                case "+":
                    return temp.add((Integer)value);
                case "-":
                    return temp.sub((Integer)value);
                case "*":
                    return temp.mul((Integer)value);
                case "/":
                    return temp.div((Integer)value);
            }
        }
        return null;
    }
    private ArExpression ArExtention(Object value, String operation, ArExpression temp) {
        if (value instanceof Variable) {
            switch (operation){
                case "+":
                    return ((IntVar) value).add(temp);
                case "-":
                    return ((IntVar) value).sub(temp);
                case "*":
                    return ((IntVar) value).mul(temp);
                case "/":
                    return ((IntVar) value).div(temp);
            }
        }else{
            System.err.println("表达式不可以以常数作为开始");
//            switch (operation){
//                case "+":
//                    return temp.add((Integer)value);
//                case "-":
//                    return temp.sub((Integer)value);
//                case "*":
//                    return temp.mul((Integer)value);
//                case "/":
//                    return temp.div((Integer)value);
//            }
        }
        return null;
    }

    private Constraint ArLast(ArExpression temp, String operation, Object value) {
        if (value instanceof Variable) {
            switch (operation) {
                case "=":
                    return temp.eq((IntVar) value).decompose();
                case "!=":
                    return temp.ne((IntVar) value).decompose();
                case "<":
                    return temp.lt((IntVar) value).decompose();
                case ">":
                    return temp.gt((IntVar) value).decompose();
                case "<=":
                    return temp.le((IntVar) value).decompose();
                case ">=":
                    return temp.ge((IntVar) value).decompose();
            }
        }else{
            switch (operation) {
                case "=":
                    return temp.eq((Integer) value).decompose();
                case "!=":
                    return temp.ne((Integer) value).decompose();
                case "<":
                    return temp.lt((Integer) value).decompose();
                case ">":
                    return temp.gt((Integer) value).decompose();
                case "<=":
                    return temp.le((Integer) value).decompose();
                case ">=":
                    return temp.ge((Integer) value).decompose();
            }
        }
        return null;
    }
    /**
     * 将 str 里的约束解析出来给 model 增加约束
     *
     * @param str 输入的约束表达式，如 "a = b && b != c"
     * @return 装类 Service.meteData 的栈
     */
    public void setCons(String str) {
        if (str.isEmpty()) {
            return;
        }
        //约束str若存在浮点数区间 将区间变成值
        final String regexSection = "\\[\\S+\\]";  //选取[double,double]
        final Pattern patternSection = Pattern.compile(regexSection, Pattern.MULTILINE);
        Matcher matcherSection = patternSection.matcher(str);
        while (matcherSection.find()) {
            String doubleSection = matcherSection.group(0);   //[double,double]
            String twoNums = doubleSection.substring(1, doubleSection.length() - 1);//double,double
            String[] doubles = twoNums.split(",");//double double
            DecimalFormat df = new DecimalFormat("#0.00");
            String result = df.format((Double.parseDouble(doubles[0]) + Double.parseDouble(doubles[1])) / 2);
            str = str.replace(doubleSection, result); //[double,double]=>(double+double/2)
            switchCons = true;
        }
        ArrayList<String> output; //输出结果
        ArrayList<String> input = ReversePolishNotation.cons2Strs(str);
        ArrayList<String> tempList = new ArrayList<>();
        for (String s : input) {
            tempList.add(s.replace("\"", ""));
        }
        output = ReversePolishNotation.rpn(tempList);
//        System.out.println("\n----------------------");
//        for(int i = 0 ; i<output.size();i++){
//            System.out.print(output.get(i));
//        }
//        System.out.println("\n----------------------");
        Stack<meteData> stack = new Stack<>();
        int i = 0;
//        System.out.println(str);
//        for (String out : output) {
//            System.out.print(out+" ");
//        }
//        System.out.println();
        while (i < output.size()) {
            String o = output.get(i++);
            // 如果o是操作符
            if (Operator.isOperator(o)) {
                meteData metaOp = new meteData();
                // 单目运算符
                if ("!".equals(o)) {
                    metaOp = stack.pop();
                    try {
                        if (metaOp.type == meteData.Type.VAR) {
                            metaOp.setVar(metaOp.getVar().asBoolVar().not());
                        } else if (metaOp.type == meteData.Type.CON) {
                            metaOp.setCons(metaOp.getCons().getOpposite());
                        }
                        stack.push(metaOp);
                    } catch (Exception ignored) {
                        System.err.println("The format of '!' is not true.");
                        System.exit(-1);
                    }
                    continue;
                }
                // 双目运算符
                // 后出栈的反而在前面
                meteData metaOp2 = stack.pop(), metaOp1 = stack.pop();
//                System.out.println(metaOp1.getValue());
//                System.out.println(metaOp2.getValue());
//                System.out.println(o);
                Object a = metaOp1.getValue();
//                if (a instanceof String) {
//                    a = variableHashMap.get(a);
//                }
                if (a instanceof String && variableHashMap.containsKey(a) ) {
                    a = variableHashMap.get(a);
                    metaOp1 = new meteData(variableHashMap.get(a));
                }
//                System.out.println("a"+a+" "+metaOp2+" "+ o);

                Object b = changeData(a, metaOp2, o,metaOp1).getValue();
//                System.out.println(a.toString());
//                System.out.println(a instanceof IntVar);
//                System.out.println(a instanceof RealVar);
//                System.out.println(a instanceof BoolVar);
                if (a instanceof IntVar || metaOp1.getType()== meteData.Type.AREXP|| metaOp2.getType()== meteData.Type.AREXP) {
                    switch (o) {
                        case "+":
                        case "-":
                        case "*":
                        case "/":
                            //a o b o2 o1  a b + c > 如a+b>c c整型数字和变量分别考虑
                            //a 2 * b <  a
                            ArExpression temp;
                            if(metaOp2.getType()== meteData.Type.AREXP){
                                temp = ArExtention(a,o,metaOp2.getArExp());
                            }else if(metaOp1.getType()== meteData.Type.AREXP){
                                temp = ArExtention(metaOp1.getArExp(),o,b);
                            }else{
                                temp = Arinit(a, o, b);
                            }
                            metaOp.setArExpression(temp);
                            break;
//                            ArExpression temp = Arinit(a,o,b);
//                            while (output.get(i + 1).equals("+") ||output.get(i + 1).equals("-")
//                                    ||output.get(i + 1).equals("/") ||output.get(i + 1).equals("*")) {
//                                meteData v1meta = new meteData(output.get(i++));
//                                String o1 = output.get(i++);
//                                Object v1 = changeData(a, v1meta, o1).getValue();
//                                temp = ArExtention(temp, o1, v1);
//                            }
//                            meteData vauleLastmeta = new meteData(output.get(i++));
//                            String oLast = output.get(i++);
//                            Object vauleLast = changeData(a, vauleLastmeta, oLast).getValue();
//                            metaOp.setCons(ArLast(temp, oLast, vauleLast));
//                            break;

//                            Object o1 = output.get(i++);
//                            String o2 = output.get(i++);
//                            if (variableHashMap.containsKey(o1)) {
//                                o1 = variableHashMap.get(o1);
//                                metaOp.setCons(model.arithm(((Variable) a).asIntVar(), o, ((Variable) b).asIntVar(), o2, ((Variable) o1).asIntVar()));
//                            } else {
//                                o1 = Integer.parseInt((String) o1);
//                                metaOp.setCons(model.arithm(((Variable) a).asIntVar(), o, ((Variable) b).asIntVar(), o2, (Integer) o1));
//                            }
//                            break;
                        case ">":
                            if (metaOp1.getType()== meteData.Type.AREXP && b instanceof Integer) {
                                metaOp.setCons(metaOp1.getArExp().gt((Integer)b).decompose());
                            } else if(metaOp1.getType()== meteData.Type.AREXP){
                                metaOp.setCons(metaOp1.getArExp().gt(((Variable) b).asIntVar()).decompose());
                            }else if(b instanceof Integer){
                                metaOp.setCons(((Variable) a).asIntVar().gt((Integer)b).decompose());
                            }else{
                                metaOp.setCons(((Variable) a).asIntVar().gt(((Variable) b).asIntVar()).decompose());
                            }
                            break;
                        case ">=":
                            if (metaOp1.getType()== meteData.Type.AREXP && b instanceof Integer) {
                                metaOp.setCons(metaOp1.getArExp().ge((Integer)b).decompose());
                            } else if(metaOp1.getType()== meteData.Type.AREXP){
                                metaOp.setCons(metaOp1.getArExp().ge(((Variable) b).asIntVar()).decompose());
                            }else if(b instanceof Integer){
                                metaOp.setCons(((Variable) a).asIntVar().ge((Integer)b).decompose());
                            }else{
                                metaOp.setCons(((Variable) a).asIntVar().ge(((Variable) b).asIntVar()).decompose());
                            }
                            break;
                        case "<":
                            if (metaOp1.getType()== meteData.Type.AREXP && b instanceof Integer) {
                                metaOp.setCons(metaOp1.getArExp().lt((Integer)b).decompose());
                            } else if(metaOp1.getType()== meteData.Type.AREXP){
                                metaOp.setCons(metaOp1.getArExp().lt(((Variable) b).asIntVar()).decompose());
                            }else if(b instanceof Integer){
                                metaOp.setCons(((Variable) a).asIntVar().lt((Integer)b).decompose());
                            }else{
                                metaOp.setCons(((Variable) a).asIntVar().lt(((Variable) b).asIntVar()).decompose());
                            }
                            break;
                        case "<=":
                            if (metaOp1.getType()== meteData.Type.AREXP && b instanceof Integer) {
                                metaOp.setCons(metaOp1.getArExp().le((Integer)b).decompose());
                            } else if(metaOp1.getType()== meteData.Type.AREXP){
                                metaOp.setCons(metaOp1.getArExp().le(((Variable) b).asIntVar()).decompose());
                            }else if(b instanceof Integer){
                                metaOp.setCons(((Variable) a).asIntVar().le((Integer)b).decompose());
                            }else{
                                metaOp.setCons(((Variable) a).asIntVar().le(((Variable) b).asIntVar()).decompose());
                            }
                            break;
                        case "=":
                            if (metaOp1.getType()== meteData.Type.AREXP && b instanceof Integer) {
                                metaOp.setCons(metaOp1.getArExp().eq((Integer)b).decompose());
                            } else if(metaOp1.getType()== meteData.Type.AREXP){
                                metaOp.setCons(metaOp1.getArExp().eq(((Variable) b).asIntVar()).decompose());
                            }else if(b instanceof Integer){
                                metaOp.setCons(((Variable) a).asIntVar().eq((Integer)b).decompose());
                            }else{
                                metaOp.setCons(((Variable) a).asIntVar().eq(((Variable) b).asIntVar()).decompose());
                            }
                            break;
                        case "!=":
//                            System.out.println("setCons:"+a+" "+o+" "+ b);
//                            if (b instanceof Integer) {
//                                metaOp.setCons(model.arithm(((Variable) a).asIntVar(), o, (Integer) b));
//                            } else {
//                                metaOp.setCons(model.arithm(((Variable) a).asIntVar(), o, ((Variable) b).asIntVar()));
//                            }
                            if (metaOp1.getType()== meteData.Type.AREXP && b instanceof Integer) {
                                metaOp.setCons(metaOp1.getArExp().ne((Integer)b).decompose());
                            } else if(metaOp1.getType()== meteData.Type.AREXP){
                                metaOp.setCons(metaOp1.getArExp().ne(((Variable) b).asIntVar()).decompose());
                            }else if(b instanceof Integer){
                                metaOp.setCons(((Variable) a).asIntVar().ne((Integer)b).decompose());
                            }else{
                                metaOp.setCons(((Variable) a).asIntVar().ne(((Variable) b).asIntVar()).decompose());
                            }
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + o);
                    }
                } else if (a instanceof RealVar) {
                    // 如果是 a 是 RealVar
                    switch (o) {
                        case ">":
                            if(b instanceof Double)
                                metaOp.setCons(((RealVar) a).asRealVar().gt((double)b).equation());
                            else if(b instanceof Float)
                                metaOp.setCons(((RealVar) a).asRealVar().gt(((Float) b).doubleValue()).equation());
                            else
                                metaOp.setCons(((RealVar) a).asRealVar().gt(((Variable) b).asRealVar()).equation());
                            break;
                        case ">=":
                            if(b instanceof Double)
                                metaOp.setCons(((RealVar) a).asRealVar().ge((double)b).equation());
                            else if(b instanceof Float)
                                metaOp.setCons(((RealVar) a).asRealVar().ge(((Float) b).doubleValue()).equation());
                            else
                                metaOp.setCons(((RealVar) a).asRealVar().ge(((Variable) b).asRealVar()).equation());
                            break;
                        case "<":
//                            System.out.println("<double");
                            if(b instanceof Double)
                                metaOp.setCons(((RealVar) a).asRealVar().lt((double)b).equation());
                            else if(b instanceof Float)
                                metaOp.setCons(((RealVar) a).asRealVar().lt(((Float) b).doubleValue()).equation());
                            else{
                                metaOp.setCons(((RealVar) a).asRealVar().lt(((Variable) b).asRealVar()).equation());}
                            break;
                        case "<=":
                            if(b instanceof Double)
                                metaOp.setCons(((RealVar) a).asRealVar().le((double)b).equation());
                            else if(b instanceof Float)
                                metaOp.setCons(((RealVar) a).asRealVar().le(((Float) b).doubleValue()).equation());
                            else
                                metaOp.setCons(((RealVar) a).asRealVar().le(((Variable) b).asRealVar()).equation());
                            break;
                        case "=":
                            if(b instanceof Double)
                                metaOp.setCons(((RealVar) a).asRealVar().eq((double)b).equation());
                            else if(b instanceof Float)
                                metaOp.setCons(((RealVar) a).asRealVar().eq(((Float) b).doubleValue()).equation());
                            else
                                metaOp.setCons(((RealVar) a).asRealVar().eq(((Variable) b).asRealVar()).equation());
                            break;
                        case "!=":
                            if(b instanceof Double)
                                metaOp.setCons(((RealVar) a).asRealVar().eq((double)b).equation().getOpposite());
                            else if(b instanceof Float)
                                metaOp.setCons(((RealVar) a).asRealVar().eq(((Float) b).doubleValue()).equation().getOpposite());
                            else
                                metaOp.setCons(((RealVar) a).asRealVar().eq(((Variable) b).asRealVar()).equation().getOpposite());
                            break;
//                            // 两个实数没有不等号，所以暂时用两个整数来代替
//                            metaOp.setCons(((RealVar) a).asRealVar().eq(((Variable) b).asRealVar()).equation().getOpposite());
//                            break;
                        case "+":
                        case "-":
                        case "*":
                        case "/":
                        case "%":
                        default:
                            break;
                    }
                } else if (a instanceof Constraint) {
                    try {
                        switch (o) {
                            case "&&":
                                metaOp.setCons(((Constraint) a).reify().and(((Constraint) b).reify()).decompose());
                                break;
                            case "||":
                                metaOp.setCons(((Constraint) a).reify().or(((Constraint) b).reify()).decompose());
                                break;
                            case "=>":
                                // p->q <=> !p or q
                                metaOp.setCons(((Constraint) a).reify().imp(((Constraint) b).reify()).decompose());
                                break;
                            default:
                                break;
                        }
                    } catch (ClassCastException e) {
                        System.err.println("Boolean expression can only be applied to two boolean type.");
                        System.exit(-1);
                    }
                } else {
                    switch (o) {
                        case "=":
                            metaOp.setCons(((Variable) a).asBoolVar().decompose());
                            break;
                        case "!=":
                            metaOp.setCons(((Variable) a).asBoolVar().not().decompose());
                            break;
                        default:
                            System.err.println("Boolean expression can only be applied to two boolean type.");
                            System.exit(-1);
                    }
                }
                stack.push(metaOp);
            } else {
                stack.push(new meteData(o));
            }
        }

        meteData out = stack.pop();
        if (out.type == meteData.Type.CON) {
//            System.out.println("约束："+str+" "+out.getCons());
            out.getCons().post();
        }
//        long time2 = System.currentTimeMillis();
//        System.out.println((time2-time1)+ " setCons:"+str);

    }

    /**
     * 将操作符和参数值延迟到解析输出时处理，先根据类型映射一个合法的值。
     *
     * @param a      前置参数
     * @param metaOp 后置参数
     * @param op     操作符
     * @return metaOp
     */
    private meteData changeData(Object a, meteData metaOp, String op,meteData m1) {
        Number value = Integer.MAX_VALUE;
        Object b = metaOp.getValue();
        if (a instanceof Constraint) {
            return metaOp;
        }
        if(m1.getType() == meteData.Type.AREXP){
            if (variableHashMap.containsKey(b)) {
                return new meteData(variableHashMap.get(b));
            }else
                return new meteData(Integer.parseInt((String) b));
        }
        if (b instanceof String) {
            if (variableHashMap.containsKey(b)) {
                return new meteData(variableHashMap.get(b));
            } else if (switchCons) {
                String name = ((Variable) a).getName();
                Parameter p = parameterHashMap.get(name);
                String s = op + " " + b;
                switch (p.getType().getDataType()) {
                    case ENUM:
                        value = 0;
                        for (int i = 0; i < p.getType().getInclude().length; i++) {
                            if (p.getType().getInclude()[i].equals(b)) {
                                value = i;
                                break;
                            }
                        }
                        break;
                    case BOOL:
                        value = "true".equals(b) ? 1 : 0;
                        break;
                    case NUMBER:
                        switch (p.getType().getType()) {
                            case "float":
                            case "double":
                                value = Double.parseDouble((String) b);
                                break;
                            default:
                                value = Integer.parseInt((String) b);
                        }
                        break;
                    default:
                        break;
                }
                parameterHashMap.put(name, p);
            } else {
                value = Integer.parseInt((String) b);
            }
        } else {
            return metaOp;
        }
        return new meteData(value);
    }

    public void getSolver(String info, int index) {
        Solver solver = model.getSolver();
        // 本条覆盖准则是否有解
        boolean haveSolver = false;
        while (solver.solve()) {
            ++redundancy;
            Solution s = new Solution(model);
            s.record();
            // 去冗余
            if (totalNormalSolutions.isEmpty() || !solutionExists(totalNormalSolutions, s)) {
                haveSolver = true;
                normalSolutions.add(s);
                totalNormalSolutions.add(s);
                if (index != -1) {
                    if(info!=null)
                        queryInfo.append("覆盖准则").append(index).append(": \"").append(info).append("\" 被覆盖!\n");
                    int i = normalSolutions.size() - 1;
                    // 正例的信息
//                    caseInfo.add(coverages[index].getInfo());
                }
                // 找到解就跳出循环
                break;
            }
        }
        // 如果本条覆盖准则无解
        if (!haveSolver && index != -1) {
            if(info!=null)
                queryInfo.append("覆盖准则").append(index).append(": \"").append(info).append("\" 没有解!\n");
        }
        solver.hardReset();
    }

    private Boolean solutionExists(ArrayList<Solution> solutions, Solution s) {
        for (Solution ms : solutions) {
            String[] sStr = s.toString().replace(" ", "").split("[,:]");
            String[] msStr = ms.toString().replace(" ", "").split("[,:]");
            int i = 1;
            for (; i <= variableHashMap.size(); i++) {
//                System.out.println(sStr[i]+" "+msStr[i]);
                // 一个变量的值不相等说明 s 与 ms 不相等，跳过这个解
                if (!sStr[i].equals(msStr[i])) {
                    break;
                }
            }
            if (i > variableHashMap.size()) {
                return true;
            }
        }
        return false;
    }

    // flag 表示只在 uModel 里给覆盖信息添加一次
    void addCoverage(Boolean flag) {
        if (coverages.length == 1 && coverages[0].getRules() == null) {
            getSolver(coverages[0].getInfo(), 0);
            // 回退至原来的状态
            backToOriginalModel(parasNumWithCons, consNum);
        } else {
            for (int i = 0; i < coverages.length; ++i) {
                String[] rules = coverages[i].getRules();
                for (String str : rules) {
                    setCons(str);
                }
                if (flag) {
                    getSolver(coverages[i].getInfo(), i);
                } else {
                    getSolver(coverages[i].getInfo(), -1);
                }
                // 回退至原来的状态
                backToOriginalModel(parasNumWithCons, consNum);
            }
        }
    }
    // flag 表示只在 uModel 里给覆盖信息添加一次
    void addSetCov(Boolean flag) {
        if (setCons.length == 1 && setCons[0].getRules() == null) {
            getSolver(setCons[0].getInfo(), 0);
            // 回退至原来的状态
            backToOriginalModel(parasNumWithCons, consNum);
        } else {
            for (int i = 0; i < setCons.length; ++i) {
                String[] rules = setCons[i].getRules();
                for (String str : rules) {
                    setCons(str);
                }
                if (flag) {
                    getSolver(setCons[i].getInfo(), i);
                } else {
                    getSolver(setCons[i].getInfo(), -1);
                }
                // 回退至原来的状态
                backToOriginalModel(parasNumWithCons, consNum);
            }
        }
    }

    private String SysToACTS() {
        StringBuilder res = new StringBuilder();
        res.append("[System]\n" +
                "Name: ");
        res.append(this.model.getName()).append("\n");
        return res.toString();
    }

    private String ParToACTS(Parameter[] parameters){
        StringBuilder res = new StringBuilder();
        res.append("[Parameter]\n");
        for (Parameter p : parameters) {
            StringBuilder sb = new StringBuilder();
            switch (p.getType().getType()){
                case "int":
                    sb.append(p.getName()).append("(int):");
                    for (int i = p.getType().getLb().intValue(); i < p.getType().getUb().intValue(); i++) {
                        sb.append(i).append(",");
                    }
                    sb.append(p.getType().getUb().intValue());
                    break;
                case "bool":
                    sb.append(p.getName()).append("(boolean):true, false");
                    break;
                case "enum":
                    sb.append(p.getName()).append("(enum):");
                    for(String i : p.getType().getInclude()){
                        sb.append(i).append(",");
                    }
                    sb.deleteCharAt(sb.length()-1);
                    break;
            }
            sb.append("\n");
            res.append(sb.toString());
        }
        return res.toString();
    }

    private String ConToACTS() {
        StringBuilder res = new StringBuilder();
        res.append("[Constraint]\n");
        for (Cons c : this.cons) {
            for (String s : c.getRules()) {
                res.append(s).append("\n");
            }
        }
        return res.toString();
    }

    private String SetToACTS() throws ParseException {
        StringBuilder res = new StringBuilder();
        res.append("[Test Set]\n");
        String REGEX = "[\\s(\\w]+REIF_[\\d)=,]+";
        String REGEX2 = "[\\S]+=[0-9\\w\\.\\-]+";
        Pattern PATTERN = Pattern.compile(REGEX2, Pattern.MULTILINE);
        String value = this.normalSolutions.get(0).toString().replaceAll(REGEX, "");
        Matcher matcher = PATTERN.matcher(value);
        int j = 0;
        while (matcher.find()) {
            String[] strs = matcher.group(0).split("=");
            if (j < this.parasNum) {
                res.append(strs[0]).append(",");
                j++;
            }
        }
        res.deleteCharAt(res.length()-1);
        res.append("\n");
        for (Solution s : this.normalSolutions) {
//            System.out.println("solution:"+s.toString());
            value = s.toString().replaceAll(REGEX, "");
            matcher = PATTERN.matcher(value);
            j = 0;
            while (matcher.find()) {
                String[] strs = matcher.group(0).split("=");
                if (j < this.parasNum) {
                    res.append(Outputs.getParaValue(this, strs)).append(",");
                    j++;
                }
            }
            res.deleteCharAt(res.length()-1);
            res.append("\n");
        }
        return res.toString();
    }

    void addCombinationCoverage() throws Exception {
        String[] names = this.model.getName().split("\\.");
        String name = names[names.length - 1];
        StringBuilder sb = new StringBuilder();
        sb.append(SysToACTS()).append(ParToACTS(this.inputParas));
        sb.append(ConToACTS());
        sb.append(SetToACTS());
        OutputStream os = new FileOutputStream(name+"_model_acts.txt");
        os.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        os.close();
        //1.use cmd
//        String cmd = "java -Dmode=extend -jar /Users/kk/IdeaProjects/AutoTSGen/src/main/resources/sys/acts_3.2.jar /Users/kk/IdeaProjects/AutoTSGen/src/main/resources/sys/sys1.txt /Users/kk/IdeaProjects/AutoTSGen/src/main/resources/sys/sys1.csv";
//        Runtime.getRuntime().exec(cmd, null, new File("/Users/kk/IdeaProjects/AutoTSGen/src/main/resources/sys/"));
        //2.use api in acts
        SUTInfoReader sutInfoReader = new SUTInfoReader(name+"_model_acts.txt");
        SUT sut = sutInfoReader.getSUT();
        sut.addDefaultRelation (tWay);
//        TestGenProfile.instance().setIgnoreConstraints(false);
        TestGenProfile.instance().setConstraintMode(TestGenProfile.ConstraintMode.forbiddentuples);
        TestGenProfile.instance().setDOI(tWay);
        TestGenProfile.instance().setMode(TestGenProfile.PV_EXTEND);
        IpoEngine engine = new IpoEngine (sut);
        engine.build ();
        TestSet ts = engine.getTestSet();
        HashMap<String, Integer> map = new HashMap<>();
        int index= 0 ;
        for(edu.uta.cse.fireeye.common.Parameter p : ts.getParams()){
            map.put(p.getName(), index++);
        }
        int[][] comTs = new int[ts.getNumOfTests()][ts.getNumOfParams()];
        for(int i = 0;i<ts.getNumOfTests();i++){
            for(int j = 0 ; j < ts.getNumOfParams();j++){
                if(inputParas[j].getType().getDataType()==Data.NUMBER)
                    comTs[i][j] = ts.getTest(i)[map.get(inputParas[j].getName())] + (Integer) this.inputParas[j].getType().getLb();
                else
                    comTs[i][j] = ts.getTest(i)[map.get(inputParas[j].getName())];
            }
        }
        this.combTs  = comTs;
        int initSetSize = this.normalSolutions.size();
        this.setCons = new Cons[ts.getNumOfTests()-initSetSize];
        for (int i = initSetSize; i < ts.getNumOfTests(); i++) {
            StringBuilder setCon = new StringBuilder();
            for (int j = 0; j < ts.getNumOfParams(); j++) {
                setCon.append(ts.getParams().get(j).getName()).append("=");
                String[] strs = new String[2];
                strs[0] = ts.getParams().get(j).getName();
                strs[1] = String.valueOf(ts.getTest(i)[j]);
                setCon.append(Outputs.getParaValue(this, strs)).append("&&");
            }
            setCon.delete(setCon.length() - 2, setCon.length());
            Cons con = new Cons();
            String[] rs = new String[1];
            rs[0] = setCon.toString();
            con.setRules(rs);
            setCons[i-initSetSize] = con;
        }
        addSetCov(true);

//        TestSetWrapper wrapper1 = new TestSetWrapper (ts, sut);
//        wrapper1.outputInCSVFormat (name+"_acts_TestSet_result_"+tWay+".csv");
    }

    void addTriMCT() throws ParseException {
        MetaRelation metaRelation = new MetaRelation(this);
        for (Cons metamorphic : metamorphics) {
            if(metamorphic.getRules()==null)
                return;
            Constraint[] mr = new Constraint[metamorphic.getRules().length];
            for (int j = 0; j < mr.length; j++) {
                mr[j] = metaRelation.addMrPart(metamorphic.getRules()[j]);
            }
            metaRelation.addMR(mr);
            metaRelation.addMRInfo(metamorphic.getInfo());
        }

        ArrayList<int[]> art = new ArrayList<>(Arrays.asList(combTs));

        // metaRelation：蜕变关系
        // art：sourceSet
        TriMCT triMCT = new TriMCT(metaRelation, art);
        metamorphicSolutionInfo = new ArrayList<>();

        List<List<int[]>> follow = triMCT.process(metamorphicSolutionInfo);
        //follow int[] -> Cons -> Solution
        this.metamorphicsConSet = new ArrayList<>();
        for (List<int[]> f : follow) {
            ArrayList<Cons> temp = new ArrayList<>();
            for (int[] set : f) {
                StringBuilder metaCon = new StringBuilder();
                for (int j = 0; j < set.length; j++) {
                    metaCon.append(this.inputParas[j].getName()).append("=");
                    String[] strs = new String[2];
                    strs[0] = this.inputParas[j].getName();
//                    if(inputParas[j].getType().getDataType()==Data.NUMBER){
//                        set[j] = set[j] + (Integer) inputParas[j].getType().getLb();
//                    }
                    strs[1] = String.valueOf(set[j]);
                    metaCon.append(Outputs.getParaValue(this, strs)).append("&&");
                }
                metaCon.delete(metaCon.length() - 2, metaCon.length());
                Cons con = new Cons();
                String[] rs = new String[1];
                rs[0] = metaCon.toString();
                con.setRules(rs);
                temp.add(con);
            }
            metamorphicsConSet.add(temp);
        }
        for (ArrayList<Cons> me: metamorphicsConSet) {
            ArrayList<Solution> temp = new ArrayList<>();
            for (Cons con : me) {
                String[] rules = con.getRules();
                for (String str : rules) {
                    setCons(str);
                }

                Solver solver = model.getSolver();
                // 本条覆盖准则是否有解
                if (solver.solve()) {
                    Solution s = new Solution(model);
                    s.record();
                    temp.add(s);
                }
                solver.hardReset();

                // 回退至原来的状态
                backToOriginalModel(parasNumWithCons, consNum);
            }
            metamorphicSolution.add(temp);
        }
    }


    void updateOracles() {
//        if (coverages.length == 1 && coverages[0].getRules() == null) {
//            if(normalSolutions.isEmpty())
//                return;
//            else {
//                oracles.add(normalSolutions.get(0));
//                return;
//            }
//        }
        if (outputCons.length == 1 && outputCons[0].getRules() == null) {
            if(normalSolutions.isEmpty())
                return;
            else {
                totalOracles = normalSolutions;
                oracles = normalSolutions;
//                totalOracles.add(normalSolutions.get(0));
//                oracles.add(normalSolutions.get(0));
                return;
            }
        }
        // 记录当前的状态
        int paraNumsIncludeOutput = model.getNbVars();
        int consNumIncludeOutput = model.getNbCstrs();

        final String regex1 = "([\\S]+=([0-9]+)|([\\S]+=\\[\\S+\\]))";
        final Pattern pattern = Pattern.compile(regex1, Pattern.MULTILINE);
        // 每条解应该都有一个输出，如果某条解不满足所有的规则该怎么办
        for (Solution s : normalSolutions) {
            Matcher matcher = pattern.matcher(s.toString());
            int i = 0;
            StringBuilder str = new StringBuilder();
            // 只需要知道输入参数的取值，不需要知道输出参数的取值
            while (i++ < parasNum && matcher.find()) {
                str.append(matcher.group(0)).append(" && ");
            }
            // 用 switch 来决定是哪个方向的参数值转换，通过改变 changeData 函数的行为实现
            switchCons = false;
            setCons(str.substring(0, str.length() - 4));
            switchCons = true;
            // 用参数和约束的个数来记录当前 model 状态
            int pNum = model.getNbVars();
            int cNum = model.getNbCstrs();
            // judge whether this solution has assertion
            boolean isSolve = false;
            String[] rules = outputCons[0].getRules();
            if (rules != null) {
                for (String rule : rules) {
                    setCons(rule.split("=>")[0]);
                    Solver solver = model.getSolver();
                    if (solver.solve()){
                        isSolve = true;
//                        System.out.println(str.substring(0, str.length() - 4)+":::::"+rule.split("=>")[0]);
                    }
                    solver.hardReset();
                    backToOriginalModel(pNum, cNum);
                }
                model.getSolver().hardReset();
                // 回退至输出参数加入模型后的状态
                backToOriginalModel(paraNumsIncludeOutput, consNumIncludeOutput);
            }
            if(isSolve){
                switchCons = false;
                setCons(str.substring(0, str.length() - 4));
                switchCons = true;
                for (Cons c : outputCons) {
                    putConsToModel(c);
                    // 如果有解的话
                    Solver solver = model.getSolver();
                    if (solver.solve()) {
                        Solution ms = new Solution(model);
                        ms.record();

                        if (totalOracles.isEmpty() || !solutionExists(totalOracles, ms)) {
                            oracles.add(ms);
                            totalOracles.add(ms);
                        }
                        break;
                    }
                    // 回退至 c 作为约束加入模型前的状态
                    solver.hardReset();
                    backToOriginalModel(pNum, cNum);
                }
                model.getSolver().hardReset();
                // 回退至输出参数加入模型后的状态
                backToOriginalModel(paraNumsIncludeOutput, consNumIncludeOutput);
            }
        }

        // 回退至 c 作为约束加入模型前的状态
        model.getSolver().hardReset();
        // 回退至输出参数加入模型后的状态
        backToOriginalModel(paraNumsIncludeOutput, consNumIncludeOutput);
    }

    private void putConsToModel(Cons c) {      //constraints
        String[] rules = c.getRules();
        if (rules != null) {
            for (String str : rules) {
//                System.out.println(str);
                setCons(str);
            }
        }
    }


    void backToOriginalModel(int varsNum, int cstrsNum) {
        // 需要去掉的约束
        Constraint[] needUnPostCons = new Constraint[model.getNbCstrs() - cstrsNum];
        System.arraycopy(model.getCstrs(), cstrsNum, needUnPostCons, 0, model.getNbCstrs() - cstrsNum);
        // 解除约束
        model.unpost(needUnPostCons);

        // 需要去掉的变量
        Variable[] needUnPostVars = new Variable[model.getNbVars() - varsNum];
        System.arraycopy(model.getVars(), varsNum, needUnPostVars, 0, model.getNbVars() - varsNum);
        // 去掉参数
        for (Variable var : needUnPostVars) {
            model.unassociates(var);
        }
    }

    public StringBuilder getQueryInfo() {
        return queryInfo;
    }
}

