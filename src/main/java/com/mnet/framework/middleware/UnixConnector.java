package com.mnet.framework.middleware;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.reporting.FrameworkLog;
import com.mnet.framework.utilities.CommonUtils;
import com.mnet.framework.utilities.Timeout;

/**
 * Handles SSH connections and shell commands for virtual machine.
 * @version Spring 2023
 * @author Arya Biswas
 */
public class UnixConnector {
	
	private FrameworkLog log;
	private Session session;
	private ChannelShell shell;
	private ChannelSftp fileTransfer;
	private PipedInputStream input;
	private PipedOutputStream output;
	private BufferedWriter shellCommand;
	
	private List<UnixCommand> queuedCommands;
	
	private String hostName, user, userPwd;
	
	private static final String UNIX_HOST = FrameworkProperties.getProperty("UNIX_HOST");
	private static final String UNIX_USERNAME = FrameworkProperties.getProperty("VPN2_USERNAME");
	
	/**Maximum session timeout (in ms)*/
	private static final String UNIX_SESSION_TIMEOUT = FrameworkProperties.getProperty("UNIX_SESSION_TIMEOUT");
	/**Maximum command timeout (in ms)*/
	private static final String UNIX_COMMAND_TIMEOUT = FrameworkProperties.getProperty("UNIX_COMMAND_TIMEOUT");
	
	private static final long TIMEOUT_SESSION_L = Long.parseLong(UNIX_SESSION_TIMEOUT);
	private static final long TIMEOUT_COMMAND_L = Long.parseLong(UNIX_COMMAND_TIMEOUT);
	/**Thread sleep interval while waiting for command completion*/
	private static final long TIMEOUT_SLEEP_INTERVAL = 1000L; 
	
	private static final int TIMEOUT_SESSION_I = Integer.parseInt(UNIX_SESSION_TIMEOUT);
	private static final int UNIX_PORT = Integer.parseInt(FrameworkProperties.getProperty("UNIX_PORT"));
	
	protected static final String UNIX_PASSWORD = FrameworkProperties.getProperty("VPN2_PASSWORD");
	
	public UnixConnector(FrameworkLog frameworkLog) {
		this(frameworkLog, null, null, null);
	}
	
	public UnixConnector(FrameworkLog frameworkLog, String host, String userName, String password) {
		log = frameworkLog;
		this.hostName = host == null ? UNIX_HOST : host;
		this.user = userName == null ? UNIX_USERNAME : userName;
		this.userPwd = password == null ? UNIX_PASSWORD : password;
		
		queuedCommands = new ArrayList<UnixCommand>();
		
		openConnection();
	}
	
	/**
	 * Disconnects SSH connection. 
	 **/
	public void closeConnection() {
		session.disconnect();
	}
	
	/**
	 * Queues any number of commands to be run in the UNIX shell instance.
	 * To execute the commands in sequence, invoke executeShellCommands() after all desired commands are queued.
	 */
	public void queueCommands(UnixCommand... commands) {
		for (UnixCommand command : commands) {			
			queuedCommands.add(command);
		}
	}

	/**
	 * Sequentially executes all queued UNIX commands in current instance of shell.
	 * After execution, the queued commands are purged and the shell state resets.
	 **/
	public void executeShellCommands() {
		String mergedCommand = getMergedCommand();
		String loggableCommand = mergedCommand.replace(UNIX_PASSWORD, "<userPwd>");
		
		openShell();
		
		try {
			shellCommand.write(mergedCommand + " ; exit \n");
			shellCommand.flush();
			log.info("Running shell commands:");
			log.info(loggableCommand);
		} catch (IOException ioe) {
			log.error("Failed to run commands in shell: " + loggableCommand);
			log.printStackTrace(ioe);
			throw new RuntimeException(ioe);
		}
		
		long startTime = System.nanoTime();
		
		while(shell.isConnected()) {
			if (CommonUtils.millisFromTime(startTime) > TIMEOUT_COMMAND_L) {
				break;
			}
		
			Timeout.waitForTimeout(log, TIMEOUT_SLEEP_INTERVAL);
		}
		
		closeShell(); // required for shell commands to resolve
	}
	
	/**
	 * Copies file from local machine to remote via SFTP.
	 * @param localSource Source path (absolute) of file on local machine.
	 * @param remoteDest Destination path (absolute) of file on remote machine.
	 */
	public void copyFileToRemote(String localSource, String remoteDest) {
		String unixRemote = FilenameUtils.separatorsToUnix(remoteDest);
		
		openSFTP();
	
        try {
			log.info("Transferring file from local: " + localSource + " to remote: " + unixRemote);
			fileTransfer.put(localSource, unixRemote);
		} catch (SftpException sfe) {
			log.error("Failed to transfer file from local: " + localSource + " to remote: " + remoteDest);
			log.printStackTrace(sfe);
			throw new RuntimeException(sfe);
		}
		
		Timeout.waitForTimeout(log, TIMEOUT_SESSION_L);
		fileTransfer.disconnect();
	}
	
	/**
	 * Copies file from remote to local machine via SFTP.
	 * @param remoteSource Source path (absolute) of file on remote machine.
	 * @param localDest Destination path (absolute) of file on local machine.
	 */
	public void copyFileToLocal(String remoteSource, String localDest) {
		String unixRemote = FilenameUtils.separatorsToUnix(remoteSource);
		String unixRemotePath = unixRemote.substring(0, unixRemote.lastIndexOf("/"));
		
		openSFTP();
		
		try {
			log.info("Transferring file from remote: " + unixRemote + " to local: " + localDest);
			fileTransfer.cd(unixRemotePath);
			fileTransfer.get(unixRemote, localDest);
		} catch (SftpException sfe) {
			log.error("Failed to transfer file from remote: " + remoteSource + " to local: " + localDest);
			log.printStackTrace(sfe);
			throw new RuntimeException(sfe);
		}
		
		Timeout.waitForTimeout(log, TIMEOUT_SESSION_L);
		fileTransfer.disconnect();
	}
	
	/** Check file existence */
	public boolean fileExists(String filePath) {
		openSFTP();
		
		try {
			fileTransfer.lstat(filePath);
			return true;
		}catch (SftpException sfe) {
			log.error("Failed to verify existence of " + filePath);
			log.printStackTrace(sfe);
			throw new RuntimeException(sfe);
		}
	}
	
	/**
	 * Helper functions
	 */
	
	private void openConnection() {
		JSch jsch = new JSch();
		
		try {
			// jsch.setKnownHosts("~/.ssh/known_hosts");
			session = jsch.getSession(user, hostName, UNIX_PORT);
		} catch (JSchException jse) {
			log.error("Failed to obtain Unix session for: " + hostName + ":" + UNIX_PORT + " | " + user);
			log.printStackTrace(jse);
			throw new RuntimeException(jse);
		}
		
		Properties config = new Properties();
		config.put("StrictHostKeyChecking", "no");
		
		session.setPassword(userPwd);
		session.setConfig(config);
		
		try {
			session.setTimeout(TIMEOUT_SESSION_I);
			session.connect();
		} catch (JSchException jse) {
			log.error("Failed to connect to Unix session: " + hostName + ":" + UNIX_PORT + " | " + user);
			log.printStackTrace(jse);
			throw new RuntimeException(jse);
		}
	}
	
	/**
	 * Starts new terminal shell instance. 
	 **/
	private void openShell() {
		try {
			shell = (ChannelShell)session.openChannel("shell");
		} catch (JSchException jse) {
			log.error("Failed to open shell channel");
			log.printStackTrace(jse);
			throw new RuntimeException(jse);
		}
				
		input = new PipedInputStream();
		try {
			output = new PipedOutputStream(input);
		} catch (IOException ioe) {
			log.error("Failed to create output stream for shell");
			log.printStackTrace(ioe);
			throw new RuntimeException(ioe);
		}
		
		shell.setPty(false);
		shell.setInputStream(input);
		shell.setOutputStream(output);
		
		try {
			shell.connect();
		} catch (JSchException jse) {
			log.error("Failed to connect to shell");
			log.printStackTrace(jse);
			throw new RuntimeException(jse);
		}
		
		shellCommand = new BufferedWriter(new OutputStreamWriter(output));
	}
	
	private void openSFTP() {
		try {
			fileTransfer = (ChannelSftp)session.openChannel("sftp");
			fileTransfer.connect();
		} catch (JSchException jse) {
			log.error("Failed to connect to SFTP channel");
			log.printStackTrace(jse);
			throw new RuntimeException(jse);
		}
		
	}
	
	/**
	 * Closes active shell channel instance and allows result of shell operation to resolve. 
	 **/
	private void closeShell() {
		shell.disconnect();
		
		try {
			shellCommand.close();
			input.close();
			output.close();
		} catch (IOException ioe) {
			log.warn("Failed to close shell I/O");
			log.printStackTrace(ioe);
		}
	}
	
	/**
	 * Concatenates all queued commands with UNIX command separator (;)
	 * As JSch shells do not retain memory of prior commands, it is necessary to combine dependent commands in a single string.
	 */
	private String getMergedCommand() {
		String mergedCommand = "";
		
		for (UnixCommand command : queuedCommands) {
			mergedCommand += command.getCommand() + " ; ";
		}
		
		return StringUtils.removeEnd(mergedCommand, " ; ");
	}
	
}
