package core.accountserver.controller;

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

import core.accountserver.domain.transaction.TransactionResult;
import core.accountserver.dto.request.transaction.UserBalanceRequest;
import core.accountserver.dto.response.transaction.UseBalanceResponse;
import core.accountserver.exception.AccountAlreadyUnregisteredException;
import core.accountserver.exception.AccountExceedBalanceException;
import core.accountserver.exception.AccountNotFoundException;
import core.accountserver.exception.UserAccountUnMatchException;
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
				.transactionResult(TransactionResult.SUCCESS)
				.build());
		UserBalanceRequest request = new UserBalanceRequest(1L, "2000000000", 3000L);

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
	@MethodSource("invalidRequestProvider")
	@DisplayName("유효하지 못한 거래 요청이 올시 응답코드 400과 함께 실패한 이유가 응답으로 와야한다")
	void useBalance_invalidRequest(UserBalanceRequest userBalanceRequest, String fieldName) throws Exception{
		//expect
		mockMvc.perform(post("/transaction/use")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(userBalanceRequest)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
			.andExpect(jsonPath("$.reasons." + fieldName).exists());

	}
	public static Stream<Arguments> invalidRequestProvider() {
		return Stream.of(
			Arguments.of(new UserBalanceRequest(-1L, "1111111111", 100L), "userId"),
			Arguments.of(new UserBalanceRequest(1L, "11111111110", 10L), "accountNumber"),
			Arguments.of(new UserBalanceRequest(1L, "1111111111", 9L), "amount"),
			Arguments.of(new UserBalanceRequest(1L, "1111111111", 1000_000_001L), "amount")
		);
	}

	@ParameterizedTest
	@MethodSource("invalidTransactionProvider")
	@DisplayName("거래에 실패 할 시 응답코드 400과 함께 실패한 이유가 응답으로 와야한다.")
	void useBalance_exception(RuntimeException e) throws Exception {
		//given
		given(transactionService.useBalance(anyLong(), anyString(), anyLong()))
			.willThrow(e);
		UserBalanceRequest request = new UserBalanceRequest(1L, "2000000000", 3000L);

		//expect
		mockMvc.perform(post("/transaction/use")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
			.andExpect(jsonPath("$.reasons.transaction").value(e.getMessage()));
	}

	public static Stream<Arguments> invalidTransactionProvider() {
		return Stream.of(Arguments.of(new UserNotFoundException("해당 사용자가 존재하지 않습니다.")),
			Arguments.of(new AccountNotFoundException("해당 계좌가 존재하지 않습니다.")),
			Arguments.of(new UserAccountUnMatchException("사용자와 계좌의 소유주가 다릅니다.")),
			Arguments.of(new AccountAlreadyUnregisteredException("이미 해지된 계좌번호 입니다.")),
			Arguments.of(new AccountExceedBalanceException("거래금액이 계좌 잔액보다 큽니다."))
			);
	}
}