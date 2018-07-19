package com.arango.spark.instacart;

import java.io.Serializable;

import com.arangodb.entity.DocumentField;
import com.arangodb.entity.DocumentField.Type;

public class Product implements Serializable{
	private static final long serialVersionUID = 1L;

	@DocumentField(Type.ID)
	private String _id;

	@DocumentField(Type.KEY)
	private String _key;

	@DocumentField(Type.REV)
	private String _revision;
	    
	private String product_name;
	private Integer aisle_id,department_id;

	public String getproduct_name() {
    	return this.product_name;
    }
    public void setproduct_name(String product_name) {
    	this.product_name = product_name;
    }
    public Integer getdepartment() {
    	return this.department_id;
    }
    public void setdepartment(Integer department_id) {
    	this.department_id = department_id;
    }
	public Integer getaisle() {
    	return this.aisle_id;
    }
    public void setaisle(Integer aisle_id) {
    	this.aisle_id = aisle_id;
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