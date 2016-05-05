package com.sjsu.booktrade.model;

public class SchedulesTO {

	private int scheduleId;
	private int bookId;
	private int buyerId;
	private int sellerId;
	private String dayFrom;
	private String dayTo;
	private String timeFrom;
	private String timeTo;
	
	public int getScheduleId() {
		return scheduleId;
	}
	public void setScheduleId(int scheduleId) {
		this.scheduleId = scheduleId;
	}
	public int getBookId() {
		return bookId;
	}
	public void setBookId(int bookId) {
		this.bookId = bookId;
	}
	public int getBuyerId() {
		return buyerId;
	}
	public void setBuyerId(int buyerId) {
		this.buyerId = buyerId;
	}
	public String getDayFrom() {
		return dayFrom;
	}
	public void setDayFrom(String dayFrom) {
		this.dayFrom = dayFrom;
	}
	public String getDayTo() {
		return dayTo;
	}
	public void setDayTo(String dayTo) {
		this.dayTo = dayTo;
	}
	public String getTimeFrom() {
		return timeFrom;
	}
	public void setTimeFrom(String timeFrom) {
		this.timeFrom = timeFrom;
	}
	public String getTimeTo() {
		return timeTo;
	}
	public void setTimeTo(String timeTo) {
		this.timeTo = timeTo;
	}
	public int getSellerId() {
		return sellerId;
	}
	public void setSellerId(int sellerId) {
		this.sellerId = sellerId;
	}
	
}
