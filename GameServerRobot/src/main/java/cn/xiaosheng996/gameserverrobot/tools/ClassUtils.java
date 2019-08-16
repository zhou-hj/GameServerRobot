package cn.xiaosheng996.gameserverrobot.tools;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import com.google.protobuf.Message;

/**
 * class操纵类
 */
public class ClassUtils {

    /**
     * 获取同一路径下所有子类或接口实现类
     */
    public static List<Class<?>> getAllAssignedClass(Class<?> cls) throws IOException, ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        for (Class<?> c : getClasses(cls)) {
            if (cls.isAssignableFrom(c) && !cls.equals(c)) {
                classes.add(c);
            }
        }
        return classes;
    }

    /**
     * 取得当前类路径下的所有类
     */
    private static List<Class<?>> getClasses(Class<?> cls) throws IOException,
        ClassNotFoundException {
        String pk = cls.getPackage().getName();
        String path = pk.replace('.', '/');
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        URL url = classloader.getResource(path);
        return getClasses(new File(url.getFile()), pk);
    }

    /**
     * 迭代查找类
     */
    private static List<Class<?>> getClasses(File dir, String pk) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        if (!dir.exists()) {
            return classes;
        }
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                classes.addAll(getClasses(f, pk + "." + f.getName()));
            }
            String name = f.getName();
            if (name.endsWith(".class")) {
                classes.add(Class.forName(pk + "." + name.substring(0, name.length() - 6)));
            }
        }
        return classes;
    }

    /**
     * 迭代组装协议
     */
    public static TreeMap<Integer, Class<?>> getClasses(String packageName, Class<?> clazz, String delimiter)
        throws ClassNotFoundException {
    	TreeMap<Integer, Class<?>> map = new TreeMap<>();
        String path = packageName.replace('.', '/');
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        URL url = classloader.getResource(path);
        for (Class<?> c : getClasses(new File(url.getFile()), packageName)) {
            if (Message.class.isAssignableFrom(c) && !Message.class.equals(c)) {
                if (c.getSimpleName().contains(delimiter)) {
                	String[] subDelimiter = delimiter.split("_");
                    int protocol = Integer.parseInt(
                        c.getSimpleName().substring(c.getSimpleName().indexOf(delimiter) + subDelimiter[0].length() + 1));//c.getSimpleName().indexOf(delimiter) + delimiter.length())
                    map.put(protocol, c);
                }
            }
        }
        return map;
    }
    
    /**
	 * 从类名中获取message_id
	 *
	 * @param clazz     类名
	 * @return
	 */
	public static int getMessageID(Class<?> clazz) {
        String name = clazz.getSimpleName();
        return Integer.parseInt(name.substring(name.lastIndexOf('_') + 1));
    }
	
    public static Method findMethod(Class<?> clazz, String methodName) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName))
                return method;
        }
        return null;
    }
}
