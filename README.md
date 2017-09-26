# TrackFind

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/c3f38d6ea0184dab99bf012a04892c4c)](https://www.codacy.com/app/dtitov/trackfind?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=elixir-no-nels/trackfind&amp;utm_campaign=Badge_Grade)
[![Build Status](https://travis-ci.org/elixir-no-nels/trackfind.svg?branch=master)](https://travis-ci.org/elixir-no-nels/trackfind)
[![Docker Pulls](https://img.shields.io/docker/pulls/dtitov/trackfind.svg)](https://hub.docker.com/r/dtitov/trackfind/)

## Requirements
There are no any special requirements for deploying application using containering (via [Docker](#Docker) or [Singularity](#Singularity])), but if you prefer to run it natively, then you will need these parts to be installed on the host system:
- **JRE 8**
- **Git** + **Git LFS**

(also you will need **Maven 3** and **JDK 8** instead of **JRE 8** for building and running from [sources](#Sources)).

## Deployment
### Docker
```bash
docker run -d \
  -it \
  -v /absolute/path/to/indices/directory:/trackfind/indices \
  -v /absolute/path/to/trackfind.properties:/trackfind/trackfind.properties \
  -p 8888:8888 \
  --name trackfind \
  dtitov/trackfind:1.0.0
```
*Note: specifying volumes to mount is optional: first one will init application with pre-populated indices and the second one will override default application properties.*

Logs are available at `docker logs -f trackfind`

Next you can stop container using command `docker stop trackfind`, start container using command `docker start trackfind` and update container using next sequence:
```bash
docker stop trackfind && \
docker rm trackfind && \
docker rmi dtitov/trackfind:1.0.0 && \
```
...and the first step.

### Singularity
```bash
singularity create trackfind.img && \
singularity import trackfind.img docker://dtitov/trackfind:1.0.0 && \
nohup singularity run trackfind.img &
```
Logs are available at `tail -f console.log`

App can be stopped by executing `kill -9 $!` as `$!` is the PID of the last launched process (alternatively you can find it using `ps auxw | grep trackfind`.

### Java
```bash
wget https://github.com/elixir-no-nels/trackfind/releases/download/1.0.0/trackfind-1.0.0.jar && \
nohup java -jar trackfind-1.0.0.jar &
```
Logs are available at `tail -f console.log`

App can be stopped by executing `kill -9 $!` as `$!` is the PID of the last launched process (alternatively you can find it using `ps auxw | grep trackfind`.

### Sources
#### Checkout via Git
```bash
git clone https://github.com/elixir-no-nels/trackfind.git && \
cd trackfind && \
nohup mvn spring-boot:run &
```
#### Download as tar.gz
```bash
wget https://github.com/elixir-no-nels/trackfind/archive/1.0.0.tar.gz && \
tar -zxvf 1.0.0.tar.gz && \
cd trackfind-1.0.0 && \
nohup mvn spring-boot:run &
```

App can be stopped by executing `kill -9 $!` as `$!` is the PID of the last launched process (alternatively you can find it using `ps auxw | grep trackfind`.

# Acknowledgments

[![jProfiler](https://www.ej-technologies.com/images/product_banners/jprofiler_large.png)](https://www.ej-technologies.com/products/jprofiler/overview.html)
