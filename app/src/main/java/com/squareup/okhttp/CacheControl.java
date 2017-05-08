/*     */ package com.squareup.okhttp;
/*     */ 
/*     */ import com.squareup.okhttp.internal.http.HeaderParser;
/*     */ import java.util.concurrent.TimeUnit;
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
/*     */ public final class CacheControl
/*     */ {
/*  20 */   public static final CacheControl FORCE_NETWORK = new Builder().noCache().build();
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*  28 */   public static final CacheControl FORCE_CACHE = new Builder()
/*  29 */     .onlyIfCached()
/*  30 */     .maxStale(Integer.MAX_VALUE, TimeUnit.SECONDS)
/*  31 */     .build();
/*     */   
/*     */   private final boolean noCache;
/*     */   
/*     */   private final boolean noStore;
/*     */   
/*     */   private final int maxAgeSeconds;
/*     */   private final int sMaxAgeSeconds;
/*     */   private final boolean isPrivate;
/*     */   private final boolean isPublic;
/*     */   private final boolean mustRevalidate;
/*     */   private final int maxStaleSeconds;
/*     */   private final int minFreshSeconds;
/*     */   private final boolean onlyIfCached;
/*     */   private final boolean noTransform;
/*     */   String headerValue;
/*     */   
/*     */   private CacheControl(boolean noCache, boolean noStore, int maxAgeSeconds, int sMaxAgeSeconds, boolean isPrivate, boolean isPublic, boolean mustRevalidate, int maxStaleSeconds, int minFreshSeconds, boolean onlyIfCached, boolean noTransform, String headerValue)
/*     */   {
/*  50 */     this.noCache = noCache;
/*  51 */     this.noStore = noStore;
/*  52 */     this.maxAgeSeconds = maxAgeSeconds;
/*  53 */     this.sMaxAgeSeconds = sMaxAgeSeconds;
/*  54 */     this.isPrivate = isPrivate;
/*  55 */     this.isPublic = isPublic;
/*  56 */     this.mustRevalidate = mustRevalidate;
/*  57 */     this.maxStaleSeconds = maxStaleSeconds;
/*  58 */     this.minFreshSeconds = minFreshSeconds;
/*  59 */     this.onlyIfCached = onlyIfCached;
/*  60 */     this.noTransform = noTransform;
/*  61 */     this.headerValue = headerValue;
/*     */   }
/*     */   
/*     */   private CacheControl(Builder builder) {
/*  65 */     this.noCache = builder.noCache;
/*  66 */     this.noStore = builder.noStore;
/*  67 */     this.maxAgeSeconds = builder.maxAgeSeconds;
/*  68 */     this.sMaxAgeSeconds = -1;
/*  69 */     this.isPrivate = false;
/*  70 */     this.isPublic = false;
/*  71 */     this.mustRevalidate = false;
/*  72 */     this.maxStaleSeconds = builder.maxStaleSeconds;
/*  73 */     this.minFreshSeconds = builder.minFreshSeconds;
/*  74 */     this.onlyIfCached = builder.onlyIfCached;
/*  75 */     this.noTransform = builder.noTransform;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public boolean noCache()
/*     */   {
/*  87 */     return this.noCache;
/*     */   }
/*     */   
/*     */   public boolean noStore()
/*     */   {
/*  92 */     return this.noStore;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public int maxAgeSeconds()
/*     */   {
/* 100 */     return this.maxAgeSeconds;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public int sMaxAgeSeconds()
/*     */   {
/* 109 */     return this.sMaxAgeSeconds;
/*     */   }
/*     */   
/*     */   public boolean isPrivate() {
/* 113 */     return this.isPrivate;
/*     */   }
/*     */   
/*     */   public boolean isPublic() {
/* 117 */     return this.isPublic;
/*     */   }
/*     */   
/*     */   public boolean mustRevalidate() {
/* 121 */     return this.mustRevalidate;
/*     */   }
/*     */   
/*     */   public int maxStaleSeconds() {
/* 125 */     return this.maxStaleSeconds;
/*     */   }
/*     */   
/*     */   public int minFreshSeconds() {
/* 129 */     return this.minFreshSeconds;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public boolean onlyIfCached()
/*     */   {
/* 140 */     return this.onlyIfCached;
/*     */   }
/*     */   
/*     */   public boolean noTransform() {
/* 144 */     return this.noTransform;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public static CacheControl parse(Headers headers)
/*     */   {
/* 152 */     boolean noCache = false;
/* 153 */     boolean noStore = false;
/* 154 */     int maxAgeSeconds = -1;
/* 155 */     int sMaxAgeSeconds = -1;
/* 156 */     boolean isPrivate = false;
/* 157 */     boolean isPublic = false;
/* 158 */     boolean mustRevalidate = false;
/* 159 */     int maxStaleSeconds = -1;
/* 160 */     int minFreshSeconds = -1;
/* 161 */     boolean onlyIfCached = false;
/* 162 */     boolean noTransform = false;
/*     */     
/* 164 */     boolean canUseHeaderValue = true;
/* 165 */     String headerValue = null;
/*     */     
/* 167 */     int i = 0; for (int size = headers.size(); i < size; i++) {
/* 168 */       String name = headers.name(i);
/* 169 */       String value = headers.value(i);
/*     */       
/* 171 */       if (name.equalsIgnoreCase("Cache-Control")) {
/* 172 */         if (headerValue != null)
/*     */         {
/* 174 */           canUseHeaderValue = false;
/*     */         } else
/* 176 */           headerValue = value;
/*     */       } else {
/* 178 */         if (!name.equalsIgnoreCase("Pragma"))
/*     */           continue;
/* 180 */         canUseHeaderValue = false;
/*     */       }
/*     */       
/*     */ 
/*     */ 
/* 185 */       int pos = 0;
/* 186 */       while (pos < value.length()) {
/* 187 */         int tokenStart = pos;
/* 188 */         pos = HeaderParser.skipUntil(value, pos, "=,;");
/* 189 */         String directive = value.substring(tokenStart, pos).trim();
/*     */         String parameter;
/*     */         String parameter;
/* 192 */         if ((pos == value.length()) || (value.charAt(pos) == ',') || (value.charAt(pos) == ';')) {
/* 193 */           pos++;
/* 194 */           parameter = null;
/*     */         } else {
/* 196 */           pos++;
/* 197 */           pos = HeaderParser.skipWhitespace(value, pos);
/*     */           
/*     */ 
/* 200 */           if ((pos < value.length()) && (value.charAt(pos) == '"')) {
/* 201 */             pos++;
/* 202 */             int parameterStart = pos;
/* 203 */             pos = HeaderParser.skipUntil(value, pos, "\"");
/* 204 */             String parameter = value.substring(parameterStart, pos);
/* 205 */             pos++;
/*     */           }
/*     */           else
/*     */           {
/* 209 */             int parameterStart = pos;
/* 210 */             pos = HeaderParser.skipUntil(value, pos, ",;");
/* 211 */             parameter = value.substring(parameterStart, pos).trim();
/*     */           }
/*     */         }
/*     */         
/* 215 */         if ("no-cache".equalsIgnoreCase(directive)) {
/* 216 */           noCache = true;
/* 217 */         } else if ("no-store".equalsIgnoreCase(directive)) {
/* 218 */           noStore = true;
/* 219 */         } else if ("max-age".equalsIgnoreCase(directive)) {
/* 220 */           maxAgeSeconds = HeaderParser.parseSeconds(parameter, -1);
/* 221 */         } else if ("s-maxage".equalsIgnoreCase(directive)) {
/* 222 */           sMaxAgeSeconds = HeaderParser.parseSeconds(parameter, -1);
/* 223 */         } else if ("private".equalsIgnoreCase(directive)) {
/* 224 */           isPrivate = true;
/* 225 */         } else if ("public".equalsIgnoreCase(directive)) {
/* 226 */           isPublic = true;
/* 227 */         } else if ("must-revalidate".equalsIgnoreCase(directive)) {
/* 228 */           mustRevalidate = true;
/* 229 */         } else if ("max-stale".equalsIgnoreCase(directive)) {
/* 230 */           maxStaleSeconds = HeaderParser.parseSeconds(parameter, Integer.MAX_VALUE);
/* 231 */         } else if ("min-fresh".equalsIgnoreCase(directive)) {
/* 232 */           minFreshSeconds = HeaderParser.parseSeconds(parameter, -1);
/* 233 */         } else if ("only-if-cached".equalsIgnoreCase(directive)) {
/* 234 */           onlyIfCached = true;
/* 235 */         } else if ("no-transform".equalsIgnoreCase(directive)) {
/* 236 */           noTransform = true;
/*     */         }
/*     */       }
/*     */     }
/*     */     
/* 241 */     if (!canUseHeaderValue) {
/* 242 */       headerValue = null;
/*     */     }
/* 244 */     return new CacheControl(noCache, noStore, maxAgeSeconds, sMaxAgeSeconds, isPrivate, isPublic, mustRevalidate, maxStaleSeconds, minFreshSeconds, onlyIfCached, noTransform, headerValue);
/*     */   }
/*     */   
/*     */   public String toString()
/*     */   {
/* 249 */     String result = this.headerValue;
/* 250 */     return result != null ? result : (this.headerValue = headerValue());
/*     */   }
/*     */   
/*     */   private String headerValue() {
/* 254 */     StringBuilder result = new StringBuilder();
/* 255 */     if (this.noCache) result.append("no-cache, ");
/* 256 */     if (this.noStore) result.append("no-store, ");
/* 257 */     if (this.maxAgeSeconds != -1) result.append("max-age=").append(this.maxAgeSeconds).append(", ");
/* 258 */     if (this.sMaxAgeSeconds != -1) result.append("s-maxage=").append(this.sMaxAgeSeconds).append(", ");
/* 259 */     if (this.isPrivate) result.append("private, ");
/* 260 */     if (this.isPublic) result.append("public, ");
/* 261 */     if (this.mustRevalidate) result.append("must-revalidate, ");
/* 262 */     if (this.maxStaleSeconds != -1) result.append("max-stale=").append(this.maxStaleSeconds).append(", ");
/* 263 */     if (this.minFreshSeconds != -1) result.append("min-fresh=").append(this.minFreshSeconds).append(", ");
/* 264 */     if (this.onlyIfCached) result.append("only-if-cached, ");
/* 265 */     if (this.noTransform) result.append("no-transform, ");
/* 266 */     if (result.length() == 0) return "";
/* 267 */     result.delete(result.length() - 2, result.length());
/* 268 */     return result.toString();
/*     */   }
/*     */   
/*     */   public static final class Builder
/*     */   {
/*     */     boolean noCache;
/*     */     boolean noStore;
/* 275 */     int maxAgeSeconds = -1;
/* 276 */     int maxStaleSeconds = -1;
/* 277 */     int minFreshSeconds = -1;
/*     */     boolean onlyIfCached;
/*     */     boolean noTransform;
/*     */     
/*     */     public Builder noCache()
/*     */     {
/* 283 */       this.noCache = true;
/* 284 */       return this;
/*     */     }
/*     */     
/*     */     public Builder noStore()
/*     */     {
/* 289 */       this.noStore = true;
/* 290 */       return this;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     public Builder maxAge(int maxAge, TimeUnit timeUnit)
/*     */     {
/* 302 */       if (maxAge < 0) throw new IllegalArgumentException("maxAge < 0: " + maxAge);
/* 303 */       long maxAgeSecondsLong = timeUnit.toSeconds(maxAge);
/* 304 */       this.maxAgeSeconds = (maxAgeSecondsLong > 2147483647L ? Integer.MAX_VALUE : (int)maxAgeSecondsLong);
/*     */       
/*     */ 
/* 307 */       return this;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     public Builder maxStale(int maxStale, TimeUnit timeUnit)
/*     */     {
/* 320 */       if (maxStale < 0) throw new IllegalArgumentException("maxStale < 0: " + maxStale);
/* 321 */       long maxStaleSecondsLong = timeUnit.toSeconds(maxStale);
/* 322 */       this.maxStaleSeconds = (maxStaleSecondsLong > 2147483647L ? Integer.MAX_VALUE : (int)maxStaleSecondsLong);
/*     */       
/*     */ 
/* 325 */       return this;
/*     */     }
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
/*     */     public Builder minFresh(int minFresh, TimeUnit timeUnit)
/*     */     {
/* 339 */       if (minFresh < 0) throw new IllegalArgumentException("minFresh < 0: " + minFresh);
/* 340 */       long minFreshSecondsLong = timeUnit.toSeconds(minFresh);
/* 341 */       this.minFreshSeconds = (minFreshSecondsLong > 2147483647L ? Integer.MAX_VALUE : (int)minFreshSecondsLong);
/*     */       
/*     */ 
/* 344 */       return this;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */     public Builder onlyIfCached()
/*     */     {
/* 352 */       this.onlyIfCached = true;
/* 353 */       return this;
/*     */     }
/*     */     
/*     */     public Builder noTransform()
/*     */     {
/* 358 */       this.noTransform = true;
/* 359 */       return this;
/*     */     }
/*     */     
/*     */     public CacheControl build() {
/* 363 */       return new CacheControl(this, null);
/*     */     }
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\CacheControl.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */