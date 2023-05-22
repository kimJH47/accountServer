package core.accountserver.controller;

import javax.validation.Valid;
import javax.validation.constraints.Min;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import core.accountserver.dto.request.account.CreateAccountRequest;
import core.accountserver.dto.request.account.DeleteAccountRequest;
import core.accountserver.dto.response.account.CreateAccountResponse;
import core.accountserver.dto.response.account.DeleteAccountResponse;
import core.accountserver.dto.response.Response;
import core.accountserver.service.AccountService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AccountController {

	private final AccountService accountService;

	@PostMapping("/account")
	public ResponseEntity<Response> createAccount(@RequestBody @Valid CreateAccountRequest createAccountRequest) {
		CreateAccountResponse response = accountService.createAccount(createAccountRequest.getUserId(),
			createAccountRequest.getInitialBalance());
		return Response.createSuccess("성공적으로 계좌가 생성되었습니다.", response);
	}

	@DeleteMapping("/account")
	public ResponseEntity<Response> deleteAccount(@RequestBody @Valid DeleteAccountRequest deleteAccountRequest) {
		DeleteAccountResponse response = accountService.deleteAccount(deleteAccountRequest.getUserId(),
			deleteAccountRequest.getAccountNumber());
		return Response.createSuccess("성공적으로 계좌가 해지 되었습니다.", response);
	}

	@GetMapping("/account")
	public ResponseEntity<Response> findByAccountUserId(
		@RequestParam("user_id") @Min(value = 1, message = "아이디는 1 이상 이여야 합니다.") @Valid Long id) {
		return Response.createSuccess("성공적으로 계좌가 조회되었습니다.", accountService.findAccountByUserId(id));
	}
}
