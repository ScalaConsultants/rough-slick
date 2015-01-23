# The rough experience with Slick

## Adventure begins

Recently me and a few of my colleagues from ScalaC worked on an integration project which aim was to provide
an interface between a machine learning engine and its clients via a Spray-based HTTP server and (internally)
a persistence layer. As our work meant to be just a part of a larger system we did not have much say in how the data
should be persisted or where - MySQL in this case. It was however up to us to decide how we would access the data.
Having had a lot of positive experience with Typesafe's technologies we immediately turned to Slick. It's
also worth mentioning that one of the expectations was to achieve a reasonable request rate with
minimal scaling as our client was a startup with limited funds.

## Spoiler alert
I'm about to present a couple of issues we ran into. Although they look bad, they are not meant to put you off altogether, rather they are caveats that should be considered before fully comitting to Slick. Your interactions with database (or performance expectations) may be different and you may never run into these kinds of problems. Also these issues relate mostly to MySQL, see the note about PostgreSQL close to the end.


## Use case
As a running example let's consider 4 tables which have similar relations as the ones we dealt with in the project:

```
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
```

What you see above is an example of Slick's Lifted Embedding which is a DSL for producing queries in a type safe manner. The classes defined here represent the database tables and their columns and they enable constructing queries using for comprehensions and other methods familiar from working with Scala collections. More on that here: http://slick.typesafe.com/doc/2.1.0/schemas.html. Slick also provides a way of writing queries in a Plain SQL mode which will turn out very useful as we'll see later.

As you can see all those tables are related and in fact all our queries required joining 2 or more tables.

## Join the Dark Side
Let's try to see who bought what from whom and for how much. Joining all 4 tables is not rocket science and in SQL it would look something like:

```
SELECT purchaser.name, supplier.name, product.name, sale.total
FROM sale join purchaser join product join supplier
ON (sale.purchaser_id = purchaser.id AND
    sale.product_id = product.id AND
    product.supplier_id = supplier.id)
WHERE sale.total >= 500
```

Having defined TableQuery objects like so: `val sales = TableQuery[Sale]`, we can construct the analogous query in Slick:

```
val salesQuery = {
  // Join the four tables
  val salesJoin = sales join purchasers join products join suppliers on {
    case (((sale, purchaser), product), supplier) =>
      sale.productId === product.id &&
      sale.purchaserId === purchaser.id &&
      product.supplierId === supplier.id
  }

  // Add a predicate and extract relevant columns
  def query(minTotal: Column[BigDecimal]) = for {
    (((sale, purchaser), product), supplier) <- salesJoin
    if sale.total >= minTotal
  } yield (purchaser.name, supplier.name, product.name, sale.total)

  // Precompile the query so it's efficiently reusable
  Compiled(query _)
}

def fetchSales(minTotal: BigDecimal): List[SaleRecord] = db.withSession { implicit session =>
  // fetch the results as a list of Scala types and transform to a model case class
  salesQuery(minTotal).list.map(SaleRecord.tupled)
}
```

The nested tuples in the matched pattern look a bit fishy, but aside from that the code is consice, readable, well typed and it works:

```
scala> fetchSales(500)
res1: List[io.scalac.slick.db.Dao.SaleRecord] = List(SaleRecord(Michael Palin,Birds Inc.,Norwegian Blue,500), SaleRecord(Hilton,Birds Inc.,Defeathering,2387), SaleRecord(Hilton,Soap Company,All Natural,1000))
```

But how exactly does it work? One thing I really like about Slick is the amount of information it gives about its performance if you let it log in DEBUG mode (the default). You can also call `selectStatement` on the query to reveal the true nature of Slick's SQL generator:

```
scala> println(salesQuery(500).selectStatement) //note the use of println to view the whole result
select s4.s56, s5.s62, s4.s60, s4.s54 from (select s6.s41 as s51, s6.s42 as s52, s6.s43 as s53, s6.s44 as s54, s6.s45 as s55, s6.s46 as s56, s6.s47 as s57, s7.s48 as s58, s7.s49 as s59, s7.s50 as s60 from (select s8.s34 as s41, s8.s35 as s42, s8.s36 as s43, s8.s37 as s44, s9.s38 as s45, s9.s39 as s46, s9.s40 as s47 from (select s81.`id` as s34, s81.`purchaser_id` as s35, s81.`product_id` as s36, s81.`total` as s37 from `sale` s81) s8 inner join (select s83.`id` as s38, s83.`name` as s39, s83.`address` as s40 from `purchaser` s83) s9 on 1=1) s6 inner join (select s85.`id` as s48, s85.`supplier_id` as s49, s85.`name` as s50 from `product` s85) s7 on 1=1) s4 inner join (select s87.`id` as s61, s87.`name` as s62, s87.`address` as s63 from `supplier` s87) s5 on ((s4.s53 = s4.s58) and (s4.s52 = s4.s55)) and (s4.s59 = s5.s61) where s4.s54 >= ?
```
Pure evil. Counting the selects and figuring out the nesting is left as an exercise to the reader.


## How fast can this thing go?
Given that the generated code looks like some kind of SQL assembly, maybe it's faster than we think? To verify, I wrote a very simple performance test, you can find it (and all the accompanying code, btw.) here: https://github.com/ScalaConsultants/rough-slick. Have a look if you're interested in the details of the test data or its volume. All that really matters is that test runs the query defined above and its Plain SQL equivalent 100 times on exactly the same data and it prints the respective times taken:

```
> runMain io.scalac.slick.JoinPerfTest
[info] Running io.scalac.slick.JoinPerfTest
Lifted Embedding: 87.09 s
Plain SQL:        0.10 s
[success] Total time: 89 s, completed Dec 21, 2014 10:06:29 AM
```

This issue is well known to Slick developers, you can find more info about it here: https://github.com/slick/slick/issues/623

## Who's in?

If you look closely at the `salesQuery` above you can see that it was `Compiled`. This step is optional but it's crucial to have if you're writing a performant application. Query compilation is a very expensive process and all details of it are logged as DEBUG messages, the most important one being:

```
20:27:03.184 [run-main-2] DEBUG s.s.compiler.QueryCompilerBenchmark -                     TOTAL:  185.287000 ms
```

That's just the time taken to prepare the SQL statement - no interaction with the RDBS at this point. Obviously we cannot afford to repeat this process for every db interaction and `Compiled` remedies that. 

However, there are cases where you cannot compile a query. One notable example is the `IN` operator. Consider this query:

```
SELECT name FROM product WHERE id IN (201, 202)
```

and its implementation in Slick:

```
def fetchProductNamesByIds(ids: List[Int]): List[String] = db.withSession { implicit session =>
  val query = products.filter(_.id inSet ids).map(_.name)
  query.list
}
```

A query to be compiled has to be a function of arguments of type `Column`, but unlike for scalar types, there is no conversion from a collection type to a `Column`. This issue is described in more detail here: https://github.com/slick/slick/issues/718

This simple query took about 20 ms on average to compile, but that can still be a lot for a high troughput application, and the `IN` operator can be a part of a more elaborate query like the one with the joins (as was the case in our project). The only solution then is to go with Plain SQL.


## If you can count - count on yourself

Even seemingly trivial queries can befuddle the SQL generator. Let's count rows in a table:

```
def countSales(): Int = db.withSession { implicit session =>
  sales.length.run
}
```

This innocent looking query surprisingly performs a full table scan and might hide from an unaware eye as a silent performance killer:

```
select s18.s17 from (select count(1) as s17 from (select s15.`id` as s19, s15.`purchaser_id` as s20, s15.`product_id` as s21, s15.`total` as s22 from `sale` s15) s24) s18
```

This issue is raised here: https://github.com/slick/slick/issues/489



## Some like it hot. I prefer classic SQL

The issues presented here led us to give up on Lifted Embedding altogether and go with Plain SQL. I was happy with this outcome. Any paradigm changing abstractions over relational databases seem like a bad idea to me, even if they try to mimic something as familiar and enjoyable as Scala collection API. 

There are multiple reasons to prefer the good old SQL:

* Maturity - it has been around for 40 years now as the one proper method of interacting with relational databases
* Practically everyone knows it to some extent, plenty of resources on the Internet - quite the opposite of Slick or any other new library
* The best choice for performance, free from limitations imposed by abstractions.

Fortunately Slick provides convenient methods of writing SQL queries. Parameteres to queries can be provided in 3 different ways including string interpolation and it's straight-forward to convert the results to tuples or case classes. Details here: http://slick.typesafe.com/doc/2.1.0/sql.html


## PostgreSQL case
If you investigate the issues on github you can see that people report them mostly against MySQL, so perhaps switching to another RDBMS would solve our problems. To verify, I modified the code a bit to run with PostgreSQL (to be found in branch `postgres`). The first look at the results is not promising: 

```
scala> println(salesQuery(BigDecimal(500)).selectStatement)
select s4.s56, s5.s62, s4.s60, s4.s54 from (select s6.s41 as s51, s6.s42 as s52, s6.s43 as s53, s6.s44 as s54, s6.s45 as s55, s6.s46 as s56, s6.s47 as s57, s7.s48 as s58, s7.s49 as s59, s7.s50 as s60 from (select s8.s34 as s41, s8.s35 as s42, s8.s36 as s43, s8.s37 as s44, s9.s38 as s45, s9.s39 as s46, s9.s40 as s47 from (select s81."id" as s34, s81."purchaser_id" as s35, s81."product_id" as s36, s81."total" as s37 from "sale" s81) s8 inner join (select s83."id" as s38, s83."name" as s39, s83."address" as s40 from "purchaser" s83) s9 on 1=1) s6 inner join (select s85."id" as s48, s85."supplier_id" as s49, s85."name" as s50 from "product" s85) s7 on 1=1) s4 inner join (select s87."id" as s61, s87."name" as s62, s87."address" as s63 from "supplier" s87) s5 on ((s4.s53 = s4.s58) and (s4.s52 = s4.s55)) and (s4.s59 = s5.s61) where s4.s54 >= 500
```

With minor exceptions this is practically the same query as for MySQL. How can it perform?

```
> runMain io.scalac.slick.JoinPerfTest
[info] Running io.scalac.slick.JoinPerfTest
Lifted Embedding: 0.85 s
Plain SQL:        0.13 s
[success] Total time: 2 s, completed Jan 23, 2015 8:07:50 PM
```
Wow! The difference is staggering. The most reasonable explanation lies in Postgres's advanced query optimizer. You can read about the details here: http://www.postgresql.org/docs/9.4/static/geqo.html

Still, there seems to be no way to circumvent the `IN` clause compilation problem.



## Bring it to the table

It certainly wouldn't be fair only to focus on the drawbacks of Slick as the DSL it introduces (which is a rather new concept often refferred to as Functional Relation Mapping) is certainly appealing to the fans of functional and statically typed programming. But tastes aside, probably the most important consequence is the short learning curve, and thus easy adoption into your software project as the methods of operating on tables look familiar even to beginners in Scala. 

Another thing to look out for is the upcoming release of Slick 3.0 (http://slick.typesafe.com/news/2014/12/19/slick-3.0.0-M1-released.html) which promises to take relational databases into the reactive world, which is another novelty amongst competing frameworks. Surely Slick developers know how to keep with the current trends.

So if I haven't scared you enough with the issues presented here, at the very least you might consider Slick's DSL as a fast and convenient prototyping tool, gradually falling back to Plain SQL mode once the bottlenecks in your application are recognized. If however, you know beforehand that you will need to squeeze the best out of a RDBMS you might be better of with frameworks like Anorm (https://www.playframework.com/documentation/2.3.x/ScalaAnorm) or ScalikeJDBC (http://scalikejdbc.org/) which aim to operate on databases in the fashion they were designed to (doesn't that sound like a good idea?)

Again, the code accompanying this blog post can be found here: https://github.com/ScalaConsultants/rough-slick

Happy querying!





