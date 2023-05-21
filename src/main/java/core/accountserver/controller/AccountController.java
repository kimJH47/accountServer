package core.accountserver.controller;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import core.accountserver.dto.request.CreateAccountRequest;
import core.accountserver.dto.response.CreateAccountResponse;
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
		return Response.createSuccessResponse("성공적으로 계좌가 생성되었습니다.", response);
	}
}
