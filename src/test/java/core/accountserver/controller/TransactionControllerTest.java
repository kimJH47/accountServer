package core.accountserver.controller;

import static core.accountserver.domain.transaction.TransactionResult.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import core.accountserver.domain.transaction.TransactionType;
import core.accountserver.dto.request.transaction.CancelBalanceRequest;
import core.accountserver.dto.request.transaction.UseBalanceRequest;
import core.accountserver.dto.response.transaction.CancelBalanceResponse;
import core.accountserver.dto.response.transaction.UseBalanceResponse;
import core.accountserver.exception.account.AccountAlreadyUnregisteredException;
import core.accountserver.exception.account.AccountExceedBalanceException;
import core.accountserver.exception.account.AccountNotFoundException;
import core.accountserver.exception.account.UserAccountUnMatchException;
import core.accountserver.exception.transaction.AccountTransactionUnMatchException;
import core.accountserver.exception.transaction.CancelMustFullyException;
import core.accountserver.exception.transaction.TooOldOrderToCancelException;
import core.accountserver.exception.transaction.TransactionAlreadyCancelException;
import core.accountserver.exception.transaction.TransactionNotFoundException;
import core.accountserver.exception.transaction.TransactionResultFailedException;
import core.accountserver.exception.user.UserNotFoundException;
import core.accountserver.service.TransactionService;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

	@MockBean
	TransactionService transactionService;

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper mapper;

	@Test
	@DisplayName("/transaction/use post 로 거래요청을 보내면 응답코드 200과 함깨 거래내역이 응답으로 와야한다.")
	void useBalance() throws Exception {
		//given
		given(transactionService.useBalance(anyLong(), anyString(), anyLong()))
			.willReturn(UseBalanceResponse.builder()
				.accountNumber("1000000000")
				.transactedAt(LocalDateTime.now())
				.amount(12345L)
				.transactionId("transactionId")
				.transactionResult(SUCCESS)
				.build());
		UseBalanceRequest request = new UseBalanceRequest(1L, "2000000000", 3000L);

		//expect
		mockMvc.perform(post("/transaction/use")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.entity.accountNumber").value("1000000000"))
			.andExpect(jsonPath("$.entity.transactionResult").value("SUCCESS"))
			.andExpect(jsonPath("$.entity.transactionId").value("transactionId"))
			.andExpect(jsonPath("$.entity.amount").value(12345));

		then(transactionService).should(times(1)).useBalance(anyLong(), anyString(), anyLong());
	}

	@ParameterizedTest
	@MethodSource("invalidUseBalanceRequestProvider")
	@DisplayName("유효하지 못한 거래 요청이 올시 응답코드 400과 함께 실패한 이유가 응답으로 와야한다")
	void useBalance_invalidRequest(UseBalanceRequest useBalanceRequest, String fieldName) throws Exception {
		//expect
		mockMvc.perform(post("/transaction/use")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(useBalanceRequest)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
			.andExpect(jsonPath("$.reasons." + fieldName).exists());

		then(transactionService)
			.should(times(0))
			.saveFailedTransaction(anyString(), anyLong(), any(TransactionType.class));
	}

	public static Stream<Arguments> invalidUseBalanceRequestProvider() {
		return Stream.of(
			Arguments.of(new UseBalanceRequest(-1L, "1111111111", 100L), "userId"),
			Arguments.of(new UseBalanceRequest(1L, "11111111110", 10L), "accountNumber"),
			Arguments.of(new UseBalanceRequest(1L, "1111111111", 9L), "amount"),
			Arguments.of(new UseBalanceRequest(1L, "1111111111", 1000_000_001L), "amount")
		);
	}

	@ParameterizedTest
	@MethodSource("invalidUseTransactionProvider")
	@DisplayName("거래에 실패 할 시 응답코드 400과 함께 실패한 이유가 응답으로 와야한다.")
	void useBalance_exception(RuntimeException e) throws Exception {
		//given
		given(transactionService.useBalance(anyLong(), anyString(), anyLong()))
			.willThrow(e);
		UseBalanceRequest request = new UseBalanceRequest(1L, "2000000000", 3000L);

		//expect
		mockMvc.perform(post("/transaction/use")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
			.andExpect(jsonPath("$.reasons.transaction").value(e.getMessage()));

		then(transactionService).should(times(1)).useBalance(anyLong(), anyString(), anyLong());

		then(transactionService)
			.should(times(1))
			.saveFailedTransaction(anyString(), anyLong(), any(TransactionType.class));
	}

	public static Stream<Arguments> invalidUseTransactionProvider() {
		return Stream.of(Arguments.of(new UserNotFoundException("해당 사용자가 존재하지 않습니다.")),
			Arguments.of(new AccountNotFoundException("해당 계좌가 존재하지 않습니다.")),
			Arguments.of(new UserAccountUnMatchException("사용자와 계좌의 소유주가 다릅니다.")),
			Arguments.of(new AccountAlreadyUnregisteredException("이미 해지된 계좌번호 입니다.")),
			Arguments.of(new AccountExceedBalanceException("거래금액이 계좌 잔액보다 큽니다."))
		);
	}

	@Test
	@DisplayName("/transaction/cancel post 로 거래취소 요청을 보낼 시 응답코드 200과 취소내역을 응답받아야한다.")
	void cancelTransaction() throws Exception {
		//given
		String accountNumber = "1111111111";

		LocalDateTime now = LocalDateTime.now();
		CancelBalanceResponse cancelBalanceResponse = new CancelBalanceResponse
			(accountNumber, SUCCESS, "transactionId", 1000L, now);
		given(transactionService.cancelBalance(anyString(), anyString(), anyLong())).willReturn(cancelBalanceResponse);

		//expect
		mockMvc.perform(post("/transaction/cancel")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(new CancelBalanceRequest("transactionId", accountNumber, 1000L))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.entity.accountNumber").value(accountNumber))
			.andExpect(jsonPath("$.entity.transactionId").value("transactionId"))
			.andExpect(jsonPath("$.entity.transactionResult").value(SUCCESS.toString()))
			.andExpect(jsonPath("$.entity.amount").value(1000));
		then(transactionService).should(times(1)).cancelBalance((anyString()), anyString(), anyLong());
	}

	@ParameterizedTest
	@MethodSource("invalidCancelRequestProvider")
	@DisplayName("유효하지않은 요청을 보낼 시 응답코드 400과 함께 이유를 응답 받아야한다.")
	void cancel_invalidRequest(CancelBalanceRequest cancelBalanceRequest, String fieldName) throws Exception {
		String accountNumber = "1111111111";
		LocalDateTime now = LocalDateTime.now();
		CancelBalanceResponse cancelBalanceResponse = new CancelBalanceResponse
			(accountNumber, SUCCESS, "transactionId", 1000L, now);
		given(transactionService.cancelBalance(anyString(), anyString(), anyLong())).willReturn(cancelBalanceResponse);

		//expect
		mockMvc.perform(post("/transaction/cancel")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(cancelBalanceRequest)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
			.andExpect(jsonPath("$.reasons." + fieldName).exists());
	}

	public static Stream<Arguments> invalidCancelRequestProvider() {
		return Stream.of(
			Arguments.of(new CancelBalanceRequest("transactionId", "100000010", 100L), "accountNumber"),
			Arguments.of(new CancelBalanceRequest("transactionId", "1000000101", 0L), "amount")
		);
	}

	@Test
	@DisplayName("유효하지 않은 요청값이 여러개 일시 응답코드 400과 함깨 모든 이유를 응답 받아야한다.")
	void cancel_invalidRequestAll() throws Exception {
		//given
		CancelBalanceRequest invalidRequest = new CancelBalanceRequest("transactionId", "100000010", -1L);

		//expect
		mockMvc.perform(post("/transaction/cancel")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(invalidRequest)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
			.andExpect(jsonPath("$.reasons.accountNumber").exists())
			.andExpect(jsonPath("$.reasons.amount").exists());
	}

	@ParameterizedTest
	@MethodSource("invalidCancelTransactionProvider")
	@DisplayName("거래에 실패 할 시 응답코드 400과 함께 실패한 이유가 응답으로 와야한다.")
	void cancel_exception(RuntimeException e) throws Exception {
		//given
		given(transactionService.cancelBalance(anyString(), anyString(), anyLong())).willThrow(e);
		CancelBalanceRequest request = new CancelBalanceRequest("transactionId", "2000000000", 3000L);
		//expect
		mockMvc.perform(post("/transaction/cancel")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
			.andExpect(jsonPath("$.reasons.transaction").value(e.getMessage()));

		then(transactionService).should(times(1)).cancelBalance(anyString(), anyString(), anyLong());
		then(transactionService)
			.should(times(1))
			.saveFailedTransaction(anyString(), anyLong(), any(TransactionType.class));
	}

	public static Stream<Arguments> invalidCancelTransactionProvider() {
		return Stream.of(
			Arguments.of(new AccountNotFoundException("해당 계좌가 존재하지 않습니다.")),
			Arguments.of(new AccountTransactionUnMatchException("해당계좌에서 발생된 거래가 아닙니다.")),
			Arguments.of(new TransactionNotFoundException("사용자와 계좌의 소유주가 다릅니다.")),
			Arguments.of(new AccountAlreadyUnregisteredException("이미 해지된 계좌번호 입니다.")),
			Arguments.of(new TransactionAlreadyCancelException("이미 취소된 거래입니다.")),
			Arguments.of(new TransactionResultFailedException("해당 거래는 실패한 거래입니다.")),
			Arguments.of(new TooOldOrderToCancelException("취소 가능한 거래 날짜가 지났습니다.")),
			Arguments.of(new CancelMustFullyException("취소금액은 거래된 금액과 일치 해야 합니다."))
		);
	}
}