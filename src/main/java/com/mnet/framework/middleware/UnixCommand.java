package com.mnet.framework.middleware;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * Represents a standalone command to be run by a Unix shell.
 * @author Arya Biswas
 * @version Fall 2023
 */
@Getter(AccessLevel.PROTECTED)
public class UnixCommand {
	
	/**String representation of the command passed to the Unix shell.*/
	private String command;
	
	/**
	 * Creates a shell command to be run from the specified directory.
	 * @param shellCommand Command to be run by Unix shell.
	 * @param absoluteDirectory Absolute directory from which command is to be run. If null, runs from the user's home directory.
	 */
	public UnixCommand(String shellCommand, String absoluteDirectory) {
		this(shellCommand, absoluteDirectory, false);
	}
	
	/**
	 * Creates a shell command to be run from the specified directory with the option to run as root (sudo).
	 * @param shellCommand Command to be run by Unix shell.
	 * @param absoluteDirectory Absolute Unix directory from which command is to be run. If null, runs from the user's home directory.
	 * @param runAsRoot If true, runs the command as root (sudo) using the saved credentials.
	 */
	public UnixCommand(String shellCommand, String absoluteDirectory, boolean runAsRoot) {
		if (absoluteDirectory == null) {
			absoluteDirectory = "";
		} else if (absoluteDirectory.charAt(0) != '/') {
			absoluteDirectory = "/" + absoluteDirectory;
		}
		
		if (runAsRoot) {
			shellCommand = "echo " + UnixConnector.UNIX_PASSWORD + "|sudo -S " + shellCommand;
		}
		
		command = "cd " + absoluteDirectory + " ; " + shellCommand;
	}
	
	/**Returns a log-safe version of the command (passwords redacted)*/
	public String getLoggableCommand() {
		return command.replace(UnixConnector.UNIX_PASSWORD, "<UNIX_PASSWORD>");
	}
}
