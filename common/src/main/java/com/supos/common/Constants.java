package com.supos.common;

public class Constants {

    public static final String RESULT_TOPIC_PREV = "_rs/";// 处理结果 topic前缀
    public static final String MSG_RAW_DATA_KEY = "_source_"; // 原始数据的 json key
    public static final String MSG_RES_DATA_KEY = "_resource_";// 处理过的 json key

    public static final int UNS_FLAG_WITH_FLOW = 1 << 0;// 是否添加数据采集流程
    public static final int UNS_FLAG_WITH_DASHBOARD = 1 << 1;// 是否添加数据看板

    public static final int UNS_FLAG_WITH_SAVE2DB = 1 << 2;// 是否持久化到数据库
    public static final int UNS_FLAG_RETAIN_TABLE_WHEN_DEL_INSTANCE = 1 << 3;// 刪除实例时是否保留数据表
    public static final int UNS_FLAG_ALARM_ACCEPT_PERSON = 1 << 4;// 报警规则接收方式 16人员
    public static final int UNS_FLAG_ALARM_ACCEPT_WORKFLOW = 1 << 5;// 报警规则接收方式 32工作流

    public static boolean withFlow(int unsFlag) {
        return (unsFlag & UNS_FLAG_WITH_FLOW) == UNS_FLAG_WITH_FLOW;
    }

    public static boolean withDashBoard(int unsFlag) {
        return (unsFlag & UNS_FLAG_WITH_DASHBOARD) == UNS_FLAG_WITH_DASHBOARD;
    }

    public static boolean withSave2db(int unsFlag) {
        return (unsFlag & UNS_FLAG_WITH_SAVE2DB) == UNS_FLAG_WITH_SAVE2DB;
    }

    public static boolean withRetainTableWhenDeleteInstance(int unsFlag) {
        return (unsFlag & UNS_FLAG_RETAIN_TABLE_WHEN_DEL_INSTANCE) == UNS_FLAG_RETAIN_TABLE_WHEN_DEL_INSTANCE;
    }

    public static final String SYSTEM_FIELD_PREV = "_";
    public static final String SYS_FIELD_CREATE_TIME = Constants.SYSTEM_FIELD_PREV + "ct";
    public static final String SYS_SAVE_TIME = Constants.SYSTEM_FIELD_PREV + "st";
    public static final String QOS_FIELD = SYSTEM_FIELD_PREV + "qos";

    /**
     * 时序类型
     */
    public static final int TIME_SEQUENCE_TYPE = 1;
    /**
     * 关系类型
     */
    public static final int RELATION_TYPE = 2;


    public static final int CALCULATION_REAL_TYPE = 3;// 实时计算
    public static final int CALCULATION_HIST_TYPE = 4;// 历史值计算

    /**
     * 报警规则类型
     */
    public static final int ALARM_RULE_TYPE = 5;

    /**
     * 引用类型
     */
    public static final int REFER_TYPE = 6;

    public static boolean isValidDataType(int type) {
        return type >= TIME_SEQUENCE_TYPE && type <= REFER_TYPE;
    }

    /**
     * 分页条数
     */
    public final static int DEFAULT_PAGE_SIZE = 20;
    /**
     * 分页起始页
     */
    public final static int DEFAULT_PAGE_NUM = 1;

    /**
     * 最大分页条数
     */
    public final static int MAX_PAGE_SIZE = 1000;

    public static final int SQL_BATCH_SIZE = 200;

    public static final String ROOT_PATH = "/data";

    public static final String LOG_PATH = "/logs";

    public static final String EXCEL_ROOT = "/excel/";

    public static final String EXAMPLE_ROOT = "/example/";

    public static final String UNS_ROOT = "/uns/";

    public static final String SYSTEM_ROOT = "/system/";

    public final static String EXCEL_TEMPLATE_PATH = "/templates/all-namespace.xlsx";
    public final static String JSON_TEMPLATE_PATH = "/templates/all-namespace.json";
    public final static String EXCEL_OUT_PATH = "/export/all-namespace.xlsx";
    public final static String JSON_OUT_PATH = "/export/all-namespace.json";

    public static final String ACCESS_TOKEN_KEY = "supos_community_token";

    public static final String ALIAS_REG = "[a-zA-Z_\\$][a-zA-Z0-9_\\$]*$";
    public static final String TOPIC_REG = "^[\\u4e00-\\u9fa5a-zA-Z0-9/_-]+$";

    public static final String VAR_PREV = "a";

    public static final String ALARM_TOPIC_PREFIX = "/$alarm";


    public static final String DEFAULT_ROLE_ID = "d12d7ca2-34e1-4f26-9a03-6b4f7f411567";

    public static final long ATTACHMENT_MAX_SIZE = 10 * 1024 * 1024;

    public static final String TD_JDBC_URL = "tdengine:6041";

    public static final String PG_JDBC_URL = "postgresql:5432";

    public static final String AUTH_CHECK_KONG_PLUGIN_ID = "1845ee75-d704-40e1-a8b0-aa2baaf9d71b";

    public static final String EMQX_API_KEY = "b441dbabd9bd5c26";
    public static final String EMQX_SECRET_KEY = "59CdRlRvDaygamiil6789A2JvbXfO9ADRcLEcgxB9CYVv5Y";

    public static final String UNKNOWN_USER = "Unknown User";

    public static final String EXAMPLE_FUXA_FILE = "fuxa-";
    public static final String EXAMPLE_GRAFANA = "grafana-";
    public static final String EXAMPLE_METADATA= "metadata.json";
    public static final String EXAMPLE_PROTOCOL= "protocol.json";

    public static final String FUXA_API_URL = "http://fuxa:1881";


}
