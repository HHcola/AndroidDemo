/*     */ package com.squareup.okhttp;
/*     */ 
/*     */ import com.squareup.okhttp.internal.Util;
/*     */ import java.io.IOException;
/*     */ import java.util.ArrayList;
/*     */ import java.util.List;
/*     */ import java.util.UUID;
/*     */ import okio.Buffer;
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
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public final class MultipartBuilder
/*     */ {
/*  38 */   public static final MediaType MIXED = MediaType.parse("multipart/mixed");
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*  45 */   public static final MediaType ALTERNATIVE = MediaType.parse("multipart/alternative");
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*  53 */   public static final MediaType DIGEST = MediaType.parse("multipart/digest");
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*  60 */   public static final MediaType PARALLEL = MediaType.parse("multipart/parallel");
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*  68 */   public static final MediaType FORM = MediaType.parse("multipart/form-data");
/*     */   
/*  70 */   private static final byte[] COLONSPACE = { 58, 32 };
/*  71 */   private static final byte[] CRLF = { 13, 10 };
/*  72 */   private static final byte[] DASHDASH = { 45, 45 };
/*     */   
/*     */   private final ByteString boundary;
/*  75 */   private MediaType type = MIXED;
/*     */   
/*     */ 
/*  78 */   private final List<Headers> partHeaders = new ArrayList();
/*  79 */   private final List<RequestBody> partBodies = new ArrayList();
/*     */   
/*     */   public MultipartBuilder()
/*     */   {
/*  83 */     this(UUID.randomUUID().toString());
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public MultipartBuilder(String boundary)
/*     */   {
/*  92 */     this.boundary = ByteString.encodeUtf8(boundary);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public MultipartBuilder type(MediaType type)
/*     */   {
/* 101 */     if (type == null) {
/* 102 */       throw new NullPointerException("type == null");
/*     */     }
/* 104 */     if (!type.type().equals("multipart")) {
/* 105 */       throw new IllegalArgumentException("multipart != " + type);
/*     */     }
/* 107 */     this.type = type;
/* 108 */     return this;
/*     */   }
/*     */   
/*     */   public MultipartBuilder addPart(RequestBody body)
/*     */   {
/* 113 */     return addPart(null, body);
/*     */   }
/*     */   
/*     */   public MultipartBuilder addPart(Headers headers, RequestBody body)
/*     */   {
/* 118 */     if (body == null) {
/* 119 */       throw new NullPointerException("body == null");
/*     */     }
/* 121 */     if ((headers != null) && (headers.get("Content-Type") != null)) {
/* 122 */       throw new IllegalArgumentException("Unexpected header: Content-Type");
/*     */     }
/* 124 */     if ((headers != null) && (headers.get("Content-Length") != null)) {
/* 125 */       throw new IllegalArgumentException("Unexpected header: Content-Length");
/*     */     }
/*     */     
/* 128 */     this.partHeaders.add(headers);
/* 129 */     this.partBodies.add(body);
/* 130 */     return this;
/*     */   }
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
/*     */   private static StringBuilder appendQuotedString(StringBuilder target, String key)
/*     */   {
/* 145 */     target.append('"');
/* 146 */     int i = 0; for (int len = key.length(); i < len; i++) {
/* 147 */       char ch = key.charAt(i);
/* 148 */       switch (ch) {
/*     */       case '\n': 
/* 150 */         target.append("%0A");
/* 151 */         break;
/*     */       case '\r': 
/* 153 */         target.append("%0D");
/* 154 */         break;
/*     */       case '"': 
/* 156 */         target.append("%22");
/* 157 */         break;
/*     */       default: 
/* 159 */         target.append(ch);
/*     */       }
/*     */       
/*     */     }
/* 163 */     target.append('"');
/* 164 */     return target;
/*     */   }
/*     */   
/*     */   public MultipartBuilder addFormDataPart(String name, String value)
/*     */   {
/* 169 */     return addFormDataPart(name, null, RequestBody.create(null, value));
/*     */   }
/*     */   
/*     */   public MultipartBuilder addFormDataPart(String name, String filename, RequestBody value)
/*     */   {
/* 174 */     if (name == null) {
/* 175 */       throw new NullPointerException("name == null");
/*     */     }
/* 177 */     StringBuilder disposition = new StringBuilder("form-data; name=");
/* 178 */     appendQuotedString(disposition, name);
/*     */     
/* 180 */     if (filename != null) {
/* 181 */       disposition.append("; filename=");
/* 182 */       appendQuotedString(disposition, filename);
/*     */     }
/*     */     
/* 185 */     return addPart(Headers.of(new String[] { "Content-Disposition", disposition.toString() }), value);
/*     */   }
/*     */   
/*     */   public RequestBody build()
/*     */   {
/* 190 */     if (this.partHeaders.isEmpty()) {
/* 191 */       throw new IllegalStateException("Multipart body must have at least one part.");
/*     */     }
/* 193 */     return new MultipartRequestBody(this.type, this.boundary, this.partHeaders, this.partBodies);
/*     */   }
/*     */   
/*     */   private static final class MultipartRequestBody extends RequestBody {
/*     */     private final ByteString boundary;
/*     */     private final MediaType contentType;
/*     */     private final List<Headers> partHeaders;
/*     */     private final List<RequestBody> partBodies;
/* 201 */     private long contentLength = -1L;
/*     */     
/*     */     public MultipartRequestBody(MediaType type, ByteString boundary, List<Headers> partHeaders, List<RequestBody> partBodies)
/*     */     {
/* 205 */       if (type == null) { throw new NullPointerException("type == null");
/*     */       }
/* 207 */       this.boundary = boundary;
/* 208 */       this.contentType = MediaType.parse(type + "; boundary=" + boundary.utf8());
/* 209 */       this.partHeaders = Util.immutableList(partHeaders);
/* 210 */       this.partBodies = Util.immutableList(partBodies);
/*     */     }
/*     */     
/*     */     public MediaType contentType() {
/* 214 */       return this.contentType;
/*     */     }
/*     */     
/*     */     public long contentLength() throws IOException {
/* 218 */       long result = this.contentLength;
/* 219 */       if (result != -1L) return result;
/* 220 */       return this.contentLength = writeOrCountBytes(null, true);
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     private long writeOrCountBytes(BufferedSink sink, boolean countBytes)
/*     */       throws IOException
/*     */     {
/* 230 */       long byteCount = 0L;
/*     */       
/* 232 */       Buffer byteCountBuffer = null;
/* 233 */       if (countBytes) {
/* 234 */         sink = byteCountBuffer = new Buffer();
/*     */       }
/*     */       
/* 237 */       int p = 0; for (int partCount = this.partHeaders.size(); p < partCount; p++) {
/* 238 */         Headers headers = (Headers)this.partHeaders.get(p);
/* 239 */         RequestBody body = (RequestBody)this.partBodies.get(p);
/*     */         
/* 241 */         sink.write(MultipartBuilder.DASHDASH);
/* 242 */         sink.write(this.boundary);
/* 243 */         sink.write(MultipartBuilder.CRLF);
/*     */         
/* 245 */         if (headers != null) {
/* 246 */           int h = 0; for (int headerCount = headers.size(); h < headerCount; h++)
/*     */           {
/*     */ 
/*     */ 
/* 250 */             sink.writeUtf8(headers.name(h)).write(MultipartBuilder.COLONSPACE).writeUtf8(headers.value(h)).write(MultipartBuilder.CRLF);
/*     */           }
/*     */         }
/*     */         
/* 254 */         MediaType contentType = body.contentType();
/* 255 */         if (contentType != null)
/*     */         {
/*     */ 
/* 258 */           sink.writeUtf8("Content-Type: ").writeUtf8(contentType.toString()).write(MultipartBuilder.CRLF);
/*     */         }
/*     */         
/* 261 */         long contentLength = body.contentLength();
/* 262 */         if (contentLength != -1L)
/*     */         {
/*     */ 
/* 265 */           sink.writeUtf8("Content-Length: ").writeDecimalLong(contentLength).write(MultipartBuilder.CRLF);
/* 266 */         } else if (countBytes)
/*     */         {
/* 268 */           byteCountBuffer.clear();
/* 269 */           return -1L;
/*     */         }
/*     */         
/* 272 */         sink.write(MultipartBuilder.CRLF);
/*     */         
/* 274 */         if (countBytes) {
/* 275 */           byteCount += contentLength;
/*     */         } else {
/* 277 */           ((RequestBody)this.partBodies.get(p)).writeTo(sink);
/*     */         }
/*     */         
/* 280 */         sink.write(MultipartBuilder.CRLF);
/*     */       }
/*     */       
/* 283 */       sink.write(MultipartBuilder.DASHDASH);
/* 284 */       sink.write(this.boundary);
/* 285 */       sink.write(MultipartBuilder.DASHDASH);
/* 286 */       sink.write(MultipartBuilder.CRLF);
/*     */       
/* 288 */       if (countBytes) {
/* 289 */         byteCount += byteCountBuffer.size();
/* 290 */         byteCountBuffer.clear();
/*     */       }
/*     */       
/* 293 */       return byteCount;
/*     */     }
/*     */     
/*     */     public void writeTo(BufferedSink sink) throws IOException {
/* 297 */       writeOrCountBytes(sink, false);
/*     */     }
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\MultipartBuilder.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */