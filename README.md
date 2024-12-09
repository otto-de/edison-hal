# Edison HAL

Library to produce and consume [application/hal+json](https://tools.ietf.org/html/draft-kelly-json-hal-08) 
representations of REST resources using Jackson.

[![Build Status](https://travis-ci.org/otto-de/edison-hal.svg)](https://travis-ci.org/otto-de/edison-hal) 
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.otto.edison/edison-hal/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.otto.edison/edison-hal)
[![Javadoc](https://www.javadoc.io/badge/de.otto.edison/edison-hal.svg)](https://www.javadoc.io/doc/de.otto.edison/edison-hal)
[![codecov](https://codecov.io/gh/otto-de/edison-hal/branch/master/graph/badge.svg)](https://codecov.io/gh/otto-de/edison-hal)
![OSS Lifecycle](https://img.shields.io/osslifecycle?file_url=https%3A%2F%2Fraw.githubusercontent.com%2Fotto-de%2Fedison-hal%2Fmain%2FOSSMETADATA)


## 1. Documentation
* [JavaDoc](https://www.javadoc.io/doc/de.otto.edison/edison-hal)
* [User Guide](./USERGUIDE.md) explains how to use edison-hal in your projects.
* [Change Log](./CHANGES.md) for latest changes.

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
