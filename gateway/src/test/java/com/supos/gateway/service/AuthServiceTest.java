package com.supos.gateway.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.jwt.JWT;
import com.supos.common.vo.UserInfoVo;
import org.junit.Test;

import java.util.List;

/**
 * @author xinwangji@supos.com
 * @date 2024/11/20 9:09
 * @description
 */
public class AuthServiceTest {


    @Test
    public void testJwt(){
        String token = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJDcTRaQWxkdy1oVk1iUGhkSFRVbTltX0ZTb1R0UFpKMXNNenYwX2hGMHM0In0.eyJleHAiOjE3MzE5MjQyNDgsImlhdCI6MTczMTkwNjI2NCwiYXV0aF90aW1lIjoxNzMxODk0NTUxLCJqdGkiOiJhNmU5MTM2YS02NzQxLTQwZTAtOTA2Ni1mOTFmMjA2MGNiNWMiLCJpc3MiOiJodHRwOi8vb2ZmaWNlLnVuaWJ1dHRvbi5jb206MTE0ODgva2V5Y2xvYWsvaG9tZS9hdXRoL3JlYWxtcy9zdXBvcyIsImF1ZCI6WyJyZWFsbS1tYW5hZ2VtZW50IiwiYnJva2VyIiwiYWNjb3VudCJdLCJzdWIiOiJhNGY1Y2MyZC00Y2ZjLTRmNmQtYjVlMy0yNWY1NjY4ODIyMjEiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJzdXBvcyIsInNpZCI6IjQyOTUxMjZmLTY5OGUtNGM3NS1hNjk0LTEzMGU4YWRjZTYyZSIsImFjciI6IjAiLCJhbGxvd2VkLW9yaWdpbnMiOlsiKiJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiIsImRlZmF1bHQtcm9sZXMtc3Vwb3MiXX0sInJlc291cmNlX2FjY2VzcyI6eyJyZWFsbS1tYW5hZ2VtZW50Ijp7InJvbGVzIjpbImltcGVyc29uYXRpb24iLCJjcmVhdGUtY2xpZW50Il19LCJicm9rZXIiOnsicm9sZXMiOlsicmVhZC10b2tlbiJdfSwiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsInZpZXctYXBwbGljYXRpb25zIiwidmlldy1jb25zZW50Iiwidmlldy1ncm91cHMiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsIm1hbmFnZS1jb25zZW50IiwiZGVsZXRlLWFjY291bnQiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6Im9wZW5pZCBlbWFpbCBwcm9maWxlIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJuYW1lIjoiWGluIiwicHJlZmVycmVkX3VzZXJuYW1lIjoieGlud2FuZ2ppIiwiZ2l2ZW5fbmFtZSI6IlhpbiIsImVtYWlsIjoieHdqMTIzNzg5QDE2My5jb20ifQ.aOnwbGZMkX1Ue_akCk99Pt81sjjKhKitGgM-5K_0jXU6mE-oMn4r9lYxKKpq3_43TSxoMYJo2zRDTX_h_UxGrv4DsyiHrVOkdRd5KY8Cn4NyXlFyaWbbXTtvqUqM_FWs79kStyoMx7ad1i306Uq9DRkPvF0aUfzInWPYb4VOEzZ8ihaYlKrEwHpOA6AtTQwpd8jli44JJIpo9Q0auCZHQC_CVuIU5ovc9olyAM-nt3OxTPQYxoCPGUqpRG8YVbl_oQZAhEHX8aD-UH6wzkJPiYbH_CWi9Isv2JF3mgI4kgxQ2FPUrc0nraIfXAPtb9VO9g6fhAucAz6qUSvWPdLU2Q";
        JWT jwt = JWT.of(token);
        UserInfoVo userInfoVo = JSONUtil.toBean(jwt.getPayloads(), UserInfoVo.class);
        System.out.println(userInfoVo);
        long exp = jwt.getPayloads().getLong("exp");

        // 5. 获取当前时间（UNIX 时间戳，单位：秒）
        long currentTimeInSeconds = System.currentTimeMillis() / 1000;

        // 6. 判断是否过期
        if (currentTimeInSeconds < exp) {
            System.out.println("Token is valid.");
        } else {
            System.out.println("Token is expired.");
        }
    }

    public void testMethods() {
//        String s = "/dashboard/test$get,post,put,delete";
        String s = "/dashboard/test";


        System.out.println(StrUtil.subAfter(s,"$",true));


        List<String> methods = AuthService.transMethodList(s);
        System.out.println();
    }
}
