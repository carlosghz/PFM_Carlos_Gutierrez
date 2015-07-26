/**
 * Created by cgutierrez on 22/07/15.
 */

import org.apache.spark.mllib.classification.{SVMModel, SVMWithSGD}
import com.datastax.spark.connector._
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.mllib.recommendation.{ALS, Rating, MatrixFactorizationModel}
import org.apache.spark.mllib.linalg.Matrices
import org.apache.spark.mllib.linalg.distributed.RowMatrix
import org.apache.spark.mllib.linalg.Vectors


import org.apache.spark.mllib.tree.model.DecisionTreeModel
object processFromCass {


  def main(args: Array[String]) {

    val conf = new SparkConf().set("spark.cassandra.connection.host", "127.0.0.1").setMaster("local[1]").setAppName("processFromCass").set("spark.driver.allowMultipleContexts", "true")
    val sc = new SparkContext(conf)

    val keySpace = "proyectoviernes"
    //val paciente = "carlos"


    var listaTipos = List ("kinect","IMU")
    var listaPacientes = List ("ana","carlos","bea")
    for (tipo<-listaTipos; paciente<-listaPacientes) {
      System.out.println("Procesamos :"+paciente+" "+tipo)
      //comprobamos ultimo timeStamp procesado
      val limiteTemp = sc.cassandraTable(keySpace, "paclasif").where("paciente='"+paciente+"'").map(x => x.dataAsString).map(x => x.split(" ")).map(x => (x.apply(3).replace(",", ""), x.apply(5).replace(",", ""))).filter(x => x._1 == tipo).map(x => x._2.replace(".", "0").substring(0, x._2.length - 1).toLong).toArray()
      var lim = 0L
      if (limiteTemp.length > 0) {
        lim = limiteTemp.max
      }

      // obtenemos datos para trabajar
      val conjunto = sc.cassandraTable(keySpace, "databasestreaming").where("paciente='"+paciente+"'").map(x => x.dataAsString).map(_.split(" ")).map(x => (x.apply(1).replace(",", ""), x.apply(3).replace(",", ""), x.apply(5).replace(",", ""), x.apply(7)))
      var arrayDatos = conjunto.filter(x => x._2.contains(tipo)).map(x => x._4).map(x => x.substring(0, x.length - 1)).map(line => {
        val values = line.split(',').map(_.toDouble);
        Vectors.dense(values)
      }).toArray()

      // creamos matriz
      val PCAcas = sc.cassandraTable(keySpace, "pacmatred").where("paciente='"+paciente+"'")
      val matriz = PCAcas.toArray().map(x => x.dataAsString).filter(_.contains(tipo)).map(x => x.split(" ").apply(7)).apply(0)
      var cadena = matriz.substring(0, matriz.length - 1)
      val PCA = cadena.split("/").map(_.toDouble)
      var row = 64; if (PCA.length % 204 == 0) {
        row = 204
      }
      val col = PCA.length / row
      val Matrix = Matrices.dense(row, col, PCA.toArray)

      // Proyecta datos
      val dataConvert = new RowMatrix(sc.parallelize(arrayDatos)).multiply(Matrix)

      // Clasificamos
      val tree = DecisionTreeModel.load(sc, "./arboles/tree" + tipo + "_" + paciente)
      val prediction = tree.predict(dataConvert.rows)
      System.out.println("Total: " + prediction.count())
      val unos = prediction.filter(X => X == 1).count()
      val ceros = prediction.filter(X => X == 0.0).count()

      //juntamos los datos
      val zipDatos = conjunto.map(x => (x._1, x._2, x._3)).filter(x => x._2.contains(tipo)).zipWithIndex().map(x => (x._2, x._1))
      val zipPred = prediction.zipWithIndex().map(x => (x._2, x._1))
      val joinData = zipPred.join(zipDatos).map(x => (x._2._2._1, x._2._2._2, x._2._2._3, x._2._1))

      //guarda cassandra
      joinData.saveToCassandra(keySpace, "paclasif", SomeColumns("paciente", "source", "date", "clasif"))
    }

    sc.stop()

  }
}
