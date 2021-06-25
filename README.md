<img src="https://www.artipie.com/logo.svg" width="64px" height="64px"/>

[![EO principles respected here](https://www.elegantobjects.org/badge.svg)](https://www.elegantobjects.org)
[![DevOps By Rultor.com](http://www.rultor.com/b/artipie/gem-adapter)](http://www.rultor.com/p/artipie/gem-adapter)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Build Status](https://img.shields.io/travis/artipie/gem-adapter/master.svg)](https://travis-ci.org/artipie/gem-adapter)
[![Javadoc](http://www.javadoc.io/badge/com.artipie/gem-adapter.svg)](http://www.javadoc.io/doc/com.artipie/gem-adapter)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/artipie/gem-adapter/blob/master/LICENSE.txt)
[![Hits-of-Code](https://hitsofcode.com/github/artipie/gem-adapter)](https://hitsofcode.com/view/github/artipie/gem-adapter)
[![Maven Central](https://img.shields.io/maven-central/v/com.artipie/gem-adapter.svg)](https://maven-badges.herokuapp.com/maven-central/com.artipie/gem-adapter)
[![PDD status](http://www.0pdd.com/svg?name=artipie/gem-adapter)](http://www.0pdd.com/p?name=artipie/gem-adapter)

`gem-adapter` is a slice in Artpie, aimed to support gem packages.

This is the dependency you need:

```xml
<dependency>
  <groupId>com.artipie</groupId>
  <artifactId>gem-adapter</artifactId>
  <version>[...]</version>
</dependency>
```

Read the [Javadoc](http://www.javadoc.io/doc/com.artipie/gem-adapter)
for more technical details.

## Extra gems

`lib/` dir contains additional ruby gems, required by the project.
The directory is generated at maven clean phase.

## Useful links

* [RubyGem Index Internals](https://blog.packagecloud.io/eng/2015/12/15/rubygem-index-internals/) - File structure and gem format

* [Make Your Own Gem](https://guides.rubygems.org/make-your-own-gem/) - How to create and publish
a simple ruby gem into rubygems.org registry.

* [rubygems.org API](https://guides.rubygems.org/rubygems-org-api/) - A page with rubygems.org 
API specification 

* [Gugelines at rubygems.org](https://guides.rubygems.org/) - Guidelines around the `gem` package 
manager.

## Similar solutions

* [Artifactory RubyGems Repositories](https://www.jfrog.com/confluence/display/JFROG/RubyGems+Repositories)
* [Gem in a Box](https://github.com/geminabox/geminabox)
* [Gemfury](https://gemfury.com/l/gem-server)
* `gem server` [command](https://guides.rubygems.org/run-your-own-gem-server/)   
 
## How to contribute

Fork repository, make changes, send us a pull request. We will review
your changes and apply them to the `master` branch shortly, provided
they don't violate our quality standards. To avoid frustration, before
sending us your pull request please run full Maven build:

```
$ mvn clean install -Pqulice
```

To avoid build errors use Maven 3.2+.

