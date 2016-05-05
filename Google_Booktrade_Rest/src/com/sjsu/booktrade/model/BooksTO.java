package com.sjsu.booktrade.model;

public class BooksTO {

	private int bookId;
	private String bookName;
	private String author;
	private int edition;
	private String pickUpOrShip;
	private double price;
	private String category;
	private UserTO user;
	private AddressTO address;
	private String imageURLSmall;
	private String imageURLLarge;
	private SchedulesTO schedules;
	private boolean transactionComplete;
	private String notes;
	
	public String getBookName() {
		return bookName;
	}
	public void setBookName(String bookName) {
		this.bookName = bookName;
	}
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}
	public int getEdition() {
		return edition;
	}
	public void setEdition(int edition) {
		this.edition = edition;
	}
	public String getPickUpOrShip() {
		return pickUpOrShip;
	}
	public void setPickUpOrShip(String pickUpOrShip) {
		this.pickUpOrShip = pickUpOrShip;
	}
	public double getPrice() {
		return price;
	}
	public void setPrice(double price) {
		this.price = price;
	}
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public int getBookId() {
		return bookId;
	}
	public void setBookId(int bookId) {
		this.bookId = bookId;
	}
	public UserTO getUser() {
		return user;
	}
	public void setUser(UserTO user) {
		this.user = user;
	}
	public AddressTO getAddress() {
		return address;
	}
	public void setAddress(AddressTO address) {
		this.address = address;
	}
	public String getImageURLSmall() {
		return imageURLSmall;
	}
	public void setImageURLSmall(String imageURLSmall) {
		this.imageURLSmall = imageURLSmall;
	}
	public String getImageURLLarge() {
		return imageURLLarge;
	}
	public void setImageURLLarge(String imageURLLarge) {
		this.imageURLLarge = imageURLLarge;
	}
	public SchedulesTO getSchedules() {
		return schedules;
	}
	public void setSchedules(SchedulesTO schedules) {
		this.schedules = schedules;
	}
	public boolean isTransactionComplete() {
		return transactionComplete;
	}
	public void setTransactionComplete(boolean transactionComplete) {
		this.transactionComplete = transactionComplete;
	}
	public String getNotes() {
		return notes;
	}
	public void setNotes(String notes) {
		this.notes = notes;
	}
}
