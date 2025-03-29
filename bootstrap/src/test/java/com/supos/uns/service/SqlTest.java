package com.supos.uns.service;

import com.supos.uns.dao.mapper.UnsMapper;
import org.junit.Test;

import java.util.Arrays;

public class SqlTest {

    @Test
    public void testSql() {
        String sql = UnsMapper.UnsRefUpdateProvider.updateRefUns("1", Arrays.asList("a", "b", "c"));
        System.out.println(sql);
    }
}
