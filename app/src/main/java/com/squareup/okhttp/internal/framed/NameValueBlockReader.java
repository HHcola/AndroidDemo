/*     */ package com.squareup.okhttp.internal.framed;
/*     */ 
/*     */ import java.io.IOException;
/*     */ import java.util.ArrayList;
/*     */ import java.util.List;
/*     */ import java.util.zip.DataFormatException;
/*     */ import java.util.zip.Inflater;
/*     */ import okio.Buffer;
/*     */ import okio.BufferedSource;
/*     */ import okio.ByteString;
/*     */ import okio.ForwardingSource;
/*     */ import okio.InflaterSource;
/*     */ import okio.Okio;
/*     */ import okio.Source;
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
/*     */ class NameValueBlockReader
/*     */ {
/*     */   private final InflaterSource inflaterSource;
/*     */   private int compressedLimit;
/*     */   private final BufferedSource source;
/*     */   
/*     */   public NameValueBlockReader(BufferedSource source)
/*     */   {
/*  54 */     Source throttleSource = new ForwardingSource(source) {
/*     */       public long read(Buffer sink, long byteCount) throws IOException {
/*  56 */         if (NameValueBlockReader.this.compressedLimit == 0) return -1L;
/*  57 */         long read = super.read(sink, Math.min(byteCount, NameValueBlockReader.this.compressedLimit));
/*  58 */         if (read == -1L) return -1L;
/*  59 */         NameValueBlockReader.this.compressedLimit = ((int)(NameValueBlockReader.this.compressedLimit - read));
/*  60 */         return read;
/*     */       }
/*     */       
/*     */ 
/*  64 */     };
/*  65 */     Inflater inflater = new Inflater()
/*     */     {
/*     */       public int inflate(byte[] buffer, int offset, int count) throws DataFormatException {
/*  68 */         int result = super.inflate(buffer, offset, count);
/*  69 */         if ((result == 0) && (needsDictionary())) {
/*  70 */           setDictionary(Spdy3.DICTIONARY);
/*  71 */           result = super.inflate(buffer, offset, count);
/*     */         }
/*  73 */         return result;
/*     */       }
/*     */       
/*  76 */     };
/*  77 */     this.inflaterSource = new InflaterSource(throttleSource, inflater);
/*  78 */     this.source = Okio.buffer(this.inflaterSource);
/*     */   }
/*     */   
/*     */   public List<Header> readNameValueBlock(int length) throws IOException {
/*  82 */     this.compressedLimit += length;
/*     */     
/*  84 */     int numberOfPairs = this.source.readInt();
/*  85 */     if (numberOfPairs < 0) throw new IOException("numberOfPairs < 0: " + numberOfPairs);
/*  86 */     if (numberOfPairs > 1024) { throw new IOException("numberOfPairs > 1024: " + numberOfPairs);
/*     */     }
/*  88 */     List<Header> entries = new ArrayList(numberOfPairs);
/*  89 */     for (int i = 0; i < numberOfPairs; i++) {
/*  90 */       ByteString name = readByteString().toAsciiLowercase();
/*  91 */       ByteString values = readByteString();
/*  92 */       if (name.size() == 0) throw new IOException("name.size == 0");
/*  93 */       entries.add(new Header(name, values));
/*     */     }
/*     */     
/*  96 */     doneReading();
/*  97 */     return entries;
/*     */   }
/*     */   
/*     */   private ByteString readByteString() throws IOException {
/* 101 */     int length = this.source.readInt();
/* 102 */     return this.source.readByteString(length);
/*     */   }
/*     */   
/*     */ 
/*     */   private void doneReading()
/*     */     throws IOException
/*     */   {
/* 109 */     if (this.compressedLimit > 0) {
/* 110 */       this.inflaterSource.refill();
/* 111 */       if (this.compressedLimit != 0) throw new IOException("compressedLimit > 0: " + this.compressedLimit);
/*     */     }
/*     */   }
/*     */   
/*     */   public void close() throws IOException {
/* 116 */     this.source.close();
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\framed\NameValueBlockReader.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */