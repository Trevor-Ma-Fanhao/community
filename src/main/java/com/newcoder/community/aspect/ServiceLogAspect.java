package com.newcoder.community.aspect;

import jakarta.servlet.http.HttpServletRequest;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
@Aspect
public class ServiceLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(ServiceLogAspect.class);

    @Pointcut("execution(* com.newcoder.community.service.*.*(..))")
    public void pointcut(){

    }

    @Before("pointcut()")
    public void before(JoinPoint joinPoint){
        // 用户【1，2，3，4】，在【xxx】，访问了【com.nowcoder.community.service.xxx()】.
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            String ip = request.getRemoteHost();
            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            String target = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
            logger.info(String.format("用户[%s],在[%s],访问了[%s]",ip,now,target));
        } catch (IllegalStateException e) {
            // 此处选择不打印异常堆栈信息，只记录简单的警告或调试信息
            logger.debug("Attempted to access request attributes outside of an actual web request. Ignoring IllegalStateException.");
            // 或者您也可以完全省略日志记录，这样就不会输出任何信息
            // 直接 return，不处理异常
            return;
        }
    }
}
