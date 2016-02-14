package org.puder.trs80.market;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "market_app")
public class MarketApp {
    public static final String ID_FIELD = "id";
    @DatabaseField(generatedId = true, columnName = ID_FIELD)
    Long id;

    public static final String NAME_FIELD = "name";
    @DatabaseField(columnName = NAME_FIELD)
    String name;

    public static final String PRICE_FIELD = "price";
    @DatabaseField(columnName = PRICE_FIELD)
    double price;

    public static final String CATEGORY_FIELD = "category";
    @DatabaseField(columnName = "CATEGORY_FIELD")
    int category;

    public static final String THUMBNAIL_URL_FIELD = "thumbnail_url";
    @DatabaseField(columnName = THUMBNAIL_URL_FIELD)
    String thumbnail_url;

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

    public int getCategory() {
        return category;
    }

    public void setCategory(int category) {
        this.category = category;
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