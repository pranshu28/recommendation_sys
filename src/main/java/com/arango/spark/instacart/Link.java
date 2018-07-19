package com.arango.spark.instacart;

import java.io.Serializable;

import com.arangodb.entity.DocumentField;
import com.arangodb.entity.DocumentField.Type;

public class Link implements Serializable{
	private static final long serialVersionUID = 1L;

	@DocumentField(Type.ID)
	private String _id;

	@DocumentField(Type.KEY)
	private String _key;

	@DocumentField(Type.REV)
	private String _revision;

	@DocumentField(Type.FROM)
	private String fromNodeId;

	@DocumentField(Type.TO)
	private String toNodeId;

	private Integer add_to_cart_order,reordered;
	private Double order_dow,order_hour_of_day;
	private String freq;
	
	public String getfreq() {
    	return this.freq;
    }
    public Link setfreq(Double freq) {
    	this.freq = freq.intValue()+"%";
    	return this;
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
    public Integer getreordered() {
		return this.reordered;
	}
	public void setreordered(Integer reordered) {
		this.reordered = reordered;
	}
	public Integer getadd_to_cart_order() {
		return this.add_to_cart_order;
	}
	public void setadd_to_cart_order(Integer add_to_cart_order) {
		this.add_to_cart_order = add_to_cart_order;
	}
	public String fromNode() {
        return fromNodeId;
    }
    public String toNode() {
        return toNodeId;
    }
	public Link setfromNodeId(String newFrom) {
		this.fromNodeId = newFrom;
		return this;
	}
	public Link settoNodeId(String newTo) {
		this.toNodeId = newTo;
		return this;
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
	public Link setKey(String newKey) {
		this._key = newKey;
		return this;
	}
}