package com.supos.common.adpater;

import com.supos.common.dto.StreamInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface StreamHandler {

    void createStream(Map<String, String> namedSQL);

    void deleteStream(String name);

    List<StreamInfo> listByNames(Collection<String> names);

    void stopStream(String name);

    void resumeStream(String name);

}
