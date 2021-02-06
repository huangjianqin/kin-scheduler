#!/usr/bin/env bash

# app name
app_name=Kin-Scheduler-Admin
# 指定运行脚本user
app_user=bigdata1

java="java"
# 项目根目录
base_dir=$(dirname $0)/..
# 项目main class
main_class=org.kin.conf.diamond.DiamondApplication
CLASSPATH=.:${base_dir}/lib/*:${base_dir}/resources/*:${CLASSPATH}

if [ ! -d ${base_dir}/gclogs ]; then
    mkdir ${base_dir}/gclogs -p
fi

# java jvm参数, 可修改
JAVA_OPT="${JAVA_OPT} -server -Xms256m -Xmx512m"
JAVA_OPT="${JAVA_OPT} -XX:MaxMetaspaceSize=256m"
JAVA_OPT="${JAVA_OPT} -verbose:gc -Xloggc:${base_dir}/gclogs/gc.log -XX:+PrintGCDetails -XX:+PrintGCTimeStamps"
JAVA_OPT="${JAVA_OPT} -XX:+UserGCLogFileRotation -XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=100m"
JAVA_OPT="${JAVA_OPT} -XX:-OmitStackTraceInFastThrow"
JAVA_OPT="${JAVA_OPT} -XX:MaxDirectMemorySize=512m"
JAVA_OPT="${JAVA_OPT} -XX:+HeapDumpOnOutOfMemoryError"
JAVA_OPT="${JAVA_OPT} -cp ${CLASSPATH}"

cmd=`nohup $java $JAVA_OPT $main_class $@ > /dev/null 2>&1 &`
current_user=`whoami`
if [ "${current_user}" = "root" ]; then
  su $app_user -c "$cmd"
else
  `$cmd`
fi

current_path=$(dirname $0)
echo $! >"${current_path}"/app.pid
echo "${app_name} started"