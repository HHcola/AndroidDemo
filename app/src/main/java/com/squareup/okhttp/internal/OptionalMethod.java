/*     */ package com.squareup.okhttp.internal;
/*     */ 
/*     */ import java.lang.reflect.InvocationTargetException;
/*     */ import java.lang.reflect.Method;
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
/*     */ class OptionalMethod<T>
/*     */ {
/*     */   private final Class<?> returnType;
/*     */   private final String methodName;
/*     */   private final Class[] methodParams;
/*     */   
/*     */   public OptionalMethod(Class<?> returnType, String methodName, Class... methodParams)
/*     */   {
/*  46 */     this.returnType = returnType;
/*  47 */     this.methodName = methodName;
/*  48 */     this.methodParams = methodParams;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public boolean isSupported(T target)
/*     */   {
/*  55 */     return getMethod(target.getClass()) != null;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Object invokeOptional(T target, Object... args)
/*     */     throws InvocationTargetException
/*     */   {
/*  67 */     Method m = getMethod(target.getClass());
/*  68 */     if (m == null) {
/*  69 */       return null;
/*     */     }
/*     */     try {
/*  72 */       return m.invoke(target, args);
/*     */     } catch (IllegalAccessException e) {}
/*  74 */     return null;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Object invokeOptionalWithoutCheckedException(T target, Object... args)
/*     */   {
/*     */     try
/*     */     {
/*  87 */       return invokeOptional(target, args);
/*     */     } catch (InvocationTargetException e) {
/*  89 */       Throwable targetException = e.getTargetException();
/*  90 */       if ((targetException instanceof RuntimeException)) {
/*  91 */         throw ((RuntimeException)targetException);
/*     */       }
/*  93 */       AssertionError error = new AssertionError("Unexpected exception");
/*  94 */       error.initCause(targetException);
/*  95 */       throw error;
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Object invoke(T target, Object... args)
/*     */     throws InvocationTargetException
/*     */   {
/* 107 */     Method m = getMethod(target.getClass());
/* 108 */     if (m == null) {
/* 109 */       throw new AssertionError("Method " + this.methodName + " not supported for object " + target);
/*     */     }
/*     */     try {
/* 112 */       return m.invoke(target, args);
/*     */     }
/*     */     catch (IllegalAccessException e) {
/* 115 */       AssertionError error = new AssertionError("Unexpectedly could not call: " + m);
/* 116 */       error.initCause(e);
/* 117 */       throw error;
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Object invokeWithoutCheckedException(T target, Object... args)
/*     */   {
/*     */     try
/*     */     {
/* 130 */       return invoke(target, args);
/*     */     } catch (InvocationTargetException e) {
/* 132 */       Throwable targetException = e.getTargetException();
/* 133 */       if ((targetException instanceof RuntimeException)) {
/* 134 */         throw ((RuntimeException)targetException);
/*     */       }
/* 136 */       AssertionError error = new AssertionError("Unexpected exception");
/* 137 */       error.initCause(targetException);
/* 138 */       throw error;
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private Method getMethod(Class<?> clazz)
/*     */   {
/* 149 */     Method method = null;
/* 150 */     if (this.methodName != null) {
/* 151 */       method = getPublicMethod(clazz, this.methodName, this.methodParams);
/* 152 */       if ((method != null) && (this.returnType != null))
/*     */       {
/* 154 */         if (!this.returnType.isAssignableFrom(method.getReturnType()))
/*     */         {
/*     */ 
/* 157 */           method = null; }
/*     */       }
/*     */     }
/* 160 */     return method;
/*     */   }
/*     */   
/*     */   private static Method getPublicMethod(Class<?> clazz, String methodName, Class[] parameterTypes) {
/* 164 */     Method method = null;
/*     */     try {
/* 166 */       method = clazz.getMethod(methodName, parameterTypes);
/* 167 */       if ((method.getModifiers() & 0x1) == 0) {
/* 168 */         method = null;
/*     */       }
/*     */     }
/*     */     catch (NoSuchMethodException localNoSuchMethodException) {}
/*     */     
/* 173 */     return method;
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\OptionalMethod.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */