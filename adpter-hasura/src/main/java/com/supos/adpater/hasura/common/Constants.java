package com.supos.adpater.hasura.common;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2024/12/12 15:34
 */
public class Constants {

    public static final String COLLECTION = "supos_collection";

    public static final String CREATE_REST_CMD_FORMATE = "{\"type\":\"bulk\",\"source\":\"default\",\"args\":[%s]}";

    public static final String CREATE_REST_ARGS_QUERY_COLLECTION_FORMATE = "{\"type\" : \"export_metadata\",\"args\": {}}";

    public static final String CREATE_REST_ARGS_ADD_COLLECTION_FORMATE = "{\"type\" : \"create_query_collection\",\"args\": {\"name\": \""+COLLECTION+"\",\"comment\": \"an optional comment\",\"definition\": {\"queries\": []}}}";



    public static final String CREATE_REST_ARGS_ADD_QUERY_FORMATE = "{\"type\":\"add_query_to_collection\",\"args\":{\"collection_name\":\""+COLLECTION+"\",\"query_name\":\"%s\",\"query\":\"query %s($limit: Int!,$offset: Int!) {\\r\\n  %s(limit: $limit, offset: $offset) {\\r\\n    %s  }\\r\\n}\"}}";
    public static final String CREATE_REST_ARGS_CREATE_REST_FORMATE = "{\"type\":\"create_rest_endpoint\",\"args\":{\"name\":\"%s\",\"url\":\"%s/:offset/:limit\",\"definition\":{\"query\":{\"query_name\":\"%s\",\"collection_name\":\""+COLLECTION+"\"}},\"methods\":[\"GET\"],\"comment\":\"\"}}";

    public static final String CREATE_REST_ARGS_DROP_QUERY_FORMATE = "{\"type\":\"drop_query_from_collection\",\"args\":{\"collection_name\":\""+COLLECTION+"\",\"query_name\":\"%s\"}}";
    public static final String CREATE_REST_ARGS_DROP_REST_FORMATE = "{\"type\":\"drop_rest_endpoint\",\"args\":{\"name\":\"%s\"}}";

/*    public static void main(String[] args) {

*//*        if (!collectionExist("http://192.168.236.100:8088/hasura/home/v1/metadata")) {
            createCollection("http://192.168.236.100:8088/hasura/home/v1/metadata");
        }*//*
        //System.out.println(collectionExist("http://192.168.235.123:8088/hasura/home/v1/metadata"));

        StringBuilder args1 = new StringBuilder();
        //args1.append(CREATE_REST_ARGS_ADD_COLLECTION_FORMATE);
        //String restQuery = String.format(Constants.CREATE_REST_CMD_FORMATE, args1);


        for (int i = 0; i < 1; i++) {
            CreateTopicDto topic = new CreateTopicDto();
            topic.setAlias("_lwltest_aaa_d34c4ce9ac8e4d0a8");
            StringBuilder fieldQuery = new StringBuilder();
            fieldQuery.append("_id").append("\\r\\n");
            String addQuery = String.format(Constants.CREATE_REST_ARGS_ADD_QUERY_FORMATE, topic.getTable(), topic.getTable(), topic.getTable(), fieldQuery);
            String createRest = String.format(Constants.CREATE_REST_ARGS_CREATE_REST_FORMATE, topic.getTable(), topic.getTable(), topic.getTable());
            args1.append(addQuery).append(",").append(createRest);
        }
        String restQuery = String.format(Constants.CREATE_REST_CMD_FORMATE, args1);
        try {
            HttpUtil.createPost("http://192.168.236.100:8088/hasura/home/v1/metadata").body(restQuery).then(httpResponse -> {
                log.debug("notifyHasura createRestApi[{}]: request: {}", "http://192.168.236.100:8090", restQuery);
                final int status = httpResponse.getStatus();
                final String respBody = httpResponse.body();
                log.info("createRestApiResp[{}]: body[{}], head[{}]", status, respBody, httpResponse.headers());
                if (status != 200 && respBody != null) {
                    log.error("createRestApiResp error!");
                }
            });
        } catch (Exception ex) {
            log.error("createRestApi[{}]: request: {}", "http://192.168.235.123:8090", restQuery, ex);
        }
    }

    public static boolean collectionExist(String url) {
        AtomicBoolean exist = new AtomicBoolean(false);
        try {
            HttpUtil.createPost(url).body(Constants.CREATE_REST_ARGS_QUERY_COLLECTION_FORMATE).then(httpResponse -> {
                log.debug("notifyHasura queryCollection[{}]: request: {}", url, Constants.CREATE_REST_ARGS_QUERY_COLLECTION_FORMATE);
                final int status = httpResponse.getStatus();
                final String respBody = httpResponse.body();
                log.info("queryCollection[{}]: body[{}], head[{}]", status, respBody, httpResponse.headers());
                if (status != 200 && respBody != null) {
                    log.error("queryCollection error!");
                    exist.set(false);
                    return ;
                }

                if (respBody.contains(Constants.COLLECTION)) {
                    exist.set(true);
                }
            });
        } catch (Exception ex) {
            log.error("queryCollection[{}]: request: {}", url, Constants.CREATE_REST_ARGS_QUERY_COLLECTION_FORMATE, ex);
        }
        return exist.get();
    }

    public static void createCollection(String url) {
        StringBuilder args = new StringBuilder();
        args.append(Constants.CREATE_REST_ARGS_ADD_COLLECTION_FORMATE);

        String restQuery = String.format(Constants.CREATE_REST_CMD_FORMATE, args);
        try {
            HttpUtil.createPost(url).body(restQuery).then(httpResponse -> {
                log.debug("notifyHasura createCollection[{}]: request: {}", url, restQuery);
                final int status = httpResponse.getStatus();
                final String respBody = httpResponse.body();
                log.info("createCollection[{}]: body[{}], head[{}]", status, respBody, httpResponse.headers());
                if (status != 200 && respBody != null) {
                    log.error("createCollection error!");
                }
            });
        } catch (Exception ex) {
            log.error("createCollection[{}]: request: {}", url, restQuery, ex);
        }
    }*/
}
