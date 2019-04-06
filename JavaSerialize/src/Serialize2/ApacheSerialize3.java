package Serialize2;

import java.io.FileInputStream;
import java.io.ObjectInputStream;

public class ApacheSerialize3 {
    public static void main(String[] args) throws Exception{
        FileInputStream fileInputStream = new FileInputStream("1.txt");
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
        Object result = objectInputStream.readObject();
        objectInputStream.close();
        System.out.println(result);
    }
}
