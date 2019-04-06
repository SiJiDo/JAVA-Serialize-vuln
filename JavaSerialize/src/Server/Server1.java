package Server;

import java.io.BufferedInputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server1 {
    public static void main(String[] args) throws Exception{
        ServerSocket serverSocket = new ServerSocket(7777);
        while(true){
            Socket socket = serverSocket.accept();
            getserialize(socket);
        }
    }

    public static void getserialize(final Socket socket){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ObjectInputStream objectInputStream = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
                    Object object = objectInputStream.readObject();
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
    }
}
