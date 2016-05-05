package com.sjsu.booktrade.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;

import com.sjsu.booktrade.model.SchedulesTO;
import com.sjsu.booktrade.util.ConnectionPool;

public class SchedulesAndSlotService {

	public static boolean saveDaysAvailableAndTime(int bookId, int userId, String fromDay, String toDay, String timeFrom, String timeTo) {
		boolean addStatus = false;
		try{
			Connection connection = ConnectionPool.getConnectionFromPool();

			{
				PreparedStatement preparedStatement = connection
						.prepareStatement("insert into booktrade.pickup_schedules values(null,? ,? ,? ,? ,? ,?, null, null, null)");
				preparedStatement.setInt(1, bookId);
				preparedStatement.setInt(2, userId);
				preparedStatement.setString(3, fromDay);
				preparedStatement.setString(4, toDay);
				preparedStatement.setString(5, timeFrom);
				preparedStatement.setString(6, timeTo);

				int registerUpdate = preparedStatement.executeUpdate();
				ConnectionPool.addConnectionBackToPool(connection);
				if(registerUpdate == 1){
					addStatus = true;
				}	
			}

		}catch(Exception e){
			e.printStackTrace();
		}
		return addStatus;
	}

	public static SchedulesTO getSchedulesFromBookId(int bookId) {

		try{
			SchedulesTO schedules = new SchedulesTO();
			Connection connection = ConnectionPool.getConnectionFromPool();

			PreparedStatement preparedStatement = connection
					.prepareStatement("select * from booktrade.pickup_schedules where pickup_schedules.bookId = ?");
			preparedStatement.setInt(1, bookId);

			ResultSet resultSet = preparedStatement.executeQuery();

			while(resultSet.next()) {
				schedules.setScheduleId(resultSet.getInt("schedule_id"));
				schedules.setBookId(resultSet.getInt("book_id"));
				schedules.setSellerId(resultSet.getInt("seller_id"));
				schedules.setDayFrom(resultSet.getString("day_from"));
				schedules.setDayTo(resultSet.getString("day_to"));
				schedules.setTimeFrom(resultSet.getString("time_from"));
				schedules.setTimeTo(resultSet.getString("time_to"));
			}
			ConnectionPool.addConnectionBackToPool(connection);
			return schedules;
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}

	public static boolean updateBuyersPickupDetails(int bookId, Date pickupDate, String collectionTime, String day) {
		boolean addStatus = false;
		try{
			Connection connection = ConnectionPool.getConnectionFromPool();

			{
				PreparedStatement preparedStatement = connection
						.prepareStatement("update booktrade.pickup_schedules set pickup_date=?, pickup_time=?, pickup_day=? where book_id=?");
				preparedStatement.setTimestamp(1, new Timestamp(pickupDate.getTime()));
				preparedStatement.setString(2, collectionTime);
				preparedStatement.setString(3, day);
				preparedStatement.setInt(4, bookId);

				int registerUpdate = preparedStatement.executeUpdate();
				ConnectionPool.addConnectionBackToPool(connection);
				if(registerUpdate == 1){
					addStatus = true;
				}	
			}

		}catch(Exception e){
			e.printStackTrace();
		}
		return addStatus;
	}
}
