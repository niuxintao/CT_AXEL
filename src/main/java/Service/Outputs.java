package Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import org.chocosolver.solver.Solution;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public interface Outputs {
    /**
     * REGEX: 匹配所有非变量
     */
    String REGEX = "[\\s(\\w]+REIF_[\\d)=,]+";
    String REGEX2 = "[\\S]+=[0-9\\w\\.\\-]+";
    Pattern PATTERN = Pattern.compile(REGEX2, Pattern.MULTILINE);
    /**
     * 获得各类测试用例，返回测试用例数组 JSONArray
     *
     * @param sut 待测软件的 uModel 实例
     * @return 返回一个 JSONArray
     * @throws JSONException                   异常
     * @throws ParseException                  异常
     * @throws Xeger.FailedRandomWalkException 异常
     */
    static int[] getOutput(JSONArray testcases, uModel sut) throws JSONException, ParseException, Xeger.FailedRandomWalkException {
        int positiveCaseNum = 0;
        // 将正常解放入结果文件
        positiveCaseNum += putModel2output(sut, testcases, "positive ", -1,true,null);
        // 拿一个正常的解，用作 base solution
        Solution baseSolution = sut.oracles.get(0);
        // 加入合法边界值相关的测试用例
        positiveCaseNum += boundaryCases(sut, testcases);
        // 加入蜕变关系生成的用例
        int metePair = metaRelationCases(sut, testcases);
        // 反例
        // 将违反约束的解放入结果文件
        violateConsCases(sut, testcases);
        // 将类型违背的测试用例放入到测试用例集合中
        putInType2output(sut, testcases, baseSolution);
        // 将空值和缺失的解放入结果文件
        putEmLoToOutput(sut, testcases, baseSolution);
        // 将非法的边界值放进测试用例集中
        putIbToOutput(sut, testcases, baseSolution);
        // 返回 JSONArray 对象
        return new int[]{positiveCaseNum,metePair,testcases.size()-positiveCaseNum-metePair};
    }

    static int putModel2output(uModel sut, JSONArray testcases, String caseName, int index, boolean label,String errmsg) throws ParseException {
        int size = testcases.size();
        int i = 0;
        for (Solution s : sut.oracles) {
//            System.out.println("solution:"+s);
            // 不考虑设置约束而产生的参数
            String value = s.toString().replaceAll(REGEX, "");
//            System.out.println("before:"+value);
            if (index < 0) {
                // 按覆盖准则生成的正例
                // 如果没有相关正例信息，默认名称为正例
                if (sut.caseInfo == null || i >= sut.caseInfo.size()) {
                    testcases.add(put2output(sut, value, "positive/ " + ++i, true,null));
                } else {
                    testcases.add(put2output(sut, value, "positive/ " + sut.caseInfo.get(i++), true,null));
                }
            } else {
                if(label)//边际 正例
                    if(index==1){
                        testcases.add(put2output(sut, value, caseName + " upper bound", true,null));
                        index++;
                    }else{
                        testcases.add(put2output(sut, value, caseName + " lower bound", true,null));
                    }
                else//反例
                    testcases.add(put2output(sut, value, caseName , false,errmsg));
            }
        }
        return testcases.size() - size;
    }

    static int metaRelationCases(uModel sut, JSONArray testcases) throws ParseException {
        int sfirst = 0;
        for (ArrayList<Solution> solutionList : sut.metamorphicSolution) {
            ArrayList<String> metainfos = sut.metamorphicSolutionInfo.get(sfirst++);
            ArrayList<String> values = new ArrayList<>();
            for (Solution s : solutionList) {
                // 不考虑设置约束而产生的参数
                String value = s.toString().replaceAll(REGEX, "");
                values.add(value);
            }
            testcases.add(putMeta2output(sut, values, metainfos));
        }
        return sut.metamorphicSolutionInfo.size();
    }

    static void putStr2output(String s, JSONArray testcases, uModel sut, String caseName, int index,String errmsg) throws ParseException {
        // 不考虑设置约束而产生的参数
        String value = s.replaceAll(REGEX, "");
        testcases.add(put2output(sut, value, caseName, false, errmsg));
    }

    static JSONObject putMeta2output(uModel sut, ArrayList<String> values, ArrayList<String> caseName) throws ParseException {
        JSONObject test = new JSONObject(true);
        int i = 0;
        for(String value: values){
            JSONObject suite = new JSONObject(true);
            final Matcher matcher = PATTERN.matcher(value);
            int j = 0;
            while (matcher.find()) {
                String[] strs = matcher.group(0).split("=");
                if (j < sut.parasNum) {
                    // 参数取值为空时
                    if ("null".equals(strs[1])) {
                        suite.put(strs[0], null);
                    } else if (!"lost".equals(strs[1])) {
                        // 参数不为缺失时
                        suite.put(strs[0], getParaValue(sut, strs));
                    }
                    j++;
                }
            }
            test.put(caseName.get(i++), suite);
        }
        return test;
    }

    static JSONObject put2output(uModel sut, String value, String caseName,boolean isTrue,String errmsg) throws ParseException {
        JSONObject test = new JSONObject(true);
        JSONObject suite = new JSONObject(true);
        JSONObject assertion = new JSONObject(true);
        final Matcher matcher = PATTERN.matcher(value);
        int j = 0;
        while (matcher.find()) {
            String[] strs = matcher.group(0).split("=");
            if (j < sut.parasNum) {
                // 参数取值为空时
                if ("null".equals(strs[1])) {
                    suite.put(strs[0], null);
                } else if (!"lost".equals(strs[1])) {
                    // 参数不为缺失时
                    suite.put(strs[0], getParaValue(sut, strs));
                }
                j++;
            } else if(j < sut.totalParasNum){
                assertion.put(strs[0], getParaValue(sut, strs));
                j++;
            }
        }
        test.put(caseName, suite);
        if(isTrue){ //正例
            test.put("Assertion", assertion);
        } else{     //反例
            JSONObject err = new JSONObject(true);
            err.put("errmsg", errmsg);
            test.put("Assertion", err);
        }
        return test;
    }

    /**
     * 将违反约束的测试用例加入到测试用例中
     *
     * @param sut       待测软件的 uModel 实例
     * @param testcases JSONArray 的测试用例数组
     * @throws ParseException 异常
     */
    static void violateConsCases(uModel sut, JSONArray testcases) throws ParseException {
        for (int i = 0; i < sut.cons.length; i++) {
            // 一般类约束
            if (sut.cons[i].getRules() != null) {
                // 第 i 组一般类约束的当前下标起始位置
                int caseNum = 1;
                for (int j = 0; j < sut.cons[i].getRules().length; j++) {
                    // 重置 sut 的状态
                    sut.backToOriginalModel(sut.totalParasNum, 0);
                    sut.oracles = new ArrayList<>();
                    sut.normalSolutions = new ArrayList<>();
                    sut.switchCons = true;
                    // 将 i 组的第 j 条约束取反
                    sut.cons[i].reverseRuleI(j);
                    // 为 model 增加约束，用参数个数和约束个数记录 model 的初始化状态
                    sut.addConstraints();
                    sut.getSolver(null, -1);
//                    // 用 Coverage 来决定需要哪些测试用例
//                    sut.addCoverage(false);
                    // 更新解的 Oracle 信息
                    sut.updateOracles();
                    // 将第 i 条约束变回来
                    sut.cons[i].retrieveRules(j);
                    // 将 sut 的结果放入到输出内容里
                    if (sut.cons[i].getInfo() != null) {
                        putModel2output(sut, testcases, "negative ", caseNum, false, "violate constraint："+ sut.cons[i].getInfo());
                    } else {
                        putModel2output(sut, testcases, "negative/ violate constraint " + (i + 1), caseNum, false,null);
                    }
                    caseNum += sut.oracles.size();
                }
            }
        }
    }

    /**
     * 将边界值放入到测试用例中
     * - 正例：满足边界值的测试用例用覆盖准则的形式加入到测试用例中
     * - 反例：不满足边界值的测试用例（上越界、下越界）
     *
     * @param sut       待测软件的 uModel 实例
     * @param testcases JSONArray 的测试用例数组
     * @throws ParseException 异常
     */
    static int boundaryCases(uModel sut, JSONArray testcases) throws ParseException, Xeger.FailedRandomWalkException {
        // 每个参数都有其边界值
        int value = testcases.size();
        for (Parameter para : sut.inputParas) {
            int index = 1;
            // 模型的解 - 置空
            sut.oracles = new ArrayList<>();
            sut.normalSolutions = new ArrayList<>();
            // 重置到模型设置完约束的状态
            sut.backToOriginalModel(sut.parasNumWithCons, sut.consNum);
            // 拿到合法的边界值，将其当作约束
            ArrayList<String> values = para.getBoundaryValue(true);
            if (!values.isEmpty()) {
                // 将覆盖准则用约束的形式放入到模型中
                for (String str : values) {
                    // 模型的解 - 置空
                    sut.oracles = new ArrayList<>();
                    sut.normalSolutions = new ArrayList<>();
                    // 重置到模型设置完约束的状态
                    sut.backToOriginalModel(sut.parasNumWithCons, sut.consNum);
                    // 每条覆盖准则都有一组解
                    sut.setCons(para.getName() + " = " + str);
                    // 由覆盖准则转得到输入参数的值
                    sut.getSolver(null, -1);
                    // 更新解的 Oracle 信息
                    sut.updateOracles();
                    // 将合法边界值解的结果放入到测试用例集中
                    putModel2output(sut, testcases, "positive/ " + para.getName(), index++,true,null);
                }
            }
        }
        return testcases.size() - value;
    }

    /**
     * 将非法边界值(Invalid Boundary)放入结果测试用例集中
     *
     * @param sut       待测软件的 uModel 实例
     * @param testcases JSONArray 的测试用例数组
     * @param s         基解
     * @throws ParseException 异常
     */
    static void putIbToOutput(uModel sut, JSONArray testcases, Solution s) throws ParseException, Xeger.FailedRandomWalkException {
        int size = testcases.size();
        String solution = s.toString();
        for (Parameter para : sut.inputParas) {
            // 非法边界值测试用例下标
            int index = 1;
            // 根据「参数名 + 参数取值」来匹配字符串
            String re = para.getName() + "=" + "[\\d\\.]+";
            Pattern pattern = Pattern.compile(re, Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(solution);
            // 拿到合法的边界值，将其当作约束
            ArrayList<String> values = para.getBoundaryValue(false);
            if (!values.isEmpty()) {
                String value;
                // 拿到参数的取值（是个整型的值）
                while (matcher.find()) {
                    // 「参数名 + 参数取值」
                    value = matcher.group(0);
                    for (String inValidValue : values) {
                        String s1 = para.getName() + "=" + inValidValue;
                        putStr2output(solution.replace(value, s1), testcases, sut, "negative ", index++, "parameter " + para.getName() + " illegal border");
                    }
                }
            }
        }
    }

    /**
     * 将空值(Empty)和缺失(Lost)放进测试用例集中
     * - 空值: str = 'null'
     * - 缺失: str = 'lost'
     *
     * @param sut       待测软件的 uModel 实例
     * @param testcases JSONArray 的测试用例数组
     * @param s         基解
     */
    static void putEmLoToOutput(uModel sut, JSONArray testcases, Solution s) throws ParseException {
        String solution = s.toString();
        // 每个参数都可能空值或缺失
        //匹配doublein=0.00, doublein1=0.01, doublein2=0.00
        for (Parameter para : sut.inputParas) {
            // 根据「参数名 + 参数取值」来匹配字符串
            String re = para.getName() + "=" + "[\\d\\.]+";
            Pattern pattern = Pattern.compile(re, Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(solution);
            String value;
            // 拿到参数的取值（是个整型的值）
            while (matcher.find()) {
                // 「参数名 + 参数取值」
                value = matcher.group(0);
                String s1 = para.getName() + "=null";
                putStr2output(solution.replace(value, s1), testcases, sut, "negative ", 0,"parameter "+para.getName() + " null");
                s1 = para.getName() + "=lost";
                putStr2output(solution.replace(value, s1), testcases, sut, "negative ", 0,"parameter "+para.getName() + " lost");
            }

        }
    }

    /**
     * 参数类型违背
     *
     * @param sut       待测软件的 uModel 实例
     * @param testcases JSONArray 的测试用例数组
     * @param s         基解
     * @throws ParseException 异常
     */

    static void putInType2output(uModel sut, JSONArray testcases, Solution s) throws ParseException {
        String value = s.toString().replaceAll(REGEX, "");
        HashMap<Data, Object> invalidType = new HashMap<>();
        Random rand = new Random();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd ");
        Calendar cal = Calendar.getInstance();
        cal.set(2000, Calendar.JANUARY, 1);
        long start = cal.getTimeInMillis();
        cal.set(2030, Calendar.JANUARY, 1);
        long end = cal.getTimeInMillis();
        // 每个参数都可能类型违反
        for (Parameter para : sut.inputParas) {
            // 类型违反测试用例下标
            int index = 1;
            invalidType.clear();
            int randomNumber = rand.nextInt(2000)-1000;
            invalidType.put(Data.BOOL, rand.nextBoolean());
            invalidType.put(Data.NUMBER, randomNumber);
            for (Data data : invalidType.keySet()) {
                // 这样能深拷贝，暂没有更好方案
                JSONObject jsonObject = put2output(sut, value, "Test",false,null);
                JSONObject tmpJsObject = new JSONObject(true);
                JSONObject test = jsonObject.getJSONObject("Test");
                JSONObject oracle = new JSONObject(true);
                // 如果类型违背，将其加入测试集
                if (para.getType().getDataType() != data) {
                    test.put(para.getName(), invalidType.get(data));
                    oracle.put("errmsg", "parameter " + (para.getName() + " error. should be "+para.getType().getDataType().toString()+" type"));
                    tmpJsObject.put("negative", test);
                    tmpJsObject.put("Assertion", oracle);
                    testcases.add(tmpJsObject);
                }
            }
        }
    }


    /**
     * 得到参数的实际值
     *
     * @param sut     待测软件
     * @param strings 将sut里model的参数值装换成实际值
     * @return 返回一个字符串
     * @throws ParseException 解析异常
     */
    static Object getParaValue(uModel sut, String[] strings) throws ParseException {
        Parameter para = sut.parameterHashMap.get(strings[0]);
        switch (para.getType().getType()) {
            case "int":
                return Integer.parseInt(strings[1]);
            case "enum":
            case "Enum":
                String[] include = para.getType().getInclude();
                return include[Integer.parseInt(strings[1])];
            case "bool":
            case "boolean":
            case "Bool":
                return "1".equals(strings[1]);
            default:
                System.err.println("There is not " + para.getType().getType() + " type exists !!!");
                System.exit(-1);
        }
        return "";
    }



    /**
     * 写文件
     *
     * @param filePath 写文件路径
     * @param sets     写的内容
     * @throws IOException 抛出 IO 异常
     */
    static void writeFile(String filePath, String sets) throws IOException {
        FileWriter fw = new FileWriter(filePath);
        PrintWriter out = new PrintWriter(fw);
        out.write(sets);
        out.println();
        fw.close();
        out.close();
    }
}
