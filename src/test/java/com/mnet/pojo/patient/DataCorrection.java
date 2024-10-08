package com.mnet.pojo.patient;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class DataCorrection {
	
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	Random rand = new Random();
	
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	List<String> unityDeviceList = Arrays.asList("3107-36", "1107-36", "1107-30", "2114", "3110");

	private String oldSerialNum;
	private String oldModelNum;
	private String deviceSerialNum;
	private String deviceModelNum;
	
	/**
	 *Assign a randomly generted 7 digit number to device serial number input 
	 */
	
	public DataCorrection() {
		deviceSerialNum = String.valueOf(rand.nextInt(9000000) + 1000000);
		deviceModelNum = unityDeviceList.get(rand.nextInt(unityDeviceList.size()));
	}

}
