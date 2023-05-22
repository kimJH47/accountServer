package core.accountserver.controller;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import core.accountserver.dto.request.UserBalanceRequest;
import core.accountserver.dto.response.Response;
import core.accountserver.service.TransactionService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TransactionController {

	private final TransactionService transactionService;

	@PostMapping("/transaction/use")
	public ResponseEntity<Response> useBalance(@Valid @RequestBody UserBalanceRequest request){
		return Response.createSuccessResponse("성공적으로 거래가 완료 되었습니다.",
			transactionService.useBalance(request.getUserId(), request.getAccountNumber(), request.getAmount()));
	}
}
