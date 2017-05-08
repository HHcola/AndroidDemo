/*     */ package com.squareup.okhttp.internal;
/*     */ 
/*     */ import com.squareup.okhttp.HttpUrl;
/*     */ import java.io.Closeable;
/*     */ import java.io.IOException;
/*     */ import java.io.InterruptedIOException;
/*     */ import java.io.UnsupportedEncodingException;
/*     */ import java.lang.reflect.Array;
/*     */ import java.net.ServerSocket;
/*     */ import java.net.Socket;
/*     */ import java.nio.charset.Charset;
/*     */ import java.security.MessageDigest;
/*     */ import java.security.NoSuchAlgorithmException;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Arrays;
/*     */ import java.util.Collections;
/*     */ import java.util.LinkedHashMap;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ import java.util.concurrent.ThreadFactory;
/*     */ import java.util.concurrent.TimeUnit;
/*     */ import okio.Buffer;
/*     */ import okio.ByteString;
/*     */ import okio.Source;
/*     */ import okio.Timeout;
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
/*     */ public final class Util
/*     */ {
/*  44 */   public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
/*  45 */   public static final String[] EMPTY_STRING_ARRAY = new String[0];
/*     */   
/*     */ 
/*  48 */   public static final Charset UTF_8 = Charset.forName("UTF-8");
/*     */   
/*     */ 
/*     */ 
/*     */   public static void checkOffsetAndCount(long arrayLength, long offset, long count)
/*     */   {
/*  54 */     if (((offset | count) < 0L) || (offset > arrayLength) || (arrayLength - offset < count)) {
/*  55 */       throw new ArrayIndexOutOfBoundsException();
/*     */     }
/*     */   }
/*     */   
/*     */   public static boolean equal(Object a, Object b)
/*     */   {
/*  61 */     return (a == b) || ((a != null) && (a.equals(b)));
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public static void closeQuietly(Closeable closeable)
/*     */   {
/*  69 */     if (closeable != null) {
/*     */       try {
/*  71 */         closeable.close();
/*     */       } catch (RuntimeException rethrown) {
/*  73 */         throw rethrown;
/*     */       }
/*     */       catch (Exception localException) {}
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public static void closeQuietly(Socket socket)
/*     */   {
/*  84 */     if (socket != null) {
/*     */       try {
/*  86 */         socket.close();
/*     */       } catch (AssertionError e) {
/*  88 */         if (!isAndroidGetsocknameError(e)) throw e;
/*     */       } catch (RuntimeException rethrown) {
/*  90 */         throw rethrown;
/*     */       }
/*     */       catch (Exception localException) {}
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public static void closeQuietly(ServerSocket serverSocket)
/*     */   {
/* 101 */     if (serverSocket != null) {
/*     */       try {
/* 103 */         serverSocket.close();
/*     */       } catch (RuntimeException rethrown) {
/* 105 */         throw rethrown;
/*     */       }
/*     */       catch (Exception localException) {}
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public static void closeAll(Closeable a, Closeable b)
/*     */     throws IOException
/*     */   {
/* 116 */     Throwable thrown = null;
/*     */     try {
/* 118 */       a.close();
/*     */     } catch (Throwable e) {
/* 120 */       thrown = e;
/*     */     }
/*     */     try {
/* 123 */       b.close();
/*     */     } catch (Throwable e) {
/* 125 */       if (thrown == null) thrown = e;
/*     */     }
/* 127 */     if (thrown == null) return;
/* 128 */     if ((thrown instanceof IOException)) throw ((IOException)thrown);
/* 129 */     if ((thrown instanceof RuntimeException)) throw ((RuntimeException)thrown);
/* 130 */     if ((thrown instanceof Error)) throw ((Error)thrown);
/* 131 */     throw new AssertionError(thrown);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public static boolean discard(Source source, int timeout, TimeUnit timeUnit)
/*     */   {
/*     */     try
/*     */     {
/* 141 */       return skipAll(source, timeout, timeUnit);
/*     */     } catch (IOException e) {}
/* 143 */     return false;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public static boolean skipAll(Source source, int duration, TimeUnit timeUnit)
/*     */     throws IOException
/*     */   {
/* 152 */     long now = System.nanoTime();
/*     */     
/* 154 */     long originalDuration = source.timeout().hasDeadline() ? source.timeout().deadlineNanoTime() - now : Long.MAX_VALUE;
/*     */     
/* 156 */     source.timeout().deadlineNanoTime(now + Math.min(originalDuration, timeUnit.toNanos(duration)));
/*     */     try {
/* 158 */       Buffer skipBuffer = new Buffer();
/* 159 */       while (source.read(skipBuffer, 2048L) != -1L) {
/* 160 */         skipBuffer.clear();
/*     */       }
/* 162 */       return true;
/*     */     } catch (InterruptedIOException e) { boolean bool;
/* 164 */       return false;
/*     */     } finally {
/* 166 */       if (originalDuration == Long.MAX_VALUE) {
/* 167 */         source.timeout().clearDeadline();
/*     */       } else {
/* 169 */         source.timeout().deadlineNanoTime(now + originalDuration);
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   public static String md5Hex(String s)
/*     */   {
/*     */     try {
/* 177 */       MessageDigest messageDigest = MessageDigest.getInstance("MD5");
/* 178 */       byte[] md5bytes = messageDigest.digest(s.getBytes("UTF-8"));
/* 179 */       return ByteString.of(md5bytes).hex();
/*     */     } catch (NoSuchAlgorithmException|UnsupportedEncodingException e) {
/* 181 */       throw new AssertionError(e);
/*     */     }
/*     */   }
/*     */   
/*     */   public static String shaBase64(String s)
/*     */   {
/*     */     try {
/* 188 */       MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
/* 189 */       byte[] sha1Bytes = messageDigest.digest(s.getBytes("UTF-8"));
/* 190 */       return ByteString.of(sha1Bytes).base64();
/*     */     } catch (NoSuchAlgorithmException|UnsupportedEncodingException e) {
/* 192 */       throw new AssertionError(e);
/*     */     }
/*     */   }
/*     */   
/*     */   public static ByteString sha1(ByteString s)
/*     */   {
/*     */     try {
/* 199 */       MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
/* 200 */       byte[] sha1Bytes = messageDigest.digest(s.toByteArray());
/* 201 */       return ByteString.of(sha1Bytes);
/*     */     } catch (NoSuchAlgorithmException e) {
/* 203 */       throw new AssertionError(e);
/*     */     }
/*     */   }
/*     */   
/*     */   public static <T> List<T> immutableList(List<T> list)
/*     */   {
/* 209 */     return Collections.unmodifiableList(new ArrayList(list));
/*     */   }
/*     */   
/*     */   public static <T> List<T> immutableList(T... elements)
/*     */   {
/* 214 */     return Collections.unmodifiableList(Arrays.asList((Object[])elements.clone()));
/*     */   }
/*     */   
/*     */   public static <K, V> Map<K, V> immutableMap(Map<K, V> map)
/*     */   {
/* 219 */     return Collections.unmodifiableMap(new LinkedHashMap(map));
/*     */   }
/*     */   
/*     */   public static ThreadFactory threadFactory(String name, final boolean daemon) {
/* 223 */     new ThreadFactory() {
/*     */       public Thread newThread(Runnable runnable) {
/* 225 */         Thread result = new Thread(runnable, this.val$name);
/* 226 */         result.setDaemon(daemon);
/* 227 */         return result;
/*     */       }
/*     */     };
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public static <T> T[] intersect(Class<T> arrayType, T[] first, T[] second)
/*     */   {
/* 238 */     List<T> result = intersect(first, second);
/* 239 */     return result.toArray((Object[])Array.newInstance(arrayType, result.size()));
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   private static <T> List<T> intersect(T[] first, T[] second)
/*     */   {
/* 247 */     List<T> result = new ArrayList();
/* 248 */     for (T a : first) {
/* 249 */       for (T b : second) {
/* 250 */         if (a.equals(b)) {
/* 251 */           result.add(b);
/* 252 */           break;
/*     */         }
/*     */       }
/*     */     }
/* 256 */     return result;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public static String hostHeader(HttpUrl url)
/*     */   {
/* 263 */     return url.port() != HttpUrl.defaultPort(url.scheme()) ? url.host() + ":" + url.port() : url.host();
/*     */   }
/*     */   
/*     */   public static String toHumanReadableAscii(String s)
/*     */   {
/* 268 */     int i = 0; int c; for (int length = s.length(); i < length; i += Character.charCount(c)) {
/* 269 */       c = s.codePointAt(i);
/* 270 */       if ((c <= 31) || (c >= 127))
/*     */       {
/* 272 */         Buffer buffer = new Buffer();
/* 273 */         buffer.writeUtf8(s, 0, i);
/* 274 */         for (int j = i; j < length; j += Character.charCount(c)) {
/* 275 */           c = s.codePointAt(j);
/* 276 */           buffer.writeUtf8CodePoint((c > 31) && (c < 127) ? c : 63);
/*     */         }
/* 278 */         return buffer.readUtf8();
/*     */       } }
/* 280 */     return s;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public static boolean isAndroidGetsocknameError(AssertionError e)
/*     */   {
/* 289 */     return (e.getCause() != null) && (e.getMessage() != null) && (e.getMessage().contains("getsockname failed"));
/*     */   }
/*     */   
/*     */   public static boolean contains(String[] array, String value) {
/* 293 */     return Arrays.asList(array).contains(value);
/*     */   }
/*     */   
/*     */   public static String[] concat(String[] array, String value) {
/* 297 */     String[] result = new String[array.length + 1];
/* 298 */     System.arraycopy(array, 0, result, 0, array.length);
/* 299 */     result[(result.length - 1)] = value;
/* 300 */     return result;
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\Util.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */