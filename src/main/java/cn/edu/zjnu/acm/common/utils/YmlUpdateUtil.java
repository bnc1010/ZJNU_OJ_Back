package cn.edu.zjnu.acm.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public class YmlUpdateUtil {
    public static void updateYamlFile(String src, String root, String [] keys, Object [] values) {
        Yaml yaml = new Yaml();
        FileWriter fileWriter = null;
        //层级map变量
        Map<String, Object> nowMap, resultMap;
        try {
            //读取yaml文件，默认返回根目录结构
            resultMap = (Map<String, Object>) yaml.load(new FileInputStream(new File(src)));
            //get出spring节点数据
            nowMap = (Map<String, Object>) resultMap.get(root);
            //get出数据库节点数据
            int len = keys.length;
            for (int i = 0; i < len; i++){
                nowMap.put(keys[i], values[i]);
            }
            fileWriter = new FileWriter(new File(src));
            //用yaml方法把map结构格式化为yaml文件结构
            fileWriter.write(yaml.dumpAsMap(resultMap));
            fileWriter.flush();
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("对不起，yaml文件修改失败！");
        }
    }
}
