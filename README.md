# TrackFind

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/c3f38d6ea0184dab99bf012a04892c4c)](https://www.codacy.com/app/dtitov/trackfind?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=elixir-no-nels/trackfind&amp;utm_campaign=Badge_Grade)
[![Build Status](https://travis-ci.org/elixir-no-nels/trackfind.svg?branch=master)](https://travis-ci.org/elixir-no-nels/trackfind)
[![Docker Pulls](https://img.shields.io/docker/pulls/nels/trackfind.svg)](https://hub.docker.com/r/nels/trackfind/)

## Development
TrackFind is written in **Java 8** and uses **Maven 3** as a build-tool.

## Deployment
### Docker Compose
Simply execute `docker-compose up` and it will spin up the setup with PostgreSQL database, GSuite converter microservice and TrackFind microservice itself.

To use custom `trackfind.properties` mount corresponding file to `/trackfind/trackfind.properties` within the TrackFind container.

## Acknowledgments

[![jProfiler](https://www.ej-technologies.com/images/product_banners/jprofiler_large.png)](https://www.ej-technologies.com/products/jprofiler/overview.html)
