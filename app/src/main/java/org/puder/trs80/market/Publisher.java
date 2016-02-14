package org.puder.trs80.market;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.List;

@DatabaseTable(tableName = "Publisher")
public class Publisher {

    @DatabaseField(generatedId = true)
    long id;

    @DatabaseField
    private String name;

    public String getName() {
        return  name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // needs to be accessible for DELETE
    List<MarketApp> apps;

/*    @OneToMany(methods = {OneToMany.Method.SAVE, OneToMany.Method.DELETE}, variableName = "apps")
    public List<MarketApp> getMyAnts() {
        if(apps == null) {
            apps = new Select()
                    .from(MarketApp.class)
                    .where(Condition.column(MarketApp$Table.PUBLISHERMODELCONTAINER_PUBLISHER_ID).is(id))
                    .queryList();
        }
        return apps;
    }*/
}
