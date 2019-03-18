# Edison HAL User Guide

Edison HAL is a library developed at OTTO. It implements the full RFC of application/hal+json, both for producing
and parsing HAL resources. 

Edison HAL can be considered stable, as it is actively used in production services at otto.de.

The versioning scheme is using [Semantic Versioning](https://semver.org).

## Contents

- [1. Getting Started](#1-getting-started)
  * [1.1 Add Edison HAL dependency:](#11-add-edison-hal-dependency)
  * [1.2 Provide a class for the representation of your REST API](#12-provide-a-class-for-the-representation-of-your-rest-api)
  * [1.3 Running the examples](#13-running-the-examples)
  * [1.4 Building edison-hal](#14-building-edison-hal)
- [2. Hyperlinks](#2-hyperlinks)
  * [2.1 Single Link Objects vs. Arrays of Link Objects](#21-single-link-objects-vs-arrays-of-link-objects)
  * [2.2 Curies](#22-curies)
- [3 Serializing HalRepresentations](#3-serializing-halrepresentations)
- [4 Parsing HAL representations](#4-parsing-hal-representations)
  * [4.1 Parsing with Jackson ObjectMapper](#41-parsing-with-jackson-objectmapper)
  * [4.2 Configuring the ObjectMapper](#42-configuring-the-objectmapper)
  * [4.3 Using the HalParser](#43-using-the-halparser)
  * [4.4 Parsing Nested Embedded Resources](#44-parsing-nested-embedded-resources)
  * [4.5 Parsing Unmapped Attributes](#45-parsing-unmapped-attributes)
- [5 Using the Traverson](#5-using-the-traverson)
  * [5.1 Selecting Links](#51-selecting-links)
  * [5.2 Paging over HAL resources](#52-paging-over-hal-resources)

<small><i><a href='http://ecotrust-canada.github.io/markdown-toc/'>Table of contents generated with markdown-toc</a></i></small>



## 1. Getting Started

Before using this library, you should have a good understanding of the Hypertext Application Language (HAL):

* Read Mike's article about [HAL](http://stateless.co/hal_specification.html) and 
* the current [draft of the RFC](https://tools.ietf.org/html/draft-kelly-json-hal-08).

Edison HAL only has a limited number of dependencies to 3rd party libraries:

1. [Jackson](https://github.com/FasterXML/jackson) is used to parse and generate application/hal+json resources.
2. [Handy URI Templates](https://github.com/damnhandy/Handy-URI-Templates) is the library used for URI templates
   and templated links.
3. For logging purposes, SLF4J is used. There is only a dependency to the SLF4J API, so you are free to choose 
   between whatever implementaion suits to your application.    

### 1.1 Add Edison HAL dependency
 
```gradle
    dependencies {
        compile "de.otto.edison:edison-hal:2.0.2",
        ...
    }
```

### 1.2 Provide a class for the representation of your REST API

If your representation does not need additional attributes beside of
the properties defined in application/hal+json, you can create a
HalRepresentations like this:

```java
public class Example_1_1 {
    
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

Reading the document using Jackson will work as expected, too:

````java
final HalRepresentation result = new ObjectMapper().readValue(json.getBytes(), HalRepresentation.class);
````


In many cases, you will need custom attributes in addition to HAL's `_links` and `_embedded` properties. Extending
`HalRepresentation` and adding attributes as you would do in other types (de-)serialized using Jackson is easy:

```java
public class Example_1_2 extends HalRepresentation {
    @JsonProperty("someProperty")
    private String someProperty = "some value";
    @JsonProperty("someOtherProperty")
    private String someOtherProperty = "some other value";

    Example_1_2() {
        super(linkingTo()
                .self("http://example.org/test/bar")
                .build()
        );
    }
}
```

### 1.3 Running the examples

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
  
### 1.4 Building edison-hal

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
that you are using an up-to-date version of Gradle (>= 4.x).  

## 2. Hyperlinks

### 2.1 Single Link Objects vs. Arrays of Link Objects

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
public class Example_2_1 {
    final HalRepresentation representation = new HalRepresentation(
            linkingTo()                                                 // this creates a Links.Builder instance 
                    .self("http://example.com/foo/42")                  // adds a single 'self' link
                    .single(
                            link("next", "http://example.com/foo/43"))  // adds a single 'next' link
                    .single(
                            link("prev", "http://example.com/foo/41"))  
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

### 2.2 Curies

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
public class Example_2_2 {
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

## 3 Serializing HalRepresentations

To convert your representation class into a application/hal+json document, you can use Jackson's 
ObjectMapper directly:
```java
    final ObjectMapper mapper = new ObjectMapper();
    
    final String json = mapper.writeValueAsString(representation);
```

Using Spring MVC, you can directly return HalRepresentations from your controller methods, without any further 
configuration:

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

## 4 Parsing HAL representations

A HAL document can be parsed using Jackson, too. 

### 4.1 Parsing with Jackson ObjectMapper

Given some custom `HalRepresentation` with links, custom attributes and embedded objects like this:

```json
{
  "someProperty" : "1",
  "someOtherProperty" : "2",
  "_links" : {
    "self" : {"href" : "http://example.org/test/foo"}
  },
  "_embedded" : {
    "bar" : {
      "_links" : {
        "self" : {"href" : "http://example.org/bar/01"}
      }
    }
  }
}
``` 

The following code will parse a HAL document into an instance of `TestHalRepresentation`:
```java
public class Example_4_1 {
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
### 4.2 Configuring the ObjectMapper
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

### 4.3 Using the HalParser

If you want to parse embedded resources as a sub-class of HalRepresentation, you need to use the *HalParser*:

```java
class Example_4_2 {
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

### 4.4 Parsing Nested Embedded Resources

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

### 4.5 Parsing Unmapped Attributes

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
 
## 5 Using the Traverson

Traverson is a utility to make it easy to traverse linked and/or embedded resources using link-relation types.

Given some resources A, B, C1...C3 that are linked with links having link-relation types `foo` and `item`:

     +---------+           +---------+          +---------+
     |         |    foo    |         |     item |         |
     |    A    |-----------|    B    |----------|   C1    |
     |         |           |         |    |     |         |
     +---------+           +---------+    |     +---------+
                                          |                
                                          |     +---------+
                                          |item |         |
                                          ------|   C2    |
                                          |     |         |
                                          |     +---------+
                                          |                
                                          |     +---------+
                                          |item |         |
                                          ------|   C3    |
                                                |         |
                                                +---------+

Using the Traverson, you could traverse the linked resources as follows:

```java
traverson(this::getHalJson)
        .startWith(A_URI)
        .follow("foo")
        .follow("item")
        .streamAs(HalRepresentation.class)
        .forEach(item->{
            System.out.println(item);
        });
```

The Traverson is automatically taking care of embedded resources: if a linked resource is already embedded, it is used 
instead of resolving the link. Only if it is not embedded, the Traverson is following the links. 

Just like the `HalParser`, the Traverson is using an `ObjectMapper` that is registering all Jackson Modules automatically. The default `ObjectMapper` can be replaced by a custom instance by creating the `Traverson` using the static factory method
`Traverson.traverson(LinkResolver, ObjectMapper)`.

### 5.1 Selecting Links

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
### 5.2 Paging over HAL resources

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

