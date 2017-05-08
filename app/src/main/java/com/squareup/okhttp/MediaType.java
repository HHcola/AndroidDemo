/*     */ package com.squareup.okhttp;
/*     */ 
/*     */ import java.nio.charset.Charset;
/*     */ import java.util.Locale;
/*     */ import java.util.regex.Matcher;
/*     */ import java.util.regex.Pattern;
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
/*     */ public final class MediaType
/*     */ {
/*     */   private static final String TOKEN = "([a-zA-Z0-9-!#$%&'*+.^_`{|}~]+)";
/*     */   private static final String QUOTED = "\"([^\"]*)\"";
/*  30 */   private static final Pattern TYPE_SUBTYPE = Pattern.compile("([a-zA-Z0-9-!#$%&'*+.^_`{|}~]+)/([a-zA-Z0-9-!#$%&'*+.^_`{|}~]+)");
/*  31 */   private static final Pattern PARAMETER = Pattern.compile(";\\s*(?:([a-zA-Z0-9-!#$%&'*+.^_`{|}~]+)=(?:([a-zA-Z0-9-!#$%&'*+.^_`{|}~]+)|\"([^\"]*)\"))?");
/*     */   
/*     */   private final String mediaType;
/*     */   private final String type;
/*     */   private final String subtype;
/*     */   private final String charset;
/*     */   
/*     */   private MediaType(String mediaType, String type, String subtype, String charset)
/*     */   {
/*  40 */     this.mediaType = mediaType;
/*  41 */     this.type = type;
/*  42 */     this.subtype = subtype;
/*  43 */     this.charset = charset;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public static MediaType parse(String string)
/*     */   {
/*  51 */     Matcher typeSubtype = TYPE_SUBTYPE.matcher(string);
/*  52 */     if (!typeSubtype.lookingAt()) return null;
/*  53 */     String type = typeSubtype.group(1).toLowerCase(Locale.US);
/*  54 */     String subtype = typeSubtype.group(2).toLowerCase(Locale.US);
/*     */     
/*  56 */     String charset = null;
/*  57 */     Matcher parameter = PARAMETER.matcher(string);
/*  58 */     for (int s = typeSubtype.end(); s < string.length(); s = parameter.end()) {
/*  59 */       parameter.region(s, string.length());
/*  60 */       if (!parameter.lookingAt()) { return null;
/*     */       }
/*  62 */       String name = parameter.group(1);
/*  63 */       if ((name != null) && (name.equalsIgnoreCase("charset")))
/*     */       {
/*     */ 
/*  66 */         String charsetParameter = parameter.group(2) != null ? parameter.group(2) : parameter.group(3);
/*  67 */         if ((charset != null) && (!charsetParameter.equalsIgnoreCase(charset))) {
/*  68 */           throw new IllegalArgumentException("Multiple different charsets: " + string);
/*     */         }
/*  70 */         charset = charsetParameter;
/*     */       }
/*     */     }
/*  73 */     return new MediaType(string, type, subtype, charset);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public String type()
/*     */   {
/*  81 */     return this.type;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public String subtype()
/*     */   {
/*  89 */     return this.subtype;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public Charset charset()
/*     */   {
/*  97 */     return this.charset != null ? Charset.forName(this.charset) : null;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public Charset charset(Charset defaultValue)
/*     */   {
/* 105 */     return this.charset != null ? Charset.forName(this.charset) : defaultValue;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public String toString()
/*     */   {
/* 113 */     return this.mediaType;
/*     */   }
/*     */   
/*     */   public boolean equals(Object o) {
/* 117 */     return ((o instanceof MediaType)) && (((MediaType)o).mediaType.equals(this.mediaType));
/*     */   }
/*     */   
/*     */   public int hashCode() {
/* 121 */     return this.mediaType.hashCode();
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\MediaType.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */