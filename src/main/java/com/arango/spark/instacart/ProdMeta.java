package com.arango.spark.instacart;

import java.io.Serializable;

import com.arangodb.entity.DocumentField;
import com.arangodb.entity.DocumentField.Type;

public class ProdMeta implements Serializable{
	private static final long serialVersionUID = 1L;

	@DocumentField(Type.ID)
	private String _id;

	@DocumentField(Type.REV)
	private String _revision;
	    
	private String val;
	private Long product_id;
	
	public Long getproduct_id() {
    	return this.product_id;
    }
    public void setproduct_id(Long product_id) {
    	this.product_id = product_id;
    }
    
	public String getval() {
    	return this.val;
    }
    public void setval(String val) {
    	this.val = val;
    }
	public String getId() {
		return this._id;
	}
	public void setId(String newId) {
		this._id = newId;
	}
}