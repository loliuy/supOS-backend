package com.supos.adpter.kong.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Component
@Slf4j
public class KongAdapterCommandRunner implements CommandLineRunner {

    public static final String LOCAL_MENU_CHECKED_STORAGE_PATH = "/data/menu/check-menu.properties";

    public static Map<String, String> localMenus = new HashMap<>();

    @Override
    public void run(String... args) throws Exception {
        File file = new File(LOCAL_MENU_CHECKED_STORAGE_PATH);
        if (!file.exists()) {
            // create parent file
            new File(file.getParent()).mkdirs();
            log.info("==> skip load local menu data, because file is not exist");
            return;
        }
        localMenus = loadPropertiesToHashMap(LOCAL_MENU_CHECKED_STORAGE_PATH);
    }

    private HashMap<String, String> loadPropertiesToHashMap(String filePath) {
        HashMap<String, String> map = new HashMap<>();
        Properties properties = new Properties();
        try (InputStream input =  new FileInputStream(filePath)) {
            if (input == null) {
                log.info("==> skip load local menu data, because file is empty");
                return map;
            }
            // 加载属性文件
            properties.load(input);
            // 将属性存入 HashMap
            for (String key : properties.stringPropertyNames()) {
                map.put(key, properties.getProperty(key));
            }
            log.info("==> load success, checked menu is {}", map);
        } catch (IOException ex) {
            log.error("local file({}) load error", filePath, ex);
        }
        return map;
    }
}
