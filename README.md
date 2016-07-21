# edison-microservice:edison-hateoas

Library to read and create application/hal+json representations of 
REST resources using Jackson.

See https://tools.ietf.org/html/draft-kelly-json-hal-06 for details.

## Status

[![Build Status](https://travis-ci.org/otto-de/edison-hal.svg)](https://travis-ci.org/otto-de/edison-hal) 
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.otto.edison/edison-hal/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.otto.edison/edison-hal)
[![Dependency Status](https://www.versioneye.com/user/projects/55ba6f016537620017001905/badge.svg?style=flat)](https://www.versioneye.com/user/projects/55ba6f016537620017001905)

BETA - work in progress.

The current implementation is supporting HAL compliant links and
embedded resources, but no curies.

## About

At otto.de, microservices should only communicate via REST APIs with other 
 microservices. HAL is a nice format to implement the HATEOAS part 
 of REST. Edison-hal is a simple library, to make it easy to produce
 and consume HAL representations for your REST APIs.

Currently, there are only few libraries supporting HAL and even
 less that support the full media type including all link properties,
 curies (compact URIs) and embedded resources. 
 
Spring HATEOAS, for
 example, is lacking many link properties, such as title, name, type and
 others. Beside of this, including Spring HATEOAS into Spring Boot
 applications has some unwanted (at least to me) side-effects: 
 for example, an "Actuator" endpoint is automatically registered unter 
 /internal, so we would loose the possibility to provide an html 
 representation at this URI (it is a big WTF? to me, that Spring Boot 
 Actuator endpoints do not support content negotiation, btw).
 
## Features

Creating HAL representations:
* Links with all specified attributes like rel, href, profile, type, name, title, etc. pp.
* Embedded resources
* Generation of HAL representations using Jackson via annotated classes

Parsing HAL representations:
* Mapping application/hal+json to Java classes using Jackson
* simple domain model to access links, embedded resources etc.

## Next Steps
* Support for curies
* Support for simple traversal of linked resources
* Support for collection resources including paging

## Usage

*Include edison-hateoas*:
 
```gradle
    dependencies {
        compile "de.otto.edison:edison-hal:0.1.0",
        ...
    }
```
 
*Write a representation class for your REST API*

If your representation does not need additional attributes beside of
the properties defined in application/hal+json, you can create a
HalRepresentations like this:

```java
        final HalRepresentation representation = new HalRepresentation(
                linkingTo(self("http://example.org/test/bar")),
                embeddedBuilder()
                        .withEmbedded("foo", asList(
                                new HalRepresentation(linkingTo(self("http://example.org/test/foo/01"))),
                                new HalRepresentation(linkingTo(self("http://example.org/test/foo/02")))))
                        .withEmbedded("bar", asList(
                                new HalRepresentation(linkingTo(self("http://example.org/test/bar/01"))),
                                new HalRepresentation(linkingTo(self("http://example.org/test/bar/02")))))
                        .build());

```

Otherwise, you can derive a class from HalRepresentation to add special
attributes:

```java
    public class MySpecialHalRepresentation extends HalRepresentation {
        @JsonProperty("someProperty")
        private String someProperty;
        @JsonProperty("someOtherProperty")
        private String someOtherProperty;
        
        // ...
    }

```

*Serializing HalRepresentations*

To get the JSON document, you can use Jackson directly:

```java
    final ObjectMapper mapper = new ObjectMapper();
    
    final String json = mapper.writeValueAsString(representation);
```

*Parsing application/hal+json*

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

If you want to parse embedded resources into a extended HalRepresentation, you need to use the HalParser:

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
        final SomeHalRepresentation result = parse(json).as(SomeHalRepresentation.class, withEmbedded("bar", TestHalRepresentation.class));
        // then
        final List<EmbeddedHalRepresentation> embeddedItems = result.getEmbedded().getItemsBy("bar", TestHalRepresentation.class);
        assertThat(embeddedItems, hasSize(1));
        assertThat(embeddedItems.get(0).getClass(), equalTo(TestHalRepresentation.class));
        assertThat(embeddedItems.get(0).getLinks().getLinkBy("self").get(), is(link("self", "http://example.org/test/bar/01")));
    }
```

*Using HAL in Spring controllers*

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
