package com.mnet.framework.utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.email.EmailParser;
import com.mnet.framework.reporting.FrameworkLog;

/**
 * Utility functions to perform common operations across automation
 * 
 * @author NAIKKX12
 *
 */
public class CommonUtils {
	public enum SortOrder {
		ASCENDING, DESCENDING
	}
	
	public enum StringType {
		UPPER, LOWER, NUMBER, SPECIAL_CHARS
	}
	
	/**
	 * 
	 * @param length
	 * @return random string of specified length
	 */
	public static String randomStringSimple(int length) {
		String alphanumericCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvxyz";

		StringBuffer randomString = new StringBuffer(length);
		Random random = new Random();

		for (int i = 0; i < length; i++) {
			int randomIndex = random.nextInt(alphanumericCharacters.length());
			char randomChar = alphanumericCharacters.charAt(randomIndex);
			randomString.append(randomChar);
		}

		return randomString.toString();
	}

	/**
	 * 
	 * @param min
	 * @param max
	 * @return random integer between min & max specified
	 */
	public static int getRandomNumber(int min, int max) {
		return (int) Math.floor(Math.random() * (max - min + 1)) + min;
	}
	
	/**
	 * Random integer between min & max and excluding range specified by rangeFrom to rangeTo
	 * @param min
	 * @param rangeFrom
	 * @param rangeTo
	 * @param max
	 * @return
	 */
	public static int getRandomNumberOutsideRange(int min, int rangeFrom, int rangeTo, int max) {
		List<Integer> numbers = Arrays.asList(CommonUtils.getRandomNumber(min, rangeFrom - 1), CommonUtils.getRandomNumber(rangeTo + 1, max));
		Random rand = new Random();
		return numbers.get(rand.nextInt(numbers.size()));
	}

	/**
	 * 
	 * @param length
	 * @return random alphanumeric string containing lower case, upper case
	 *         characters as well number
	 */
	public static String randomAlphanumericString(int length) {
		String alphanumericCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvxyz";

		StringBuffer randomString = new StringBuffer(length);
		Random random = new Random();

		for (int i = 0; i < length; i++) {
			int randomIndex = random.nextInt(alphanumericCharacters.length());
			char randomChar = alphanumericCharacters.charAt(randomIndex);
			randomString.append(randomChar);
		}

		return randomString.toString();
	}
	
	/**
	 * Returns time that has elapsed (in ms) since designated system start time.
	 * @param startTime Start time in JVM nanoseconds. Use System.nanoTime() to obtain this value.
	 * @author Arya Biswas
	 * */
	public static double millisFromTime(long startTime) {
		return (System.nanoTime() - startTime) / Math.pow(10, 6);
	}
	
	/**
	 * 
	 * @param length
	 * @return random string containing lower case, upper case, special chars, numbers
	 */
	@Deprecated
	// Instead use randomString(int length, StringType... stringTypes)
	public static String randomString(int length, boolean upper, boolean lower, boolean number, boolean special) {
		
		String upperChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	    String lowerChars = "abcdefghijklmnopqrstuvwxyz";
	    String numbers = "0123456789";
	    String specialChars = "~!#$^()-_+{}[]|:;<>,.?";

	    String allTypeMixed = "";
	    if (upper) {
	    	allTypeMixed = allTypeMixed + upperChars;	    	
	    }
	    if (lower) {
	    	allTypeMixed = allTypeMixed + lowerChars;
	    }
	    if (number) {
	    	allTypeMixed = allTypeMixed + numbers;
	    }
	    if (special) {
	    	allTypeMixed = allTypeMixed + specialChars;
	    }
	    
	    boolean upperExist = false, lowerExist = false, specialExist = false, numberExist = false;
		
		StringBuffer randomString = new StringBuffer(length);
		Random random = new Random();

		// Generate first half expected string with random chars
		for (int i = 0; i < length/2; i++) {
			if (allTypeMixed.length() == 0) {
				allTypeMixed = allTypeMixed + upperChars.charAt(upperChars.length() - 1);
			}
			int randomIndex = random.nextInt(allTypeMixed.length());
			char randomChar = allTypeMixed.charAt(randomIndex);
			
			if (upper && upperExist == false) {
				if (upperChars.indexOf(randomChar) > 0) {
					upperExist = true;
				}
			}
			if (lower && lowerExist == false) {
				if (lowerChars.indexOf(randomChar) > 0) {
					lowerExist = true;
				}
			}
			if (special && specialExist == false) {
				if (specialChars.indexOf(randomChar) > 0) {
					specialExist = true;
				}
			}
			if (number && numberExist == false) {
				if (numbers.indexOf(randomChar) > 0) {
					numberExist = true;
				}
			}
			randomString.append(randomChar);
		}
		
		int currentLen = randomString.length();
		// Check if at least one character from each exists else generate
		if (upper && upperExist == false) {
			randomString.append(upperChars.charAt(random.nextInt(upperChars.length())));
			currentLen++;
		}
		if (lower && lowerExist == false) {
			randomString.append(lowerChars.charAt(random.nextInt(lowerChars.length())));
			currentLen++;
		}
		if (number && numberExist == false) {
			randomString.append(numbers.charAt(random.nextInt(numbers.length())));
			currentLen++;
		}
		if (special && specialExist == false) {
			randomString.append(specialChars.charAt(random.nextInt(specialChars.length())));
			currentLen++;
		}
		
		for (int i = currentLen; i < length; i++) {
			int randomIndex = random.nextInt(allTypeMixed.length());
			char randomChar = allTypeMixed.charAt(randomIndex);
			randomString.append(randomChar);
		}

		return randomString.toString();
	}
	
	/**
	 * 
	 * @param length
	 * @return random string containing lower case, upper case, special chars, numbers
	 */
	public static String randomString(int length, StringType...stringTypes) {
		
		String upperChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	    String lowerChars = "abcdefghijklmnopqrstuvwxyz";
	    String numbers = "0123456789";
	    String specialChars = "~!#$^()-_+{}[]|:;<>,.?";

	    String allTypeMixed = "";
	    if (Arrays.binarySearch(stringTypes, StringType.UPPER) >= 0) {
	    	allTypeMixed = allTypeMixed + upperChars;	    	
	    }
	    if (Arrays.binarySearch(stringTypes, StringType.LOWER) >= 0) {
	    	allTypeMixed = allTypeMixed + lowerChars;
	    }
	    if (Arrays.binarySearch(stringTypes, StringType.NUMBER) >= 0) {
	    	allTypeMixed = allTypeMixed + numbers;
	    }
	    if (Arrays.binarySearch(stringTypes, StringType.SPECIAL_CHARS) >= 0) {
	    	allTypeMixed = allTypeMixed + specialChars;
	    }
	    
	    boolean upperExist = false, lowerExist = false, specialExist = false, numberExist = false;
		
		StringBuffer randomString = new StringBuffer(length);
		Random random = new Random();

		// Generate first half expected string with random chars
		for (int i = 0; i < length/2; i++) {
			int randomIndex = random.nextInt(allTypeMixed.length());
			char randomChar = allTypeMixed.charAt(randomIndex);
			
			if (Arrays.binarySearch(stringTypes, StringType.UPPER) >= 0 && upperExist == false) {
				if (upperChars.indexOf(randomChar) > 0) {
					upperExist = true;
				}
			}
			if (Arrays.binarySearch(stringTypes, StringType.LOWER) >= 0 && lowerExist == false) {
				if (lowerChars.indexOf(randomChar) > 0) {
					lowerExist = true;
				}
			}
			if (Arrays.binarySearch(stringTypes, StringType.SPECIAL_CHARS) >= 0 && specialExist == false) {
				if (specialChars.indexOf(randomChar) > 0) {
					specialExist = true;
				}
			}
			if (Arrays.binarySearch(stringTypes, StringType.NUMBER) >= 0 && numberExist == false) {
				if (numbers.indexOf(randomChar) > 0) {
					numberExist = true;
				}
			}
			randomString.append(randomChar);
		}
		
		int currentLen = randomString.length();
		// Check if at least one character from each exists else generate
		if (Arrays.binarySearch(stringTypes, StringType.UPPER) >= 0 && upperExist == false) {
			randomString.append(upperChars.charAt(random.nextInt(upperChars.length())));
			currentLen++;
		}
		if (Arrays.binarySearch(stringTypes, StringType.LOWER) >= 0 && lowerExist == false) {
			randomString.append(lowerChars.charAt(random.nextInt(lowerChars.length())));
			currentLen++;
		}
		if (Arrays.binarySearch(stringTypes, StringType.NUMBER) >= 0 && numberExist == false) {
			randomString.append(numbers.charAt(random.nextInt(numbers.length())));
			currentLen++;
		}
		if (Arrays.binarySearch(stringTypes, StringType.SPECIAL_CHARS) >= 0 && specialExist == false) {
			randomString.append(specialChars.charAt(random.nextInt(specialChars.length())));
			currentLen++;
		}
		
		for (int i = currentLen; i < length; i++) {
			int randomIndex = random.nextInt(allTypeMixed.length());
			char randomChar = allTypeMixed.charAt(randomIndex);
			randomString.append(randomChar);
		}

		return randomString.toString();
	}

	/**
	 * String generation overloading randomString() function with random selection of upper, lower, number and special characters in the sting
	 */
	public static String randomString(int length) {
		Random random = new Random();
		return randomString(length, random.nextBoolean(), random.nextBoolean(), random.nextBoolean(), random.nextBoolean());
	}
	
	/**
	 * Generate random email id with abott.com domain
	 * @return
	 */
	public static String generateRandomEmail() {
		return randomString(5, false, true, false, false) + "." + randomString(4, false, true, false, false) + "@abbott.com";
	}
	
	/**
	 * Compare source list matches with target
	 * @param ignoreCase - ignore the case of the strings in the list
	 * @param mustMatch - if true, it will check if both lists are same; if false, will check both lists are different
	 * 
	 */
	public static boolean compareList(boolean mustMatch, List<String> sourceList, List<String> targetList, boolean ignoreCase, FrameworkLog log) {
		boolean success = true;
		
		if (sourceList.size() != targetList.size() && mustMatch) {
			log.error("Size of lists do not match; source list size: " + sourceList.size() + " Target list size: " + targetList.size());
			success = false;
		}
		if (ignoreCase) {
			sourceList.replaceAll(String::toLowerCase);
			targetList.replaceAll(String::toLowerCase);
		}
		
		List<String> tempList = new ArrayList<>(sourceList);
		tempList.removeAll(targetList);
		if (mustMatch) {
			if (tempList.size() > 0) {		
				success = false;
				log.error("Source list items which does not exists in target list are: " + tempList.toString());
			}
			tempList = new ArrayList<>(targetList);
			tempList.removeAll(sourceList);
			if (tempList.size() > 0) {		
				success = false;
				log.error("Target list items which does not exists in source list are: " + tempList.toString());
			}
		} else {
			if (!tempList.equals(sourceList)) {
				success = false;
				sourceList.retainAll(targetList);
				log.error("Items common in both list are: " + sourceList);
			}
		}
		
		return success;
	}
	
	/**
	 * Generate subsequent email id by adding number to the email address
	 * e.g. emailaddres -> user@abbott.com will be returned as user+1@abbott.com if accountIndex is 1
	 * @param accountIndex
	 * @return
	 */
	public static String generateValidEmail(int accountIndex) {
		String emailId = "";
		if(Boolean.parseBoolean(FrameworkProperties.getProperty("MANUAL_CUSTOMER_SETUP"))) {
			emailId = FrameworkProperties.getProperty("EMAIL_USER_ID");
		}else {
			emailId = EmailParser.getMFAEmailID();
		}
		int atCharIndex = emailId.indexOf("@");
		String domain = emailId.substring(atCharIndex);
		String userid = emailId.substring(0, atCharIndex);
		emailId = userid + "+" + String.valueOf(accountIndex) + domain; 
		return emailId;
	}
	
	/**
	 * Check if list is in expected sort order
	 */
	public static boolean isSorted(List<String> listToValidate, SortOrder order, FrameworkLog log) {
		boolean success = true; 
	    String prevValue = null;
	    
	    switch (order) {
	    	case ASCENDING:
	    		for (String value: listToValidate) {
	    	        if (prevValue != null && value.compareTo(prevValue) < 0) {
	    	        	log.error(value + " is not in expected order");
	    	        	success = false;
	    	        }
	    	        prevValue = value;
	    	    }
	    		break;
	    	case DESCENDING:
	    		for (String value: listToValidate) {
	    	        if (prevValue != null && value.compareTo(prevValue) > 0) {
	    	        	log.error(value + " is not in expected order");
	    	        	success = false;
	    	        }
	    	        prevValue = value;
	    	    }
	    		break;
	    }
	    
	    return success;
	}
	
	/** Compare two maps */
	public static boolean areMapsEqual(Map<String, Object> sourceMap, Map<String, Object> targetMap) {
	    if (sourceMap.size() != targetMap.size()) {
	        return false;
	    }
	    return sourceMap.entrySet().stream().allMatch(e -> e.getValue().equals(targetMap.get(e.getKey())));
	}
	
	/** check if input string in encoded format */
	public static boolean checkForEncode(String input) {
	    String pattern = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$";
	    Pattern r = Pattern.compile(pattern);
	    Matcher m = r.matcher(input);
	    return m.find();
	}
}