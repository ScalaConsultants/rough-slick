These are simple experiments with Slick 2.1 framework which supplement the blog post available here: *link to blog post*

### Running

```
sbt run
```

then choose the main class:

* `io.scalac.slick.Main` - just a playground
* `io.scalac.slick.JoinPerfTest` - a test comparing performance of a query with multiple joins defined in Slick DSL vs Plain SQL.

### Prerequisities

Have MySQL 5.5+ installed and run `sql/mysql-reload.sh`