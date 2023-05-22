package core.accountserver.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import core.accountserver.aop.AccountLockRequest;
import core.accountserver.dto.request.transaction.UseBalanceRequest;
import core.accountserver.exception.transaction.TransactionFailedException;

@ExtendWith(MockitoExtension.class)
class LockAopAspectTest {

	@Mock
	LockService lockService;
	@Mock
	ProceedingJoinPoint proceedingJoinPoint;
	@InjectMocks
	LockAopAspect lockAopAspect;

	@Test
	@DisplayName("계좌번호를 받아 락과 언락 서비스가 진행되어야야 한다.")
	void lockAndUnlock() {
		//given
		String accountNumber = "1231111111";
		ArgumentCaptor<String> lockArgumentCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> unlockArgumentCaptor = ArgumentCaptor.forClass(String.class);
		AccountLockRequest request = new UseBalanceRequest(1L, accountNumber, 100L);
		//when
		lockAopAspect.aroundMethod(proceedingJoinPoint, request);
		//then
		then(lockService).should(times(1)).lock(lockArgumentCaptor.capture());
		then(lockService).should(times(1)).unlock(unlockArgumentCaptor.capture());

		assertThat(accountNumber).isEqualTo(lockArgumentCaptor.getValue());
		assertThat(accountNumber).isEqualTo(unlockArgumentCaptor.getValue());
	}

	@Test
	@DisplayName("계좌번호를 받아 락을 진행중 예외가 발생해도 언락이 되어야한다.")
	void lockAndUnlock_evenIfThrow() throws Throwable {
		String accountNumber = "1231111111";
		ArgumentCaptor<String> lockArgumentCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> unlockArgumentCaptor = ArgumentCaptor.forClass(String.class);
		AccountLockRequest request = new UseBalanceRequest(1L, accountNumber, 100L);
		given(proceedingJoinPoint.proceed())
			.willThrow(new TransactionFailedException("계좌내역이 존재하지 않습니다."));

		//when
		assertThatThrownBy(() -> lockAopAspect.aroundMethod(proceedingJoinPoint, request))
			.isInstanceOf(TransactionFailedException.class);

		//then
		then(lockService).should(times(1)).lock(lockArgumentCaptor.capture());
		then(lockService).should(times(1)).unlock(unlockArgumentCaptor.capture());

		assertThat(accountNumber).isEqualTo(lockArgumentCaptor.getValue());
		assertThat(accountNumber).isEqualTo(unlockArgumentCaptor.getValue());
	}
}