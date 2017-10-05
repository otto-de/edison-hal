package de.otto.edison.hal.example;

import java.io.IOException;

public class Client {

    public static void main(String[] args) {
        try (final HalShopClient shopClient = new HalShopClient()) {
            shopClient.traverse("", true);
            shopClient.traverse("Spring", false);
        } catch (final IOException e) {
            System.out.println("\n\n\tPlease first start example-springboot so we can get some products from a server");
        }
    }

}
