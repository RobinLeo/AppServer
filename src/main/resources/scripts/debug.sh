cd `dirname $0`/..
BASE_DIR="`pwd`"

JAVA_HOME=/usr/local/jdk

echo Using BASE_DIR:   $BASE_DIR
echo Using JAVA_HOME:  $JAVA_HOME

b=`ps -ef|grep java |grep com.sz.lvban.task.TaskStart|awk '{print $2}'`
if [ ! -z $b ]; then
    echo "program is exists,pls check!"
    exit 1
fi

JAVA_OPTS="-XX:ErrorFile=$BASE_DIR/hs_err_pid%p.log -Xdebug -Xrunjdwp:transport=dt_socket,address=8888,server=y,suspend=n -Xms1536m -Xmx1536m -XX:NewSize=320m -XX:MaxNewSize=320m -XX:PermSize=96m -XX:MaxPermSize=96m"

$JAVA_HOME/bin/java $JAVA_OPTS -cp $BASE_DIR/bin:$JAVA_HOME/jre/lib/ext/sunjce_provider.jar -Djava.ext.dirs=$BASE_DIR/lib com.sz.lvban.task.TaskStart $1 $2 $3 $4 $5 $6 $7 $8 $9 &
