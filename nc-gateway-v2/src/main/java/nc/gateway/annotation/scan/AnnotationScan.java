package nc.gateway.annotation.scan;


import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import nc.gateway.annotation.RPCMethod;
import nc.gateway.annotation.RPCService;
import nc.gateway.util.Constant;
import nc.gateway.util.E2s;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Slf4j
@Component
public class AnnotationScan {
    
    boolean initFlag = false;
    
    private Map<String, Object> cmdMap = new HashMap<String, Object>(); // 存放配置信息
    
    private Set<String> notCheckUri = new HashSet<>(); // 不需要登录的uri
    
    // 得到配置信息
    final public Map<String, Object> getCmdMap() {
        init();
        return this.cmdMap;
    }
    
    final public Set<String> getNotCheckUri() {
        init();
        return this.notCheckUri;
    }
    
    public static final String DEFAULT_RPCMETHOD_NAME = "RPCMethod";
    
    public static final String DEFAULT_NEADLOGIN = "neadlogin";
    
    public static final String DEFAULT_RPCCLASS_NAME = "RPCClass";
    
    public static final String DEFAULT_SERVER_REG_NAME = "scfname";
    
    public static final String DEFAULT_PACKAGE_NAME = "com.gateway.rpc";

    @PostConstruct
    // 扫描注解
    public void init() {
        if (!initFlag) {
            log.warn("---------------注解扫描开始------------------");
            Set<Class<?>> classSet = getClasses(DEFAULT_PACKAGE_NAME);
            for (Class<?> classN : classSet) {
                if (classN.isAnnotationPresent(RPCService.class)) {
                    // 带有服务注解的class，扫描所有接口
                    setCmdMap(classN);
                }
            }
            initFlag = true;
        }
    }
    
    // 设置service注解信息
    final private void setCmdMap(Class<?> classN) {
        Method[] methods = classN.getMethods();
        RPCService serverAnnotation = classN.getAnnotation(RPCService.class);
        String serverRegister = serverAnnotation.scfname();
        String cmd = "";
        String methodName = "";
        for (Method method : methods) {
            if (method.isAnnotationPresent(RPCMethod.class)) {
                cmd = method.getAnnotation(RPCMethod.class).cmd();
                boolean neadLogin = method.getAnnotation(RPCMethod.class).needLogin();
                if ("".equals(cmd) || 0 == cmd.length()) {
                    // 配置cmd注解读取cmd配置,没有配置cmd注解读取方法名
                    cmd = method.getName();
                }
                
                methodName = method.getName();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(DEFAULT_RPCCLASS_NAME, classN.getName());
                jsonObject.put(DEFAULT_SERVER_REG_NAME, serverRegister);
                jsonObject.put(DEFAULT_RPCMETHOD_NAME, methodName);
                jsonObject.put(DEFAULT_NEADLOGIN, neadLogin);
                if (!neadLogin) {
                    notCheckUri.add(cmd);
                }
                if (cmdMap.containsKey(cmd)) {
                    throw new RuntimeException("cmd 已经存在 : " + cmd);
                } else {
                    cmdMap.put(cmd, jsonObject);
                }
            }
        }
    }
    
    /**
     * 从包package中获取所有的Class
     *
     * @param pack 包名
     * @return 包下的所有类
     */
    public static Set<Class<?>> getClasses(String pack) {
        Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
        // 是否循环迭代
        boolean recursive = true;
        // 获取包的名字 并进行替换
        String packageName = pack;
        String packageDirName = packageName.replace(DEFAULT_POINT, DEFAULT_SLASH);
        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
            while (dirs.hasMoreElements()) {
                URL url = dirs.nextElement();
                String protocol = url.getProtocol();

                if (Constant.FILE_TYPE.equals(protocol)) {
                    System.err.println("file类型的扫描");
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    findAndAddClassesInPackageByFile(packageName, filePath, recursive, classes);
                } else if (Constant.JAR_TYPE.equals(protocol)) {
                    System.err.println("jar类型的扫描");
                    JarFile jar;
                    try {
                        jar = ((JarURLConnection)url.openConnection()).getJarFile();
                        Enumeration<JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            if (name.charAt(0) == DEFAULT_SLASH) {
                                name = name.substring(1);
                            }
                            if (name.startsWith(packageDirName)) {
                                int idx = name.lastIndexOf(DEFAULT_SLASH);
                                if (idx != -1) {
                                    packageName = name.substring(0, idx).replace(DEFAULT_SLASH, DEFAULT_POINT);
                                }
                                if ((idx != -1) || recursive) {
                                    if (name.endsWith(DEFAULT_SUFFIX_CLASS) && !entry.isDirectory()) {
                                        String className = name.substring(packageName.length() + 1, name.length() - 6);
                                        try {
                                            if (packageName.contains(DEFAULT_KEY_WORD) || packageName.contains(DEFAULT_KEY_WORD_SERVICE)) {
                                                classes.add(Class.forName(packageName + DEFAULT_POINT + className));
                                            }
                                        } catch (ClassNotFoundException e) {
                                            log.error("e=" + E2s.exception2String(e));
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("e=" + E2s.exception2String(e));
                    }
                }
            }
        } catch (Exception e) {
            log.error("e=" + E2s.exception2String(e));
        }
        
        return classes;
    }
    
    private static final String DEFAULT_SUFFIX_CLASS = ".class";
    
    private static final char DEFAULT_POINT = '.';
    
    private static final char DEFAULT_SLASH = '/';
    
    /**
     * 以文件的形式来获取包下的所有Class
     *
     * @param packageName
     * @param packagePath
     * @param recursive
     * @param classes
     */
    public static void findAndAddClassesInPackageByFile(String packageName, String packagePath, final boolean recursive, Set<Class<?>> classes) {
        File dir = new File(packagePath);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        File[] dirfiles = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return (recursive && file.isDirectory()) || (file.getName().endsWith(DEFAULT_SUFFIX_CLASS));
            }
        });
        for (File file : dirfiles) {
            if (file.isDirectory()) {
                findAndAddClassesInPackageByFile(packageName + DEFAULT_POINT + file.getName(), file.getAbsolutePath(), recursive, classes);
            } else {
                String className = file.getName().substring(0, file.getName().length() - 6);
                try {
                    if (packageName.contains(DEFAULT_PACKAGE_NAME)) {
                        classes.add(Thread.currentThread().getContextClassLoader().loadClass(packageName + DEFAULT_POINT + className));
                    }
                } catch (Exception e) {
                    log.error("e=" + E2s.exception2String(e));
                }
            }
        }
    }
    
    private static final String DEFAULT_KEY_WORD = "contract";
    
    private static final String DEFAULT_KEY_WORD_SERVICE = "Service";
}
