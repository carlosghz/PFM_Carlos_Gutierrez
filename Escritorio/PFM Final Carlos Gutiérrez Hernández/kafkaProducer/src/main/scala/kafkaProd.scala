/**
 * Created by cgutierrez on 21/07/15.
 */



import java.util.HashMap

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.{ProducerConfig, KafkaProducer, ProducerRecord}

import scala.io.Source


object kafkaProd {

  def main(args: Array[String]) {

    val paciente = "bea"
    val source = "kinect"

    val homeDirData="./datos/"
    val dataKGA = Source.fromFile(homeDirData+"_kinect_good_A.csv").getLines()
    val dataKBA = Source.fromFile(homeDirData+"_kinect_bad_A.csv").getLines()
    val dataKGB = Source.fromFile(homeDirData+"_kinect_good_B.csv").getLines()
    val dataKBB = Source.fromFile(homeDirData+"_kinect_bad_B.csv").getLines()
    val dataIGA = Source.fromFile(homeDirData+"_imu_good_A.csv").getLines()
    val dataIBA = Source.fromFile(homeDirData+"_imu_bad_A.csv").getLines()
    val dataIGB = Source.fromFile(homeDirData+"_imu_good_B.csv").getLines()
    val dataIBB = Source.fromFile(homeDirData+"_imu_bad_B.csv").getLines()
    val dataKcal = Source.fromFile(homeDirData+"_kinect_calibracion.csv").getLines()
    val dataIcal = Source.fromFile(homeDirData+"_imu_calibracion.csv").getLines()
    val dataKG=dataKGA.toSeq++dataKGB.toSeq
    val dataKB=dataKBA.toSeq++dataKBB.toSeq
    val dataK=(dataKcal++dataKG++dataKB).toArray

    val dataIG=dataIGA.toSeq++dataIGB.toSeq
    val dataIB=dataIBA.toSeq++dataIBB.toSeq
    val dataI=(dataIcal++dataIG++dataIB).toArray


    // Zookeeper connection properties

    val brokers= "localhost:9092"
    val topic = "pfm"
    val props = new HashMap[String, Object]()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
      "org.apache.kafka.common.serialization.StringSerializer")

    val producer = new KafkaProducer[String, String](props)

    // Send some messages
    while(true) {
      for (i <- 0 to dataI.length-1) {
        val str1 = dataK.toArray.apply(i)
        val message = new ProducerRecord[String, String](topic, paciente+"/kinect/"+System.currentTimeMillis.toString, str1)
        producer.send(message)
        val str2 = dataI.toArray.apply(i)
        val message2 = new ProducerRecord[String, String](topic, paciente+"/IMU/"+System.currentTimeMillis.toString, str2)
        producer.send(message2)
        Thread.sleep(500)
      }
    }
  }


}
