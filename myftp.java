//updated Project 2

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * This is the FTP client for the project. On executing this file, you are given
 * a "myftp>" prompt where you can type the available commands.
 */
public class myftp {

	private final static String FILE_SEP = System.getProperty("file.separator");
	private final static int BUFFER_SIZE = 1000;
	private static final Scanner scan = new Scanner(System.in);
	public static String commandID;
	public static ArrayList<String> runningGetCommands = new ArrayList<String>(); // keeps track of running get commands,
																																								// helps to terminate get commands

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		String machineName = null;
		Socket server = null;
		int tport = -1;
		int nport = -1;

		try {
			machineName = args[0];
			nport = Integer.parseInt(args[1]);
			tport = Integer.parseInt(args[2]);
			server = new Socket(machineName, nport);
		} catch (Exception e) {
			System.out.println("usage: java myftp <MACHINE_NAME> <NPORT> <TPORT>");
			System.exit(1);
		}
		String splitCommand[] = null;
		DataOutputStream output = new DataOutputStream(server.getOutputStream());
		DataInputStream input = new DataInputStream(new BufferedInputStream(server.getInputStream()));
		ObjectInputStream objInput = new ObjectInputStream(server.getInputStream());

		boolean quit = false;
		while (!quit) {
			output.flush();
			System.out.print("myftp> ");
			String command = scan.nextLine();
			output.writeUTF(command);
			int cmdLength = command.split(" ").length;
			splitCommand = command.split("\\s+");

			if (command.equals("ls")) {
				String listOfFiles[] = (String[]) objInput.readObject();
				for (int i = 0; i < listOfFiles.length; i++) {
					System.out.printf("%s \t", listOfFiles[i]);
				}
				System.out.println();

			}

			else if (command.equals("quit")) {
				server.close();
				quit = true;
			}

			else if ((cmdLength == 2 || cmdLength == 3) && splitCommand[0].equals("get")) {
				if (command.endsWith("&")) {
					new getAndPutWithAmpersand(server, splitCommand[0], input, splitCommand[1], output); // creates new thread
				} else {
					get(input, splitCommand[1], output);
				}
				try {
					Thread.sleep(500); // sleeps in order to get commandID before ">myftp"
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			else if ((cmdLength == 2 || cmdLength == 3) && splitCommand[0].equals("put")) {
				if (command.endsWith("&")) {
					new getAndPutWithAmpersand(server, splitCommand[0], input, splitCommand[1], output); // creates new thread
				} else {
					put(output, input, splitCommand[1]);
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else if (cmdLength == 2 && splitCommand[0].equals("terminate")) {
				terminate(splitCommand[1], machineName, tport);
			} else {
				System.out.println(input.readUTF());
			}

		} // end while

	}

	static void put(DataOutputStream output, DataInputStream input, String command) {
		try {

			File sendFile = new File(command);
			output.writeUTF(String.valueOf(sendFile.exists()));
			commandID = input.readUTF();
			System.out.println("CommandID " + commandID);
			if (sendFile.exists()) {
				FileInputStream fileInStream = new FileInputStream(sendFile.getAbsolutePath());
				output.writeLong(sendFile.length());
				byte[] putByteArray = new byte[(int) sendFile.length()];

				DataInputStream dis = new DataInputStream(fileInStream);
				boolean terminated = false;
				String status;
				int pointer = 0, readChars;
				int fileSize = putByteArray.length;

				for (; pointer < fileSize; pointer += readChars) {
					if ((fileSize - pointer) > 1000) {
						readChars = 1000;
					} else {
						readChars = 1;
					}

					dis.read(putByteArray, pointer, readChars);
					output.write(putByteArray, pointer, readChars);

					if (readChars % 1000 == 0) {
						status = input.readUTF();
						if (status.equals("terminate")) {
							break;
						}
					}
				}

				fileInStream.close();
				dis.close();
			} else {
				System.out.println("File does not exist!");
			}
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	static void get(DataInputStream input, String fileName, DataOutputStream output) {
		try {
			if (input.readUTF().equals("true")) {
				commandID = input.readUTF();
				runningGetCommands.add(commandID);
				System.out.println("Command ID " + commandID);
				output.writeUTF(System.getProperty("user.dir") + FILE_SEP + fileName);
				long size = input.readLong();
				boolean terminate = false;
				int bytesRead = 0;
				byte[] buffer = new byte[BUFFER_SIZE];

				FileOutputStream fileOutStream = new FileOutputStream(System.getProperty("user.dir") + FILE_SEP + fileName);
				while (size > 0 && (bytesRead = input.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
					fileOutStream.write(buffer, 0, bytesRead);
					size -= bytesRead;

					if (bytesRead % 1000 == 0) {
						if (!runningGetCommands.contains(commandID)) { // searches for commandID in runningGetCommands
							terminate = true;
							output.writeUTF("terminate");
							break;
						} else {
							output.writeUTF("continue");
						}
					}
				}
				fileOutStream.close();
				if (terminate == true) {
					File getFile = new File(System.getProperty("user.dir") + FILE_SEP + fileName);
					if (getFile.exists()) {
						getFile.delete();
					}
				} else {
					myftp.runningGetCommands.remove(commandID);
				}
			}
			Thread.sleep(1000);
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		}
	}

	// method for connecting to terminal port
	static void terminate(String commandID, String machineName, int tport) throws UnknownHostException, IOException {
		Socket server = new Socket(machineName, tport);
		DataOutputStream output = new DataOutputStream(server.getOutputStream());
		DataInputStream input = new DataInputStream(new BufferedInputStream(server.getInputStream()));
		output.writeUTF(commandID);
		if (myftp.runningGetCommands.contains(commandID)) {
			myftp.runningGetCommands.remove(commandID);
		}
		System.out.println(input.readUTF());
		server.close();
	}
}

// creates new thread for command with &
class getAndPutWithAmpersand implements Runnable {
	Thread getPutThread;
	String command, fileName;
	DataInputStream input;
	DataOutputStream output;

	getAndPutWithAmpersand(Socket server, String command, DataInputStream input, String fileName,
			DataOutputStream output) {
		this.input = input;
		this.output = output;
		this.command = command;
		this.fileName = fileName;
		getPutThread = new Thread(this);
		getPutThread.start();
	}

	public void run() {
		if (command.equals("get")) {
			myftp.get(input, fileName, output);
		} else if (command.equals("put")) {
			myftp.put(output, input, fileName);
		}
	}
}