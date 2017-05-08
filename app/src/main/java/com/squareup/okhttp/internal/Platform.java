/*     */ package com.squareup.okhttp.internal;
/*     */ 
/*     */ import android.util.Log;
/*     */ import com.squareup.okhttp.Protocol;
/*     */ import com.squareup.okhttp.internal.tls.AndroidTrustRootIndex;
/*     */ import com.squareup.okhttp.internal.tls.RealTrustRootIndex;
/*     */ import com.squareup.okhttp.internal.tls.TrustRootIndex;
/*     */ import java.io.IOException;
/*     */ import java.io.PrintStream;
/*     */ import java.lang.reflect.Field;
/*     */ import java.lang.reflect.InvocationHandler;
/*     */ import java.lang.reflect.InvocationTargetException;
/*     */ import java.lang.reflect.Method;
/*     */ import java.lang.reflect.Proxy;
/*     */ import java.net.InetSocketAddress;
/*     */ import java.net.Socket;
/*     */ import java.net.SocketException;
/*     */ import java.util.ArrayList;
/*     */ import java.util.List;
/*     */ import java.util.logging.Level;
/*     */ import java.util.logging.Logger;
/*     */ import javax.net.ssl.SSLSocket;
/*     */ import javax.net.ssl.SSLSocketFactory;
/*     */ import javax.net.ssl.X509TrustManager;
/*     */ import okio.Buffer;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class Platform
/*     */ {
/*  67 */   private static final Platform PLATFORM = ;
/*     */   
/*     */   public static Platform get() {
/*  70 */     return PLATFORM;
/*     */   }
/*     */   
/*     */   public String getPrefix()
/*     */   {
/*  75 */     return "OkHttp";
/*     */   }
/*     */   
/*     */   public void logW(String warning) {
/*  79 */     System.out.println(warning);
/*     */   }
/*     */   
/*     */   public void tagSocket(Socket socket) throws SocketException
/*     */   {}
/*     */   
/*     */   public void untagSocket(Socket socket) throws SocketException
/*     */   {}
/*     */   
/*     */   public X509TrustManager trustManager(SSLSocketFactory sslSocketFactory) {
/*  89 */     return null;
/*     */   }
/*     */   
/*     */   public TrustRootIndex trustRootIndex(X509TrustManager trustManager) {
/*  93 */     return new RealTrustRootIndex(trustManager.getAcceptedIssuers());
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void configureTlsExtensions(SSLSocket sslSocket, String hostname, List<Protocol> protocols) {}
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void afterHandshake(SSLSocket sslSocket) {}
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public String getSelectedProtocol(SSLSocket socket)
/*     */   {
/* 115 */     return null;
/*     */   }
/*     */   
/*     */   public void connectSocket(Socket socket, InetSocketAddress address, int connectTimeout) throws IOException
/*     */   {
/* 120 */     socket.connect(address, connectTimeout);
/*     */   }
/*     */   
/*     */   public void log(String message) {
/* 124 */     System.out.println(message);
/*     */   }
/*     */   
/*     */   private static Platform findPlatform()
/*     */   {
/*     */     try
/*     */     {
/*     */       Class<?> sslParametersClass;
/*     */       try {
/* 133 */         sslParametersClass = Class.forName("com.android.org.conscrypt.SSLParametersImpl");
/*     */       } catch (ClassNotFoundException e) {
/*     */         Class<?> sslParametersClass;
/* 136 */         sslParametersClass = Class.forName("org.apache.harmony.xnet.provider.jsse.SSLParametersImpl");
/*     */       }
/*     */       
/*     */ 
/* 140 */       OptionalMethod<Socket> setUseSessionTickets = new OptionalMethod(null, "setUseSessionTickets", new Class[] { Boolean.TYPE });
/*     */       
/* 142 */       OptionalMethod<Socket> setHostname = new OptionalMethod(null, "setHostname", new Class[] { String.class });
/*     */       
/* 144 */       Method trafficStatsTagSocket = null;
/* 145 */       Method trafficStatsUntagSocket = null;
/* 146 */       OptionalMethod<Socket> getAlpnSelectedProtocol = null;
/* 147 */       OptionalMethod<Socket> setAlpnProtocols = null;
/*     */       
/*     */       try
/*     */       {
/* 151 */         Class<?> trafficStats = Class.forName("android.net.TrafficStats");
/* 152 */         trafficStatsTagSocket = trafficStats.getMethod("tagSocket", new Class[] { Socket.class });
/* 153 */         trafficStatsUntagSocket = trafficStats.getMethod("untagSocket", new Class[] { Socket.class });
/*     */         
/*     */         try
/*     */         {
/* 157 */           Class.forName("android.net.Network");
/* 158 */           getAlpnSelectedProtocol = new OptionalMethod(byte[].class, "getAlpnSelectedProtocol", new Class[0]);
/* 159 */           setAlpnProtocols = new OptionalMethod(null, "setAlpnProtocols", new Class[] { byte[].class });
/*     */         }
/*     */         catch (ClassNotFoundException localClassNotFoundException1) {}
/*     */       }
/*     */       catch (ClassNotFoundException|NoSuchMethodException localClassNotFoundException2) {}
/*     */       
/* 165 */       return new Android(sslParametersClass, setUseSessionTickets, setHostname, trafficStatsTagSocket, trafficStatsUntagSocket, getAlpnSelectedProtocol, setAlpnProtocols);
/*     */ 
/*     */     }
/*     */     catch (ClassNotFoundException localClassNotFoundException3)
/*     */     {
/*     */ 
/*     */       try
/*     */       {
/*     */ 
/* 174 */         Class<?> sslContextClass = Class.forName("sun.security.ssl.SSLContextImpl");
/*     */         
/*     */         try
/*     */         {
/* 178 */           String negoClassName = "org.eclipse.jetty.alpn.ALPN";
/* 179 */           Class<?> negoClass = Class.forName(negoClassName);
/* 180 */           Class<?> providerClass = Class.forName(negoClassName + "$Provider");
/* 181 */           Class<?> clientProviderClass = Class.forName(negoClassName + "$ClientProvider");
/* 182 */           Class<?> serverProviderClass = Class.forName(negoClassName + "$ServerProvider");
/* 183 */           Method putMethod = negoClass.getMethod("put", new Class[] { SSLSocket.class, providerClass });
/* 184 */           Method getMethod = negoClass.getMethod("get", new Class[] { SSLSocket.class });
/* 185 */           Method removeMethod = negoClass.getMethod("remove", new Class[] { SSLSocket.class });
/* 186 */           return new JdkWithJettyBootPlatform(sslContextClass, putMethod, getMethod, removeMethod, clientProviderClass, serverProviderClass);
/*     */ 
/*     */         }
/*     */         catch (ClassNotFoundException|NoSuchMethodException localClassNotFoundException4)
/*     */         {
/* 191 */           return new JdkPlatform(sslContextClass);
/*     */         }
/*     */         
/*     */ 
/* 195 */         return new Platform();
/*     */       }
/*     */       catch (ClassNotFoundException localClassNotFoundException5) {}
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */   private static class Android
/*     */     extends Platform
/*     */   {
/*     */     private static final int MAX_LOG_LENGTH = 4000;
/*     */     
/*     */     private final Class<?> sslParametersClass;
/*     */     
/*     */     private final OptionalMethod<Socket> setUseSessionTickets;
/*     */     private final OptionalMethod<Socket> setHostname;
/*     */     private final Method trafficStatsTagSocket;
/*     */     private final Method trafficStatsUntagSocket;
/*     */     private final OptionalMethod<Socket> getAlpnSelectedProtocol;
/*     */     private final OptionalMethod<Socket> setAlpnProtocols;
/*     */     
/*     */     public Android(Class<?> sslParametersClass, OptionalMethod<Socket> setUseSessionTickets, OptionalMethod<Socket> setHostname, Method trafficStatsTagSocket, Method trafficStatsUntagSocket, OptionalMethod<Socket> getAlpnSelectedProtocol, OptionalMethod<Socket> setAlpnProtocols)
/*     */     {
/* 218 */       this.sslParametersClass = sslParametersClass;
/* 219 */       this.setUseSessionTickets = setUseSessionTickets;
/* 220 */       this.setHostname = setHostname;
/* 221 */       this.trafficStatsTagSocket = trafficStatsTagSocket;
/* 222 */       this.trafficStatsUntagSocket = trafficStatsUntagSocket;
/* 223 */       this.getAlpnSelectedProtocol = getAlpnSelectedProtocol;
/* 224 */       this.setAlpnProtocols = setAlpnProtocols;
/*     */     }
/*     */     
/*     */     public void connectSocket(Socket socket, InetSocketAddress address, int connectTimeout) throws IOException
/*     */     {
/*     */       try {
/* 230 */         socket.connect(address, connectTimeout);
/*     */       } catch (AssertionError e) {
/* 232 */         if (Util.isAndroidGetsocknameError(e)) throw new IOException(e);
/* 233 */         throw e;
/*     */       }
/*     */       catch (SecurityException e)
/*     */       {
/* 237 */         IOException ioException = new IOException("Exception in connect");
/* 238 */         ioException.initCause(e);
/* 239 */         throw ioException;
/*     */       }
/*     */     }
/*     */     
/*     */     public X509TrustManager trustManager(SSLSocketFactory sslSocketFactory) {
/* 244 */       Object context = readFieldOrNull(sslSocketFactory, this.sslParametersClass, "sslParameters");
/* 245 */       if (context == null)
/*     */       {
/*     */         try
/*     */         {
/* 249 */           Class<?> gmsSslParametersClass = Class.forName("com.google.android.gms.org.conscrypt.SSLParametersImpl", false, sslSocketFactory
/*     */           
/* 251 */             .getClass().getClassLoader());
/* 252 */           context = readFieldOrNull(sslSocketFactory, gmsSslParametersClass, "sslParameters");
/*     */         } catch (ClassNotFoundException e) {
/* 254 */           return null;
/*     */         }
/*     */       }
/*     */       
/* 258 */       X509TrustManager x509TrustManager = (X509TrustManager)readFieldOrNull(context, X509TrustManager.class, "x509TrustManager");
/*     */       
/* 260 */       if (x509TrustManager != null) { return x509TrustManager;
/*     */       }
/* 262 */       return (X509TrustManager)readFieldOrNull(context, X509TrustManager.class, "trustManager");
/*     */     }
/*     */     
/*     */     public TrustRootIndex trustRootIndex(X509TrustManager trustManager) {
/* 266 */       TrustRootIndex result = AndroidTrustRootIndex.get(trustManager);
/* 267 */       if (result != null) return result;
/* 268 */       return super.trustRootIndex(trustManager);
/*     */     }
/*     */     
/*     */ 
/*     */     public void configureTlsExtensions(SSLSocket sslSocket, String hostname, List<Protocol> protocols)
/*     */     {
/* 274 */       if (hostname != null) {
/* 275 */         this.setUseSessionTickets.invokeOptionalWithoutCheckedException(sslSocket, new Object[] { Boolean.valueOf(true) });
/* 276 */         this.setHostname.invokeOptionalWithoutCheckedException(sslSocket, new Object[] { hostname });
/*     */       }
/*     */       
/*     */ 
/* 280 */       if ((this.setAlpnProtocols != null) && (this.setAlpnProtocols.isSupported(sslSocket))) {
/* 281 */         Object[] parameters = { concatLengthPrefixed(protocols) };
/* 282 */         this.setAlpnProtocols.invokeWithoutCheckedException(sslSocket, parameters);
/*     */       }
/*     */     }
/*     */     
/*     */     public String getSelectedProtocol(SSLSocket socket) {
/* 287 */       if (this.getAlpnSelectedProtocol == null) return null;
/* 288 */       if (!this.getAlpnSelectedProtocol.isSupported(socket)) { return null;
/*     */       }
/* 290 */       byte[] alpnResult = (byte[])this.getAlpnSelectedProtocol.invokeWithoutCheckedException(socket, new Object[0]);
/* 291 */       return alpnResult != null ? new String(alpnResult, Util.UTF_8) : null;
/*     */     }
/*     */     
/*     */     public void tagSocket(Socket socket) throws SocketException {
/* 295 */       if (this.trafficStatsTagSocket == null) return;
/*     */       try
/*     */       {
/* 298 */         this.trafficStatsTagSocket.invoke(null, new Object[] { socket });
/*     */       } catch (IllegalAccessException e) {
/* 300 */         throw new RuntimeException(e);
/*     */       } catch (InvocationTargetException e) {
/* 302 */         throw new RuntimeException(e.getCause());
/*     */       }
/*     */     }
/*     */     
/*     */     public void untagSocket(Socket socket) throws SocketException {
/* 307 */       if (this.trafficStatsUntagSocket == null) return;
/*     */       try
/*     */       {
/* 310 */         this.trafficStatsUntagSocket.invoke(null, new Object[] { socket });
/*     */       } catch (IllegalAccessException e) {
/* 312 */         throw new RuntimeException(e);
/*     */       } catch (InvocationTargetException e) {
/* 314 */         throw new RuntimeException(e.getCause());
/*     */       }
/*     */     }
/*     */     
/*     */     public void log(String message)
/*     */     {
/* 320 */       int i = 0; for (int length = message.length(); i < length; i++) {
/* 321 */         int newline = message.indexOf('\n', i);
/* 322 */         newline = newline != -1 ? newline : length;
/*     */         do {
/* 324 */           int end = Math.min(newline, i + 4000);
/* 325 */           Log.d("OkHttp", message.substring(i, end));
/* 326 */           i = end;
/* 327 */         } while (i < newline);
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   private static class JdkPlatform extends Platform
/*     */   {
/*     */     private final Class<?> sslContextClass;
/*     */     
/*     */     public JdkPlatform(Class<?> sslContextClass) {
/* 337 */       this.sslContextClass = sslContextClass;
/*     */     }
/*     */     
/*     */     public X509TrustManager trustManager(SSLSocketFactory sslSocketFactory) {
/* 341 */       Object context = readFieldOrNull(sslSocketFactory, this.sslContextClass, "context");
/* 342 */       if (context == null) return null;
/* 343 */       return (X509TrustManager)readFieldOrNull(context, X509TrustManager.class, "trustManager");
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */   private static class JdkWithJettyBootPlatform
/*     */     extends Platform.JdkPlatform
/*     */   {
/*     */     private final Method putMethod;
/*     */     private final Method getMethod;
/*     */     private final Method removeMethod;
/*     */     private final Class<?> clientProviderClass;
/*     */     private final Class<?> serverProviderClass;
/*     */     
/*     */     public JdkWithJettyBootPlatform(Class<?> sslContextClass, Method putMethod, Method getMethod, Method removeMethod, Class<?> clientProviderClass, Class<?> serverProviderClass)
/*     */     {
/* 359 */       super();
/* 360 */       this.putMethod = putMethod;
/* 361 */       this.getMethod = getMethod;
/* 362 */       this.removeMethod = removeMethod;
/* 363 */       this.clientProviderClass = clientProviderClass;
/* 364 */       this.serverProviderClass = serverProviderClass;
/*     */     }
/*     */     
/*     */     public void configureTlsExtensions(SSLSocket sslSocket, String hostname, List<Protocol> protocols)
/*     */     {
/* 369 */       List<String> names = new ArrayList(protocols.size());
/* 370 */       int i = 0; for (int size = protocols.size(); i < size; i++) {
/* 371 */         Protocol protocol = (Protocol)protocols.get(i);
/* 372 */         if (protocol != Protocol.HTTP_1_0)
/* 373 */           names.add(protocol.toString());
/*     */       }
/*     */       try {
/* 376 */         Object provider = Proxy.newProxyInstance(Platform.class.getClassLoader(), new Class[] { this.clientProviderClass, this.serverProviderClass }, new Platform.JettyNegoProvider(names));
/*     */         
/* 378 */         this.putMethod.invoke(null, new Object[] { sslSocket, provider });
/*     */       } catch (InvocationTargetException|IllegalAccessException e) {
/* 380 */         throw new AssertionError(e);
/*     */       }
/*     */     }
/*     */     
/*     */     public void afterHandshake(SSLSocket sslSocket) {
/*     */       try {
/* 386 */         this.removeMethod.invoke(null, new Object[] { sslSocket });
/*     */       } catch (IllegalAccessException|InvocationTargetException ignored) {
/* 388 */         throw new AssertionError();
/*     */       }
/*     */     }
/*     */     
/*     */     public String getSelectedProtocol(SSLSocket socket)
/*     */     {
/*     */       try {
/* 395 */         Platform.JettyNegoProvider provider = (Platform.JettyNegoProvider)Proxy.getInvocationHandler(this.getMethod.invoke(null, new Object[] { socket }));
/* 396 */         if ((!provider.unsupported) && (provider.selected == null)) {
/* 397 */           Internal.logger.log(Level.INFO, "ALPN callback dropped: SPDY and HTTP/2 are disabled. Is alpn-boot on the boot class path?");
/*     */           
/* 399 */           return null;
/*     */         }
/* 401 */         return provider.unsupported ? null : provider.selected;
/*     */       } catch (InvocationTargetException|IllegalAccessException e) {
/* 403 */         throw new AssertionError();
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */   private static class JettyNegoProvider
/*     */     implements InvocationHandler
/*     */   {
/*     */     private final List<String> protocols;
/*     */     
/*     */     private boolean unsupported;
/*     */     
/*     */     private String selected;
/*     */     
/*     */ 
/*     */     public JettyNegoProvider(List<String> protocols)
/*     */     {
/* 421 */       this.protocols = protocols;
/*     */     }
/*     */     
/*     */     public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
/* 425 */       String methodName = method.getName();
/* 426 */       Class<?> returnType = method.getReturnType();
/* 427 */       if (args == null) {
/* 428 */         args = Util.EMPTY_STRING_ARRAY;
/*     */       }
/* 430 */       if ((methodName.equals("supports")) && (Boolean.TYPE == returnType))
/* 431 */         return Boolean.valueOf(true);
/* 432 */       if ((methodName.equals("unsupported")) && (Void.TYPE == returnType)) {
/* 433 */         this.unsupported = true;
/* 434 */         return null; }
/* 435 */       if ((methodName.equals("protocols")) && (args.length == 0))
/* 436 */         return this.protocols;
/* 437 */       if (((methodName.equals("selectProtocol")) || (methodName.equals("select"))) && (String.class == returnType) && (args.length == 1) && ((args[0] instanceof List)))
/*     */       {
/* 439 */         List<String> peerProtocols = (List)args[0];
/*     */         
/* 441 */         int i = 0; for (int size = peerProtocols.size(); i < size; i++) {
/* 442 */           if (this.protocols.contains(peerProtocols.get(i))) {
/* 443 */             return this.selected = (String)peerProtocols.get(i);
/*     */           }
/*     */         }
/* 446 */         return this.selected = (String)this.protocols.get(0); }
/* 447 */       if (((methodName.equals("protocolSelected")) || (methodName.equals("selected"))) && (args.length == 1))
/*     */       {
/* 449 */         this.selected = ((String)args[0]);
/* 450 */         return null;
/*     */       }
/* 452 */       return method.invoke(this, args);
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   static byte[] concatLengthPrefixed(List<Protocol> protocols)
/*     */   {
/* 462 */     Buffer result = new Buffer();
/* 463 */     int i = 0; for (int size = protocols.size(); i < size; i++) {
/* 464 */       Protocol protocol = (Protocol)protocols.get(i);
/* 465 */       if (protocol != Protocol.HTTP_1_0) {
/* 466 */         result.writeByte(protocol.toString().length());
/* 467 */         result.writeUtf8(protocol.toString());
/*     */       } }
/* 469 */     return result.readByteArray();
/*     */   }
/*     */   
/*     */   static <T> T readFieldOrNull(Object instance, Class<T> fieldType, String fieldName) {
/* 473 */     for (Class<?> c = instance.getClass(); c != Object.class; c = c.getSuperclass()) {
/*     */       try {
/* 475 */         Field field = c.getDeclaredField(fieldName);
/* 476 */         field.setAccessible(true);
/* 477 */         Object value = field.get(instance);
/* 478 */         if ((value == null) || (!fieldType.isInstance(value))) return null;
/* 479 */         return (T)fieldType.cast(value);
/*     */       }
/*     */       catch (NoSuchFieldException localNoSuchFieldException) {}catch (IllegalAccessException e) {
/* 482 */         throw new AssertionError();
/*     */       }
/*     */     }
/*     */     
/*     */ 
/* 487 */     if (!fieldName.equals("delegate")) {
/* 488 */       Object delegate = readFieldOrNull(instance, Object.class, "delegate");
/* 489 */       if (delegate != null) { return (T)readFieldOrNull(delegate, fieldType, fieldName);
/*     */       }
/*     */     }
/* 492 */     return null;
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\Platform.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */