package com.medvision.aspect;

import com.medvision.service.AuditService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditAspect {

    private static final Logger logger = LoggerFactory.getLogger(AuditAspect.class);

    @Autowired
    private AuditService auditService;

    @Pointcut("execution(* com.medvision.controller.DicomController.*(..)) || " +
              "execution(* com.medvision.controller.AnalysisController.*(..))")
    public void controllerMethods() {}

    @Around("controllerMethods()")
    public Object auditControllerAction(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();

        String maskedArgs = maskPHI(args);

        logger.info("Audit: Executing {}.{} with args {}", className, methodName, maskedArgs);

        Object result;
        try {
            result = joinPoint.proceed();
            auditService.log(methodName, className, "N/A", "Execution completed successfully with args: " + maskedArgs, "SUCCESS");
            return result;
        } catch (Exception e) {
            auditService.log(methodName, className, "N/A", "Error: " + e.getClass().getSimpleName() + " for args: " + maskedArgs, "ERROR");
            throw e;
        }
    }

    private String maskPHI(Object[] args) {
        if (args == null || args.length == 0) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg == null) {
                sb.append("null");
            } else {
                String argStr = arg.toString();
                // Simple PHI masking logic: mask potential patient names or sensitive IDs
                // In a real scenario, this would use a library or more complex regex
                if (argStr.contains("patientName") || argStr.contains("Patient") || argStr.contains("Name")) {
                    sb.append("***PHI_MASKED***");
                } else if (argStr.length() > 50) {
                    sb.append(argStr.substring(0, 10)).append("...[TRUNCATED/MASKED]");
                } else {
                    sb.append(argStr);
                }
            }
            if (i < args.length - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}
