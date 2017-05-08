/*     */ package com.squareup.okhttp;
/*     */ 
/*     */ import com.squareup.okhttp.internal.Util;
/*     */ import java.io.Closeable;
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.io.InputStreamReader;
/*     */ import java.io.Reader;
/*     */ import java.nio.charset.Charset;
/*     */ import okio.Buffer;
/*     */ import okio.BufferedSource;
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
/*     */ public abstract class ResponseBody
/*     */   implements Closeable
/*     */ {
/*     */   private Reader reader;
/*     */   
/*     */   public abstract MediaType contentType();
/*     */   
/*     */   public abstract long contentLength()
/*     */     throws IOException;
/*     */   
/*     */   public final InputStream byteStream()
/*     */     throws IOException
/*     */   {
/*  43 */     return source().inputStream();
/*     */   }
/*     */   
/*     */   public abstract BufferedSource source() throws IOException;
/*     */   
/*     */   public final byte[] bytes() throws IOException {
/*  49 */     long contentLength = contentLength();
/*  50 */     if (contentLength > 2147483647L) {
/*  51 */       throw new IOException("Cannot buffer entire body for content length: " + contentLength);
/*     */     }
/*     */     
/*  54 */     BufferedSource source = source();
/*     */     byte[] bytes;
/*     */     try {
/*  57 */       bytes = source.readByteArray();
/*     */     } finally {
/*  59 */       Util.closeQuietly(source);
/*     */     }
/*  61 */     if ((contentLength != -1L) && (contentLength != bytes.length)) {
/*  62 */       throw new IOException("Content-Length and stream length disagree");
/*     */     }
/*  64 */     return bytes;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public final Reader charStream()
/*     */     throws IOException
/*     */   {
/*  73 */     Reader r = this.reader;
/*  74 */     return r != null ? r : (this.reader = new InputStreamReader(byteStream(), charset()));
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public final String string()
/*     */     throws IOException
/*     */   {
/*  83 */     return new String(bytes(), charset().name());
/*     */   }
/*     */   
/*     */   private Charset charset() {
/*  87 */     MediaType contentType = contentType();
/*  88 */     return contentType != null ? contentType.charset(Util.UTF_8) : Util.UTF_8;
/*     */   }
/*     */   
/*     */   public void close() throws IOException {
/*  92 */     source().close();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public static ResponseBody create(MediaType contentType, String content)
/*     */   {
/* 100 */     Charset charset = Util.UTF_8;
/* 101 */     if (contentType != null) {
/* 102 */       charset = contentType.charset();
/* 103 */       if (charset == null) {
/* 104 */         charset = Util.UTF_8;
/* 105 */         contentType = MediaType.parse(contentType + "; charset=utf-8");
/*     */       }
/*     */     }
/* 108 */     Buffer buffer = new Buffer().writeString(content, charset);
/* 109 */     return create(contentType, buffer.size(), buffer);
/*     */   }
/*     */   
/*     */   public static ResponseBody create(MediaType contentType, byte[] content)
/*     */   {
/* 114 */     Buffer buffer = new Buffer().write(content);
/* 115 */     return create(contentType, content.length, buffer);
/*     */   }
/*     */   
/*     */ 
/*     */   public static ResponseBody create(MediaType contentType, final long contentLength, BufferedSource content)
/*     */   {
/* 121 */     if (content == null) throw new NullPointerException("source == null");
/* 122 */     new ResponseBody() {
/*     */       public MediaType contentType() {
/* 124 */         return this.val$contentType;
/*     */       }
/*     */       
/*     */       public long contentLength() {
/* 128 */         return contentLength;
/*     */       }
/*     */       
/*     */       public BufferedSource source() {
/* 132 */         return this.val$content;
/*     */       }
/*     */     };
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\ResponseBody.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */