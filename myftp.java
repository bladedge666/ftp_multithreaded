import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * This is the FTP client for the project. On executing this file, you are given
 * a "myftp>" prompt where you can type the available commands.
 */
public class myftp {

  private final static String FILE_SEP = System.getProperty("file.separator");
  private final static int BUFFER_SIZE = 1024;
  private static final Scanner scan = new Scanner(System.in);

  public static void main(String[] args) throws IOException, ClassNotFoundException {

    int port = Integer.parseInt(args[0]);
    String machineName = args[1];
    Socket server = new Socket(machineName, port);
    DataOutputStream output = new DataOutputStream(server.getOutputStream());
    DataInputStream input = new DataInputStream(new BufferedInputStream(server.getInputStream()));
    ObjectInputStream objInput = new ObjectInputStream(server.getInputStream());
    ObjectOutputStream objOutput = new ObjectOutputStream(server.getOutputStream());

    FileOutputStream fileOutStream;
    FileInputStream fileInStream;

    boolean quit = false;
    while (!quit) {
      output.flush();
      // outStream.flush();
      System.out.print("myftp> ");
      String command = scan.nextLine();
      int cmdLength = command.split(" ").length;
      output.writeUTF(command);

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

      else if (cmdLength == 2 && command.startsWith("get")) {
        if (input.readUTF().equals("true")) {

          long size = input.readLong();
          int bytesRead = 0;
          byte[] buffer = new byte[BUFFER_SIZE];
          System.out.println("Size of get file: " + size);
          String getFile = command.substring(4);
          System.out.println(getFile);
          fileOutStream = new FileOutputStream(System.getProperty("user.dir") + FILE_SEP + getFile);
          while (size > 0 && (bytesRead = input.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
            fileOutStream.write(buffer, 0, bytesRead);
            size -= bytesRead;
          }

        } else {
          System.out.println("File does not exist!");
        }
      }

      else if (cmdLength == 2 && command.startsWith("put")) {
        File sendFile = new File(command.substring(4));
        output.writeUTF(String.valueOf(sendFile.exists()));
        if (sendFile.exists()) {
          fileInStream = new FileInputStream(sendFile.getAbsolutePath());
          System.out.println(fileInStream.available());
          byte[] putByteArray = new byte[(int) sendFile.length()];

          DataInputStream dis = new DataInputStream(fileInStream);

          output.writeLong(putByteArray.length); // send the size of the file
          dis.readFully(putByteArray, 0, putByteArray.length); // readfully is required to read all at once
          output.write(putByteArray, 0, putByteArray.length); //
          output.flush();
          System.out.println("File successfully uploaded on server!");
          dis.close();
        } else {
          System.out.println("File does not exist!");
        }

      }

      else {
        System.out.println(input.readUTF());
      }

    } // end while

  }

}
