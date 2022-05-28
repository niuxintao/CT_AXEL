package Boot;

import Service.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.ParseException;

@SpringBootApplication
@RestController
@CrossOrigin
public class TsgenSpringBoot {

    @PostMapping("/tsgen")
    public String getByDbId(@RequestParam("id") String id, @RequestParam("url") String url, @RequestParam("user") String user, @RequestParam("pwd") String pwd) {
        DdFormat df = new DdFormat(id, url, user, pwd);
        try {
            // 将数据库中输入、覆盖准则、输出信息整合到 JSONObject 对象
            JSONObject jsonObject = df.projectJson();
            String projectName = df.projectName();
            uModel sut = new uModel(jsonObject);
            JSONArray testcases = new JSONArray();
            int[] caseNum = Outputs.getOutput(testcases, sut);
            int totalNum = testcases.size();
            // df.getJdbConnection().executeUpdate("create table output(id int auto_increment primary key, content MEDIUMTEXT) charset utf8mb4", null);
            // 将结果放入数据库中, id 自增
//            df.getJdbConnection().executeUpdate("insert into output (id, projectName, content) values (null, " + "'"+projectName+"','" + testcases + "')", null);

            System.out.println();
            System.out.println("----------------------------------------------");
            System.err.println(sut.getQueryInfo());

            // 将获取的json数据封装一层，然后在给返回
            JSONObject result = new JSONObject(true);
//            result.put("msg", "ok");
//            result.put("method", "request");
            result.put("total testcases nums", totalNum);
            result.put("positive testcases nums", caseNum[0]);
            result.put("metaRelation testcases pairs", caseNum[1]);
            result.put("negative testcases nums", caseNum[2]);
            result.put("testcases", testcases);
            String ret = JSON.toJSONString(result, SerializerFeature.WriteMapNullValue);
            df.getJdbConnection().executeUpdate("insert into output (id, projectName, content) values (null, " + "'"+projectName+"','" + ret + "')", null);
            return ret;
        } catch (SQLException | Xeger.FailedRandomWalkException | ParseException throwAbles) {
            throwAbles.printStackTrace();
        } finally {
            df.getJdbConnection().closeConnection();
        }
        return null;
    }

    public static void main(String[] args) {
        SpringApplication.run(TsgenSpringBoot.class, args);
    }
}