package com.arango.spark.instacart;

import java.io.Serializable;

import com.arangodb.entity.DocumentField;
import com.arangodb.entity.DocumentField.Type;

public class Order implements Serializable{
	private static final long serialVersionUID = 1L;

	@DocumentField(Type.ID)
	private String _id;

	@DocumentField(Type.KEY)
	private String _key;

	@DocumentField(Type.REV)
	private String _revision;
	    
	private String eval_set;
	private Integer user_id;
	private Double order_number,order_dow,order_hour_of_day,days_since_prior_order;
	
	public Integer getuser_id() {
    	return this.user_id;
    }
    public void setuser_id(Integer user_id) {
    	this.user_id = user_id;
    }
    public Double getdays_since_prior_order() {
    	return this.days_since_prior_order;
    }
    public void setdays_since_prior_order(Double days_since_prior_order) {
    	this.days_since_prior_order = days_since_prior_order;
    }
    public Double getorder_hour_of_day() {
    	return this.order_hour_of_day;
    }
    public void setorder_hour_of_day(Double order_hour_of_day) {
    	this.order_hour_of_day = order_hour_of_day;
    }
    public Double getorder_dow() {
    	return this.order_dow;
    }
    public void setorder_dow(Double order_dow) {
    	this.order_dow = order_dow;
    }
    public String geteval_set() {
    	return this.eval_set;
    }
    public void setorder_number(Double order_number) {
    	this.order_number = order_number;
    }
    public Double getorder_number() {
    	return this.order_number;
    }
    public void seteval_set(String eval_set) {
    	this.eval_set = eval_set;
    }
	public String getId() {
		return this._id;
	}
	public void setId(String newId) {
		this._id = newId;
	}
	public String getKey() {
		return this._key;
	}
	public void setKey(String newKey) {
		this._key = newKey;
	}
}
