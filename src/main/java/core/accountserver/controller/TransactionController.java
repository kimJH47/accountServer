package core.accountserver.controller;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import core.accountserver.dto.request.transaction.UserBalanceRequest;
import core.accountserver.dto.response.Response;
import core.accountserver.dto.response.transaction.UseBalanceResponse;
import core.accountserver.exception.transaction.TransactionFailedException;
import core.accountserver.service.TransactionService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TransactionController {

	private final TransactionService transactionService;

	@PostMapping("/transaction/use")
	public ResponseEntity<Response> useBalance(@Valid @RequestBody UserBalanceRequest request){
		try {
			UseBalanceResponse response = transactionService.useBalance(request.getUserId(),
				request.getAccountNumber(), request.getAmount());
			return Response.createSuccess("성공적으로 거래가 완료 되었습니다.", response);
		} catch (Exception e) {
			transactionService.saveFailedTransaction(request.getAccountNumber(), request.getAmount());
			throw new TransactionFailedException(e.getMessage());
		}
	}

}
