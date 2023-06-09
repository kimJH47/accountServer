package core.accountserver.controller;

import static core.accountserver.domain.transaction.TransactionType.*;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import core.accountserver.aop.AccountLock;
import core.accountserver.dto.request.transaction.CancelBalanceRequest;
import core.accountserver.dto.request.transaction.UseBalanceRequest;
import core.accountserver.dto.response.Response;
import core.accountserver.dto.response.transaction.CancelBalanceResponse;
import core.accountserver.dto.response.transaction.UseBalanceResponse;
import core.accountserver.exception.transaction.TransactionFailedException;
import core.accountserver.service.TransactionService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TransactionController {

	private final TransactionService transactionService;

	@PostMapping("/transaction/use")
	@AccountLock
	public ResponseEntity<Response> useBalance(@Valid @RequestBody UseBalanceRequest request) {
		try {
			UseBalanceResponse response = transactionService.useBalance(request.getUserId(),
				request.getAccountNumber(), request.getAmount());
			return Response.createSuccess("성공적으로 거래가 완료 되었습니다.", response);
		} catch (Exception e) {
			transactionService.saveFailedTransaction(request.getAccountNumber(), request.getAmount(), USE);
			throw new TransactionFailedException(e.getMessage());
		}
	}

	@PostMapping("/transaction/cancel")
	@AccountLock
	public ResponseEntity<Response> cancelBalance(@Valid @RequestBody CancelBalanceRequest request) {
		try {
			CancelBalanceResponse response = transactionService.cancelBalance(request.getTransactionId(),
				request.getAccountNumber(), request.getAmount());
			return Response.createSuccess("성공적으로 거래가 취소 되었습니다.", response);
		} catch (Exception e) {
			transactionService.saveFailedTransaction(request.getAccountNumber(), request.getAmount(), CANCEL);
			throw new TransactionFailedException(e.getMessage());
		}
	}

	@GetMapping("/transaction/{transactionId}")
	public ResponseEntity<Response> findOne(@PathVariable String transactionId) {
		return Response.createSuccess("성공적으로 조회가 완료되었습니다.",
			transactionService.findByTransactionId(transactionId));
	}
}
