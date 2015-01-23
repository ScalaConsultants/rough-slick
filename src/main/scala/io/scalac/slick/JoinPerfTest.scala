package io.scalac.slick


import ch.qos.logback.classic.{Level, Logger}
import com.typesafe.config.ConfigFactory
import io.scalac.slick.db.{PlainDao, Dao, LiftedDao}
import org.slf4j.LoggerFactory

import scala.util.Random

object JoinPerfTest extends App {

  Random.setSeed(0L)

  val logger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger]
  logger.setLevel(Level.INFO)

  val testConfig = ConfigFactory.load().getConfig("join-perf-test")
  val nSuppliers = testConfig.getInt("n-suppliers")
  val nProducts = testConfig.getInt("n-products")
  val nPurchasers = testConfig.getInt("n-purchasers")
  val nSales = testConfig.getInt("n-sales")
  val selectThreshold = testConfig.getDouble("select-threshold")
  val nQueries = testConfig.getInt("n-queries")

  val MaxPrice = 1000.0
  val wherePrices = List.fill(nQueries)((1 - (1 - selectThreshold) * Random.nextDouble()) * MaxPrice)

  def fillData(): Unit = {
    LiftedDao.deleteAll()

    for (i <- 1 to nSuppliers) {
      LiftedDao.insertSupplier(i, Random.nextString(20))
    }

    for (i <- 1 to nProducts) {
      LiftedDao.insertProduct(i, Random.nextInt(nSuppliers) + 1, Random.nextString(20))
    }

    for (i <- 1 to nPurchasers) {
      LiftedDao.insertPurchaser(i, Random.nextString(20))
    }

    for (i <- 1 to nSales) {
      LiftedDao.insertSale(i, Random.nextInt(nPurchasers) + 1, Random.nextInt(nProducts) + 1, Random.nextDouble() * MaxPrice)
    }
  }

  def testIt(dao: Dao): Long = {
    val t0 = System.currentTimeMillis()
    for (price <- wherePrices) {
      dao.fetchSales(price)
    }
    System.currentTimeMillis() - t0
  }

  fillData()
  val liftedMillis = testIt(LiftedDao)
  val plainMillis = testIt(PlainDao)

  println("Lifted Embedding: %4.2f s".format(liftedMillis / 1000.0))
  println("Plain SQL:        %4.2f s".format(plainMillis / 1000.0))

}
