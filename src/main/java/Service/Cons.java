package Service;

import java.util.ArrayList;

public class Cons {
    private String[] rules;
    private String info;

    public static void main(String[] args) {
        //测试用例
        //String str = "1+2*3-4*5-6+7*8-9"; //123*+45*-6-78*+9-
//        String str = "a*(b-c*d)+e-f/g*(h+i*j-k)"; // abcd*-*e+fg/hij*+k-*-
        String str = "6*(5+(2+3)*8+3)<100"; //6523+8*+3+*
//        String str = "a+b*c+(d*e+f)*g"; //abc*+de*f+g*+
//        String str = " 'a' = 'b' = 'c' ";
//        String str = " 点击类型='2021-12-12' => 出发城市2 = '#' && 出发城市3 = '#' && 出发城市4 = '#' && 出发城市5 = '#' && 到达城市2 = '#' && 到达城市3 = '#' && 到达城市4 = '#' && 到达城市5 = '#' && 去程日期2 = '2021-01-01' && 去程日期3 = '2021-01-01' && 去程日期4 = '2021-01-01' && 去程日期5 = '2021-01-01' && 点击添加航段1 = '#' && 点击添加航段2 = '#' && 点击添加航段3 = '#' ";

        ArrayList<String> output; //输出结果
        ArrayList<String> input = ReversePolishNotation.cons2Strs(str);
        output = ReversePolishNotation.rpn(input);
        System.out.println(output);

    }

    public void setRules(String[] rules) {
        this.rules = rules;
    }

    public void reverseRuleI(int i) {
        if (i > rules.length) {
            System.out.println("Input i should not longer than rules' length!");
            System.exit(-1);
        } else {
            rules[i] = "!(" + rules[i] + ")";
        }
    }

    public void retrieveRules(int i) {
        if (i > rules.length) {
            System.out.println("Input i should not longer than rules' length!");
            System.exit(-1);
        } else {
            rules[i] = rules[i].substring(2, rules[i].length() - 1);
        }
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String[] getRules() {
        return rules;
    }

    Cons() {
    }
}
