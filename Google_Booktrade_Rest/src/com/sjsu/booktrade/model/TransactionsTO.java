package com.sjsu.booktrade.model;

import java.util.Date;

public class TransactionsTO {

	private int transactionId;
	private long totalCredits;
	private int discountPercent;
	private int shippingFee;
	private Date transactionDate;
	private TransactionType type;
	public int getTransactionId() {
		return transactionId;
	}
	public void setTransactionId(int transactionId) {
		this.transactionId = transactionId;
	}
	public long getTotalCredits() {
		return totalCredits;
	}
	public void setTotalCredits(long totalCredits) {
		this.totalCredits = totalCredits;
	}
	public int getDiscountPercent() {
		return discountPercent;
	}
	public void setDiscountPercent(int discountPercent) {
		this.discountPercent = discountPercent;
	}
	public int getShippingFee() {
		return shippingFee;
	}
	public void setShippingFee(int shippingFee) {
		this.shippingFee = shippingFee;
	}
	public Date getTransactionDate() {
		return transactionDate;
	}
	public void setTransactionDate(Date transactionDate) {
		this.transactionDate = transactionDate;
	}
	public TransactionType getType() {
		return type;
	}
	public void setType(TransactionType type) {
		this.type = type;
	}
	
}
