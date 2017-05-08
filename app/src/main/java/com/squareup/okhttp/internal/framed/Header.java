/*    */ package com.squareup.okhttp.internal.framed;
/*    */ 
/*    */ import okio.ByteString;
/*    */ 
/*    */ 
/*    */ public final class Header
/*    */ {
/*  8 */   public static final ByteString RESPONSE_STATUS = ByteString.encodeUtf8(":status");
/*  9 */   public static final ByteString TARGET_METHOD = ByteString.encodeUtf8(":method");
/* 10 */   public static final ByteString TARGET_PATH = ByteString.encodeUtf8(":path");
/* 11 */   public static final ByteString TARGET_SCHEME = ByteString.encodeUtf8(":scheme");
/* 12 */   public static final ByteString TARGET_AUTHORITY = ByteString.encodeUtf8(":authority");
/* 13 */   public static final ByteString TARGET_HOST = ByteString.encodeUtf8(":host");
/* 14 */   public static final ByteString VERSION = ByteString.encodeUtf8(":version");
/*    */   
/*    */   public final ByteString name;
/*    */   
/*    */   public final ByteString value;
/*    */   
/*    */   final int hpackSize;
/*    */   
/*    */   public Header(String name, String value)
/*    */   {
/* 24 */     this(ByteString.encodeUtf8(name), ByteString.encodeUtf8(value));
/*    */   }
/*    */   
/*    */   public Header(ByteString name, String value) {
/* 28 */     this(name, ByteString.encodeUtf8(value));
/*    */   }
/*    */   
/*    */   public Header(ByteString name, ByteString value) {
/* 32 */     this.name = name;
/* 33 */     this.value = value;
/* 34 */     this.hpackSize = (32 + name.size() + value.size());
/*    */   }
/*    */   
/*    */   public boolean equals(Object other) {
/* 38 */     if ((other instanceof Header)) {
/* 39 */       Header that = (Header)other;
/*    */       
/* 41 */       return (this.name.equals(that.name)) && (this.value.equals(that.value));
/*    */     }
/* 43 */     return false;
/*    */   }
/*    */   
/*    */   public int hashCode() {
/* 47 */     int result = 17;
/* 48 */     result = 31 * result + this.name.hashCode();
/* 49 */     result = 31 * result + this.value.hashCode();
/* 50 */     return result;
/*    */   }
/*    */   
/*    */   public String toString() {
/* 54 */     return String.format("%s: %s", new Object[] { this.name.utf8(), this.value.utf8() });
/*    */   }
/*    */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\framed\Header.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */