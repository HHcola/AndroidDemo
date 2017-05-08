/*     */ package com.squareup.okhttp;
/*     */ 
/*     */ import com.squareup.okhttp.internal.Util;
/*     */ import java.io.File;
/*     */ import java.io.IOException;
/*     */ import java.nio.charset.Charset;
/*     */ import okio.BufferedSink;
/*     */ import okio.ByteString;
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
/*     */ public abstract class RequestBody
/*     */ {
/*     */   public abstract MediaType contentType();
/*     */   
/*     */   public long contentLength()
/*     */     throws IOException
/*     */   {
/*  36 */     return -1L;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public abstract void writeTo(BufferedSink paramBufferedSink)
/*     */     throws IOException;
/*     */   
/*     */ 
/*     */   public static RequestBody create(MediaType contentType, String content)
/*     */   {
/*  47 */     Charset charset = Util.UTF_8;
/*  48 */     if (contentType != null) {
/*  49 */       charset = contentType.charset();
/*  50 */       if (charset == null) {
/*  51 */         charset = Util.UTF_8;
/*  52 */         contentType = MediaType.parse(contentType + "; charset=utf-8");
/*     */       }
/*     */     }
/*  55 */     byte[] bytes = content.getBytes(charset);
/*  56 */     return create(contentType, bytes);
/*     */   }
/*     */   
/*     */   public static RequestBody create(MediaType contentType, final ByteString content)
/*     */   {
/*  61 */     new RequestBody() {
/*     */       public MediaType contentType() {
/*  63 */         return this.val$contentType;
/*     */       }
/*     */       
/*     */       public long contentLength() throws IOException {
/*  67 */         return content.size();
/*     */       }
/*     */       
/*     */       public void writeTo(BufferedSink sink) throws IOException {
/*  71 */         sink.write(content);
/*     */       }
/*     */     };
/*     */   }
/*     */   
/*     */   public static RequestBody create(MediaType contentType, byte[] content)
/*     */   {
/*  78 */     return create(contentType, content, 0, content.length);
/*     */   }
/*     */   
/*     */ 
/*     */   public static RequestBody create(MediaType contentType, final byte[] content, final int offset, final int byteCount)
/*     */   {
/*  84 */     if (content == null) throw new NullPointerException("content == null");
/*  85 */     Util.checkOffsetAndCount(content.length, offset, byteCount);
/*  86 */     new RequestBody() {
/*     */       public MediaType contentType() {
/*  88 */         return this.val$contentType;
/*     */       }
/*     */       
/*     */       public long contentLength() {
/*  92 */         return byteCount;
/*     */       }
/*     */       
/*     */       public void writeTo(BufferedSink sink) throws IOException {
/*  96 */         sink.write(content, offset, byteCount);
/*     */       }
/*     */     };
/*     */   }
/*     */   
/*     */   public static RequestBody create(MediaType contentType, final File file)
/*     */   {
/* 103 */     if (file == null) { throw new NullPointerException("content == null");
/*     */     }
/* 105 */     new RequestBody() {
/*     */       public MediaType contentType() {
/* 107 */         return this.val$contentType;
/*     */       }
/*     */       
/*     */       public long contentLength() {
/* 111 */         return file.length();
/*     */       }
/*     */       
/*     */       /* Error */
/*     */       public void writeTo(BufferedSink sink)
/*     */         throws IOException
/*     */       {
/*     */         // Byte code:
/*     */         //   0: aconst_null
/*     */         //   1: astore_2
/*     */         //   2: aload_0
/*     */         //   3: getfield 2	com/squareup/okhttp/RequestBody$3:val$file	Ljava/io/File;
/*     */         //   6: invokestatic 5	okio/Okio:source	(Ljava/io/File;)Lokio/Source;
/*     */         //   9: astore_2
/*     */         //   10: aload_1
/*     */         //   11: aload_2
/*     */         //   12: invokeinterface 6 2 0
/*     */         //   17: pop2
/*     */         //   18: aload_2
/*     */         //   19: invokestatic 7	com/squareup/okhttp/internal/Util:closeQuietly	(Ljava/io/Closeable;)V
/*     */         //   22: goto +10 -> 32
/*     */         //   25: astore_3
/*     */         //   26: aload_2
/*     */         //   27: invokestatic 7	com/squareup/okhttp/internal/Util:closeQuietly	(Ljava/io/Closeable;)V
/*     */         //   30: aload_3
/*     */         //   31: athrow
/*     */         //   32: return
/*     */         // Line number table:
/*     */         //   Java source line #115	-> byte code offset #0
/*     */         //   Java source line #117	-> byte code offset #2
/*     */         //   Java source line #118	-> byte code offset #10
/*     */         //   Java source line #120	-> byte code offset #18
/*     */         //   Java source line #121	-> byte code offset #22
/*     */         //   Java source line #120	-> byte code offset #25
/*     */         //   Java source line #122	-> byte code offset #32
/*     */         // Local variable table:
/*     */         //   start	length	slot	name	signature
/*     */         //   0	33	0	this	3
/*     */         //   0	33	1	sink	BufferedSink
/*     */         //   1	26	2	source	okio.Source
/*     */         //   25	6	3	localObject	Object
/*     */         // Exception table:
/*     */         //   from	to	target	type
/*     */         //   2	18	25	finally
/*     */       }
/*     */     };
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\RequestBody.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */