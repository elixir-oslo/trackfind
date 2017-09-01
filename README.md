# TrackFind

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/c3f38d6ea0184dab99bf012a04892c4c)](https://www.codacy.com/app/dtitov/trackfind?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=elixir-no-nels/trackfind&amp;utm_campaign=Badge_Grade)
[![Build Status](https://travis-ci.org/elixir-no-nels/trackfind.svg?branch=master)](https://travis-ci.org/elixir-no-nels/trackfind)
[![Docker Pulls](https://img.shields.io/docker/pulls/dtitov/trackfind.svg)](https://hub.docker.com/r/dtitov/trackfind/)

## Deployment
### Docker
```bash
docker run -d \
  -it \
  -v /path/to/indices/directory:/indices \
  -p 8888:8888 \
  --name trackfind \
  dtitov/trackfind:1.0.0
```
*Note: specifying volume to mount is optional.*

Logs are available at `docker logs -f trackfind`

### Java
```bash
wget https://github.com/elixir-no-nels/trackfind/releases/download/1.0.0/trackfind-1.0.0.jar

nohup java -jar trackfind-1.0.0.jar &
```
Logs are available at `tailf console.log`

### Sources
#### Checkout via Git
```bash
git clone https://github.com/elixir-no-nels/trackfind.git & \
cd trackfind
```
#### Download as tar.gz
```bash
wget https://github.com/elixir-no-nels/trackfind/archive/1.0.0.tar.gz && \
tar -zxvf 1.0.0.tar.gz & \
cd trackfind-1.0.0
```
#### Build an run using Maven
`mvn clean spring-boot:run`

Logs are available at `tailf console.log`
