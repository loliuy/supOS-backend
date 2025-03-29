package com.supos.adpter.pg;

import com.supos.common.adpater.StreamHandler;
import com.supos.common.dto.StreamInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

class TimeScaleStreamHandler implements StreamHandler {
    @Override
    public void createStream(Map<String, String> namedSQL) {

    }

    @Override
    public void deleteStream(String name) {

    }

    @Override
    public List<StreamInfo> listByNames(Collection<String> names) {
        return List.of();
    }

    @Override
    public void stopStream(String name) {

    }

    @Override
    public void resumeStream(String name) {

    }
}
