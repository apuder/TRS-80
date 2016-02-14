package org.puder.trs80.market;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.List;

@DatabaseTable(tableName = "Author")
public class Author {

    @DatabaseField(generatedId = true)
    long id;

    @DatabaseField
    String name;

    // needs to be accessible for DELETE
    List<MarketApp> apps;

/*    @OneToMany(methods = {OneToMany.Method.SAVE, OneToMany.Method.DELETE}, variableName = "apps")
    public List<MarketApp> getMyAnts() {
        if(apps == null) {
            apps = new Select()
                    .from(MarketApp.class)
                    .where(Condition.column(MarketApp$Table.AUTHORMODELCONTAINER_AUTHOR_ID).is(id))
                    .queryList();
        }
        return apps;
    }*/
}
