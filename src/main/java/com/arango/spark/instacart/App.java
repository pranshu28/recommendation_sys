package com.arango.spark.instacart;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;

import com.arangodb.spark.ArangoSpark;
import com.arangodb.spark.rdd.api.java.ArangoJavaRDD;

import scala.Tuple2;

public class App {
	protected static String GRAPH_NAME = "insta"; 
	protected static String LINK_COLLECTION_NAME = "order_product";
	protected static String PRODUCT_COLLECTION_NAME = "products";
	protected static String PRODMETA_COLLECTION_NAME = "all_prod";
	protected static String USERPRO_COLLECTION_NAME = "user_pro";
	protected static String ORDER_COLLECTION_NAME = "orders";
	protected static String AISLE_COLLECTION_NAME = "aisles";
	protected static String DEPARTMENT_COLLECTION_NAME = "departments";
	protected static String USER_COLLECTION_NAME = "users";
	protected static Integer user_id = -1;
	protected static Double order_dow = -1.0;
	protected static Double order_hour_of_day = -1.0;
	protected static Integer total = 25;

	
	public static void main(String[] args) throws IOException {
		
		// Parse the arguments first
		parse_args(args);
		
		// Environment definitions
		SparkConf conf = new SparkConf()
				.setMaster("local[2]")
				.setAppName("arango")
				.set("arangodb.host", "127.0.0.1")
				.set("arangodb.port", "8529")
				.set("arangodb.user", "root")
				.set("arangodb.password", "");
		JavaSparkContext sc = new JavaSparkContext(conf);
		SparkSession spark = SparkSession.builder().getOrCreate();
		
		// Import data from ArangoDB to Apache Spark RDD
		List<Department> deps =ArangoSpark.load(sc, DEPARTMENT_COLLECTION_NAME, Department.class).sortBy( a -> Integer.parseInt(a.getKey()),true, 1).collect();
		List<Aisle> aisles = ArangoSpark.load(sc, AISLE_COLLECTION_NAME, Aisle.class).sortBy( a -> Integer.parseInt(a.getKey()),true, 1).collect();
		ArangoJavaRDD<Product> rddp = ArangoSpark.load(sc, PRODUCT_COLLECTION_NAME, Product.class);
		ArangoJavaRDD<ProdMeta> rddpm = ArangoSpark.load(sc, PRODMETA_COLLECTION_NAME, ProdMeta.class);
		ArangoJavaRDD<UserPro> rddam = ArangoSpark.load(sc, USERPRO_COLLECTION_NAME, UserPro.class);
		ArangoJavaRDD<Order> rddo = ArangoSpark.load(sc, ORDER_COLLECTION_NAME, Order.class);
		System.out.println("\n\n\nImported data from ArangoDB successfully\n\n\n");
		
		// Train the models (only for once)
		SparkML defs = new SparkML(spark,sc);
       defs.trainTfidf(rddpm);
       defs.trainALS(rddam);
       defs.trainSVD(rddo,true);
		
		//Input the trained models for testing
		Test test = new Test(sc,rddpm,rddam,rddo);
		test.getTrained(defs.gettfidf(),defs.getals(),defs.getsvd());
		System.out.println("\n\n\nImported trained models\n\n\n");
		
		// Get final recommended products for given input
		List<Tuple2<Long, Double>> recommended = test.recommendTo(user_id, order_dow, order_hour_of_day, total);
	
		// Collect the results from individual models
		List<Product> prod_old = new ArrayList<Product>();
		List<Tuple2<Order, Double>> orders_svd = new ArrayList<Tuple2<Order, Double>>();
		List<Tuple2<Product, Double>> prod_als = new ArrayList<Tuple2<Product, Double>>();
		List<Tuple2<Product, Double>> prod_new = new ArrayList<Tuple2<Product, Double>>();
		for (UserPro up:rddam.filter(a -> a.getuser_id()==user_id).collect()) {
			prod_old.add(rddp.filter(a -> a.getKey().equals(up.getproduct_id().toString())).first());
		}
		for(Tuple2<Long, Double> ords:test.svd_rec()) {
			orders_svd.add(new Tuple2<Order,Double>(rddo.filter(a -> a.getKey().equals(ords._1.toString())).first(),ords._2));
		}
		for(Tuple2<Long, Double> prods:test.als_rec()) {
			prod_als.add(new Tuple2<Product,Double>(rddp.filter(a -> a.getKey().equals(prods._1.toString())).first(), prods._2));
		}
		for (Tuple2<Long, Double> pid:recommended) {
			prod_new.add(new Tuple2<Product,Double>(rddp.filter(a -> a.getKey().equals(pid._1.toString())).first(),pid._2));
		}
		Collections.sort(prod_new, new Comparator<Tuple2<Product,Double>>() {
			@Override
			public int compare(Tuple2<Product,Double> lhs, Tuple2<Product,Double> rhs) {
				// -1 - less than, 1 - greater than, 0 - equal, all inverse for descending
				return lhs._1.getdepartment() > rhs._1.getdepartment() ? -1 : (lhs._1.getdepartment() < rhs._1.getdepartment()) ? 1 : 0;
			}
		});

		// Export the recommended products for visualization in ArangoDB
		if (user_id!=-1) {
			Arango exp = new Arango(GRAPH_NAME, PRODUCT_COLLECTION_NAME, USER_COLLECTION_NAME);
			exp.exportArango(user_id, order_dow, order_hour_of_day, prod_new);
		}
		
		// Print the results
		System.out.println("\nTested for - user: "+user_id+" - DOW: "+order_dow+" - Hour: "+order_hour_of_day);
		System.out.println("\nPreviously ordered (if any): \n");
		System.out.println("\nproduct_id\tdepartment\taisle\tproduct_name\n");
		for (Product x:prod_old) {
			System.out.println(x.getKey()+"\t\t"
					+deps.get(x.getdepartment()-1).getdepartment()+"\t\t"
					+aisles.get(x.getaisle()-1).getaisle()+"\t\t"
					+x.getproduct_name());
		}
		System.out.println("\nSimilar orders (sort-by rec_percent): \n");
		System.out.println("\nrec_percent\torder_id\tuser_id order_dow\torder_hour_of_day\torder_number\tday_since_prior_order\n");
		for (Tuple2<Order,Double> x:orders_svd) {
			System.out.println("rec_percent: "+x._2.intValue()+"%\t"
					+x._1.getKey()+"\t"
					+x._1.getuser_id()+"\t"
					+x._1.getorder_dow()+"\t"
					+x._1.getorder_hour_of_day()+"\t"
					+x._1.getorder_number()+"\t"
					+x._1.getdays_since_prior_order());
		}
		System.out.println("\nALS recommended products (sort-by rec_percent):\n");
		System.out.println("\nrec_percent\tproduct_id\tdepartment\taisle\tproduct_name\n");
		for (Tuple2<Product, Double> x:prod_als) {
			System.out.println("rec_percent: "+x._2.intValue()+"%\t"
					+x._1.getKey()+"\t\t"
					+deps.get(x._1.getdepartment()-1).getdepartment()+"\t\t"
					+aisles.get(x._1.getaisle()-1).getaisle()+"\t\t"
					+x._1.getproduct_name());
		}
		System.out.println("\nTFIDF Recommended products (sort-by Department):\n");
		System.out.println("\nrec_percent\tproduct_id\tdepartment\taisle\tproduct_name\n");
		for (Tuple2<Product, Double> x:prod_new) {
			System.out.println("rec_percent: "+x._2.intValue()+"%\t"
					+x._1.getKey()+"\t\t"
					+deps.get(x._1.getdepartment()-1).getdepartment()+"\t\t"
					+aisles.get(x._1.getaisle()-1).getaisle()+"\t\t"
					+x._1.getproduct_name());
		}
	}
	
	/*----Parse the provided arguments - total recommendations, user, order_details ----*/
	private static void parse_args(String[] args) {
		try{
			total = Integer.valueOf(args[0]);
			if (args.length==2) {
				user_id = Integer.valueOf(args[1]);
			}
			else if (args.length==3) {
				order_dow = Double.valueOf(args[1]);
				order_hour_of_day = Double.valueOf(args[2]);
			}
			else if (args.length==4){
				user_id = Integer.valueOf(args[1]);
				order_dow = Double.valueOf(args[2]);
				order_hour_of_day = Double.valueOf(args[3]);
			}
			else {
				throw new NullPointerException();
			}
		}
		catch (Exception e) {
			System.out.println("\nArguments Error");
			System.out.println("Hint:");
			System.out.println("arg1 = number of recommendations");
			System.out.println("arg2 = user_id OR/AND day - time\n");
			System.exit(0);
		}
	}
}