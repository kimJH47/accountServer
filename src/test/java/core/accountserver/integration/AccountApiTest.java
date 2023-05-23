package core.accountserver.integration;

import static core.accountserver.domain.account.AccountStatus.*;
import static org.assertj.core.api.Assertions.*;

import javax.security.auth.login.AccountNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import core.accountserver.controller.AccountController;
import core.accountserver.domain.account.Account;
import core.accountserver.dto.request.account.CreateAccountRequest;
import core.accountserver.dto.response.Response;
import core.accountserver.dto.response.SuccessResponse;
import core.accountserver.dto.response.account.CreateAccountResponse;
import core.accountserver.repository.AccountRepository;
import core.accountserver.repository.AccountUserRepository;

@SpringBootTest
public class AccountApiTest {

	@Autowired
	AccountController accountController;
	@Autowired
	AccountRepository accountRepository;
	@Autowired
	AccountUserRepository accountUserRepository;

	@BeforeEach
	void init() {

	}
	@Test
	@DisplayName("계좌 생성이 성공적으로 완료 되어야한다.")
	void account_create() throws Exception {
	    //given
		CreateAccountRequest createAccountRequest = new CreateAccountRequest(1L, 10000L);

		//when
		ResponseEntity<Response> response = accountController.createAccount(createAccountRequest);

		//then
		SuccessResponse<CreateAccountResponse> body = (SuccessResponse<CreateAccountResponse>)response.getBody();
		CreateAccountResponse entity = body.getEntity();

		Account account = accountRepository.findByAccountNumber(entity.getAccountNumber())
			.orElseThrow(() -> new AccountNotFoundException(""));

		//request
		assertThat(account.getBalance()).isEqualTo(createAccountRequest.getInitialBalance());
		assertThat(account.getAccountUser().getId()).isEqualTo(createAccountRequest.getUserId());
		//response
		assertThat(account.getAccountNumber()).isEqualTo(entity.getAccountNumber());
		assertThat(account.getAccountUser().getId()).isEqualTo(entity.getUserId());
		assertThat(account.getRegisterAt()).isEqualTo(entity.getRegisteredAt());
		assertThat(account.getAccountStatus()).isEqualTo(IN_USE);
	}
}
