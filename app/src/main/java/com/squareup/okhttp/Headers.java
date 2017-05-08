/*     */ package com.squareup.okhttp;
/*     */ 
/*     */ import com.squareup.okhttp.internal.http.HttpDate;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Collections;
/*     */ import java.util.Date;
/*     */ import java.util.LinkedHashMap;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ import java.util.Map.Entry;
/*     */ import java.util.Set;
/*     */ import java.util.TreeSet;
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
/*     */ 
/*     */ 
/*     */ public final class Headers
/*     */ {
/*     */   private final String[] namesAndValues;
/*     */   
/*     */   private Headers(Builder builder)
/*     */   {
/*  52 */     this.namesAndValues = ((String[])builder.namesAndValues.toArray(new String[builder.namesAndValues.size()]));
/*     */   }
/*     */   
/*     */   private Headers(String[] namesAndValues) {
/*  56 */     this.namesAndValues = namesAndValues;
/*     */   }
/*     */   
/*     */   public String get(String name)
/*     */   {
/*  61 */     return get(this.namesAndValues, name);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Date getDate(String name)
/*     */   {
/*  70 */     String value = get(name);
/*  71 */     return value != null ? HttpDate.parse(value) : null;
/*     */   }
/*     */   
/*     */   public int size()
/*     */   {
/*  76 */     return this.namesAndValues.length / 2;
/*     */   }
/*     */   
/*     */   public String name(int index)
/*     */   {
/*  81 */     int nameIndex = index * 2;
/*  82 */     if ((nameIndex < 0) || (nameIndex >= this.namesAndValues.length)) {
/*  83 */       return null;
/*     */     }
/*  85 */     return this.namesAndValues[nameIndex];
/*     */   }
/*     */   
/*     */   public String value(int index)
/*     */   {
/*  90 */     int valueIndex = index * 2 + 1;
/*  91 */     if ((valueIndex < 0) || (valueIndex >= this.namesAndValues.length)) {
/*  92 */       return null;
/*     */     }
/*  94 */     return this.namesAndValues[valueIndex];
/*     */   }
/*     */   
/*     */   public Set<String> names()
/*     */   {
/*  99 */     TreeSet<String> result = new TreeSet(String.CASE_INSENSITIVE_ORDER);
/* 100 */     int i = 0; for (int size = size(); i < size; i++) {
/* 101 */       result.add(name(i));
/*     */     }
/* 103 */     return Collections.unmodifiableSet(result);
/*     */   }
/*     */   
/*     */   public List<String> values(String name)
/*     */   {
/* 108 */     List<String> result = null;
/* 109 */     int i = 0; for (int size = size(); i < size; i++) {
/* 110 */       if (name.equalsIgnoreCase(name(i))) {
/* 111 */         if (result == null) result = new ArrayList(2);
/* 112 */         result.add(value(i));
/*     */       }
/*     */     }
/*     */     
/*     */ 
/* 117 */     return result != null ? Collections.unmodifiableList(result) : Collections.emptyList();
/*     */   }
/*     */   
/*     */   public Builder newBuilder() {
/* 121 */     Builder result = new Builder();
/* 122 */     Collections.addAll(result.namesAndValues, this.namesAndValues);
/* 123 */     return result;
/*     */   }
/*     */   
/*     */   public String toString() {
/* 127 */     StringBuilder result = new StringBuilder();
/* 128 */     int i = 0; for (int size = size(); i < size; i++) {
/* 129 */       result.append(name(i)).append(": ").append(value(i)).append("\n");
/*     */     }
/* 131 */     return result.toString();
/*     */   }
/*     */   
/*     */   public Map<String, List<String>> toMultimap() {
/* 135 */     Map<String, List<String>> result = new LinkedHashMap();
/* 136 */     int i = 0; for (int size = size(); i < size; i++) {
/* 137 */       String name = name(i);
/* 138 */       List<String> values = (List)result.get(name);
/* 139 */       if (values == null) {
/* 140 */         values = new ArrayList(2);
/* 141 */         result.put(name, values);
/*     */       }
/* 143 */       values.add(value(i));
/*     */     }
/* 145 */     return result;
/*     */   }
/*     */   
/*     */   private static String get(String[] namesAndValues, String name) {
/* 149 */     for (int i = namesAndValues.length - 2; i >= 0; i -= 2) {
/* 150 */       if (name.equalsIgnoreCase(namesAndValues[i])) {
/* 151 */         return namesAndValues[(i + 1)];
/*     */       }
/*     */     }
/* 154 */     return null;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public static Headers of(String... namesAndValues)
/*     */   {
/* 163 */     if ((namesAndValues == null) || (namesAndValues.length % 2 != 0)) {
/* 164 */       throw new IllegalArgumentException("Expected alternating header names and values");
/*     */     }
/*     */     
/*     */ 
/* 168 */     namesAndValues = (String[])namesAndValues.clone();
/* 169 */     for (int i = 0; i < namesAndValues.length; i++) {
/* 170 */       if (namesAndValues[i] == null) throw new IllegalArgumentException("Headers cannot be null");
/* 171 */       namesAndValues[i] = namesAndValues[i].trim();
/*     */     }
/*     */     
/*     */ 
/* 175 */     for (int i = 0; i < namesAndValues.length; i += 2) {
/* 176 */       String name = namesAndValues[i];
/* 177 */       String value = namesAndValues[(i + 1)];
/* 178 */       if ((name.length() == 0) || (name.indexOf(0) != -1) || (value.indexOf(0) != -1)) {
/* 179 */         throw new IllegalArgumentException("Unexpected header: " + name + ": " + value);
/*     */       }
/*     */     }
/*     */     
/* 183 */     return new Headers(namesAndValues);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public static Headers of(Map<String, String> headers)
/*     */   {
/* 190 */     if (headers == null) {
/* 191 */       throw new IllegalArgumentException("Expected map with header names and values");
/*     */     }
/*     */     
/*     */ 
/* 195 */     String[] namesAndValues = new String[headers.size() * 2];
/* 196 */     int i = 0;
/* 197 */     for (Map.Entry<String, String> header : headers.entrySet()) {
/* 198 */       if ((header.getKey() == null) || (header.getValue() == null)) {
/* 199 */         throw new IllegalArgumentException("Headers cannot be null");
/*     */       }
/* 201 */       String name = ((String)header.getKey()).trim();
/* 202 */       String value = ((String)header.getValue()).trim();
/* 203 */       if ((name.length() == 0) || (name.indexOf(0) != -1) || (value.indexOf(0) != -1)) {
/* 204 */         throw new IllegalArgumentException("Unexpected header: " + name + ": " + value);
/*     */       }
/* 206 */       namesAndValues[i] = name;
/* 207 */       namesAndValues[(i + 1)] = value;
/* 208 */       i += 2;
/*     */     }
/*     */     
/* 211 */     return new Headers(namesAndValues);
/*     */   }
/*     */   
/*     */   public static final class Builder {
/* 215 */     private final List<String> namesAndValues = new ArrayList(20);
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */     Builder addLenient(String line)
/*     */     {
/* 222 */       int index = line.indexOf(":", 1);
/* 223 */       if (index != -1)
/* 224 */         return addLenient(line.substring(0, index), line.substring(index + 1));
/* 225 */       if (line.startsWith(":"))
/*     */       {
/*     */ 
/* 228 */         return addLenient("", line.substring(1));
/*     */       }
/* 230 */       return addLenient("", line);
/*     */     }
/*     */     
/*     */ 
/*     */     public Builder add(String line)
/*     */     {
/* 236 */       int index = line.indexOf(":");
/* 237 */       if (index == -1) {
/* 238 */         throw new IllegalArgumentException("Unexpected header: " + line);
/*     */       }
/* 240 */       return add(line.substring(0, index).trim(), line.substring(index + 1));
/*     */     }
/*     */     
/*     */     public Builder add(String name, String value)
/*     */     {
/* 245 */       checkNameAndValue(name, value);
/* 246 */       return addLenient(name, value);
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */     Builder addLenient(String name, String value)
/*     */     {
/* 254 */       this.namesAndValues.add(name);
/* 255 */       this.namesAndValues.add(value.trim());
/* 256 */       return this;
/*     */     }
/*     */     
/*     */     public Builder removeAll(String name) {
/* 260 */       for (int i = 0; i < this.namesAndValues.size(); i += 2) {
/* 261 */         if (name.equalsIgnoreCase((String)this.namesAndValues.get(i))) {
/* 262 */           this.namesAndValues.remove(i);
/* 263 */           this.namesAndValues.remove(i);
/* 264 */           i -= 2;
/*     */         }
/*     */       }
/* 267 */       return this;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */     public Builder set(String name, String value)
/*     */     {
/* 275 */       checkNameAndValue(name, value);
/* 276 */       removeAll(name);
/* 277 */       addLenient(name, value);
/* 278 */       return this;
/*     */     }
/*     */     
/*     */     private void checkNameAndValue(String name, String value) {
/* 282 */       if (name == null) throw new IllegalArgumentException("name == null");
/* 283 */       if (name.isEmpty()) throw new IllegalArgumentException("name is empty");
/* 284 */       int i = 0; for (int length = name.length(); i < length; i++) {
/* 285 */         char c = name.charAt(i);
/* 286 */         if ((c <= '\037') || (c >= '')) {
/* 287 */           throw new IllegalArgumentException(String.format("Unexpected char %#04x at %d in header name: %s", new Object[] {
/* 288 */             Integer.valueOf(c), Integer.valueOf(i), name }));
/*     */         }
/*     */       }
/* 291 */       if (value == null) throw new IllegalArgumentException("value == null");
/* 292 */       int i = 0; for (int length = value.length(); i < length; i++) {
/* 293 */         char c = value.charAt(i);
/* 294 */         if ((c <= '\037') || (c >= '')) {
/* 295 */           throw new IllegalArgumentException(String.format("Unexpected char %#04x at %d in header value: %s", new Object[] {
/* 296 */             Integer.valueOf(c), Integer.valueOf(i), value }));
/*     */         }
/*     */       }
/*     */     }
/*     */     
/*     */     public String get(String name)
/*     */     {
/* 303 */       for (int i = this.namesAndValues.size() - 2; i >= 0; i -= 2) {
/* 304 */         if (name.equalsIgnoreCase((String)this.namesAndValues.get(i))) {
/* 305 */           return (String)this.namesAndValues.get(i + 1);
/*     */         }
/*     */       }
/* 308 */       return null;
/*     */     }
/*     */     
/*     */     public Headers build() {
/* 312 */       return new Headers(this, null);
/*     */     }
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\Headers.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */