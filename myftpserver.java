//Updated Project 2
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
//import com.google.common.collect.Multimap;
//import com.google.common.collect.ArrayListMultimap;
import java.util.concurrent.locks.ReentrantReadWriteLock;;

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
		try {
			terminateSocket = new ServerSocket(TPORT);
		} catch (Exception e) {
			System.out.println("Error: " + e);
		}
		
	    while (true) {
	    	try {
	    		terminatingClient = terminateSocket.accept();

	    		DataInputStream input=new DataInputStream(new BufferedInputStream(terminatingClient.getInputStream()));
	    		DataOutputStream output = new DataOutputStream(terminatingClient.getOutputStream());
	    	  	String command[]=null;
	    	  	String response = input.readUTF();
				command = response.split("\\s+");
				if(NewThread.runningCommands.contains(command[1])) {
					NewThread.runningCommands.remove(command[1]);
					output.writeUTF("Process terminated");
				}else {
					output.writeUTF("Process not found! or It might have been executed already! ");
				}
				terminatingClient.close();
	        } catch (Exception e) {
	            System.out.println("Error: " + e);}
//	            try {
//	            	terminateSocket.close();
//	            } catch (IOException e1) {
//					// TODO Auto-generated catch block
//	            	e1.printStackTrace();
//	            }
	    }
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
	//public static HashMap<String,String> FileManager=new HashMap<String,String>(); 
	//public static Multimap<Integer,String> FileManager = ArrayListMultimap.create();
	
	public static Map<String,List<String>> FileManager = new HashMap<>();
	public static ArrayList<String> runningCommands=new ArrayList<String>();
	private final static String FILE_SEP = System.getProperty("file.separator");
	private final static int BUFFER_SIZE = 1000;
	public static int clientCount=0;
	public static int commandID=0;
	
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
			//FileInputStream fileInStream = null;
			//FileOutputStream fileOutStream;
			File currentDir, changeDir, makeDir, delFile;
			int currentThreadCommandID;
			
			DataInputStream input=new DataInputStream(new BufferedInputStream(client.getInputStream()));
			DataOutputStream output = new DataOutputStream(client.getOutputStream());
			ObjectOutputStream objOutput = new ObjectOutputStream(client.getOutputStream());
			
			System.out.println("New client connected " +Thread.currentThread().getName());
			System.out.println("Connection estblished!");
			String presentWD = System.getProperty("user.dir");
			List<String> commands=new ArrayList<>();
		
			ReentrantReadWriteLock lock=new ReentrantReadWriteLock(true);
			ReentrantReadWriteLock.ReadLock getLock=lock.readLock();
			ReentrantReadWriteLock.WriteLock putLock=lock.writeLock();
			
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
			          if (command.length == 2 || command.length==3) {
			            File sendFile = new File(presentWD + FILE_SEP + command[1]);

			            output.writeUTF(String.valueOf(sendFile.exists()));
			            System.out.println("FILE---> " + sendFile.getTotalSpace());
			            String filePath=presentWD+FILE_SEP+command[1];
			            
			            if (sendFile.exists()) {
			            	currentThreadCommandID=++commandID;
			            	runningCommands.add(""+currentThreadCommandID);
			            	output.writeUTF(""+ currentThreadCommandID);
			            	if (!FileManager.containsKey(filePath)) {
			            		commands.add("get");
			            		FileManager.put(filePath,commands);
				        		get(command[1],presentWD,currentThreadCommandID);
				        		commands=FileManager.get(filePath);
				        		commands.remove("get");
				        		if(commands.isEmpty()) {
				        			FileManager.remove(filePath);
				        		}
				        		else {
				        			FileManager.replace(filePath, commands);
				        		}
				        	  }else {
				        		  commands=FileManager.get(filePath);
				        	  if (commands.contains("put")) {
				        		  try {
				        			  getLock.lock();
				        			  commands=FileManager.get(filePath);
				        			  commands.add("get");
				        			  FileManager.put(filePath,commands);
				        			  get(command[1],presentWD,currentThreadCommandID);
				        			  commands=FileManager.get(filePath);
				        			  commands.remove("get");
				        			  if(commands.isEmpty()) {
						        			FileManager.remove(filePath);
						        		}
						        		else {
						        			FileManager.replace(filePath, commands);
						        		}
				        		  }
				        		  catch(Exception e) {
				        			  System.out.println(e);
				        		  }
				        		  finally {
				        			  getLock.unlock();
				        		  }
				        		}
				        	  else {
				        		  commands=FileManager.get(filePath);
			        			  commands.add("get");
			        			  FileManager.put(filePath,commands);
			        			  get(command[1],presentWD,currentThreadCommandID);
			        			  commands=FileManager.get(filePath);
			        			  commands.remove("get");
			        			  if(commands.isEmpty()) {
					        			FileManager.remove(filePath);
					        		}
					        		else {
					        			FileManager.replace(filePath, commands);
					        		}
				        	  }
				        	}
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
			        	
			          if ((command.length == 2 || command.length==3)&& input.readUTF().equals("true")) {
			        	  //String filePath=input.readUTF()+FILE_SEP+command[1];
			        	  //File originalPutFile=new File(filePath);
			        	  //File putFile=new File(presentWD+FILE_SEP+command[1]);
			        	  currentThreadCommandID=++commandID;
			        	  runningCommands.add(""+currentThreadCommandID);
			        	  output.writeUTF(""+ currentThreadCommandID);
			        	  String putFilePath=presentWD+FILE_SEP+command[1];
			        	  //if(originalPutFile.exists()) {
			        		  
			        		  if(!FileManager.containsKey(putFilePath)) {
			        			  
			        			  commands.add("put");
			        			  FileManager.put(putFilePath,commands);
			        			  put(output,input,command[1],presentWD,currentThreadCommandID);
			        			  commands=FileManager.get(putFilePath);
			        			  commands.remove("put");
			        			  if(commands.isEmpty()) {
			        				  FileManager.remove(putFilePath);
			        			  }else {
			        				  FileManager.put(putFilePath,commands);
			        			  }  
				        	  }else {
				        		  commands=FileManager.get(putFilePath);
				        		  if(commands.contains("get") && !commands.contains("put")) {
					        		  commands.add("put");
					        		  FileManager.put(putFilePath,commands);
			
					        		  try {
					        			  putLock.lock();
					        			  put(output,input,command[1],presentWD,currentThreadCommandID);
					        			  commands=FileManager.get(putFilePath);
					        			  commands.remove("put");
					        			  if(commands.isEmpty()) {
							        			FileManager.remove(putFilePath);
							        		}
							        		else {
							        			FileManager.replace(putFilePath, commands);
							        		}
					        		  }
					        		  catch(Exception e) {
					        			  System.out.println(e);
					        		  }
					        		  finally {
					        			  putLock.unlock();
					        		  }  
					        	  }
				        		  else {
				        			  try {
					        			  putLock.lock();
					        			  commands=FileManager.get(putFilePath);
					        			  commands.add("put");
					        			  FileManager.put(putFilePath,commands);
					        			  put(output,input,command[1],presentWD,currentThreadCommandID);
					        			  commands=FileManager.get(putFilePath);
					        			  commands.remove("put");
					        			  if(commands.isEmpty()) {
							        			FileManager.remove(putFilePath);
							        		}
							        		else {
							        			FileManager.replace(putFilePath, commands);
							        		}
					        		  }
					        		  catch(Exception e) {
					        			  System.out.println(e);
					        		  }
					        		  finally {
					        			  putLock.unlock();
					        		  }
				        		  }
				        	  } 
//			        	  }else {
//			        		  output.writeUTF("File does not exist!");
//			        	  }		            
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
			          
			        case "terminate":
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
	
	void get(String fileName, String presentWD,int commandID) {
		try {
			System.out.println("get command received for "+fileName+"        commandID "+commandID);
			File sendFile = new File(presentWD + FILE_SEP + fileName);
			FileInputStream fileInStream = new FileInputStream(sendFile);
			
			DataInputStream input=new DataInputStream(new BufferedInputStream(client.getInputStream()));
			DataOutputStream output = new DataOutputStream(client.getOutputStream());
	        System.out.println(fileInStream.available() + " bytes!");
	        
	        byte[] buffer = new byte[(int) sendFile.length()];
	        System.out.println("Get buffer length: " + buffer.length);
	        DataInputStream dis = new DataInputStream(fileInStream);
	        //boolean done=false;
	        boolean terminated=false;
	        String getFilePath=input.readUTF();
	        int pointer=0;
	        if(NewThread.runningCommands.contains(""+commandID)) {
	        	output.writeLong(buffer.length); // send the size of the file
	        	int fileSize=buffer.length;
	        	int readChars;
//    			if(fileSize>1000) {
//    				readChars=1000;
//    			}else {
//    				readChars=1;
//    			}
//	        	if(NewThread.runningCommands.contains(""+commandID)) {
	        		for(;pointer<fileSize;pointer+=readChars) {
	        			if((fileSize-pointer)>1000) {
	        				readChars=1000;
	        			}else {
	        				readChars=1;
	        			}
	        			dis.read(buffer, pointer, readChars); // readfully is required to read all at once
	        			output.write(buffer, pointer, readChars); //
	        			//System.out.println(readChars+"     "+pointer);
	        			if(readChars%1000==0) {
		        			if(!NewThread.runningCommands.contains(""+commandID)) {
		        				pointer=fileSize;
		        				terminated=true;
		        				System.out.println("Command for commandID "+ commandID +" terminated");

		        				//dis.close();
		        				output.flush();
		        				break;
		        	        }
	        			}

	        		}
	        	}	
	        	else {
	        		terminated=true;
	        	}
	        if(terminated==true) {
	        	File sendedFile=new File(getFilePath);
	        	if(sendedFile.exists()) {
	        		sendedFile.delete();
	        	}
	        }else{
	        	runningCommands.remove(""+commandID);
				System.out.println("Command for commandID "+ commandID +" completed");
	        }
	        
//	        dis.readFully(buffer, 0, buffer.length); // readfully is required to read all at once
//	        output.write(buffer, 0, buffer.length); //
	        
//	        output.flush();
//	        System.out.println("File successfully downloaded!");
//	        dis.close();
		}
		catch(Exception e){
			e.printStackTrace(System.out);
		}
	}
	
	void put(DataOutputStream output,DataInputStream input, String fileName, String presentWD,int commandID) {
		boolean terminated=false;
//		int pointer=0,readChars;
		if(NewThread.runningCommands.contains(""+commandID)) {
			try {
				System.out.println("put command received for "+fileName+"        commandID "+commandID);
				//long size = input.readLong();
				long size=input.readLong();
		        int bytesRead = 0; 
		        byte[] putByteArray = new byte[BUFFER_SIZE];
		        FileOutputStream fileOutStream = new FileOutputStream(presentWD + FILE_SEP + fileName);
//		        String status;
//		        int putChars=(int) Math.min(putByteArray.length, size);
		        while (size > 0 && (bytesRead = input.read(putByteArray, 0, (int) Math.min(putByteArray.length, size))) != -1) {
		        	fileOutStream.write(putByteArray, 0, bytesRead);
		        	size -= bytesRead;

        			if(bytesRead%1000==0) {
	        			if(!NewThread.runningCommands.contains(""+commandID)) {
	        				//pointer=fileSize;
	        				terminated=true;
	        				System.out.println("Command for commandID "+ commandID +" terminated");

	        		        fileOutStream.close();
	        		        //output.close();
	        		        //input.close();
	        				break;
	        	        }
        			}
		        }		        
//		        fileOutStream.close();
		        //System.out.println("done");
		        //output.writeUTF("done");
			}
			catch(Exception e) {
				System.out.println(e);
			}	
		}else {
			terminated=true;
		}
		
		if(terminated==true) {
			File delFile=new File(presentWD + FILE_SEP + fileName);
			if(delFile.exists()) {
				delFile.delete();
			}
		}else {
			runningCommands.remove(""+commandID);
			System.out.println("Command for commandID "+ commandID +" completed");
		}
	}
}