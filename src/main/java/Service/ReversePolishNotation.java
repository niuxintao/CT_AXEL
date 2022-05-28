package Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface ReversePolishNotation {
    /**
     * Boolean_Op:    &&, ||, =>, !
     * Relational_Op: >, <, =, !=, >=, <=
     * Arithmetic_Op: +, -, *, /, %
     */
    String[] OPS = {"&&", "||", "=>", "!=", ">=", "<=", "!", ">", "<", "=", "+", "-", "*", "/", "%"};

    static ArrayList<String> cons2Strs(String conString) {
        conString = conString.trim();
        ArrayList<String> consInput = new ArrayList<>();

        // 将 '' 中的看成一个整体，防止其中有操作符，被操作符拆分
        HashSet<Integer> excludeSet = new HashSet<>();
        final String regex = "'[\\u4e00-\\u9fa5\\d\\w\\/\\._+-|=~#\\s]*'";
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(conString);
        // 填充 excludeSet，其中的内容为 conSting 中不会是操作符的对应下标
        int base = 0;
        while (matcher.find()) {
            base = conString.indexOf(matcher.group(0), base);
            for (int i = 0; i < matcher.group(0).length(); i++) {
                if (excludeSet.isEmpty() || !excludeSet.contains(base + i)) {
                    excludeSet.add(base + i);
                }
            }
            ++base;
        }
        Map<Integer, String> opsMap = new TreeMap<>();
        for (String op : OPS) {
            int idx = -1;
            while (conString.indexOf(op, idx + 1) != -1) {
                idx = conString.indexOf(op, idx + 1);
                if (excludeSet.contains(idx)) {
                    continue;
                }
                if (opsMap.containsKey(idx) || (idx > 1 && Operator.isOperator(conString.substring(idx - 1, idx + 1)))) {
                    continue;
                }
                opsMap.put(idx, op);
            }
        }
        int idx = -1;
        int numOfleftBarce = 0;
        // 单独处理 '(' 和 ')' ，因为其个数一定是偶数，否则则是输入的问题
        while (conString.indexOf("(", idx + 1) != -1) {
            idx = conString.indexOf("(", idx + 1);
            opsMap.put(idx, "(");
            ++numOfleftBarce;
        }
        idx = 1;
        while (conString.indexOf(")", idx + 1) != -1) {
            idx = conString.indexOf(")", idx + 1);
            opsMap.put(idx, ")");
            --numOfleftBarce;
        }
        try {
            if (numOfleftBarce != 0) {
                throw new Exception("Left brace and right brace is not match!!!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        int preIdx = 0;
        Set<Integer> keySet = opsMap.keySet();
        for (int key : keySet) {
            String op = opsMap.get(key);
            String para = conString.substring(preIdx, key).trim().replace("'", "");
            if (para.length() > 0) {
                consInput.add(para);
            }
            preIdx = key + op.length();
            consInput.add(op);
        }
        if (preIdx < conString.length()) {
            String para = conString.substring(preIdx).trim().replace("'", "");
            if (para.length() > 0) {
                consInput.add(para);
            }
        }

        return consInput;
    }

    static ArrayList<String> rpn(ArrayList<String> input) {
        Stack<String> ops = new Stack<>();
        ArrayList<String> output = new ArrayList<>();
        for (String in : input) {
            if (!Operator.isOperator(in)) {
                output.add(in);
            } else if (")".equals(in)) {
                while (!"(".equals(ops.peek())) {
                    output.add(ops.pop());
                }
                ops.pop();
                // 四种情况直接压栈：ops栈空；栈顶为'('；栈顶运算符优先级较小；
            } else if (ops.empty() || (Operator.cmp(ops.peek(), in) < 0) || "(".equals(ops.peek())) {
                ops.push(in);
            } else {
                while (!ops.empty() && Operator.cmp(ops.peek(), in) >= 0 && !"(".equals(ops.peek())) {
                    output.add(ops.pop());
                }
                ops.push(in);
            }
        }

        //遍历结束，将运算符栈全部压入output
        while (!ops.empty()) {
            output.add(ops.pop());
        }

        return output;
    }
}