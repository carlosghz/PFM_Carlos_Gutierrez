/**
 * Created by cgutierrez on 21/07/15.
 */

import org.apache.spark.mllib.classification.{SVMModel, SVMWithSGD}
import com.datastax.spark.connector._
import com.datastax.spark.connector.streaming._
import org.apache.spark.SparkContext
import org.apache.spark.mllib.recommendation.{ALS, Rating, MatrixFactorizationModel}
import kafka.serializer.StringDecoder
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._
import org.apache.spark.SparkConf

object saveToCass {

  def main(args: Array[String]) {

    val conf = new SparkConf().set("spark.cassandra.connection.host", "127.0.0.1").setMaster("local[1]").setAppName("streamToCass").set("spark.driver.allowMultipleContexts", "true")
    val sc = new SparkContext(conf)

    val keySpace = "proyectoviernes"
    val paciente = "carlos"
    val brokers = "localhost:9092"
    val topics = "pfm"

    // Create context with 1 second batch interval
    val ssc = new StreamingContext(conf, Seconds(1))

    // Create direct kafka stream with brokers and topics
    val topicsSet = topics.split(",").toSet
    val kafkaParams = Map[String, String]("metadata.broker.list" -> brokers)
    val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParams, topicsSet)

    // Get the lines, split them into words, count the words and print
    val lines = messages.map(x=>(x._1.split("/").apply(0),x._1.split("/").apply(1),x._1.split("/").apply(2),x._2))
    lines.saveToCassandra(keySpace, "databasestreaming",SomeColumns("paciente", "source", "date", "data"))
    lines.print()

    ssc.start()
    ssc.awaitTermination() // Wait for the computation to terminate

  }
}
