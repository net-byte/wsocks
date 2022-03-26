# wsocks 
A fast and high performance socks5 proxy over websocket based on netty.

# Getting started
## How to build
You require the following to build wsocks:

Latest stable OpenJDK 8 +
Latest stable Apache Maven

## Maven package
```
mvn package
```
## Config file
application.properties  

### client
```
local.host=127.0.0.1
local.port=1081
ws.host=you.domain.org
ws.port=443
ws.key=123456
ws.obfs=true
ws.path=/freedom
ws.scheme=wss
```

### server
```
ws.host=127.0.0.1
ws.port=443
ws.key=123456
ws.obfs=true
ws.path=/freedom
ws.scheme=wss
```

## Run java jar
### client
```
java -jar wsocks-cli-1.0.0.jar --spring.config.location=/data/config/application.properties
```
### server
```
java -jar wsocks-srv-1.0.0.jar --spring.config.location=/data/config/application.properties
```
## Run on docker

### client
```
docker run -e JAVA_OPTS='-server -Xmx128m -Xms128m' -e ARGS='--spring.config.location=/data/config/application.properties' -v /data/config/application.properties:/data/config/application.properties -d  -p 1081:1081 -p 1081:1081/udp --restart=always  --name wsocks-cli  netbyte/wsocks-cli
```

### server
```
docker run -e JAVA_OPTS='-server -Xmx128m -Xms128m' -e ARGS='--spring.config.location=/data/config/application.properties' -v /data/config/application.properties:/data/config/application.properties -d  -p 8088:8088 --restart=always  --name wsocks-srv  netbyte/wsocks-srv
```

## Deployment
wsocks-cli(wss 443)---->nginx/caddy(tls 443)---->wsocks-srv(ws 8088)

# License
Apache License v2.0
