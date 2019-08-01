# TrackFind

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/c3f38d6ea0184dab99bf012a04892c4c)](https://www.codacy.com/app/dtitov/trackfind?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=elixir-no-nels/trackfind&amp;utm_campaign=Badge_Grade)
[![Build Status](https://travis-ci.org/elixir-no-nels/trackfind.svg?branch=master)](https://travis-ci.org/elixir-no-nels/trackfind)
[![Docker Pulls](https://img.shields.io/docker/pulls/nels/trackfind.svg)](https://hub.docker.com/r/nels/trackfind/)

## About

Thousands of genomic annotation tracks have been generated the recent years, many in the context of larger undertakings such as BLUEPRINT and ENCODE. Several data portals for tracks are providing search services to researchers, but the underlying metadata are diverse and often poorly curated. The Trackhub Registry provides a unified access point, but currently only supports limited search capabilities.

In the context of the Elixir Implementation Study: “FAIRification of Genomic Tracks”, we have developed the TrackFind service. TrackFind supports crawling of the TrackHub Registry and other data portals to fetch track metadata. Crawled metadata can be accessed through hierarchical browsing or by search queries, both through a web-based user interface, and as a RESTful API. TrackFind supports advanced SQL-based search queries that can be easily built in the user interface, and the search results can be browsed and exported in JSON or GSuite format. The RESTful API allows downstream tools and scripts to easily integrate TrackFind search, currently demonstrated by the GSuite HyperBrowser and EPICO. 

In addition to supporting most metadata models directly, TrackFind also supports the transformation of metadata into the FAIR model defined in the “FAIRification of Genomic Tracks” Implementation Study. Such transformation can be achieved on per-TrackHub basis through online scripting, thus providing a simple path for data managers to FAIRify their track metadata. TrackFind also maintains a version history of all metadata changes, including all recrawlings and transformations. We are also planning to add functionality for curating existing track metadata content.

We believe the TrackFind track search engine and metadata FAIRification service to be a major contribution, both to maintainers of genomic annotation track data, as well as to researchers and tool developers interested in making use of the wealth of track data publicly available. 


## Development
TrackFind is written in **Java 8** and uses **Maven 3** as a build-tool.

## Deployment
### Docker Compose
Simply execute `docker-compose up` and it will spin up the setup with PostgreSQL database, GSuite converter microservice and TrackFind microservice itself. Don't forget to specify all required environment variables: you can find them in the `docker-compose.yml` file.

## Acknowledgments

[![jProfiler](https://www.ej-technologies.com/images/product_banners/jprofiler_large.png)](https://www.ej-technologies.com/products/jprofiler/overview.html)
