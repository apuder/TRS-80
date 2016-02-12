package org.puder.trs80.model;

import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.structure.BaseModel;

@Table(databaseName = AppDatabase.NAME, allFields = true)

public class AppBaseModel extends BaseModel {
    @PrimaryKey
    int id;
}