/*    */ package com.squareup.okhttp;
/*    */ 
/*    */ import okio.Buffer;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public final class FormEncodingBuilder
/*    */ {
/* 26 */   private static final MediaType CONTENT_TYPE = MediaType.parse("application/x-www-form-urlencoded");
/*    */   
/* 28 */   private final Buffer content = new Buffer();
/*    */   
/*    */   public FormEncodingBuilder add(String name, String value)
/*    */   {
/* 32 */     if (this.content.size() > 0L) {
/* 33 */       this.content.writeByte(38);
/*    */     }
/* 35 */     HttpUrl.canonicalize(this.content, name, 0, name.length(), " \"':;<=>@[]^`{}|/\\?#&!$(),~", false, true, true);
/*    */     
/* 37 */     this.content.writeByte(61);
/* 38 */     HttpUrl.canonicalize(this.content, value, 0, value.length(), " \"':;<=>@[]^`{}|/\\?#&!$(),~", false, true, true);
/*    */     
/* 40 */     return this;
/*    */   }
/*    */   
/*    */   public FormEncodingBuilder addEncoded(String name, String value)
/*    */   {
/* 45 */     if (this.content.size() > 0L) {
/* 46 */       this.content.writeByte(38);
/*    */     }
/* 48 */     HttpUrl.canonicalize(this.content, name, 0, name.length(), " \"':;<=>@[]^`{}|/\\?#&!$(),~", true, true, true);
/*    */     
/* 50 */     this.content.writeByte(61);
/* 51 */     HttpUrl.canonicalize(this.content, value, 0, value.length(), " \"':;<=>@[]^`{}|/\\?#&!$(),~", true, true, true);
/*    */     
/* 53 */     return this;
/*    */   }
/*    */   
/*    */   public RequestBody build() {
/* 57 */     return RequestBody.create(CONTENT_TYPE, this.content.snapshot());
/*    */   }
/*    */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\FormEncodingBuilder.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */