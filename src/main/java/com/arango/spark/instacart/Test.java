package com.arango.spark.instacart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.distributed.RowMatrix;
import org.apache.spark.mllib.recommendation.MatrixFactorizationModel;
import org.apache.spark.mllib.recommendation.Rating;
import org.apache.spark.sql.SparkSession;

import com.arangodb.spark.rdd.api.java.ArangoJavaRDD;

import scala.Tuple2;

public class Test {
	protected static SparkSession spark = SparkSession.builder().getOrCreate();
	protected static RowMatrix tfsims = null;
	protected static MatrixFactorizationModel alsrec = null;
	protected static RowMatrix svdsims = null;
	protected static JavaSparkContext sc = null;
	protected static ArangoJavaRDD<ProdMeta> rddpm = null;
	protected static ArangoJavaRDD<UserPro> rddam = null;
	protected static ArangoJavaRDD<Order> rddo = null;
	protected static List<Tuple2<Long, Double>> sim_orders = null;
	protected static List<Tuple2<Long, Double>> als_items = null;
	protected static List<Tuple2<Long, Double>> tfidf_items = null;
	
	public Test(JavaSparkContext _sc, ArangoJavaRDD<ProdMeta> _rddpm, ArangoJavaRDD<UserPro> _rddam, ArangoJavaRDD<Order> _rddo) {
		sc = _sc;
		rddpm = _rddpm;
		rddam = _rddam;
		rddo = _rddo;
	}

	/*----Get Trained models for testing---- */
	public void getTrained(RowMatrix gettfidf, MatrixFactorizationModel alsModel, RowMatrix getsvd) {
		tfsims = gettfidf;
		alsrec = alsModel;
		svdsims = getsvd;		
	}
	
	public List<Tuple2<Long, Double>> svd_rec() {
		return sim_orders;
	}
	public List<Tuple2<Long, Double>> als_rec() {
		return als_items;
	}
	
	/*----Get input, predict most similar using 3 modules, return finally recommended products---- */
	public List<Tuple2<Long, Double>> recommendTo(double user_id, double order_dow, double order_hour_of_day, int total) {
		sim_orders = svdpred(user_id, order_dow, order_hour_of_day, 25);
		als_items = alspred(sim_orders,total);
		tfidf_items = tfidfpred(als_items,total);
		return tfidf_items;
	}

	/*----First input to SVD, to get similar orders made by other users---- */
	private List<Tuple2<Long, Double>> svdpred( Double user_id,double order_dow, double order_hour_of_day, int total) {
		
		JavaPairRDD<Order, Long> rdd_ = rddo.zipWithIndex();
		List<Tuple2<Order, Long>> sim_row = rdd_.filter(a -> (a._1.getuser_id()==user_id.intValue() || (a._1.getorder_dow()==order_dow && a._1.getorder_hour_of_day()==order_hour_of_day))).collect();
		
//		System.out.println("\n\n\nSVD Inputs:");
//		for (Tuple2<Order, Long> i:sim_row) {
//			System.out.println(i._1.getuser_id()+" "+i._1.getorder_dow()+" "+i._1.getorder_hour_of_day());
//		}
		
		JavaPairRDD<Vector, Long> rdd_sims = svdsims.rows().toJavaRDD().zipWithIndex();
		List<Long>  temp_orders = new ArrayList<Long>();
		int ind=0;
		for (Tuple2<Order, Long> a:sim_row) {
			if (ind>=10) break;
			temp_orders.add(Long.valueOf(a._1.getKey().toString()));
			List<Integer> rec_list = svd_single(rdd_,rdd_sims,a._2,5);
			for (Integer r:rec_list) temp_orders.add(Long.valueOf(r.toString()));
			ind++;
		}
		
		List<Long>  orders = new ArrayList<Long>();
		
		for (Long rec_ind:temp_orders){
			orders.add(Long.valueOf(rdd_.filter(a -> a._1.getKey().equals(rec_ind.toString())).first()._1.getKey()));
		}
		
//		System.out.println("\n\n\nSVD Predictions:");
//		for (Long order:orders) {
//			System.out.println(order);
//		}
		return sort_by_frequency(orders,total);
	}
	
	/*----A function used in svd_pred for iterating through each order---- */
	private List<Integer> svd_single(JavaPairRDD<Order, Long> rdd_, JavaPairRDD<Vector, Long> rdd_sims, Long index, int take) {
		List<Integer> user_rec = new ArrayList<Integer>();

		Vector v = rdd_sims.filter(a -> (a._2==index.longValue())).first()._1;
		int i=0;
		for (Double rec_ind:v.toArray()){
			if(i>take) break;
			user_rec.add(Integer.valueOf(rdd_.filter(a -> (a._2==rec_ind.longValue())).first()._1.getKey()));
			i++;
		}
		return user_rec;
	}
	
	/*----ALS takes list of order_id and returns similar/recommended products on the basis of data in User_Pro collection---- */
	private List<Tuple2<Long, Double>> alspred(List<Tuple2<Long, Double>> sim_orders2, int total) {
		List<Long>  temp_items = new ArrayList<Long>();
		
		for (Tuple2<Long, Double> order:sim_orders2) {
			try {
				Rating[] ratings = alsrec.recommendProducts(Integer.parseInt(order._1.toString()), total);
				for (Rating r:ratings) temp_items.add(Long.valueOf(r.product()));	
			}
			catch(Exception e){ }
		}
		
		List<UserPro> sort_products = new ArrayList<UserPro>();
		List<Long>  items = new ArrayList<Long>();
		
		for (Long rec_ind:temp_items){
			sort_products.add(rddam.filter(a -> a.getproduct_id()==rec_ind.intValue()).first());
		}
		
		// Sort the products according to their add_to_cart status
		Collections.sort(sort_products, new Comparator<UserPro>() {
            @Override
            public int compare(UserPro lhs, UserPro rhs) {
                return lhs.getadd_to_cart_order() > rhs.getadd_to_cart_order() ? 1 : (lhs.getadd_to_cart_order() < rhs.getadd_to_cart_order()) ? -1 : 0;
            }
        });
		int i=0;
		for (UserPro prod:sort_products) {
			if (i>total*5) break;
			items.add(Long.valueOf(prod.getproduct_id()));
			i++;
		}
		
		return sort_by_frequency(items,total*4);
	}

	/*----TFIDF (NLP related) takes list of product_id and returns similar products on the basis of its name---- */
	private List<Tuple2<Long, Double>> tfidfpred(List<Tuple2<Long, Double>> als_items2, int total) {	
		List<Long> items = new ArrayList<Long>();
		JavaPairRDD<ProdMeta, Long> rdd_ = rddpm.zipWithIndex();
		JavaPairRDD<Vector, Long> rdd_sims = tfsims.rows().toJavaRDD().zipWithIndex();
		Long index;
		for (Tuple2<Long, Double> prod_id:als_items2) {
			items.add(prod_id._1);
			index = rdd_.filter(a -> prod_id._1.equals(a._1.getproduct_id())).first()._2;
			List<Long> rec_item = tfidf_single(rdd_,rdd_sims,index,3);
			for (Long r:rec_item) items.add(r);
		}
		
//		System.out.println("\n\n\nTFIDF Predictions: ");
//		for (Long item:items) {
//			System.out.println(item);
//		}
		
		return sort_by_frequency(items,total);
	}
	
	/*----A function used in svd_pred for iterating through each product recommended by ALS---- */
	private List<Long> tfidf_single(JavaPairRDD<ProdMeta, Long> rdd_, JavaPairRDD<Vector, Long> rdd_sims, Long index, int take) {
		
		List<Long> rdd_rec = new ArrayList<Long>();

		Vector v = rdd_sims.filter(a -> (a._2==index.longValue())).first()._1;
		int i=0;
		for (Double rec_ind:v.toArray()){
			if (i>take) break;
			rdd_rec.add(rdd_.filter(a -> (a._2==rec_ind.longValue())).first()._1.getproduct_id());
			i++;
		}		
		return rdd_rec;
	}
	
	/*----A function to sort elements in a list on the basis of their occurrence---- */
	public List<Tuple2<Long, Double>> sort_by_frequency(List<Long> array,int total) {
		final Map<Long, Integer> counter = new HashMap<Long, Integer>();
		for (Long num : array)
		    counter.put(num, 1 + (counter.containsKey(num) ? counter.get(num) : 0));

		List<Long> list = new ArrayList<Long>(counter.keySet());
		Collections.sort(list, new Comparator<Long>() {
		    @Override
		    public int compare(Long x, Long y) {
		        return counter.get(y) - counter.get(x);
		    }
		});
		List<Tuple2<Long, Double>> ret_list = new ArrayList<Tuple2<Long,Double>>(); 
		double max = Collections.frequency(array, list.get(0))*100.0/total;
		int rank = total;
		for (Long ele:list) {
			Double prob = ((rank--)*Collections.frequency(array, ele)*100.0/total)*100.0/(total+1)/max;
			ret_list.add(new Tuple2<Long,Double>(ele,prob));
		}
		return (ret_list.size()>total) ? ret_list.subList(0, total) : ret_list;
	}
}
