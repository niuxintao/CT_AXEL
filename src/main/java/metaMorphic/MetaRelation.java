package metaMorphic;

import Service.*;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.expression.discrete.arithmetic.ArExpression;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;

import java.util.*;

public class MetaRelation {

    public static int CAND = 50;

    public List<Constraint[]> MR ;
    public List<String> MRInfo ;

    public Model model;

    public Variable[] sourceVar;
    public Variable[] followVar;
    public HashMap<String,Variable> varMap;  //all var
    public int[] param;
    HashMap<String, Parameter> parameterHashMap;

    public MetaRelation(uModel cs ){

//        this.cacs = ;
        this.model = cs.model;
        this.parameterHashMap = cs.parameterHashMap;
        this.param = new int[cs.inputParas.length];
        for (int i = 0; i < param.length; i++) {
            param[i] = cs.inputParas[i].getSize();
        }
        this.sourceVar = new IntVar[param.length];

        this.followVar = cs.inputVars;
        varMap = new HashMap<>();
        for (Variable variable : followVar) {
            varMap.put("f."+variable.getName(), variable);
        }

        for (int i = 0; i < this.param.length; i++) {
            if(cs.inputParas[i].getType().getDataType()==Data.ENUM){
                sourceVar[i] = model.intVar(cs.inputVars[i].getName(), 0, param[i] - 1);
            }else if(cs.inputParas[i].getType().getDataType()==Data.NUMBER){
                sourceVar[i] = model.intVar(cs.inputVars[i].getName(),(Integer) cs.inputParas[i].getType().getLb(), (Integer) cs.inputParas[i].getType().getUb());
            }else{
                System.err.println("type error in metaRelation");
            }
            varMap.put("s." + cs.inputVars[i].getName(), sourceVar[i]);
        }

        MR = new ArrayList<Constraint[]>();
        MRInfo = new ArrayList<>();
    }

    public void addMR (Constraint[] mr){
        MR.add(mr);
    }

    public void addMRInfo (String mrInfo){
        MRInfo.add(mrInfo);
    }

    public Constraint addMrPart(String str){
        ArrayList<String> input = ReversePolishNotation.cons2Strs(str);
        ArrayList<String> output; //输出结果
        ArrayList<String> tempList = new ArrayList<>();
        for (String s : input) {
            tempList.add(s.replace("\"", ""));
        }
        output = ReversePolishNotation.rpn(tempList);
        Stack<meteData> stack = new Stack<>();
        int i = 0;
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
                Object a = metaOp1.getValue();
                if (a instanceof String && varMap.containsKey(a) ) {
                    a = varMap.get(a);
                    metaOp1 = new meteData(varMap.get(a));
                }
                Object b = changeData(a, metaOp2, o,metaOp1).getValue();
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
            return out.getCons();
        }
        return null;
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
        }
        return null;
    }
    private meteData changeData(Object a, meteData metaOp, String op,meteData m1) {
        Number value = Integer.MAX_VALUE;
        Object b = metaOp.getValue();
        if (a instanceof Constraint) {
            return metaOp;
        }
        if (m1.getType() == meteData.Type.AREXP) {
            if (varMap.containsKey(b)) {
                return new meteData(varMap.get(b));
            } else
                return new meteData(Integer.parseInt((String) b));
        }
        if (b instanceof String) {
            if (varMap.containsKey(b)) {
                return new meteData(varMap.get(b));
            } else {
                String name = ((Variable) a).getName();
                Parameter p = parameterHashMap.get(name);
                if(p.getType().getDataType()==Data.ENUM){
                    value = 0;
                    for (int i = 0; i < p.getType().getInclude().length; i++) {
                        if (p.getType().getInclude()[i].equals(b)) {
                            value = i;
                        }
                    }
                }else{
                    value = Integer.parseInt((String) b);
                }
            }
        }
        return new meteData(value);
    }

    public Constraint addMrPart_1(int id1, String op1, int id2, String op2, int valueOrId) {

        return  model.arithm(sourceVar[id1].asIntVar(), op1, followVar[id2].asIntVar(), op2, valueOrId);
    }

    public Constraint addMrPart_2(int id1, String op1, int id2, String op2, int valueOrId) {
        return sourceVar[id1].asIntVar().eq(followVar[id2].asIntVar().mul(valueOrId)).decompose();
    }

    public Constraint addMrPart_3(int id1, String op1, int id2, String op2, int valueOrId) {
        return followVar[id2].asIntVar().eq(sourceVar[id1].asIntVar().mul(valueOrId)).decompose();
    }

    //assign value to source
    public Constraint addMrPart_forSourceValue(int id1, String op1, int value) {
        return  model.arithm(sourceVar[id1].asIntVar(), op1, value);
    }

    //gen source int[] as the type of constraints
    public Constraint[] genSource(int[] source){
        Constraint[] sour = new  Constraint[source.length];
        for(int i  = 0; i < source.length; i++){
            sour[i] = addMrPart_forSourceValue(i, "=", source[i]);
        }
        return sour;
    }

    //for one source
    public int[] genOne(int[] source, Constraint[] mr){
        Constraint[] sor = genSource(source);
        Constraint[] merge = merge(mr, sor);

        int[] follow = new int[param.length];
        postCons(merge);

        Solver solver = model.getSolver();

        boolean solbable = false;
        if(solver.solve()){
            solbable = true;
            follow = getFollow();
        }
        solver.reset();
        unPostCons(merge);

        if(solbable)
            return follow;
        else
            return null;
    }

    public Constraint[] merge(Constraint[] a, Constraint[] b){
        Constraint[] merge = new Constraint[a.length + b.length];
        for(int i = 0; i < merge.length; i++){
            if(i < a.length){
                merge[i] = a[i];
            }else{
                merge[i] = b[i-a.length];
            }
        }
        return merge;
    }

    public void postCons(Constraint[] cons) {
        for (Constraint i : cons){
            model.post(i);
        }
    }

    public void unPostCons(Constraint[] cons) {
        for (Constraint i : cons)
            model.unpost(i);
    }

    public int[] getFollow(){
        int[] result = new int[followVar.length];
        for(int i = 0; i < result.length; i++) {
            if(followVar[i] instanceof BoolVar)
                result[i] = followVar[i].asBoolVar().getValue();
            else
                result[i] = followVar[i].asIntVar().getValue();
        }
        return result;
    }
}

