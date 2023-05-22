package core.accountserver.service;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import core.accountserver.aop.AccountLockRequest;
import core.accountserver.exception.transaction.TransactionFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LockAopAspect {

	private final LockService lockService;

	@Around("@annotation(core.accountserver.aop.AccountLock) && args(request)")
	public Object aroundMethod(ProceedingJoinPoint pjp, AccountLockRequest request) {
		lockService.lock(request.getAccountNumber());
		try {
			return pjp.proceed();
		} catch (Throwable e) {
			throw new TransactionFailedException("거래를 실패하였습니다.");
		} finally {
			lockService.unlock(request.getAccountNumber());
		}
	}
}
