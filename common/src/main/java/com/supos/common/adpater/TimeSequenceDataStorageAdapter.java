package com.supos.common.adpater;

public interface TimeSequenceDataStorageAdapter extends DataStorageAdapter {
    default StreamHandler getStreamHandler() {
        return null;
    }
    String execSQL(String sql);
}
