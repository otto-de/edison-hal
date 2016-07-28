package de.otto.edison.hal.example.shop;

import java.util.UUID;

/**
 * A product in our example HAL shop.
 *
 * Only a few attributes, just enough to illustrate how to use edison-hal.
 */
public final class Product {

    public final String id = UUID.randomUUID().toString();
    public final String title;
    public final String description;
    public final long   retailPrice;

    /**
     * Build a new Product.
     *
     * @param title          title / name of the product
     * @param description    some short description of the product
     * @param retailPrice    the retail price in cent.
     */
    public Product(final String title,
                    final String description,
                    final long retailPrice) {
        this.title = title;
        this.description = description;
        this.retailPrice = retailPrice;
    }

}
