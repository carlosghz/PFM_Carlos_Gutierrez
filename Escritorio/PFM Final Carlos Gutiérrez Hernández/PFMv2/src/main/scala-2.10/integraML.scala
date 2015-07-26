/**
 * Created by cgutierrez on 22/07/15.
 */


import org.apache.spark.mllib.tree.DecisionTree
import org.apache.spark.mllib.recommendation.{ALS, Rating, MatrixFactorizationModel}
import org.apache.spark.mllib.classification.{SVMModel, SVMWithSGD}
import org.apache.spark.mllib.regression.LabeledPoint
import scala.io.Source
import com.datastax.spark.connector._
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.mllib.recommendation.{ALS, Rating, MatrixFactorizationModel}
import org.apache.spark.mllib.linalg.distributed.RowMatrix
import org.apache.spark.mllib.linalg.Vectors

object integraML {
  def main(args: Array[String]) {

    val conf = new SparkConf(true)
      .set("spark.cassandra.connection.host", "127.0.0.1")
    val sc = new SparkContext("local", "ML", conf)

    val keySpace = "proyectoviernes"

    val paciente = "ana"

    // Carga Datos
    val homeDirData="./datos/"
    val dataKGA = Source.fromFile(homeDirData+"_kinect_good_A.csv").getLines().map(line=>{      val values = line.split(',').map(_.toDouble);      Vectors.dense(values)    })
    val dataKBA = Source.fromFile(homeDirData+"_kinect_bad_A.csv").getLines().map(line=>{      val values = line.split(',').map(_.toDouble);      Vectors.dense(values)    })
    val dataKGB = Source.fromFile(homeDirData+"_kinect_good_B.csv").getLines().map(line=>{      val values = line.split(',').map(_.toDouble);      Vectors.dense(values)    })
    val dataKBB = Source.fromFile(homeDirData+"_kinect_bad_B.csv").getLines().map(line=>{      val values = line.split(',').map(_.toDouble);      Vectors.dense(values)    })
    val dataIGA = Source.fromFile(homeDirData+"_imu_good_A.csv").getLines().map(line=>{      val values = line.split(',').map(_.toDouble);      Vectors.dense(values)    })
    val dataIBA = Source.fromFile(homeDirData+"_imu_bad_A.csv").getLines().map(line=>{      val values = line.split(',').map(_.toDouble);      Vectors.dense(values)    })
    val dataIGB = Source.fromFile(homeDirData+"_imu_good_B.csv").getLines().map(line=>{      val values = line.split(',').map(_.toDouble);      Vectors.dense(values)    })
    val dataIBB = Source.fromFile(homeDirData+"_imu_bad_B.csv").getLines().map(line=>{      val values = line.split(',').map(_.toDouble);      Vectors.dense(values)    })
    val dataKcal = Source.fromFile(homeDirData+"_kinect_calibracion.csv").getLines().map(line=>{      val values = line.split(',').map(_.toDouble);      Vectors.dense(values)    })
    val dataIcal = Source.fromFile(homeDirData+"_imu_calibracion.csv").getLines().map(line=>{      val values = line.split(',').map(_.toDouble);      Vectors.dense(values)    })


    // junta Datos Kinect
    val dataKG=dataKGA.toSeq++dataKGB.toSeq
    val dataKB=dataKBA.toSeq++dataKBB.toSeq
    val dataK=dataKG++dataKB++dataKcal.toSeq

    // junta Datos IMU
    val dataIG=dataIGA.toSeq++dataIGB.toSeq
    val dataIB=dataIBA.toSeq++dataIBB.toSeq
    val dataI=dataIG++dataIB++dataIcal.toSeq

    val dataKRDD=sc.parallelize(dataK.toSeq)
    val matK = new RowMatrix(dataKRDD)
    val PCA_K  = matK.computePrincipalComponents(10)

    val dataIRDD=sc.parallelize(dataI.toSeq)
    val matI = new RowMatrix(dataIRDD)
    val PCA_I  = matI.computePrincipalComponents(10)

    // entrenamos clasificador para Kinect
    val dataKgRDD = sc.makeRDD(dataKG,1)
    val matKG = new RowMatrix(dataKgRDD)
    var goodPoints=matKG.multiply(PCA_K).rows.map(x=>new LabeledPoint(1,x))
    val dataKbRDD=sc.parallelize(dataKB)
    val matKB = new RowMatrix(dataKbRDD)
    var badPoints=matKB.multiply(PCA_K).rows.map(x=>new LabeledPoint(0,x))

    var ptos= goodPoints++badPoints
    var splits = ptos.randomSplit(Array(0.6, 0.4))
    var trainingEt= splits(0)
    var testEt= splits(1)

    val numClasses = 2
    val categoricalFeaturesInfo = Map[Int, Int]()
    val impurity = "gini"
    val maxDepth = 5
    val maxBins = 32

    var model = DecisionTree.trainClassifier(trainingEt, numClasses, categoricalFeaturesInfo,
      impurity, maxDepth, maxBins)

    // Evaluate model on test instances and compute test error
    var labelAndPreds = testEt.map { point =>
      val prediction = model.predict(point.features)
      (point.label, prediction)
    }

    var unosEt= labelAndPreds.map(x=>x._1).filter(X=>X==1).count()
    var cerosEt= labelAndPreds.map(x=>x._1).filter(X=>X==0.0).count()
    System.out.println("numero de unos: "+unosEt)
    System.out.println("numero de ceros: "+cerosEt)

    var unosPr= labelAndPreds.map(x=>x._2).filter(X=>X==1).count()
    var cerosPr= labelAndPreds.map(x=>x._2).filter(X=>X==0.0).count()
    System.out.println("numero de unosPRED: "+unosPr)
    System.out.println("numero de cerosPRED: "+cerosPr)


    var testErr = labelAndPreds.filter(r => r._1 != r._2).count.toDouble / testEt.count()
    println("Test Error = " + testErr)
    println("Learned classification tree model K:\n" + model.toDebugString)

    model.save(sc, "./arboles/treeKinect_"+paciente)


    // entrenamos clasificador para IMU
    val dataIgRDD = sc.makeRDD(dataIG,1)
    val matIG = new RowMatrix(dataIgRDD)
    goodPoints=matIG.multiply(PCA_I).rows.map(x=>new LabeledPoint(1,x))
    val dataIbRDD=sc.parallelize(dataIB)
    val matIB = new RowMatrix(dataIbRDD)
    badPoints=matIB.multiply(PCA_I).rows.map(x=>new LabeledPoint(0,x))

    ptos= goodPoints++badPoints
    splits = ptos.randomSplit(Array(0.6, 0.4))
    trainingEt= splits(0)
    testEt= splits(1)



    model = DecisionTree.trainClassifier(trainingEt, numClasses, categoricalFeaturesInfo,
      impurity, maxDepth, maxBins)

    // Evaluate model on test instances and compute test error
    labelAndPreds = testEt.map { point =>
      val prediction = model.predict(point.features)
      (point.label, prediction)
    }

    unosEt= labelAndPreds.map(x=>x._1).filter(X=>X==1).count()
    cerosEt= labelAndPreds.map(x=>x._1).filter(X=>X==0.0).count()
    System.out.println("numero de unos: "+unosEt)
    System.out.println("numero de ceros: "+cerosEt)

    unosPr= labelAndPreds.map(x=>x._2).filter(X=>X==1).count()
    cerosPr= labelAndPreds.map(x=>x._2).filter(X=>X==0.0).count()
    System.out.println("numero de unosPRED: "+unosPr)
    System.out.println("numero de cerosPRED: "+cerosPr)


    testErr = labelAndPreds.filter(r => r._1 != r._2).count.toDouble / testEt.count()
    println("Test Error = " + testErr)
    println("Learned classification tree model I:\n" + model.toDebugString)

    model.save(sc, "./arboles/treeIMU_"+paciente)

    // guarda matriz PCA kinect
    var cadena= ""
    for ( j <- 0 to 10-1 ;i <- 0 to 204-1)    {      cadena=cadena+PCA_K.apply(i,j).toString+"/"    }
    cadena=cadena.substring(0,cadena.length-1)
    System.out.print(cadena)
    sc.parallelize( Seq((paciente, "kinect", 0, cadena)) ).saveToCassandra(keySpace, "pacmatred", SomeColumns("paciente", "source", "version", "matrix"))

    // guarda matriz PCA kinect
    cadena= ""
    for ( j <- 0 to 10-1 ;i <- 0 to 64-1)    {      cadena=cadena+PCA_I.apply(i,j).toString+"/"    }
    cadena=cadena.substring(0,cadena.length-1)
    System.out.print(cadena)
    sc.parallelize( Seq((paciente, "IMU", 0, cadena)) ).saveToCassandra(keySpace, "pacmatred", SomeColumns("paciente", "source", "version", "matrix"))


    sc.stop()
  }
}
