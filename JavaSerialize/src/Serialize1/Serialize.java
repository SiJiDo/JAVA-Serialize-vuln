package Serialize1;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Serialize{
    public static void main(String[] args) throws Exception{
        //要序列化的数据
        String name = "sijidou";

        //序列化
        FileOutputStream fileOutputStream = new FileOutputStream("serialize1.txt");
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
        objectOutputStream.writeObject(name);
        objectOutputStream.close();

        //反序列化
        FileInputStream fileInputStream = new FileInputStream("serialize1.txt");
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
        Object result = objectInputStream.readObject();
        objectInputStream.close();
        System.out.println(result);
    }
}
