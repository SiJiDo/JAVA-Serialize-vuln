package Serialize1;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Serialize2 {
    public static void main(String[] args) throws Exception{
        //要序列化的数据
        Object runtime = Class.forName("java.lang.Runtime").getMethod("getRuntime", new Class[]{}).invoke(null);
        Object evil = Class.forName("java.lang.Runtime").getMethod("exec", String.class).invoke(runtime, "notepad.exe");
        //Object evil = Runtime.getRuntime().exec("calc.exe");
        //序列化
        FileOutputStream fileOutputStream = new FileOutputStream("serialize2.txt");
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
        objectOutputStream.writeObject(evil);
        objectOutputStream.close();

        //反序列化
        FileInputStream fileInputStream = new FileInputStream("serialize2.txt");
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
        Object result = objectInputStream.readObject();
        objectInputStream.close();
        System.out.println(result);
    }
}
