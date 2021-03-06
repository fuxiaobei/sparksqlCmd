package com.asiainfo.ocdp.sql.core

import com.asiainfo.ocdp.sql.impl.{HiveContextImpl, PhoenixServerImpl, ThriftServerImpl}

/**
  * Created by peng on 2016/11/16.
  */
object SQLExecutionFactory {
  val getSQLExecution:SQLExecution = {
    val sqlType = Conf.properties.getOrElse("sql.type", "thrift")
    sqlType match {
      case "thrift" => new ThriftServerImpl
      case "hbase" => new PhoenixServerImpl
      case _ => new HiveContextImpl
    }
  }
}
