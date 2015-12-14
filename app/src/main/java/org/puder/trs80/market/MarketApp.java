package org.puder.trs80.market;

import android.graphics.Bitmap;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.annotation.ForeignKey;
import com.raizlabs.android.dbflow.annotation.ForeignKeyReference;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;
import com.raizlabs.android.dbflow.structure.container.ForeignKeyContainer;

@Table(databaseName = MarketDatabase.NAME, allFields = true)

public class MarketApp extends BaseModel {
    @Column
    @PrimaryKey(autoincrement = true)
    private long id;
    @Column
    private String name;
    @Column
    private double price;
    @Column
    private String thumbnail_url;
    @Column
    private int atype;

    public static final int TYPE_GAME = 0;
    public static final int TYPE_OTHER = 1;

    @Column
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
    }

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
}
