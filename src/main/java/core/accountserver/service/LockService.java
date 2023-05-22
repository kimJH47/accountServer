package core.accountserver.service;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import core.accountserver.exception.RedisClientException;
import core.accountserver.exception.transaction.TransactionHasLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class LockService {

	private final RedissonClient redissonClient;

	public void lock(String accountNumber) {
		RLock lock = redissonClient.getLock(getLockKey(accountNumber));
		log.debug("Trying lock for accountNumber: {}", accountNumber);
		try {
			if (!lock.tryLock(1, 5, TimeUnit.SECONDS)) {
				throw new TransactionHasLockException("해당 계좌는 사용중입니다.");
			}
		} catch (InterruptedException e) {
			throw new RedisClientException(e);
		}
	}

	public void unlock(String accountNumber) {
		log.debug("Trying unlock for accountNumber: {}",accountNumber);
		redissonClient.getLock(getLockKey(accountNumber)).unlock();

	}

	private String getLockKey(String accountNumber) {
		return "ACLK:" + accountNumber;
	}
}
