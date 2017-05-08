/*    */ package com.squareup.okhttp;
/*    */ 
/*    */ import com.squareup.okhttp.internal.Util;
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
/*    */ public final class Challenge
/*    */ {
/*    */   private final String scheme;
/*    */   private final String realm;
/*    */   
/*    */   public Challenge(String scheme, String realm)
/*    */   {
/* 26 */     this.scheme = scheme;
/* 27 */     this.realm = realm;
/*    */   }
/*    */   
/*    */   public String getScheme()
/*    */   {
/* 32 */     return this.scheme;
/*    */   }
/*    */   
/*    */   public String getRealm()
/*    */   {
/* 37 */     return this.realm;
/*    */   }
/*    */   
/*    */ 
/*    */   public boolean equals(Object o)
/*    */   {
/* 43 */     return ((o instanceof Challenge)) && (Util.equal(this.scheme, ((Challenge)o).scheme)) && (Util.equal(this.realm, ((Challenge)o).realm));
/*    */   }
/*    */   
/*    */   public int hashCode() {
/* 47 */     int result = 29;
/* 48 */     result = 31 * result + (this.realm != null ? this.realm.hashCode() : 0);
/* 49 */     result = 31 * result + (this.scheme != null ? this.scheme.hashCode() : 0);
/* 50 */     return result;
/*    */   }
/*    */   
/*    */   public String toString() {
/* 54 */     return this.scheme + " realm=\"" + this.realm + "\"";
/*    */   }
/*    */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\Challenge.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */