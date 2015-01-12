package io.scalac.slick.db

import scala.slick.driver.MySQLDriver.simple._

class Supplier(tag: Tag) extends Table[(Int, String, String)](tag, "supplier") {
  def id = column[Int]("id", O.PrimaryKey)
  def name = column[String]("name")
  def address = column[String]("address")

  def * = (id, name, address)
}

class Purchaser(tag: Tag) extends Table[(Int, String, String)](tag, "purchaser") {
  def id = column[Int]("id", O.PrimaryKey)
  def name = column[String]("name")
  def address = column[String]("address")

  def * = (id, name, address)
}

class Product(tag: Tag) extends Table[(Int, Int, String)](tag, "product") {
  def id = column[Int]("id", O.PrimaryKey)
  def supplierId = column[Int]("supplier_id")
  def name = column[String]("name")

  def * = (id, supplierId, name)
}

class Sale(tag: Tag) extends Table[(Int, Int, Int, BigDecimal)](tag, "sale") {
  def id = column[Int]("id", O.PrimaryKey)
  def purchaserId = column[Int]("purchaser_id")
  def productId = column[Int]("product_id")
  def total = column[BigDecimal]("total")

  def * = (id, purchaserId, productId, total)
}