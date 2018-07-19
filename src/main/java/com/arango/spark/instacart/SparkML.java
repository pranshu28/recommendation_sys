package com.arango.spark.instacart;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.feature.HashingTF;
import org.apache.spark.ml.feature.IDF;
import org.apache.spark.ml.feature.IDFModel;
import org.apache.spark.ml.feature.Tokenizer;
import org.apache.spark.mllib.linalg.DenseMatrix;
import org.apache.spark.mllib.linalg.Matrices;
import org.apache.spark.mllib.linalg.Matrix;
import org.apache.spark.mllib.linalg.SingularValueDecomposition;
import org.apache.spark.mllib.linalg.SparseVector;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.mllib.linalg.distributed.RowMatrix;
import org.apache.spark.mllib.recommendation.ALS;
import org.apache.spark.mllib.recommendation.MatrixFactorizationModel;
import org.apache.spark.mllib.recommendation.Rating;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import com.arangodb.spark.rdd.api.java.ArangoJavaRDD;

import scala.collection.Iterator;

public class SparkML implements Serializable{
	private static final long serialVersionUID = 1L;
	protected static SparkSession spark = null;
	protected static JavaSparkContext sc = null;
	protected static String als_path = "als";
	protected static String tfidf_path = "tfidf";
	protected static String svd_path = "svd";
	
	
	public SparkML(SparkSession sp, JavaSparkContext jsc) {
		spark = sp;
		sc = jsc;
	}
		
	/*----Return the model saved in a file---- */
	public RowMatrix gettfidf() {
		return readTextFile(tfidf_path);
	}
	public MatrixFactorizationModel getals() {
		return MatrixFactorizationModel.load(sc.sc(), als_path);
	}
	public RowMatrix getsvd() {
		return readTextFile(svd_path);
	}
	
	/*----Train SVD---- */
	public void trainSVD(ArangoJavaRDD<Order> rddo, boolean useSVD) {
		Dataset<Row> df = spark.createDataFrame(rddo, Order.class);
		JavaRDD<Vector> countVectors = df.select("order_number","order_dow","order_hour_of_day","days_since_prior_order")
				.toJavaRDD().map(r -> {
				    double[] values = new double[r.size()];
				    for (int i = 0; i < r.size(); i++) {
				    	values[i] = (double)r.get(i);
				    }
				    return Vectors.dense(values);  
				});
		RowMatrix mat = new RowMatrix(countVectors.rdd());
		if (useSVD) {
			SingularValueDecomposition<RowMatrix, Matrix> svd = mat.computeSVD(3, true, 1.0E-9d);
			mat = svd_sim(svd);
		}
		RowMatrix sim = transpose_to_row(mat).columnSimilarities().toRowMatrix();
		saveSim(sim,svd_path);
		System.out.println("\n\n\nSVD Model Trained and Saved\n\n\n");
	}
	
	public RowMatrix svd_sim(SingularValueDecomposition<RowMatrix, Matrix> svd) {
		RowMatrix U = svd.U();  // The U factor is a RowMatrix.
	    Vector s = svd.s();     // The singular values are stored in a local dense vector.
	    Matrix V = svd.V();     // The V factor is a local dense matrix.
//	    System.out.println("U factor is:");
//	    for (Vector vector : (Vector[]) U.rows().collect()) {
//	      System.out.println("\t" + vector);
//	    }
//	    System.out.println("Singular values are: " + s);
//	    System.out.println("V factor is:\n" + V);
	    RowMatrix R = U.multiply(Matrices.diag(s)).multiply(V.transpose());
//	    System.out.println("\n\n\n"+mat_.numRows()+" "+mat_.numCols()+"\n\n\n");
		return R;
	}
	
	/*----Train ALS---- */
	public void trainALS(ArangoJavaRDD<UserPro> rddam) {
		// Run this ArangoQL query before executing this function, it adds a weight attribute to each document in User_pro collection 
		/*
		 * let c = (for i in user_pro collect j = i.user_id AGGREGATE m = MAX(i.add_to_cart_order) return {a:j,b:m})
		 * for x in user_pro
		 * let rat = (for i in c filter i.a == x.user_id return i.b)[0]
		 * let w = x.reordered + 1 - (x.add_to_cart_order/rat)
		 * update x with {weight:w} in user_pro
		 * */
		Dataset<Row> df = spark.createDataFrame(rddam, UserPro.class).select("order_id","product_id","weight");
		JavaRDD<Rating> ratings = sc.parallelize(df.collectAsList())
				.map(s -> new Rating(Integer.parseInt(s.get(0).toString()),Integer.parseInt(s.get(1).toString()),Double.parseDouble(s.get(2).toString())));
		int rank = 10; // 10 latent factors
		int numIterations = 5; // number of iterations
		MatrixFactorizationModel model = ALS.trainImplicit(JavaRDD.toRDD(ratings),rank, numIterations);
		model.save(sc.sc(), als_path);
		
		System.out.println("\n\n\nALS Model Trained and Saved\n\n\n");
	}
	
	/*----Train TFIDF---- */
	public void trainTfidf(ArangoJavaRDD<ProdMeta> rddpm){
		Dataset<Row> proddf = spark.createDataFrame(rddpm, ProdMeta.class);
		Tokenizer tokenizer = new Tokenizer().setInputCol("val").setOutputCol("words");
		Dataset<Row> wordsData = tokenizer.transform(proddf);
//		wordsData.show();
		int numFeatures = 20;
		HashingTF hashingTF = new HashingTF()
				.setInputCol("words")
				.setOutputCol("rawFeatures")
				.setNumFeatures(numFeatures);
		Dataset<Row> featurizedData = hashingTF.transform(wordsData);
//		featurizedData.show();
		IDF idf = new IDF().setInputCol("rawFeatures").setOutputCol("features");
		IDFModel idfModel = idf.fit(featurizedData);		
		Dataset<Row> rescaledData = idfModel.transform(featurizedData);
//		rescaledData.select("product_id","val","features").show();;
		RowMatrix sim_mat = tfidf_sim(rescaledData);
		
		saveSim(sim_mat,tfidf_path);
		System.out.println("\n\n\nTF-IDF Model Trained and Saved\n\n\n");
	}
	
	public RowMatrix tfidf_sim(Dataset<Row> tfidfs) {
		JavaRDD<Vector> countVectors = tfidfs.select("features").toJavaRDD()
	              .map(row -> SparseVector.fromML((org.apache.spark.ml.linalg.SparseVector)row.get(0)));
		RowMatrix mat = new RowMatrix(countVectors.rdd());
		RowMatrix rsr = transpose_to_row(mat);
//		tfidfs.select("product_id","val","features").show();
		return rsr.columnSimilarities().toRowMatrix();
	}
	
	/*----A function to take transpose of RowMatrix---- */
	public RowMatrix transpose_to_row(RowMatrix R) {
		DenseMatrix mat = new DenseMatrix((int) R.numRows(),(int) R.numCols(), R.toBreeze().toArray$mcD$sp(), true);
		Iterator<Vector> rs = mat.transpose().rowIter();
		List<Vector> list = new ArrayList<>();
		while (rs.hasNext()) list.add(rs.next());
		JavaRDD<Vector> dfcol = sc.parallelize(list);
		RowMatrix mat_trans = new RowMatrix(dfcol.rdd());
		return mat_trans;
	}
	
	/*----Take the similarity matrix as input, and select top 10 most similar rows indices and save as a file in the FileSystem---- */
	private void saveSim(RowMatrix mat,String path) {
		mat.rows().toJavaRDD().map(c -> {
			  
			  	List<Double> i_list = new ArrayList<Double>();
			  	for (int i=0;i<c.size();i++) i_list.add(c.apply(i));

				ArrayList<Double> sorted = new ArrayList<Double>(i_list);
				Collections.sort(i_list);

				int ind = 0, total=11;
				String arr = "";
				for (Double is:i_list){
					if(ind>i_list.size()-total && is>0.0){
						int k = sorted.indexOf(is);
						arr += k+",";
						sorted.set(k, 0.0);
					}
					ind++;
				}
		  		return arr.substring(0,arr.length()-1);
		    }).saveAsTextFile(path);
	}
	
	/*----Read the saved file and return as RowMatrix---- */
	private RowMatrix readTextFile(String path) {
		JavaRDD<Vector> mat = sc.textFile(path).map(s -> {
					String[] sarray = s.trim().split(",");
				    double[] values = new double[sarray.length];
				    for (int i = 0; i < sarray.length; i++) {
				      values[i] = Double.parseDouble(sarray[i]);
				    }	
				    return Vectors.dense(values);  
				    });
		return new RowMatrix(mat.rdd());
	}
}
