/*     */ package com.squareup.okhttp.internal.http;
/*     */ 
/*     */ import com.squareup.okhttp.CacheControl;
/*     */ import com.squareup.okhttp.Headers;
/*     */ import com.squareup.okhttp.HttpUrl;
/*     */ import com.squareup.okhttp.Request;
/*     */ import com.squareup.okhttp.Request.Builder;
/*     */ import com.squareup.okhttp.Response;
/*     */ import com.squareup.okhttp.Response.Builder;
/*     */ import java.util.Date;
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
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public final class CacheStrategy
/*     */ {
/*     */   public final Request networkRequest;
/*     */   public final Response cacheResponse;
/*     */   
/*     */   private CacheStrategy(Request networkRequest, Response cacheResponse)
/*     */   {
/*  40 */     this.networkRequest = networkRequest;
/*  41 */     this.cacheResponse = cacheResponse;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public static boolean isCacheable(Response response, Request request)
/*     */   {
/*  51 */     switch (response.code())
/*     */     {
/*     */     case 200: 
/*     */     case 203: 
/*     */     case 204: 
/*     */     case 300: 
/*     */     case 301: 
/*     */     case 308: 
/*     */     case 404: 
/*     */     case 405: 
/*     */     case 410: 
/*     */     case 414: 
/*     */     case 501: 
/*     */       break;
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */     case 302: 
/*     */     case 307: 
/*  71 */       if ((response.header("Expires") != null) || 
/*  72 */         (response.cacheControl().maxAgeSeconds() != -1) || 
/*  73 */         (response.cacheControl().isPublic()) || 
/*  74 */         (response.cacheControl().isPrivate())) {
/*     */         break;
/*     */       }
/*     */     
/*     */ 
/*     */ 
/*     */     default: 
/*  81 */       return false;
/*     */     }
/*     */     
/*     */     
/*  85 */     return (!response.cacheControl().noStore()) && (!request.cacheControl().noStore());
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public static class Factory
/*     */   {
/*     */     final long nowMillis;
/*     */     
/*     */ 
/*     */     final Request request;
/*     */     
/*     */ 
/*     */     final Response cacheResponse;
/*     */     
/*     */ 
/*     */     private Date servedDate;
/*     */     
/*     */ 
/*     */     private String servedDateString;
/*     */     
/*     */ 
/*     */     private Date lastModified;
/*     */     
/*     */ 
/*     */     private String lastModifiedString;
/*     */     
/*     */ 
/*     */     private Date expires;
/*     */     
/*     */ 
/*     */     private long sentRequestMillis;
/*     */     
/*     */ 
/*     */     private long receivedResponseMillis;
/*     */     
/*     */     private String etag;
/*     */     
/* 123 */     private int ageSeconds = -1;
/*     */     
/*     */     public Factory(long nowMillis, Request request, Response cacheResponse) {
/* 126 */       this.nowMillis = nowMillis;
/* 127 */       this.request = request;
/* 128 */       this.cacheResponse = cacheResponse;
/*     */       
/* 130 */       if (cacheResponse != null) {
/* 131 */         Headers headers = cacheResponse.headers();
/* 132 */         int i = 0; for (int size = headers.size(); i < size; i++) {
/* 133 */           String fieldName = headers.name(i);
/* 134 */           String value = headers.value(i);
/* 135 */           if ("Date".equalsIgnoreCase(fieldName)) {
/* 136 */             this.servedDate = HttpDate.parse(value);
/* 137 */             this.servedDateString = value;
/* 138 */           } else if ("Expires".equalsIgnoreCase(fieldName)) {
/* 139 */             this.expires = HttpDate.parse(value);
/* 140 */           } else if ("Last-Modified".equalsIgnoreCase(fieldName)) {
/* 141 */             this.lastModified = HttpDate.parse(value);
/* 142 */             this.lastModifiedString = value;
/* 143 */           } else if ("ETag".equalsIgnoreCase(fieldName)) {
/* 144 */             this.etag = value;
/* 145 */           } else if ("Age".equalsIgnoreCase(fieldName)) {
/* 146 */             this.ageSeconds = HeaderParser.parseSeconds(value, -1);
/* 147 */           } else if (OkHeaders.SENT_MILLIS.equalsIgnoreCase(fieldName)) {
/* 148 */             this.sentRequestMillis = Long.parseLong(value);
/* 149 */           } else if (OkHeaders.RECEIVED_MILLIS.equalsIgnoreCase(fieldName)) {
/* 150 */             this.receivedResponseMillis = Long.parseLong(value);
/*     */           }
/*     */         }
/*     */       }
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */     public CacheStrategy get()
/*     */     {
/* 161 */       CacheStrategy candidate = getCandidate();
/*     */       
/* 163 */       if ((candidate.networkRequest != null) && (this.request.cacheControl().onlyIfCached()))
/*     */       {
/* 165 */         return new CacheStrategy(null, null, null);
/*     */       }
/*     */       
/* 168 */       return candidate;
/*     */     }
/*     */     
/*     */ 
/*     */     private CacheStrategy getCandidate()
/*     */     {
/* 174 */       if (this.cacheResponse == null) {
/* 175 */         return new CacheStrategy(this.request, null, null);
/*     */       }
/*     */       
/*     */ 
/* 179 */       if ((this.request.isHttps()) && (this.cacheResponse.handshake() == null)) {
/* 180 */         return new CacheStrategy(this.request, null, null);
/*     */       }
/*     */       
/*     */ 
/*     */ 
/*     */ 
/* 186 */       if (!CacheStrategy.isCacheable(this.cacheResponse, this.request)) {
/* 187 */         return new CacheStrategy(this.request, null, null);
/*     */       }
/*     */       
/* 190 */       CacheControl requestCaching = this.request.cacheControl();
/* 191 */       if ((requestCaching.noCache()) || (hasConditions(this.request))) {
/* 192 */         return new CacheStrategy(this.request, null, null);
/*     */       }
/*     */       
/* 195 */       long ageMillis = cacheResponseAge();
/* 196 */       long freshMillis = computeFreshnessLifetime();
/*     */       
/* 198 */       if (requestCaching.maxAgeSeconds() != -1) {
/* 199 */         freshMillis = Math.min(freshMillis, TimeUnit.SECONDS.toMillis(requestCaching.maxAgeSeconds()));
/*     */       }
/*     */       
/* 202 */       long minFreshMillis = 0L;
/* 203 */       if (requestCaching.minFreshSeconds() != -1) {
/* 204 */         minFreshMillis = TimeUnit.SECONDS.toMillis(requestCaching.minFreshSeconds());
/*     */       }
/*     */       
/* 207 */       long maxStaleMillis = 0L;
/* 208 */       CacheControl responseCaching = this.cacheResponse.cacheControl();
/* 209 */       if ((!responseCaching.mustRevalidate()) && (requestCaching.maxStaleSeconds() != -1)) {
/* 210 */         maxStaleMillis = TimeUnit.SECONDS.toMillis(requestCaching.maxStaleSeconds());
/*     */       }
/*     */       
/* 213 */       if ((!responseCaching.noCache()) && (ageMillis + minFreshMillis < freshMillis + maxStaleMillis)) {
/* 214 */         Response.Builder builder = this.cacheResponse.newBuilder();
/* 215 */         if (ageMillis + minFreshMillis >= freshMillis) {
/* 216 */           builder.addHeader("Warning", "110 HttpURLConnection \"Response is stale\"");
/*     */         }
/* 218 */         long oneDayMillis = 86400000L;
/* 219 */         if ((ageMillis > oneDayMillis) && (isFreshnessLifetimeHeuristic())) {
/* 220 */           builder.addHeader("Warning", "113 HttpURLConnection \"Heuristic expiration\"");
/*     */         }
/* 222 */         return new CacheStrategy(null, builder.build(), null);
/*     */       }
/*     */       
/* 225 */       Request.Builder conditionalRequestBuilder = this.request.newBuilder();
/*     */       
/* 227 */       if (this.etag != null) {
/* 228 */         conditionalRequestBuilder.header("If-None-Match", this.etag);
/* 229 */       } else if (this.lastModified != null) {
/* 230 */         conditionalRequestBuilder.header("If-Modified-Since", this.lastModifiedString);
/* 231 */       } else if (this.servedDate != null) {
/* 232 */         conditionalRequestBuilder.header("If-Modified-Since", this.servedDateString);
/*     */       }
/*     */       
/* 235 */       Request conditionalRequest = conditionalRequestBuilder.build();
/* 236 */       return hasConditions(conditionalRequest) ? new CacheStrategy(conditionalRequest, this.cacheResponse, null) : new CacheStrategy(conditionalRequest, null, null);
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     private long computeFreshnessLifetime()
/*     */     {
/* 246 */       CacheControl responseCaching = this.cacheResponse.cacheControl();
/* 247 */       if (responseCaching.maxAgeSeconds() != -1)
/* 248 */         return TimeUnit.SECONDS.toMillis(responseCaching.maxAgeSeconds());
/* 249 */       if (this.expires != null)
/*     */       {
/* 251 */         long servedMillis = this.servedDate != null ? this.servedDate.getTime() : this.receivedResponseMillis;
/*     */         
/* 253 */         long delta = this.expires.getTime() - servedMillis;
/* 254 */         return delta > 0L ? delta : 0L; }
/* 255 */       if ((this.lastModified != null) && 
/* 256 */         (this.cacheResponse.request().httpUrl().query() == null))
/*     */       {
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 262 */         long servedMillis = this.servedDate != null ? this.servedDate.getTime() : this.sentRequestMillis;
/*     */         
/* 264 */         long delta = servedMillis - this.lastModified.getTime();
/* 265 */         return delta > 0L ? delta / 10L : 0L;
/*     */       }
/* 267 */       return 0L;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     private long cacheResponseAge()
/*     */     {
/* 276 */       long apparentReceivedAge = this.servedDate != null ? Math.max(0L, this.receivedResponseMillis - this.servedDate.getTime()) : 0L;
/*     */       
/*     */ 
/* 279 */       long receivedAge = this.ageSeconds != -1 ? Math.max(apparentReceivedAge, TimeUnit.SECONDS.toMillis(this.ageSeconds)) : apparentReceivedAge;
/*     */       
/* 281 */       long responseDuration = this.receivedResponseMillis - this.sentRequestMillis;
/* 282 */       long residentDuration = this.nowMillis - this.receivedResponseMillis;
/* 283 */       return receivedAge + responseDuration + residentDuration;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     private boolean isFreshnessLifetimeHeuristic()
/*     */     {
/* 292 */       return (this.cacheResponse.cacheControl().maxAgeSeconds() == -1) && (this.expires == null);
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     private static boolean hasConditions(Request request)
/*     */     {
/* 301 */       return (request.header("If-Modified-Since") != null) || (request.header("If-None-Match") != null);
/*     */     }
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\http\CacheStrategy.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */