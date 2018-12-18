# Edison HAL

Library to produce and consume [application/hal+json](https://tools.ietf.org/html/draft-kelly-json-hal-08) 
representations of REST resources using Jackson.

## 1. Status

[![Build Status](https://travis-ci.org/otto-de/edison-hal.svg)](https://travis-ci.org/otto-de/edison-hal) 
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.otto.edison/edison-hal/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.otto.edison/edison-hal)
[![Javadoc](https://www.javadoc.io/badge/de.otto.edison/edison-hal.svg)](https://www.javadoc.io/doc/de.otto.edison/edison-hal)
[![codecov](https://codecov.io/gh/otto-de/edison-hal/branch/master/graph/badge.svg)](https://codecov.io/gh/otto-de/edison-hal)

## 2. About

At otto.de, microservices should only communicate via REST APIs with other 
 microservices. HAL is a nice format to implement the HATEOAS part 
 of REST. Edison-HAL is a library to make it easy to produce
 and consume HAL representations for your REST APIs.

Currently, there are only a couple of libraries supporting HAL and even
 less that support the full media type including all link properties,
 curies (compact URIs) and embedded resources.
 
Spring HATEOAS, for example, is lacking many link properties, such as 
 title, name, type and others. 

## 3. Features

Creating HAL representations:
* Links with all specified attributes like rel, href, profile, type, name, title, etc. pp.
* Embedded resources, even deeply nested
* Curies in links and embedded resources
* Generation of HAL representations using Jackson annotations

Parsing HAL representations:
* Mapping application/hal+json to Java classes using Jackson. This also works for deeply nested embedded items.
* Simple domain model to access links, embedded resources etc.
* Curies are automatically resolved: Given a curi with name=ex and href=http://example.com/rels/{rel}, links
and embedded items can be access either in curied format `ex:foo`, or expanded format `http://exampl.com/rels/foo` 

Traversion of HAL representations:
* Simple client-side navigation through linked and embedded REST resources using
Traverson API
* Embedded resources are transparantly used, if present. 
* Curies are resolved transparantly, too. Clients of the Traverson API do not need to 
know anything about curies or embedded resources.

## 4. Usage

Before using this library, you should have a good understanding of the Hypertext Application Language (HAL):

* Read Mike's article about [HAL](http://stateless.co/hal_specification.html) and 
* the current [draft of the RFC](https://tools.ietf.org/html/draft-kelly-json-hal-08).

### 4.1 Include edison-hal into your project:
 
```gradle
    dependencies {
        compile "de.otto.edison:edison-hal:2.0.0",
        ...
    }
```
  
### 4.2 Provide a class for the representation of your REST API

If your representation does not need additional attributes beside of
the properties defined in application/hal+json, you can create a
HalRepresentations like this:

```java
public class Example_4_2_1 {
    
    final HalRepresentation representation = new HalRepresentation(
            linkingTo()
                    .self("http://example.org/test/bar")
                    .item("http://example.org/test/01")
                    .item("http://example.org/test/02")
                    .build(),
            embeddedBuilder()
                    .with("item", asList(
                            new HalRepresentation(linkingTo().self("http://example.org/test/01").build()),
                            new HalRepresentation(linkingTo().self("http://example.org/test/02").build())))
                    .build());
}
```

Using Jackson's ObjectMapper (e.g. `new ObjectMapper().writeValueAsString(representation)), this will will be 
serialized into the following application/hal+json document:

```json
{
  "_links" : {
    "self" : {
      "href" : "http://example.org/test/bar"
    },
    "item" : [ {
      "href" : "http://example.org/test/01"
    }, {
      "href" : "http://example.org/test/02"
    } ]
  },
  "_embedded" : {
    "item" : [ {
      "_links" : {
        "self" : {
          "href" : "http://example.org/test/01"
        }
      }
    }, {
      "_links" : {
        "self" : {
          "href" : "http://example.org/test/02"
        }
      }
    } ]
  }
}
```

In many cases, you will need custom attributes in addition to HAL's `_links` and `_embedded` properties. Extending
`HalRepresentation` and adding attributes as you would do in other types (de-)serialized using Jackson is easy:

```java
public class Example_4_2_2 extends HalRepresentation {
    @JsonProperty("someProperty")
    private String someProperty = "some value";
    @JsonProperty("someOtherProperty")
    private String someOtherProperty = "some other value";

    Example_4_2_2() {
        super(linkingTo()
                .self("http://example.org/test/bar")
                .build()
        );
    }
}
```

### 4.3 Single Link-Objects vs. Arrays of Link-Objects

A resource may have multiple links that share the same link relation.

For link relations that may have multiple links, we use an array of links - even if there is no or only a single
link available.

The `item` Link-Relation Type, for example, is often used for items of a collection resource. Because a collection
may have multiple items, an array is used:
```json
{
    "_links": {
        "item": [
            { "href": "http://example.com/items/foo" },
            { "href": "http://example.com/items/bar" }
        ]
    }
}
```

A collection with only a single item link, should still use an array of link objects, because otherwise clients (at
least those not implemented using Edison HAL) would have to handle both arrays and single link objects for the same 
Link-Relation Type. 

```json
{
    "_links": {
        "item": [
            { "href": "http://example.com/items/the-single-item" }
        ]
    }
}
```

While `item` links will most likely be rendered as an array, links like `self`, `next`, `prev` etc. generally are 
single link objects:
```json
{
    "_links": {
        "self": { "href": "http://example.com/foo/42" },
        "next": { "href": "http://example.com/foo/43" },
        "prev": { "href": "http://example.com/foo/41" }
        
    }
}
```

> Note: If you're unsure whether the link should be singular, assume it will be multiple.
> If you pick singular and find you need to change it, you will need to create a new link
> relation or face breaking existing clients.

_[text + example taken from http://stateless.co/hal_specification.html)_

When creating a `HalRepresentation` object, a `de.otto.edison.hal.Links` object is used to specify the `_links` section
of the document. In order to create an instance of `Links`, a `Links.Builder` is used: 

```java
public class Example_4_3 {
    final HalRepresentation representation = new HalRepresentation(
            linkingTo()                                                 // this creates a Links.Builder instance 
                    .self("http://example.com/foo/42")                  // adds a single 'self' link
                    .single(link("next", "http://example.com/foo/43"))  // adds a single 'next' link
                    .single(link("prev", "http://example.com/foo/41"))  
                    .array(                                             // now we add some links that will become arrays 
                            link("item", "http://example.com/bar/01"),  // of links
                            link("item", "http://example.com/bar/02"),  
                            link("item", "http://example.com/bar/03")
                    )
                    .build()                                            // Finally, we build the Links object
    );
}
```   

The `Links.Builder` has a number of methods used to add links with a predefined Link-Relation Types like, for example,
`self`, `curies` or `item`. The JavaDoc specifies, which links will be rendered as an array, and which as a single 
link object. Other links can be explicitly added using `single(Link, Link...)`, `single(List<Link>)`, 
`array(Link, Link...)` or `array(List<Link>)`. 

### 4.4 Curies

The semantics of links in application/hal+json documents are specified by the link-relation type (rel). It is 
recommended to use [predfined and documented rels](https://www.iana.org/assignments/link-relations/link-relations.xhtml) 
like, for example, `self`, `prev` or `next`. 

In case of custom Link-Relation Types, fully qualified URIs should be used: a link to a product, for example, could be
specified by something like `http://spec.example.com/link-relations/product`. The URI should be resolvable, pointing to
some human readable documentation that describes the semantics of the Link-Relation Type. 


A curi ('compact URI') is a feature in HAL+JSON that is helpful to use URIs for custom Link-Relation Types while 
keeping the document format compact.

For example:
```json
{
  "_links" : {
    "http://spec.example.com/link-relations/product" : [
      {"href" : "http://example.com/products/1"},
      {"href" : "http://example.com/products/1"}
    ],
    "http://spec.example.com/link-relations/shoppingcart" :
      {"href" : "http://example.com/shoppingcart/42"},
    "http://spec.example.com/link-relations/wishlist" :
      {"href" : "http://example.com/wishlist/0815"}
  },
  "_embedded" : {
    "http://spec.example.com/link-relations/product" : [
      {
        "_links" : {
          "http://spec.example.com/link-relations/similar-articles" : 
            {"href" : "http://example.com/recommendations/similar-articles?productId=1"}
        },
        "title" : "Jeans", 
        "more" : "attributes"
      },
      {
        "_links" : {
          "http://spec.example.com/link-relations/similar-articles" : 
            {"href" : "http://example.com/recommendations/similar-articles?productId=2"}
        },
        "title" : "Shirt", 
        "more" : "attributes"
      }
    ]  
  }
}
```  

By specifying a curi, the same document would look like this:
```json
{
  "_links" : {
    "curies" : [
      {"name" : "ex", "href" : "http://spec.example.com/link-relations/{rel}", "templated" : true}
    ],
    "ex:product" : [
      {"href" : "http://example.com/products/1"},
      {"href" : "http://example.com/products/1"}
    ],
    "ex:shoppingcart" :
      {"href" : "http://example.com/shoppingcart/42"},
    "ex:wishlist" :
      {"href" : "http://example.com/wishlist/0815"}
  },
  "_embedded" : {
    "ex:product" : [
      {
        "_links" : {
          "ex:similar-articles" : 
            {"href" : "http://example.com/recommendations/similar-articles?productId=1"}
        },
        "title" : "Jeans", 
        "more" : "attributes"
      },
      {
        "_links" : {
          "ex:similar-articles" : 
            {"href" : "http://example.com/recommendations/similar-articles?productId=2"}
        },
        "title" : "Shirt", 
        "more" : "attributes"
      }
    ]  
  }
}
```  

Curies are fully supported by edison-hal: By just adding a curi to the links of a HalRepresentation, the library
takes care of matching link-relation types and replacing them during serialization:

```java
public class Example_4_4_1 {
    final HalRepresentation representation = new HalRepresentation(
            linkingTo()
                    .curi("x", "http://example.org/rels/{rel}")
                    .curi("y", "http://example.com/rels/{rel}")
                    .single(
                            link("http://example.org/rels/foo", "http://example.org/test"))
                    .array(
                            link("http://example.com/rels/bar", "http://example.org/test/1"),
                            link("http://example.com/rels/bar", "http://example.org/test/2"))
                    .build()
            );
}
``` 
...will be rendered as
```json
{
  "_links" : {
    "curies" : [ {
      "href" : "http://example.org/rels/{rel}",
      "templated" : true,
      "name" : "x"
    }, {
      "href" : "http://example.com/rels/{rel}",
      "templated" : true,
      "name" : "y"
    } ],
    "x:foo" : {
      "href" : "http://example.org/test"
    },
    "y:bar" : [ {
      "href" : "http://example.org/test/1"
    }, {
      "href" : "http://example.org/test/2"
    } ]
  }
}
```  
In the same way, curies are automatically applied to link-relation types in embedded resources, even if the embedded
resources itself have nested embedded resources.
```json
{
  "_links" : {
    "curies" : [
      {"name" : "x", "href" : "http://example.org/rels/{rel}", "templated" : true},
      {"name" : "y", "href" : "http://example.com/rels/{rel}", "templated" : true}
    ],
    "x:foo" : {"href" : "http://example.org/test"},
    "y:bar" : {"href" : "http://example.org/test"}
  },
  "_embedded" : {
    "x:deeply-embedded" : {
      "_links" : {
        "x:foobar" : {"href" : "http://example.org/test"},
        "y:barbar" : {"href" : "http://example.org/test"}
      },
      "_embedded" : {
        "x:even-deeper" : {
          "_links" : {
            "x:barfoo" : {"href" : "http://example.org/test"},
            "y:foofoo" : {"href" : "http://example.org/test"}
          }
        }      
      }
    }
  }
}
```  

### 4.5 Serializing HalRepresentations

To convert your representation class into a application/hal+json document, you can use Jackson's 
ObjectMapper directly:
```java
    final ObjectMapper mapper = new ObjectMapper();
    
    final String json = mapper.writeValueAsString(representation);
```

Using Spring MVC, you can directly return HalRepresentations from your controller methods:

```java
    @RequestMapping(
            value = "/my/hal/resource",
            produces = {
                    "application/hal+json",
                    "application/json"},
            method = GET
    )
    public MyHalRepresentation getMyHalResource() {    
        return new MyHalRepresentation(getTheInputData());
    }
```

### 4.6 Parsing application/hal+json documents

A HAL document can be parsed using Jackson, too. 

Given some custom `HalRepresentation` with links, attributes and embedded objects like this:

```java
public class TestHalRepresentation extends HalRepresentation {
    @JsonProperty("someProperty")
    private String someProperty;
    @JsonProperty("someOtherProperty")
    private String someOtherProperty;

    TestHalRepresentation() {
        super(
                linkingTo()
                        .self("http://example.org/test/foo")
                        .build(),
                embedded("bar", singletonList(new HalRepresentation(
                        linkingTo()
                                .self("http://example.org/test/bar/01")
                                .build()
                )))
        );
    }
}

```

The following code will parse a HAL document into an instance of `TestHalRepresentation`:
```java
public class Example_4_6_1 {
    @Test 
    public void shouldParseHal() throws IOException {

        // given
        final String json =
                "{" +
                        "\"someProperty\":\"1\"," +
                        "\"someOtherProperty\":\"2\"," +
                        "\"_links\":{\"self\":{\"href\":\"http://example.org/test/foo\"}}," +
                        "\"_embedded\":{\"bar\":[" +
                                "{" +
                                "\"_links\":{\"self\":{\"href\":\"http://example.org/test/bar/01\"}}" +
                                "}" +
                        "]}" +
                "}";

        // when
        final TestHalRepresentation result = new ObjectMapper().readValue(json.getBytes(), TestHalRepresentation.class);

        // then
        assertThat(result.someProperty, is("1"));
        assertThat(result.someOtherProperty, is("2"));

        // and
        final Links links = result.getLinks();
        assertThat(links.getLinkBy("self").get(), is(self("http://example.org/test/foo")));

        // and
        final List<HalRepresentation> embeddedItems = result.getEmbedded().getItemsBy("bar");
        assertThat(embeddedItems, hasSize(1));
        assertThat(embeddedItems.get(0).getLinks().getLinkBy("self").get(), is(link("self", "http://example.org/test/bar/01")));
    }
}
```
#### 4.6.1 Configuring the ObjectMapper
There are some special cases, where it is required to configure the ObjectMapper as follows:
```java    
    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
```

This will be necessary, if there are single embedded items for a link-relation type, instead of an array of items:
```json
{
    "_embedded" : {
        "item" : {
            "msg" : "This item can not be parsed without the ObjectMapper configuration",
            "_links" : { "self" : {"href" : "http://example.com/a-single-embedded-item"}}
        },
        "multipleItems": [
          {"foo" : 42},
          {"foo" : 4711}
        ]
    }
}
```

#### 4.6.2 Using the HalParser

If you want to parse embedded resources as a sub-class of HalRepresentation, you need to use the *HalParser*:

```java
class Example_4_6_2 {
    @Test
    public void shouldParseEmbeddedItemsWithSpecificType() throws IOException {
        // given
        final String json =
                "{" +
                "   \"_embedded\":{\"bar\":[" +
                "       {" +
                "           \"someProperty\":\"3\"," +
                "           \"someOtherProperty\":\"3\"," +
                "           \"_links\":{\"self\":[{\"href\":\"http://example.org/test/bar/01\"}]}" +
                "       }" +
                "   ]}" +
                "}";
        
        // when
        final HalRepresentation result = HalParser
                .parse(json)
                .as(HalRepresentation.class, withEmbedded("bar", EmbeddedHalRepresentation.class));
        
        // then
        final List<EmbeddedHalRepresentation> embeddedItems = result
                .getEmbedded()
                .getItemsBy("bar", EmbeddedHalRepresentation.class);
        
        assertThat(embeddedItems, hasSize(1));
        assertThat(embeddedItems.get(0).getClass(), equalTo(EmbeddedHalRepresentation.class));
        assertThat(embeddedItems.get(0).getLinks().getLinkBy("self").get(), is(link("self", "http://example.org/test/bar/01")));
    }
}
```

The 'ObjectMapper' used by the HalParser by default will automatically register all Jackson modules using `ObjectMapper#findAndRegisterModules()`. You may specify a different `ObjectMapper` instance like this:

```java
final ObjectMapper myObjectMapper = new ObjectMapper();
final HalRepresentation result = HalParser
        .parse(json, myObjectMapper)
        .as(HalRepresentation.class);
```

### 4.6.3 Nested embedded resources

The `HalParser` is also able to parse nested embedded resources into different subclasses of HalRepresentation. For 
example, you could parse a shopping-cart document with embedded shopping-cart items, which in turn have embedded 
products:

```json
{
  "_links" : {"self" : {"href" : "http://shop.example.com/shoppingcarts/42"}},
  "_embedded" : {
    "http://example.com/rels/customer" : {
      "_links" : {"self" : {"href" : "http://shop.example.com/customers/1234"}},
      "customerId" : "some customer id",
      "name" : "Max Mustermann"
    },
    "item" : [
      {
        "_links" : {"self" : {"href" : "http://shop.example.com/shoppingcarts/42/item/1"}},
        "_embedded" : {
          "item" : [
            {
              "_links" : {"self" : {"href" : "http://shop.example.com/products/0815"}},
              "count" : 2
            },
            {
              "_links" : {"self" : {"href" : "http://shop.example.com/products/4711"}},
              "count" : 2
            }
          ]
        }
      },
      {
      "more" : "shopping-cart items..."
      }
    ]
  }
}
``` 

This shopping-cart document could be parsed into some java `ShoppingCart` object containing embedded `Customer`and 
`ShoppingCartItem`s, which in turn have embedded `Product`s like this:

```java
final ShoppingCart cart = HalParser
        .parse(json)
        .as(ShoppingCart.class, 
            withEmbedded("http://example.com/rels/customer", Customer.class),
            withEmbedded("item", ShoppingCartItem.class,
                withEmbedded("item", Product.class)
            )
        );
```  
`EmbeddedTypeInfo.withEmbedded()` is a factory method used to specify the types of embedded items. By nesting
the type infos, you can tell the HalParser about the types of any embedded item type, even deeply nested in complex
HAL representations.

### 4.6.4 Unmapped attributes

`HalRepresentation` supports access to attributes, that could not be mapped to properties. For example:
```json
{
  "foo" : "Hello World",
  "bar" : [
    "Hello",
    "World"
  ]
}
```
Using `HalParser` or Jackson's `ObjectMapper`, this plain json document can be parsed into a HalRepresentation:
```java
        HalRepresentation resource = HalParser.parse(json).as(HalRepresentation.class);
```
Because HalRepresentation does neither have `foo` nor `bar`properties, the values of these properties can only
be accessed using `HalRepresentation.getAttributes` or `HalRepresentation.getAttribute(name)` The values of 
such attributes are Jackson `JsonNode` objects:
```java
        assertThat(
                resource.getAttributes().keySet(), 
                containsInAnyOrder("foo", "bar"));
        assertThat(
                resource.getAttribute("foo").asText(), 
                is("Hello World"));
        assertThat(
                resource.getAttribute("bar").at("/0").asText(), 
                is("Hello"));
        assertThat(
                resource.getAttribute("bar").at("/1").asText(), 
                is("World"));

```
 
### 4.7 Using the Traverson

Traverson is a utility to make it easy to traverse linked and/or embedded resources using link-relation types:

```java
    class ProductHalJson extends HalRepresentation {
        @JsonProperty String price;
    }
    
    void printProducts(final String query) {
        traverson(this::getHalJson)
                .startWith(HOME_URI)
                .follow(REL_SEARCH, withVars("q", query))
                .follow(REL_PRODUCT)
                .streamAs(ProductHalJson.class)
                .forEach(product->{
                    System.out.println(product.title + ": " + product.price);
                });
    }
    
    String getHalJson(final Link link) {
        try {
            final HttpGet httpget = new HttpGet(HOST + link.getHref());
            if (link.getType().isEmpty()) {
                httpget.addHeader("Accept", "application/hal+json");
            } else {
                httpget.addHeader("Accept", link.getType());
            }
            final HttpEntity entity = httpclient.execute(httpget).getEntity();
            return EntityUtils.toString(entity);
        } catch (final IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
```

The Traverson is automatically taking care of embedded resources: if a linked resource is already embedded, it is used 
instead of resolving the link. Only if it is not embedded, the Traverson is following the links. 

Just like the `HalParser`, the Traverson is using an `ObjectMapper` that is registering all Jackson Modules automatically. The default `ObjectMapper` can be replaced by a custom instance by creating the `Traverson` using the static factory method
`Traverson.traverson(LinkResolver, ObjectMapper)`.

### 4.8 Selecting Links

In some situations, there are multiple links to the same resource, but with different `profile`,
`name` or `type` attributes. The type, for example, could be used to distinguish links to a web page
from links pointing to a JSON representation of the resource:

```json
{
    "_links": {
        "user": [
            { "href": "http://example.com/users/42.json", "type": "application/json"},
            { "href": "http://example.com/users/42.html", "type": "text/html"}
        ]
    }
}
```

The `Traverson` can be used to navigate linked resources using predicates to select one of multiple links:    

```java
User user = traverson(this::getHalJson)
        .startWith("http://example.com/baskets/4711")
        .follow("user", LinkPredicates.havingType("application/json"))
        .getResourceAs(User.class);
```

Because `Traverson.follow()` only expects a `java.util.function.Predicate<Link>`, you are free to write your own 
predicates, of simply compose them:
```java
Predicate<Link> selectJsonOrHal = havingType("application/json").or(havingType("application/hal+json"));

...
traverson.follow("user", selectJsonOrHal)
...
```
### 4.9 Paging over HAL resources

Iterating over pages of items is supported by the Traverson, too. `Traverson.paginateNext()` can be used
to iterate pages by following 'next' links. The callback function provided to `paginateNext()` is
called for every page, until either `false` is returned from the callback, or the last page is reached.

The `Traverson` parameter of the callback function is then used to traverse the items of the page.

```java
        traverson(this::getHalJson)
                .startWith("/example/products")
                // Page of products by following link-relation type 'next':
                .paginateNext((Traverson pageTraverson) -> {
                    // Follow all 'item' links
                    pageTraverson
                            .follow("item")
                            .streamAs(ProductHalRepresentation.class)
                            .forEach(product->{
                                System.out.println(product.title + ": " + product.price);
                            };
                    // proceed to next page of products:
                    return true;
                });

```

It is also possible to page by following other link-relation types using `Traverson.paginatePrev` (for 'prev') or
`Traverson.paginate()` for other rels.

If the page is needed as a subtype of `HalRepresentation` (for example, if it contains required extra attributes),
`paginateNextAs()` or `paginatePrevAs()` can be used:

```java
        traverson(this::getHalJson)
                .startWith("/example/products")
                // Paginate using next. Pages are parsed as ProductPageHalRepresentation:
                .paginateNextAs(ProductPageHalRepresentation.class, (Traverson pageTraverson) -> {
                    // Fetch the page resource:
                    pageTraverson
                            .getResourceAs(ProductPageHalRepresentation.class)
                            .ifPresent((page) -> {
                                System.out.println(productPage.pageTitle);
                            });
                    // Process linked items of the page:
                    pageTraverson
                            .follow("item")
                            .streamAs(ProductHalRepresentation.class)
                            .forEach(product->{
                                System.out.println(product.title + ": " + product.price);
                            };
                    return true;
                });

```

If the paged items may be embedded into the page, it is necessary to specify the type of the embedded
items:
```java
        traverson(this::getHalJson)
                .startWith("/example/products")
                .paginateNext(
                        withEmbedded("item", ProductHalRepresentation.class),
                        (Traverson pageTraverson) -> { ... }
                );
```

## 5. Building edison-hal

If you want to build edison-hal using Gradle, you might want to use
 the included Gradle wrapper:
 
```
    bin/go build
```
 or

```
    bin/gradlew build
```
 
 An IntelliJ IDEA Workspace can be created using 

```
    bin/go idea
```

If you do not want to use the provided gradle wrapper, please make sure 
that you are using an up-to-date version of Gradle (>= 2.12.0).

## 6. Running the examples

Currently, there are two examples: 
* One for the server side, using Spring Boot
* One for the client side, which is a simple Java application using
 an Apache HttpClient to access the example server.
 
The server can be started using Gradle:

```
    gradle :example-springboot:bootRun
```

Alternatively, you can simply run the class Server in your favorite IDE.

Open 'http://localhost:8080' in your Browser and navigate to the included 
HAL Browser to play around with the example.

The client can be started like this:

```
    gradle :example-client:run
```

It requires the server to be running. The REST resources are traversed
in different ways. 

## 7. Version History

### 2.0.2

*Bugfixes*

Fixed issues when more than two parameters in the Traverson.withVars() function.

### 2.0.1

*Bugfixes*

* Fixes next page bug for zero-based paging (Issue #24)

### 2.0.0

*New Features / API extensions*

* Issue 23: Allow customization of the Jackson `ObjectMapper` used in `HalParser` `Traverson`.

*Dependency Updates*
* Updated `com.fasterxml.jackson.core:jackson-databind` from 2.9.1 to 2.9.6
* Updated `com.damnhandy:handy-uri-templates` from 2.1.6 to 2.1.7
 
### 2.0.0-m2

*Bugfixes*

* Issue 22: Duplicate curies are now removed from embedded objects, if they are specified in the embedding `HalRepresentation`.

*Breaking Changes*

* The API to create Links instances has changed: the former `Links.linkingTo(List<Link>)`, 
`Links.linkingTo(Link, Link...)` is now replaced by `Links.linkingTo()` which is returning a `Links.Builder`. The 
reason for this is that it is now possible to specify more easily, whether a `Link` should be rendered as a single
link object, or an array of link objects. The `Links.Builder` has now methods for this like, for example, 
`single(Link, Link...)` or `array(Link, Link...)`. Have a look at 
[section 4.3](#4.3-single-link-objects-vs.-arrays-of-link-objects) for more details about adding links to a
`HalRepresentation`.  
* Because the `Links.Builder` is now able to create single link objects as well as arrays of link objects, the 
corresponding functionality to register link-relation types to be rendered as arrays has been removed from 
`RelRegistry`.
* Issue 21: Similar to the links, it is now possible to specify whether or not embedded resources are embedded as 
single resource objects, or arrays of resources objects.
* Renamed `RelRegistry` to `Curies`     
* Issue 20: HalRepresentation was previously annotated with @JsonInclude(NON_NULL). This was changed so that only 
_links and _embedded are now annotated this way. This might change the behaviour / structure of existing applications.
  You should now annotate classes extending HalRepresentation, or attributes of such classes appropriately.
  
### 2.0.0-m1

*Breaking Changes*

* The error handling in the Traverson API has been changed in that exceptions are now thrown instead of catching them 
and exposing a `Traverson.getLastError()`. In 1.0.0, the client had to check for the last error. This is rather unusual 
and is easy to overlook. Beginning with 2.0.0, `getLastError` is removed and the following method-signatures are now 
throwing `java.io.IOException`:
  * Traverson.paginateNext()  
  * Traverson.paginateNextAs()  
  * Traverson.paginatePrev()  
  * Traverson.paginatePrevAs()
  * Traverson.stream()
  * Traverson.streamAs()
  * Traverson.getResource()  
  * Traverson.getResourceAs()
* The static factory method `Traverson.traverson()` does not accept a `java.util.function.Function<Link,String>` 
anymore. Instead, a `de.otto.edison.hal.traverson.LinkResolver` was introduced. The major difference between the new
`PageHandler` and the previous `Function` is that the `PageHandler.apply(Link)` is now throwing `IOException` while
`Function` does not allow this.
* For the same reasons (being able to throw IOExceptions), the different `paginate*` methods now expect a 
`de.otto.edison.hal.traverson.PageHandler` instead of a `java.util.function.Function<Traverson,Boolean>`. Beside of this,
using dedicated functional interfaces instead of generic Functions is a little easier to understand.    
  

*New Features / API extensions* 

* The `Traverson` now supports traversing linked resources while ignoring embedded resources: instead of returning an 
embedded item, clients are now able to force the `Traverson` to follow the links instead. This is especially helpful,
if only a reduced set of attributes is embedded into a resource. A set of `Traverson.followLink()` methods was added
to support this.
* Added new methods `Traverson.paginate()` and `Traverson.paginateAs()` to paginate over paged resources using 
link-relation types other than `next` or `prev`.
* Added a `CuriTemplate` helper to expand / shorten link-relation types using a CURI.  

### 1.0.0 (CURRENT RELEASE)

*New Features / API extensions* 
* It is now possible to configure the link-relation types that are serialized as an array of links.
* Parsing of nested embedded items
* Support for curies in deeply nested embedded items
* The HalParser now supports multiple type infos so more than one link-relation type can
be configured with the type of the embedded items. 
* Support for parsing and accessing attributes that were not mapped to properties of HalRepresentations
* Added TRACE logging to Traverson to make it easier to analyse the behaviour of the Traverson.

### 1.0.0.RC5

*Bugfixes*

* Fixed signature of HalRepresentation.withEmbedded(): using List<? extends HalRepresentation
instead of List<HalRepresentation

### 1.0.0.RC4

*New Features / API extensions*

* Added support for traversal auf HAL documents with relative hrefs.

### 1.0.0.RC3

*New Features / API extensions*

* Added `Traverson.getResourceAs(Class<T>, EmbeddedTypeInfo)` and `Traverson.streamAs(Class<T>, EmbeddedTypeInfo>
so it is possible to specify the type of embedded items of a resource using Traversons.
* Added support for client-side traversal of paged resources using
    - `Traverson.paginateNext()`
    - `Traverson.paginateNextAs()`
    - `Traverson.paginatePrev()`
    - `Traverson.paginatePrevAs()`
    - `Traverson.paginate()`
    - `Traverson.paginateAs()`

### 1.0.0.RC2

*Bugfixes*

* Fixed traversion of links using predicates
* Fixed parsing of embedded items, where a rel has only only a single item instead of a list of items.
* Fixed getter for SkipLimitPaging.hasMore

### 1.0.0.RC1

*New Features / API extensions*

* New Traverson methods to select links matching some given predicate.

### 0.7.0

*Breaking Changes*

* Deprecated NumberedPaging.numberedPaging().

*New Features / API extensions*

* Introduced support for 1-based paging.
* New builder methods NumberedPaging.zeroBasedNumberedPaging() and
NumberedPaging.oneBasedNumberedPaging()

### 0.6.2

*Bugfixes*

* The constructors of NumberedPaging are now protected instead of final.
This prevented changing the names of the page/pageSize variables used
in collection resources.
* Fixed numbering of last-page links in NumberedPaging.

*New Features / API extensions*

* Added NumberedPaging.getLastPage()

### 0.6.1

*Bugfixes*

* Fixed a bug that prevented the use of paging for empty collections.
 
### 0.6.0

*Breaking Changes*

* Moved Traverson classes to package de.otto.edison.hal.traverson

*Bugfixes*

* Fixed shortening of embedded links using curies when adding links to 
a HalResource after construction.

*New Features / API extensions*

* Added Link.getHrefAsTemplate() 
* Added helpers to create links for paged resources: NumberedPaging and SkipLimitPaging

### 0.5.0

*Breaking Changes*

* Renamed Link.Builder.fromPrototype() and Links.Builder.fromPrototype()
 to copyOf()

*New Features / API extensions*

* Added Link.isEquivalentTo(Link)
* Link.Builder is not adding equivalent links anymore
* Added HalRepresentation.withEmbedded() and HalRepresentation.withLinks()
 so links and embedded items can be added after construction.

### 0.4.1

*New Features / API extensions*

* Added Traverson.startWith(HalRepresentation) to initialize a Traverson from a given resource.

*Bufixes*

* JsonSerializers and -Deserializers for Links and Embedded are now public to avoid problems with some testing szenarios in Spring.

### 0.4.0

*Breaking Changes*

* Simplified creation of links by removing unneeded factory methods for
 templated links. Whether or not a link is templated is now automatically
 identified by the Link.
* Removed duplicate factory method to create a Link.Builder.

*New Features / API extensions*

* Added a Traverson API to navigate through HAL resources.


### 0.3.0

*Bugfixes*

* curies are now rendered as an array of links instead of a single link
document

*New Features / API extensions*

* Added factory method Link.curi() to build CURI links.
* Support for curies in links and embedded resources.
* Improved JavaDoc
* Added Links.getRels()
* Added Links.stream()
* Added Embedded.getRels()
* Added simple example for a client of a HAL service.

### 0.2.0

*Bugfixes*

* Fixed generation + parsing of non-trivial links
* Fixed type and name of 'deprecation' property in links
* Fixed rendering of empty embedded items
* Fixed rendering of empty links

*Breaking Changes*

* Renamed Link.LinkBuilder to Link.Builder 
* Renamed Embedded.EmbeddedItemsBuilder to Embedded.Builder 
* Renamed Embedded.Builder.withEmbedded() to Embedded.Builder.with()
* Renamed Embedded.Builder.withoutEmbedded() to Embedded.Builder.without()
* Added getter methods to Link instead of public final attributes

*New Features / API extensions*

* Introduced factory methods for Embedded.Builder
* Improved JavaDoc
* Added Spring-Boot example aplication incl HAL Browser
* Added Links.linkingTo(List<Link> links)
* Added Links.Builder
* Added Embedded.isEmpty()

### 0.1.0 

* Initial Release
* Full support for all link properties specified by https://tools.ietf.org/html/draft-kelly-json-hal-08
* Full support for embedded resources.
* Serialization and deserialization of HAL resources.

