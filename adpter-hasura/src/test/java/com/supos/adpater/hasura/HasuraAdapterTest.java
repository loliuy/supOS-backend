package com.supos.adpater.hasura;

import com.alibaba.fastjson.JSON;
import org.junit.Test;

import java.util.Arrays;

public class HasuraAdapterTest {

    @Test
    public void testHasuraTrack() {
        String json = HasuraAdapter.getPostBody("public", HasuraAdapter.TRACK, Arrays.asList("/pg/rta"));
        System.out.println(JSON.toJSONString(JSON.parseObject(json), true));
    }

    @Test
    public void testHasuraUnTrack() {
        String paramJson = HasuraAdapter.getPostBody("public", HasuraAdapter.UnTRACK, Arrays.asList("/pg/rta"));
        System.out.println(JSON.toJSONString(JSON.parseObject(paramJson), true));

        com.alibaba.fastjson.JSONObject params = JSON.parseObject(paramJson);
        com.alibaba.fastjson.JSONArray array = params.getJSONArray("args");

        params.put("type", "bulk_keep_going");
        com.alibaba.fastjson.JSONArray head2 = new com.alibaba.fastjson.JSONArray(2);
        {
            com.alibaba.fastjson.JSONObject reload = new com.alibaba.fastjson.JSONObject();
            head2.add(reload);
            reload.put("type", "reload_metadata");
            com.alibaba.fastjson.JSONObject reloadArgs = new com.alibaba.fastjson.JSONObject();
            reload.put("args", reloadArgs);
            reloadArgs.put("reload_remote_schemas", true);
            reloadArgs.put("reload_sources", true);
            reloadArgs.put("recreate_event_triggers", true);
        }
        {
            com.alibaba.fastjson.JSONObject dropInconsistentMetadata = new com.alibaba.fastjson.JSONObject();
            head2.add(dropInconsistentMetadata);
            dropInconsistentMetadata.put("type", "drop_inconsistent_metadata");
            dropInconsistentMetadata.put("args", new com.alibaba.fastjson.JSONObject());
        }
        array.addAll(0, head2);

        System.out.println(JSON.toJSONString(params, true));
    }
}
