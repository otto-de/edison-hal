# Edison HAL

Library to produce and consume [application/hal+json](https://tools.ietf.org/html/draft-kelly-json-hal-08) 
representations of REST resources using Jackson.

## 1. Status

[![Build Status](https://travis-ci.org/otto-de/edison-hal.svg)](https://travis-ci.org/otto-de/edison-hal) 
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.otto.edison/edison-hal/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.otto.edison/edison-hal)
[![Javadoc](http://javadoc-emblem.rhcloud.com/doc/de.otto.edison/edison-hal/badge.svg)](https://www.javadoc.io/doc/de.otto.edison/edison-hal/)
[![codecov](https://codecov.io/gh/otto-de/edison-hal/branch/master/graph/badge.svg)](https://codecov.io/gh/otto-de/edison-hal)
[![Dependency Status](https://www.versioneye.com/user/projects/5790e6b326c1a40035ecd1e8/badge.svg?style=flat)](https://www.versioneye.com/user/projects/5790e6b326c1a40035ecd1e8)

RELEASE CANDIDATE - almost done.

The current implementation is supporting HAL compliant links and
embedded resources, including curies (compact URIs). 

The full media-type as defined in https://tools.ietf.org/html/draft-kelly-json-hal-08
is supported by edison-hal.

The current library is already in production at otto.de and should be more or less stable but
still might have some issues.

Please provide feedback, if something is not working as expected.

## 2. About

At otto.de, microservices should only communicate via REST APIs with other 
 microservices. HAL is a nice format to implement the HATEOAS part 
 of REST. Edison-hal is a simple library, to make it easy to produce
 and consume HAL representations for your REST APIs.

Currently, there are only a couple of libraries supporting HAL and even
 less that support the full media type including all link properties,
 curies (compact URIs) and embedded resources. 
 
Spring HATEOAS, for example, is lacking many link properties, such as 
 title, name, type and others. 
 
## 3. Features

Creating HAL representations:
* Links with all specified attributes like rel, href, profile, type, name, title, etc. pp.
* Embedded resources
* CURIs in links and embedded resources
* Generation of HAL representations using Jackson using annotated classes

Parsing HAL representations:
* Mapping application/hal+json to Java classes using Jackson
* simple domain model to access links, embedded resources etc.

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
        compile "de.otto.edison:edison-hal:1.0.0.RC6",
        ...
    }
```
  
### 4.2 Provide a class for the representation of your REST API

If your representation does not need additional attributes beside of
the properties defined in application/hal+json, you can create a
HalRepresentations like this:

```java
    final HalRepresentation representation = new HalRepresentation(
            linkingTo(
                    self("http://example.org/test/bar")),
            embeddedBuilder()
                    .with("foo", asList(
                            new HalRepresentation(linkingTo(self("http://example.org/test/foo/01"))),
                            new HalRepresentation(linkingTo(self("http://example.org/test/foo/02")))))
                    .with("bar", asList(
                            new HalRepresentation(linkingTo(self("http://example.org/test/bar/01"))),
                            new HalRepresentation(linkingTo(self("http://example.org/test/bar/02")))))
                    .build());

```

Otherwise, you can derive a class from HalRepresentation to add extra attributes:

```java
    public class MySpecialHalRepresentation extends HalRepresentation {
        @JsonProperty("someProperty")
        private String someProperty;
        @JsonProperty("someOtherProperty")
        private String someOtherProperty;
        
        // ...
    }

```

### 4.3 Multiple Links

A resource may have multiple links that share the same link relation.

For link relations that may have multiple links, we use an array of links.
```
{
    "_links": {
        "item": [
            { "href": "http://example.com/items/foo" },
            { "href": "http://example.com/items/bar" }
        ]
    }
}
```

>Note: If you're unsure whether the link should be singular, assume it will be multiple.
>If you pick singular and find you need to change it, you will need to create a new link
>relation or face breaking existing clients.

_[text + example taken from http://stateless.co/hal_specification.html)_

By default, links with link-relation type `curies`, `item`, and `items` are serialized as 
an array of links.

Using Edison HAL, you can override this default behaviour when you build the ```Links``` 
object of a ```HalRepresentation```:
```java
    final HalRepresentation representation = new HalRepresentation(
            linkingTo(
                    self("http://example.org/test/bar")
            ).withArrayRels("item", "foo", "bar")
    );
```

Instead of calling ```Links#withArrayRels()```, you can also configure this using ```Links#linkingTo(List<Link>, Set<String>)```
or using ```Links.Builder```

### 4.4 Serializing HalRepresentations

To convert your representation class into a application/hal+json document, you can use Jackson's 
ObjectMapper directly:
```java
    final ObjectMapper mapper = new ObjectMapper();
    
    final String json = mapper.writeValueAsString(representation);
```

### 4.5 Curies

The semantics of links in application/hal+json documents are specified by the link-relation type (rel). It is 
recommended to use [predfined and documented rels](https://www.iana.org/assignments/link-relations/link-relations.xhtml) 
like, for example, `self`, `prev` or `next`. 

In case of custom link-relation types, fully qualified URIs should be used: a link to a product, for example, could be
specified by something like `http://spec.example.com/link-relations/product`. The URI should be resolvable, pointing to
some human readable documentation. 


A curi (compact URI) is a feature in HAL+JSON that is helpful to use URIs while keeping the link-relation types compact.
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
takes care of matching link-relation types and replaces them during serialization:

```java
final HalRepresentation representation = new HalRepresentation(
        linkingTo(
                curi("x", "http://example.org/rels/{rel}"),
                curi("y", "http://example.com/rels/{rel}"),
                link("http://example.org/rels/foo", "http://example.org/test"),
                link("http://example.com/rels/bar", "http://example.org/test")),
        
);
``` 
...will be rendered as
```json
{
  "_links" : {
    "curies" : [
      {"name" : "x", "href" : "http://example.org/rels/{rel}", "templated" : true},
      {"name" : "y", "href" : "http://example.com/rels/{rel}", "templated" : true}
    ],
    "x:foo" : {"href" : "http://example.org/test"},
    "y:bar" : {"href" : "http://example.org/test"}
  }
}
```  
In the same way, curies are automatically applied to link-relation types in embedded resources, even if the embedded
resources have nested embedded resources.
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

### 4.6 Parsing application/hal+json documents

A HAL document can be parsed using Jackson, too:

```java
    @Test public void shouldParseHal() {
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
```
#### 4.6.1 Configuring the ObjectMapper
There are some special cases, where it is required to configure the ObjectMapper as follows:
```java    
    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
```
This will be necessary, if there a single embedded items for a link-relation type, instead of an array of items:
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
If you want to parse embedded resources into a extended HalRepresentation, you need to use the *HalParser*:

```java
    @Test
    public void shouldParseEmbeddedItemsWithSpecificType() throws IOException {
        // given
        final String json =
                "{" +
                        "\"_embedded\":{\"bar\":[" +
                        "   {" +
                        "       \"someProperty\":\"3\"," +
                        "       \"someOtherProperty\":\"3\"," +
                        "       \"_links\":{\"self\":[{\"href\":\"http://example.org/test/bar/01\"}]}" +
                        "   }" +
                        "]}" +
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
```
### 4.7 Using HAL in Spring controllers

Using Spring MVC, you can directly return HalRepresentations from you controller methods:

```java
    @RequestMapping(
            value = "/my/hal/resource",
            produces = {
                    "application/hal+json",
                    "application/json"},
            method = GET
    )
    public TestHalRepresentation getMyHalResource() {    
        return new TestHalRepresentation(getTheInputData());
    }
```

### 4.8 Using the Traverson:

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

### 4.9 Paging over HAL resources:

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
`Traverson.paginate()`.

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

### 1.0.0

*New Features / API extensions* 
* It is now possible to configure the link-relation types that are serialized as an array of links.
* Parsing of nested embedded items
* Support for curies in nested embedded items
* The HalParser now supports multiple type infos so more than one link-relation type can
be configured with the type of the embedded items. 

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

