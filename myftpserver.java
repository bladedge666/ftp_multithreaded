import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;;

/**
 * This is the FTP server for the project. The main method works as the driver
 * and calls the start method which boots up the server and allows it to serve
 * requests from the client.
 */
public class myftpserver{

  public static final int NPORT = 4445;
  public static final int TPORT = 5555;

  //public static ServerSocket serverSocket=null;
  //public static Socket client=null;

  // might remove this later in favor of command line arguments
  private static final Scanner scan = new Scanner(System.in);

  public static void main(String args[]) throws IOException {
	  
	  new NormalPortThread(NPORT);
	  new TerminatePortThread(TPORT);

  } // end main


  public void closeResources() throws IOException {
 //   serverSocket.close();
    scan.close();
  }

}

class TerminatePortThread implements Runnable{
	public static ServerSocket terminateSocket=null;
	public static Socket terminatingClient=null;
	Thread terminateThread;
	public static int TPORT;
	
	TerminatePortThread(int tport){
		terminateThread=new Thread(this);
		System.out.println("Terminate port thread created...");
		TPORT=tport;
		terminateThread.start();
	}
	
	public void run() {
		//add logic to terminate command 
	}
}

class NormalPortThread implements Runnable{
	public static ServerSocket serverSocket=null;
	public static Socket client=null;
	Thread normalThread;
	public static int NPORT;
	  
	NormalPortThread(int nport) {
		normalThread=new Thread(this);
		System.out.println("Normal port thread created...");
		NPORT=nport;
		normalThread.start();
	}
	
	public void run() {
		try {
			serverSocket = new ServerSocket(NPORT);
		} catch (Exception e) {
			System.out.println("Error: " + e);
		}
			
	      while (true) {
	          try {
	              client = serverSocket.accept();
	          } catch (Exception e) {
	              System.out.println("Error: " + e);
	              try {
					serverSocket.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
	          }
	          // new thread for a client
	  		new NewThread(client);
	      }
	  }
}

class NewThread implements Runnable{
	private final static String FILE_SEP = System.getProperty("file.separator");
	private final static int BUFFER_SIZE = 1024;
	public static int clientCount=0;
	
	//ObjectInputStream objInput = new ObjectInputStream(client.getInputStream());
	
	//String name; // name of thread
	Thread t;
	Socket client;
	NewThread(Socket socket) {
		clientCount++;
		t = new Thread(this,"client "+clientCount);
		client=socket;
	
		System.out.println("New thread: " + t);
		t.start(); // Start the thread
	}
	
	public void run() {
		
		try {
			FileInputStream fileInStream;
			FileOutputStream fileOutStream;
			File currentDir, changeDir, makeDir, delFile;
			
			DataInputStream input=new DataInputStream(new BufferedInputStream(client.getInputStream()));
			DataOutputStream output = new DataOutputStream(client.getOutputStream());
			ObjectOutputStream objOutput = new ObjectOutputStream(client.getOutputStream());
			
			System.out.println("New client connected " +Thread.currentThread().getName());
			System.out.println("Connection estblished!");
			String presentWD = System.getProperty("user.dir");
			 
			      // OutputStream outStream = client.getOutputStream();
			      // InputStream inStream = client.getInputStream();
			String command[] = null;
			System.out.println("Waiting for a command...");
			
			do {
				String response = input.readUTF();
				command = response.split("\\s+");
			    switch (command[0]) {
			        case "get":
			          // check if a filename or path is provided after the get command
			          if (command.length == 2) {
			            File sendFile = new File(System.getProperty("user.dir") + FILE_SEP + command[1]);

			            output.writeUTF(String.valueOf(sendFile.exists()));
			            System.out.println("FILE---> " + sendFile.getTotalSpace());

			            if (sendFile.exists()) {
			              fileInStream = new FileInputStream(sendFile);

			              System.out.println(fileInStream.available() + " bytes!");
			              byte[] buffer = new byte[(int) sendFile.length()];
			              System.out.println("Get buffer length: " + buffer.length);
			              DataInputStream dis = new DataInputStream(fileInStream);

			              output.writeLong(buffer.length); // send the size of the file
			              dis.readFully(buffer, 0, buffer.length); // readfully is required to read all at once
			              output.write(buffer, 0, buffer.length); //
			              System.out.println("Transferring file...");
			              output.flush();
			              System.out.println("File successfully downloaded!");
			              dis.close();
			            } else {
			              output.writeUTF("File " + sendFile.getAbsolutePath() + " not found.");
			            }
			          }

			          // If no file is specified as the argument
			          else {
			            output.writeUTF("You must specify a path after a get command.");
			          }

			          break;

			        case "put":
			          if (command.length == 2 && input.readUTF().equals("true")) {
			            long size = input.readLong();
			            int bytesRead = 0; 
			            byte[] putByteArray = new byte[BUFFER_SIZE];
			            fileOutStream = new FileOutputStream(System.getProperty("user.dir") + FILE_SEP + command[1]);

			            while (size > 0
			                && (bytesRead = input.read(putByteArray, 0, (int) Math.min(putByteArray.length, size))) != -1) {
			              fileOutStream.write(putByteArray, 0, bytesRead);
			              size -= bytesRead;
			            }
			          }

			          else {
			            output.writeUTF("You must specify a path after a put command.");
			          }
			          break;

			        case "delete":
			          if (command.length >= 2) {
			            delFile = new File(presentWD + FILE_SEP + command[1]);
			            //System.out.println(System.getProperty("user.dir") + FILE_SEP + command[1]);
			            if (delFile.exists()) {
			              delFile.delete();
			              output.writeUTF(command[1] + " successfully deleted.");
			            } else {
			              output.writeUTF("File does not exist!");
			            }
			          }

			          else {
			            output.writeUTF("You must specify a file after a delete command.");
			          }
			          break;

			        case "ls":
			          //File curDir = new File(System.getProperty("user.dir"));
			          currentDir = new File(presentWD);
			          objOutput.writeObject(currentDir.list());
			          //objOutput.flush();
			          break;

			        // For windows system
			        case "cd..":
			          //currentDir = new File(System.getProperty("user.dir"));
			          currentDir = new File(presentWD);
			          //System.setProperty("user.dir", currentDir.getAbsoluteFile().getParent());
			          
			          if(currentDir.getAbsoluteFile().getParent()!=null) {
			        	  presentWD = currentDir.getAbsoluteFile().getParent();
				          System.out.println(presentWD);
				          output.writeUTF("Directory changed to " + presentWD);
			          }
			          else {
			        	  output.writeUTF("No parent directory exists!");
			          }
			          
			          break;

			        case "cd":

			          if (command.length <= 1) {
			            output.writeUTF("You must specify a path after a cd command.");
			            break;
			          }

			          if (command[1].equals("..")) {

			        	  currentDir = new File(presentWD);
				          //System.setProperty("user.dir", currentDir.getAbsoluteFile().getParent());
				          if(currentDir.getAbsoluteFile().getParent()!=null) {
				        	  presentWD = currentDir.getAbsoluteFile().getParent();
					          System.out.println(presentWD);
					          output.writeUTF("Directory changed to " + presentWD);
				          }
				          else {
				        	  output.writeUTF("No parent directory exists!");
				          }

			          } // end if checking ".."

			          // when the second param is anything other than ".."
			          else {
			            changeDir = new File(presentWD + FILE_SEP + command[1]);

			            if (changeDir.exists()) {
			              //System.setProperty("user.dir", changeDir.getAbsoluteFile().getPath());			              
			            	presentWD = changeDir.getAbsolutePath();
			            	output.writeUTF("Directory changed to " + presentWD);
			            } else {
			            	output.writeUTF("No such directory exists!");
			            }
			          }

			          break;

			        case "mkdir":
			          if (command.length == 2) {
			            makeDir = new File(presentWD + FILE_SEP + command[1]);
			            makeDir.mkdir();
			            output.writeUTF("New directory named " + command[1] + " created.");
			          }

			          else {
			            output.writeUTF("You must specify a path after a get command.");
			          }
			          break;

			        case "pwd":
			          //output.writeUTF("Remote working directory: " + System.getProperty("user.dir"));
			          output.writeUTF(presentWD);
			          break;

			        case "quit":
			          client.close();
			          break;

			        default:
			          System.out.println("Invalid command!");
			          output.writeUTF("Invalid Command!");
			          break;
			        }

			      } while (!command[0].equals("quit"));
			
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}		
}

