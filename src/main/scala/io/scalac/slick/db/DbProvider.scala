package io.scalac.slick.db

import com.jolbox.bonecp.BoneCPDataSource
import com.typesafe.config.ConfigFactory

import scala.slick.driver.MySQLDriver.backend.Database

trait DbProvider {

  private val dataSource = {
    val dbConfig = ConfigFactory.load().getConfig("db")

    Class.forName(dbConfig.getString("driver"))

    val ds = new BoneCPDataSource()
    ds.setJdbcUrl(dbConfig.getString("jdbc.url"))
    ds.setUsername(dbConfig.getString("user"))
    ds.setPassword(dbConfig.getString("password"))
    ds
  }

  val db = Database.forDataSource(dataSource)
}
