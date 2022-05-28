package Service;

/**
 * 操作符的优先顺序
 *
 */
public enum Operator {
    /**
     * 操作符
     */
    IMPLIES("=>", 1), OR("||", 2),
    AND("&&", 3), EQ("=", 4),
    NEQ("!=", 4), LT("<", 5),
    LTE("<=", 5), GT(">", 5),
    GTE(">=", 5), ADD("+", 6),
    SUBTRACT("-", 6), DIVIDE("/", 7),
    MULTIPLY("*", 7), MOD("%", 7),
    NOT("!", 8), LEFT_BRACKET("(", 9), RIGHT_BRACKET(")", 9);
    /**
     * 操作符
     */
    String value;
    /**
     * 优先级
     */
    int priority;

    Operator(String value, int priority) {
        this.value = value;
        this.priority = priority;
    }

    /**
     * 比较两个符号的优先级
     *
     * @param c1 操作符
     * @param c2 操作符
     * @return c1的优先级是否比c2的高，高则返回正数，等于返回0，小于返回负数
     */
    public static int cmp(String c1, String c2) {
        int p1 = 0;
        int p2 = 0;
        for (Operator o : Operator.values()) {
            if (o.value.equals(c1)) {
                p1 = o.priority;
            }
            if (o.value.equals(c2)) {
                p2 = o.priority;
            }
        }
        return p1 - p2;
    }

    /**
     * 枚举出来的才视为运算符，用于扩展
     *
     * @param c 操作符
     * @return 是否为操作符
     */
    public static boolean isOperator(String c) {
        for (Operator o : Operator.values()) {
            if (o.value.equals(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为比较运算符
     *
     * @param c 操作符
     * @return 是否为操作符
     */
    public static boolean isCmpOperator(String c) {
        for (Operator o : Operator.values()) {
            if (o.value.equals(c)) {
                return o.priority == 4 || o.priority == 5;
            }
        }
        return false;
    }
}
