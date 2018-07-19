package com.arango.spark.instacart;

import java.util.List;

import org.apache.spark.sql.SparkSession;

import com.arangodb.ArangoDB;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionType;
import com.arangodb.entity.EdgeDefinition;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.spark.ArangoSpark;

import scala.Tuple2;

public class Arango {
	
	protected static String GRAPH_NAME = "insta"; 
	protected static String PRODUCT_COLLECTION_NAME = "products";
	protected static String USER_COLLECTION_NAME = "users";
	protected static ArangoDatabase database = new ArangoDB.Builder().user("root").build().db();

	protected static List<Tuple2<Product, Double>> products = null;
	protected static String PROD_REC_COLLECTION_NAME = "prods_rec";
	protected static String LINK_REC_COLLECTION_NAME = "links_rec";
	
	public Arango(String graph, String product, String user) {
		GRAPH_NAME = graph;
		PRODUCT_COLLECTION_NAME = product;
		USER_COLLECTION_NAME = user;
		newEdgeGraph();
    }
	
	private void newCollection(String name,boolean edge) {
		try {
			if (edge) database.createCollection(name,new CollectionCreateOptions().type(CollectionType.EDGES));
			else database.createCollection(name);
		} catch (final ArangoDBException e) {}
	}
	
	private void newEdgeGraph(){
		try {
			newCollection(LINK_REC_COLLECTION_NAME,true);
			EdgeDefinition ed = new EdgeDefinition().collection(LINK_REC_COLLECTION_NAME).from(PRODUCT_COLLECTION_NAME).to(USER_COLLECTION_NAME);
			System.out.println(ed+" "+ed.getCollection()+" "+ed.getFrom()+" "+ed.getTo());
			database.graph(GRAPH_NAME).addEdgeDefinition(ed);
		} catch (final Exception ex) {}
	}
	
	private void createLink (Link link) throws ArangoDBException{
		try{
			database.graph(GRAPH_NAME).edgeCollection(LINK_REC_COLLECTION_NAME).insertEdge(link);
		} catch(ArangoDBException e) {}
	}
	
	public void exportArango(Integer user_id, Double order_dow, Double order_hour_of_day, List<Tuple2<Product, Double>> prod_new) {
		products = prod_new;
		for (Tuple2<Product, Double> pro:products) {
			Link link = new Link().setfromNodeId(pro._1.getId()).settoNodeId(USER_COLLECTION_NAME+"/"+user_id).setfreq(pro._2);
			if (order_dow!=-1) link.setorder_dow(order_dow);
			if (order_hour_of_day!=-1) link.setorder_hour_of_day(order_hour_of_day);
			createLink(link);
		}
	}
	
	public void saveRecProd(SparkSession spark) {
		newCollection(PROD_REC_COLLECTION_NAME,false);		
        ArangoSpark.save(spark.createDataFrame(products, Product.class), PROD_REC_COLLECTION_NAME);
	}
	
}