package Serialize1;

import java.lang.reflect.Method;

public class ExecTest {
    public static void main(String[] args) throws Exception{
        //Runtime.getRuntime().exec("notepad.exe");
        Object runtime = Class.forName("java.lang.Runtime").getMethod("getRuntime", new Class[]{}).invoke(null);
        System.out.println(runtime.getClass().getName());
        Class.forName("java.lang.Runtime").getMethod("exec",String.class).invoke(runtime,"notepad.exe");
    }
}
