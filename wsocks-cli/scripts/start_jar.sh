#!bin/bash
export LANG="zh_CN.utf-8"
set -e
JAR_PATH="./target/"
JAR_NAME="wsocks-cli-1.0.0.jar"
LOG_NAME="wsocks-cli.out"
JAVA_OPTS="-server -Xmx128m -Xms128m"
CONFIG_ARGS="--spring.config.location=./target/config/application.properties"
kill_jar()
{
    echo "Kill "$JAR_NAME
    PID=`ps -ef | grep java | grep $JAR_NAME |awk '{print $2}'`
    for id in $PID
    	do
        	kill -9 $id
            echo "Killed pid $id"
        done
    sleep 1
}

start_jar()
{
	echo "Start "$JAR_NAME
	nohup java  $JAVA_OPTS -jar $JAR_PATH$JAR_NAME $CONFIG_ARGS > $JAR_PATH$LOG_NAME 2>&1 &
}

show_logs() 
{
	echo "Show logs"
	count=0
	while [ $count -lt 1 ]
	do
    	tail -10 $JAR_PATH$LOG_NAME
    	count=`tail -100 $JAR_PATH$LOG_NAME | grep "Started Application" | grep -v grep | wc -l`
    	if [ $count -ge 1 ]
    		then
       		echo $JAR_NAME" has started!!!"
    	fi
    	sleep 1
	done
}

git pull
mvn package
kill_jar
start_jar
show_logs
