package core.accountserver.service;

import static org.mockito.BDDMockito.*;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import core.accountserver.exception.transaction.TransactionHasLockException;

@ExtendWith(MockitoExtension.class)
class LockServiceTest {
	@Mock
	RedissonClient redissonClient;
	@Mock
	RLock rLock;
	@InjectMocks
	LockService lockService;

	@Test
	@DisplayName("성공적으로 락이 되어야한다.")
	void lock_success() throws Exception {
	    //given
		given(redissonClient.getLock(anyString())).willReturn(rLock);
		given(rLock.tryLock(anyLong(), anyLong(), any())).willReturn(true);

		//expect
		Assertions.assertThatCode(() ->
			lockService.lock("1231111111")
		).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("이미 락인 계좌를 락요청을 할시 TransactionHasLockException 이 발생해야한다.")
	void lock_transaction_has_lock() throws Exception {
	    //given
		given(redissonClient.getLock(anyString())).willReturn(rLock);
		given(rLock.tryLock(anyLong(), anyLong(), any())).willReturn(false);

		//expect
		Assertions.assertThatThrownBy(() ->
			lockService.lock("1231111111")
		).isInstanceOf(TransactionHasLockException.class);

	}
}