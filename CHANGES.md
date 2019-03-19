# Change Log

## 2.1.0-SNAPSHOT

*New Features / API extensions*

* Adds method `Links.Builder#collection(String)` to add a collection link to a HalResource´s `_links` section.

*Deprecations*

* Addes deprecation for `HalRepresentation` constructors with `Curies` parameter. No expected usages - but please
  contact me, if the removal in some future release 3.0 will break some use-cases for you.
  
## 2.0.2

*Bugfixes*

* Fixed issues when more than two parameters in the Traverson.withVars() function.

## 2.0.1

*Bugfixes*

* Fixes next page bug for zero-based paging (Issue #24)

## 2.0.0

*New Features / API extensions*

* Issue 23: Allow customization of the Jackson `ObjectMapper` used in `HalParser` `Traverson`.

*Dependency Updates*
* Updated `com.fasterxml.jackson.core:jackson-databind` from 2.9.1 to 2.9.6
* Updated `com.damnhandy:handy-uri-templates` from 2.1.6 to 2.1.7
 
## 2.0.0-m2

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
  
## 2.0.0-m1

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

## 1.0.0

*New Features / API extensions* 
* It is now possible to configure the link-relation types that are serialized as an array of links.
* Parsing of nested embedded items
* Support for curies in deeply nested embedded items
* The HalParser now supports multiple type infos so more than one link-relation type can
be configured with the type of the embedded items. 
* Support for parsing and accessing attributes that were not mapped to properties of HalRepresentations
* Added TRACE logging to Traverson to make it easier to analyse the behaviour of the Traverson.

## 1.0.0.RC5

*Bugfixes*

* Fixed signature of HalRepresentation.withEmbedded(): using List<? extends HalRepresentation
instead of List<HalRepresentation

## 1.0.0.RC4

*New Features / API extensions*

* Added support for traversal auf HAL documents with relative hrefs.

## 1.0.0.RC3

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

## 1.0.0.RC2

*Bugfixes*

* Fixed traversion of links using predicates
* Fixed parsing of embedded items, where a rel has only only a single item instead of a list of items.
* Fixed getter for SkipLimitPaging.hasMore

## 1.0.0.RC1

*New Features / API extensions*

* New Traverson methods to select links matching some given predicate.

## 0.7.0

*Breaking Changes*

* Deprecated NumberedPaging.numberedPaging().

*New Features / API extensions*

* Introduced support for 1-based paging.
* New builder methods NumberedPaging.zeroBasedNumberedPaging() and
NumberedPaging.oneBasedNumberedPaging()

## 0.6.2

*Bugfixes*

* The constructors of NumberedPaging are now protected instead of final.
This prevented changing the names of the page/pageSize variables used
in collection resources.
* Fixed numbering of last-page links in NumberedPaging.

*New Features / API extensions*

* Added NumberedPaging.getLastPage()

## 0.6.1

*Bugfixes*

* Fixed a bug that prevented the use of paging for empty collections.
 
## 0.6.0

*Breaking Changes*

* Moved Traverson classes to package de.otto.edison.hal.traverson

*Bugfixes*

* Fixed shortening of embedded links using curies when adding links to 
a HalResource after construction.

*New Features / API extensions*

* Added Link.getHrefAsTemplate() 
* Added helpers to create links for paged resources: NumberedPaging and SkipLimitPaging

## 0.5.0

*Breaking Changes*

* Renamed Link.Builder.fromPrototype() and Links.Builder.fromPrototype()
 to copyOf()

*New Features / API extensions*

* Added Link.isEquivalentTo(Link)
* Link.Builder is not adding equivalent links anymore
* Added HalRepresentation.withEmbedded() and HalRepresentation.withLinks()
 so links and embedded items can be added after construction.

## 0.4.1

*New Features / API extensions*

* Added Traverson.startWith(HalRepresentation) to initialize a Traverson from a given resource.

*Bufixes*

* JsonSerializers and -Deserializers for Links and Embedded are now public to avoid problems with some testing szenarios in Spring.

## 0.4.0

*Breaking Changes*

* Simplified creation of links by removing unneeded factory methods for
 templated links. Whether or not a link is templated is now automatically
 identified by the Link.
* Removed duplicate factory method to create a Link.Builder.

*New Features / API extensions*

* Added a Traverson API to navigate through HAL resources.


## 0.3.0

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

## 0.2.0

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

## 0.1.0 

* Initial Release
* Full support for all link properties specified by https://tools.ietf.org/html/draft-kelly-json-hal-08
* Full support for embedded resources.
* Serialization and deserialization of HAL resources.

