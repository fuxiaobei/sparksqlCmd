package com.asiainfo.ocdp.sql.impl

import java.util.Properties
import java.util.concurrent.{CountDownLatch, Executors}

import com.asiainfo.ocdp.sql.core.Conf.SQLDefinition
import com.asiainfo.ocdp.sql.core.{Conf, Logging, SQLExecution}
import org.apache.commons.lang.StringUtils
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.phoenix.jdbc.PhoenixDriver
import org.apache.phoenix.mapreduce.util.PhoenixConfigurationUtil
import org.apache.phoenix.query.HBaseFactoryProvider

import scala.collection.mutable


/**
  * Created by peng on 2016/12/12.
  */
class PhoenixServerImpl extends SQLExecution with Logging{
  override def run: Unit = {
    logDebug("Start to run...")
    
    val queryServices = PhoenixDriver.INSTANCE.getQueryServices

    val phoenixDriverExecutor = queryServices.getExecutor

    phoenixDriverExecutor.setMaximumPoolSize(Conf.properties.getOrElse(Conf.PHOENIX_QUERY_THREADPOOLSIZE, "10000").toInt)

    logInfo(s"The current phoenix.query.threadPoolSize is ${phoenixDriverExecutor.getMaximumPoolSize}")

    val executor = Executors.newCachedThreadPool()

    try
    {
      val groupSQLTask = mutable.Map[String, mutable.ArrayBuffer[SQLDefinition]]()

      Conf.allSQLDefinitions.foreach(sqlDefinition => {
        val groupId = if (StringUtils.isEmpty(sqlDefinition.groupId)) Conf.DEFAULT_GROUP_ID else sqlDefinition.groupId
        if (groupSQLTask.contains(groupId)){
          groupSQLTask(groupId) += sqlDefinition
        }else{
          groupSQLTask += (groupId -> mutable.ArrayBuffer(sqlDefinition))
        }
      })

      logDebug(s"All tasks are ${groupSQLTask}")

      groupSQLTask.foreach(group_tasks =>{
        val sqlTasks = group_tasks._2

        val latch = new CountDownLatch(sqlTasks.size)
        logInfo("Begin executing...")
        val startTime = System.currentTimeMillis()

        sqlTasks.foreach(sqlDefinition => {
          executor.execute(new SQLTask(latch, sqlDefinition))
        })

        latch.await()

        val endTime = System.currentTimeMillis()
        logInfo(s"Group ${group_tasks._1} done and take ${endTime - startTime} ms.")

      })


    } finally {
      if (null != executor){
        executor.shutdownNow()
      }
      PhoenixDriver.INSTANCE.close()
    }
  }
}
