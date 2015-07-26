import org.apache.spark.{SparkContext, SparkConf}

/**
 * Created by cgutierrez on 24/07/15.
 */
import com.datastax.spark.connector.cql.CassandraConnector

object startCassandra {

  def main(args: Array[String]) {

    val conf = new SparkConf(true)
      .set("spark.cassandra.connection.host", "127.0.0.1")
    val sc = new SparkContext("local", "startCassandra", conf)
    //creamos tablas
    val keyspace = "proyectoViernes"
    CassandraConnector(conf).withSessionDo { session =>
      session.execute("CREATE KEYSPACE IF NOT EXISTS " + keyspace + " WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1 }")
      session.execute("CREATE TABLE IF NOT EXISTS " + keyspace + ".databasestreaming (paciente text,source text,date text,data text,PRIMARY KEY (paciente, source, date))")
      session.execute("CREATE TABLE IF NOT EXISTS " + keyspace + ".pacmatred (paciente text,source text,version int,matrix text,PRIMARY KEY (paciente, source, version))")
      session.execute("CREATE TABLE IF NOT EXISTS " + keyspace + ".paclasif (paciente text,source text,date text,clasif int,PRIMARY KEY (paciente, source, date))")
    }
  }
}