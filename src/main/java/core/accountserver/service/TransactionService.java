package core.accountserver.service;

import org.springframework.stereotype.Service;

import core.accountserver.dto.response.UserBalanceResponse;

@Service
public class TransactionService {
	public UserBalanceResponse useBalance(Long userId, String accountNumber, Long amount) {
		return null;
	}
}
