package io.scalac.slick

import io.scalac.slick.db.LiftedDao

object Main extends App {
  println("SALES")
  val sales = LiftedDao.fetchSales(500).map(s => s.purchaser + ": " + s.product)
  println("COUNT")
  val count = LiftedDao.countSales()
  println("PRODS")
  val prods = LiftedDao.fetchProductNamesByIds(201 :: 202 :: Nil)

  println(sales)
  println(count)
  println(prods)
}
