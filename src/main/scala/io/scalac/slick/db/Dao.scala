package io.scalac.slick.db

object Dao {
  case class SaleRecord(purchaser: String, supplier: String, product: String, total: BigDecimal)
}

trait Dao {
  def fetchSales(minTotal: BigDecimal): Seq[Dao.SaleRecord]
}
