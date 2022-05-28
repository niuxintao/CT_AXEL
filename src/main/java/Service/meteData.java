package Service;

import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.expression.discrete.arithmetic.ArExpression;
import org.chocosolver.solver.variables.Variable;

public class meteData {
    public Type type;
    private String str;
    private Variable var;
    private Constraint cons;
    private Number number;
    private ArExpression arExpression;
    public enum Type {
        /**
         * STR: string 型
         * VAR: Variable 型
         * Con: cons 型
         * AREXP: ArExpression 型
         */
        STR, VAR, CON, NUM, AREXP
    }
    public meteData() {
    }

    meteData(ArExpression arexp) {
        this.arExpression = arexp;
        this.type = Type.AREXP;
    }

    meteData(Constraint cons) {
        this.cons = cons;
        this.type = Type.CON;
    }

    public meteData(Variable var) {
        this.type = Type.VAR;
        this.var = var;
    }

    public meteData(Number var) {
        this.type = Type.NUM;
        this.number = var;
    }

    public meteData(String string) {
        this.type = Type.STR;
        this.str = string;
    }

    public Object getValue() {
        switch (type) {
            case STR:
                return getStr();
            case CON:
                return getCons();
            case VAR:
                return getVar();
            case NUM:
                return getNumber();
            case AREXP:
                return getArExp();
            default:
                break;
        }
        return -1;
    }

    public Variable getVar() {
        return var;
    }

    public Number getNumber() {
        return number;
    }

    public Type getType() {
        return type;
    }

    public Constraint getCons() {
        return cons;
    }

    public ArExpression getArExp(){return arExpression;}

    public String getStr() {
        return str;
    }

    public void setVar(Variable var) {
        this.var = var;
        this.type = Type.VAR;
    }

    public void setCons(Constraint cons) {
        this.cons = cons;
        this.type = Type.CON;
    }
    public void setArExpression(ArExpression arExpression) {
        this.arExpression = arExpression;
        this.type = Type.AREXP;
    }

    public void setStr(String str) {
        this.str = str;
        this.type = Type.STR;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setNumber(Number number) {
        this.number = number;
    }
}
