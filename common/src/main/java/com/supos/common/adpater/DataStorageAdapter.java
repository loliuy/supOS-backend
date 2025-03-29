package com.supos.common.adpater;

import com.supos.common.SrcJdbcType;

public interface DataStorageAdapter extends Adapter {

    SrcJdbcType getJdbcType();

    DataSourceProperties getDataSourceProperties();

}
