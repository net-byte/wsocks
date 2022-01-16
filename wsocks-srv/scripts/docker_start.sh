#!bin/bash
NAME="wsocks-srv"
ARCH=`uname -m`
if [ $ARCH == 'x86_64' ]
then
TAG="latest"
elif [ $ARCH == 'aarch64' ]
then
TAG="arm64"
else
TAG=$ARCH
fi
docker pull netbyte/$NAME:$TAG
docker stop $NAME
docker rm $NAME
docker run -e JAVA_OPTS='-server -Xmx128m -Xms128m' -e ARGS='--spring.config.location=/data/config/application.properties' -v /data/config/application.properties:/data/config/application.properties -d  -p 8088:8088 --restart=always  --name $NAME netbyte/$NAME:$TAG

docker image prune -a
echo "DONE!!!"
