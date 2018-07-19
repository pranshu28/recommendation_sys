package com.arango.spark.instacart;

import java.io.Serializable;

import com.arangodb.entity.DocumentField;
import com.arangodb.entity.DocumentField.Type;

public class User implements Serializable{
	private static final long serialVersionUID = 1L;

	@DocumentField(Type.ID)
	private String _id;

	@DocumentField(Type.KEY)
	private Long _key;

	@DocumentField(Type.REV)
	private String _revision;
	    
	public String getId() {
		return this._id;
	}
	public void setId(String newId) {
		this._id = newId;
	}
	public Long getKey() {
		return this._key;
	}
	public void setKey(Long newKey) {
		this._key = newKey;
	}
}