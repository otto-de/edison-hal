# Edison HAL

Library to produce and consume [application/hal+json](https://tools.ietf.org/html/draft-kelly-json-hal-08) 
representations of REST resources using Jackson.

## Status

[![Build Status](https://travis-ci.org/otto-de/edison-hal.svg)](https://travis-ci.org/otto-de/edison-hal) 
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.otto.edison/edison-hal/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.otto.edison/edison-hal)
[![Dependency Status](https://www.versioneye.com/user/projects/5790e6b326c1a40035ecd1e8/badge.svg?style=flat)](https://www.versioneye.com/user/projects/5790e6b326c1a40035ecd1e8)

BETA - work in progress.

The current implementation is supporting HAL compliant links and
embedded resources, including curies (compact URIs). 

The full media-type as defined in https://tools.ietf.org/html/draft-kelly-json-hal-08
is supported by edison-hal.

## About

At otto.de, microservices should only communicate via REST APIs with other 
 microservices. HAL is a nice format to implement the HATEOAS part 
 of REST. Edison-hal is a simple library, to make it easy to produce
 and consume HAL representations for your REST APIs.

Currently, there are only a couple of libraries supporting HAL and even
 less that support the full media type including all link properties,
 curies (compact URIs) and embedded resources. 
 
Spring HATEOAS, for example, is lacking many link properties, such as 
 title, name, type and others. 
 
## Features

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

## Open Issues and next steps
* Improved error-handling
* Deep nesting of embedded items is currently not supported: a resource
that has embedded items, which have embedded items, and so on.

## Usage

### 0. Learn about Hypertext Application Language (HAL)

* Read Mike's article about [HAL](http://stateless.co/hal_specification.html) and 
* the current [draft of the RFC](https://tools.ietf.org/html/draft-kelly-json-hal-08).

### 1. Include edison-hal into your project:
 
```gradle
    dependencies {
        compile "de.otto.edison:edison-hal:0.4.0",
        ...
    }
```
 
The current snapshot release is 1.0.0-SNAPSHOT:

```gradle
    dependencies {
        compile "de.otto.edison:edison-hal:1.0.0-SNAPSHOT",
        ...
    }
```
 
### 2. Provide a class for the representation of your REST API

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

### 3. Serializing HalRepresentations

To convert your representation class into a application/hal+json document, you can use Jackson's ObjectMapper directly:

```java
    final ObjectMapper mapper = new ObjectMapper();
    
    final String json = mapper.writeValueAsString(representation);
```

### 4. Parsing application/hal+json documents

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
                            "\"_links\":{\"self\":[{\"href\":\"http://example.org/test/bar/01\"}]}" +
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

### 5. Using HAL in Spring controllers

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

### 6. Using the Traverson:

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

## Building edison-hal

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

## Running the examples

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

## Version History

### Current Snapshot

* Replaced TestNG by JUnit

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

