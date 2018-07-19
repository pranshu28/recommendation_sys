package com.arango.spark.instacart;

import java.io.Serializable;

import com.arangodb.entity.DocumentField;
import com.arangodb.entity.DocumentField.Type;

public class UserPro implements Serializable{
	private static final long serialVersionUID = 1L;

	@DocumentField(Type.ID)
	private String _id;

	@DocumentField(Type.KEY)
	private String _key;
	
	@DocumentField(Type.REV)
	private String _revision;
	    
	private Integer order_id, product_id,user_id,add_to_cart_order,reordered;
	private Double weight;
	
    public Integer getreordered() {
		return this.reordered;
	}
	public void setreordered(Integer reordered) {
		this.reordered = reordered;
	}
	public Integer getorder_id() {
		return this.order_id;
	}
	public void setorder_id(Integer order_id) {
		this.order_id = order_id;
	}
	public Double getweight() {
		return this.weight;
	}
	public void setweight(Double weight) {
		this.weight = weight;
	}
	public Integer getadd_to_cart_order() {
		return this.add_to_cart_order;
	}
	public void setadd_to_cart_order(Integer add_to_cart_order) {
		this.add_to_cart_order = add_to_cart_order;
	}
	public Integer getproduct_id() {
    	return this.product_id;
    }
    public void setproduct_id(Integer product_id) {
    	this.product_id = product_id;
    }
    public Integer getuser_id() {
    	return this.user_id;
    }
    public void setuser_id(Integer user_id) {
    	this.user_id = user_id;
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
