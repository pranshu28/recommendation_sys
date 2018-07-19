package com.arango.spark.instacart;

import java.io.Serializable;

import com.arangodb.entity.DocumentField;
import com.arangodb.entity.DocumentField.Type;

public class Department implements Serializable{
	private static final long serialVersionUID = 1L;

	@DocumentField(Type.ID)
	private String _id;

	@DocumentField(Type.KEY)
	private String _key;

	@DocumentField(Type.REV)
	private String _revision;
	    
	private String department;

	public String getdepartment() {
    	return this.department;
    }
    public void setdepartment(String department) {
    	this.department = department;
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