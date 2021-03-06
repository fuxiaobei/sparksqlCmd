#!/usr/bin/env bash

BIN_PATH=$(cd `dirname $0`; pwd)

confPath=$1
SPARK_HOME=$2

if [ ! -n "$confPath" ] ;then
    echo "Usage: export.sh <sql definition file path>"
    exit 1
fi


for jarFile in `ls ${BIN_PATH}/../lib/*jar`
do
  CLASSPATH=${CLASSPATH}:${jarFile}
done

sqlType=`cat "${BIN_PATH}/../conf/running.properties" |grep "sql.type"`
secure_enable=`cat "${BIN_PATH}/../conf/running.properties" |grep "secure.enable"`

if [ x"$sqlType" = x ] ;then
   java -cp ${CLASSPATH} com.asiainfo.ocdp.sql.launcher.Main ${confPath}
else
   echo ${sqlType} | grep "thrift" 1>/dev/null 2>&1
   if [ $? -eq 0 ] ;then
      java -cp ${CLASSPATH} com.asiainfo.ocdp.sql.launcher.Main ${confPath}
   else
      echo ${sqlType} | grep "hbase" 1>/dev/null 2>&1
      if [ $? -eq 0 ] ;then
          echo ${sqlType} | grep "false" 1>/dev/null 2>&1
          if [ $? -eq 0 ] ;then
              java -Xms512m -Xmx1024m -Duser.dir=${BIN_PATH}/../conf -cp ${CLASSPATH} com.asiainfo.ocdp.sql.launcher.Main ${confPath}
          else
              java -Xms512m -Xmx1024m -Djava.security.auth.login.config=${BIN_PATH}/../conf/zookeeper_client_jaas.conf -Duser.dir=${BIN_PATH}/../conf -cp ${CLASSPATH} com.asiainfo.ocdp.sql.launcher.Main ${confPath}
          fi

      else
        if [ ! -n "$SPARK_HOME" ] ;then
            echo "If sql.type is not thrift, the usage: export.sh <sql definition file path> <SPARK_HOME>"
            exit 1
        fi
        CLASSPATH=${CLASSPATH//:/,}
        ${SPARK_HOME}/bin/spark-submit --class com.asiainfo.ocdp.sql.launcher.Main --jars ${CLASSPATH} ${BIN_PATH}/../lib/export_data-1.0.jar ${confPath}
      fi
   fi
fi

if [ $? -ne 0 ]; then
   echo "Export data failed!"
   exit 2
fi

exit 0