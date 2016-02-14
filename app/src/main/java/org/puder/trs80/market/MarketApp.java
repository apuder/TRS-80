package org.puder.trs80.market;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "MarketApp")

public class MarketApp {
    @DatabaseField(generatedId = true)
    long id;

    @DatabaseField
    String name;

    @DatabaseField
    double price;

    @DatabaseField
    String thumbnail_url;

    @DatabaseField
    private int atype;

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return this.id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getPrice() {
        return this.price;
    }

    public void setThumbnail_url(String thumbnail_url) {
        this.thumbnail_url = thumbnail_url;
    }

    public String getThumbnail_url() {
        return this.thumbnail_url;
    }

    public int getAtype() {
        return atype;
    }

    public void setAtype(int atype) {
        this.atype = atype;
    }


/*    @Column
    @ForeignKey(
            references = {@ForeignKeyReference(columnName = "author_id",
                    columnType = Long.class,
                    foreignColumnName = "id")},
            saveForeignKeyModel = false)
    ForeignKeyContainer<Author> authorModelContainer;

    //setting the model for the author
    public void associateAuthor(Author author) {
        authorModelContainer = new ForeignKeyContainer<>(Author.class);
        authorModelContainer.setModel(author);
    }

    @Column
    @ForeignKey(
            references = {@ForeignKeyReference(columnName = "publisher_id",
                    columnType = Long.class,
                    foreignColumnName = "id")},
            saveForeignKeyModel = false)
    ForeignKeyContainer<Publisher> publisherModelContainer;

    //setting the model for the publisher
    public void associatePublisher(Publisher publisher) {
        publisherModelContainer = new ForeignKeyContainer<>(Publisher.class);
        publisherModelContainer.setModel(publisher);

    }*/
}