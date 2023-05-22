package core.accountserver.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

import core.accountserver.dto.request.CreateAccountRequest;
import core.accountserver.dto.request.DeleteAccountRequest;
import core.accountserver.dto.response.AccountSearchResponse;
import core.accountserver.dto.response.CreateAccountResponse;
import core.accountserver.dto.response.DeleteAccountResponse;
import core.accountserver.exception.AccountAlreadyUnregisteredException;
import core.accountserver.exception.AccountHasBalanceException;
import core.accountserver.exception.AccountNotFoundException;
import core.accountserver.exception.UserAccountUnMatchException;
import core.accountserver.exception.user.MaxAccountPerUserException;
import core.accountserver.exception.user.UserNotFoundException;
import core.accountserver.repository.AccountRepository;
import core.accountserver.repository.AccountUserRepository;
import core.accountserver.service.AccountService;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

	@Autowired
	MockMvc mockMvc;
	@Autowired
	ObjectMapper mapper;

	@MockBean
	AccountService accountService;
	@MockBean
	AccountRepository accountRepository;
	@MockBean
	AccountUserRepository accountUserRepository;

	@Test
	@DisplayName("/account post 를 통해 계좌 생성 요청을 보내면 응답코드 200과 함깨 생성된 계좌번호와, 생성일시, 사용자 id 를 응답받아야한다.")
	void create() throws Exception {
		//given
		CreateAccountRequest request = new CreateAccountRequest(1L, 10000L);

		LocalDateTime now = LocalDateTime.now();
		given(accountService.createAccount(anyLong(), anyLong()))
			.willReturn(new CreateAccountResponse(1L, "1100100111", now));

		//expect
		mockMvc.perform(post("/account")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("성공적으로 계좌가 생성되었습니다."))
			.andExpect(jsonPath("$.entity.userId").value(1L))
			.andExpect(jsonPath("$.entity.accountNumber").value("1100100111"))
			.andExpect(jsonPath("$.entity.registeredAt").value(now.toString()));

		then(accountService).should(times(1)).createAccount(anyLong(), anyLong());
	}

	@ParameterizedTest
	@MethodSource("invalidCreateRequestProvider")
	@DisplayName("유효하지않은 데이터를 요청 할시 응답코드 400과 함깨 실패한 이유가 응답되어야한다.")
	void create_request_exception(CreateAccountRequest invalidCreateRequest, String fieldName, String message) throws
		Exception {
		//expect
		mockMvc.perform(post("/account")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(invalidCreateRequest)))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
			.andExpect(jsonPath("$.reasons." + fieldName).value(message));

	}

	public static Stream<Arguments> invalidCreateRequestProvider() {
		return Stream.of(
			Arguments.of(new CreateAccountRequest(0L, 10000L), "userId", "아이디는 1 이상 이여야 합니다."),
			Arguments.of(new CreateAccountRequest(1L, 99L), "initialBalance", "금액은 최소 100 이상 존재해야 합니다."));
	}

	@Test
	@DisplayName("유효하지않은 데이터가 여러개 응답코드 400과 함깨 실패한 이유 전부가 응답되어야한다.")
	void create_request_exceptionAll() throws Exception {
		//given
		CreateAccountRequest invalidCreateRequest = new CreateAccountRequest(-1L, 99L);
		//expect
		mockMvc.perform(post("/account")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(invalidCreateRequest)))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
			.andExpect(jsonPath("$.reasons.userId").value("아이디는 1 이상 이여야 합니다."))
			.andExpect(jsonPath("$.reasons.initialBalance").value("금액은 최소 100 이상 존재해야 합니다."));
	}

	@Test
	@DisplayName("사용자가 존재하지 않을시 응답코드 400과 함께 실패 메세지가 응답되어야한다.")
	void create_userNotFoundException() throws Exception {
		//given
		given(accountService.createAccount(anyLong(), anyLong()))
			.willThrow(new UserNotFoundException("해당 사용자가 존재하지 않습니다."));
		CreateAccountRequest invalidCreateRequest = new CreateAccountRequest(131L, 990L);

		//expect
		mockMvc.perform(post("/account")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(invalidCreateRequest)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
			.andExpect(jsonPath("$.reasons.userId").value("해당 사용자가 존재하지 않습니다."));

		then(accountService).should(times(1)).createAccount(anyLong(), anyLong());

	}

	@Test
	@DisplayName("계좌가 이미 10개가 존재 할시  응답코드 400과 함께 실패 메세지가 응답되어야한다.")
	void create_maxAccountPerUserException() throws Exception {
		//given
		given(accountService.createAccount(anyLong(), anyLong()))
			.willThrow(new MaxAccountPerUserException("계좌가 이미 최대 갯수만큼 존재합니다."));
		CreateAccountRequest invalidCreateRequest = new CreateAccountRequest(131L, 990L);

		//expect
		mockMvc.perform(post("/account")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(invalidCreateRequest)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
			.andExpect(jsonPath("$.reasons.account").value("계좌가 이미 최대 갯수만큼 존재합니다."));

		then(accountService).should(times(1)).createAccount(anyLong(), anyLong());
	}

	@Test
	@DisplayName("/account delete 를 통해 계좌 해지 요청을 보내면 응답코드 200과 함깨 해지된 계좌번호와, 해지일시, 사용자 id 를 응답받아야한다.")
	void delete_success() throws Exception {
		DeleteAccountRequest request = new DeleteAccountRequest(1L, "1111111111");
		LocalDateTime now = LocalDateTime.now();
		given(accountService.deleteAccount(anyLong(), anyString()))
			.willReturn(new DeleteAccountResponse(1L, "1111111111", now));

		//expect
		mockMvc.perform(delete("/account")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("성공적으로 계좌가 해지 되었습니다."))
			.andExpect(jsonPath("$.entity.userId").value(1L))
			.andExpect(jsonPath("$.entity.accountNumber").value("1111111111"))
			.andExpect(jsonPath("$.entity.unRegisteredAt").value(now.toString()));

		then(accountService).should(times(1)).deleteAccount(anyLong(), anyString());
	}

	@ParameterizedTest
	@MethodSource("FailedResponseProvider")
	@DisplayName("계좌해지 검증에 실패하면 보내면 응답코드 400과 함께 실패이유를 응답받아야한다.")
	void delete_exception(RuntimeException e, String fieldName) throws Exception {

		given(accountService.createAccount(anyLong(), anyLong()))
			.willThrow(e);
		CreateAccountRequest invalidCreateRequest = new CreateAccountRequest(131L, 990L);
		//expect
		mockMvc.perform(post("/account")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(invalidCreateRequest)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
			.andExpect(jsonPath("$.reasons." + fieldName).value(e.getMessage()));

		then(accountService).should(times(1)).createAccount(anyLong(), anyLong());

	}

	public static Stream<Arguments> FailedResponseProvider() {
		return Stream.of(
			Arguments.of(new AccountAlreadyUnregisteredException("이미 해지된 계좌번호 입니다."), "account"),
			Arguments.of(new AccountHasBalanceException("해지하려는 계좌에 잔액이 존재합니다."), "account"),
			Arguments.of(new AccountNotFoundException("해당 계좌가 존재하지 않습니다."), "account"),
			Arguments.of(new UserNotFoundException("해당 사용자가 존재하지 않습니다."), "userId"),
			Arguments.of(new UserAccountUnMatchException("사용자와 계좌의 소유주가 다릅니다."), "account")
		);
	}

	@ParameterizedTest
	@MethodSource("invalidRequestProvider")
	@DisplayName("유효하지 않는 데이터를 요청으로 보내면 응답코드 400과 함께 실패이유를 응답 받아야한다.")
	void delete_unValid(DeleteAccountRequest invalidRequest, String fieldName, String reasons) throws Exception {
		//expect
		mockMvc.perform(delete("/account")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(invalidRequest)))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
			.andExpect(jsonPath("$.reasons." + fieldName).value(reasons));
	}

	public static Stream<Arguments> invalidRequestProvider() {
		return Stream.of(
			Arguments.of(new DeleteAccountRequest(-1L, "1111111111"), "userId", "아이디는 1 이상 이여야 합니다."),
			Arguments.of(new DeleteAccountRequest(1L, "123456789"), "accountNumber", "계좌번호는 10자리여야합니다."),
			Arguments.of(new DeleteAccountRequest(1L, "12345678901"), "accountNumber", "계좌번호는 10자리여야합니다.")
		);
	}

	@Test
	@DisplayName("user id 를 응답으로 보내면 해당 하는 계좌들이 응답코드 200과 함깨 응답되어야한다.")
	void find() throws Exception {
		//given
		ArrayList<AccountSearchResponse> responses = new ArrayList<>();
		responses.add(new AccountSearchResponse("1111111111", 10000L));
		responses.add(new AccountSearchResponse("1111111112", 20000L));
		responses.add(new AccountSearchResponse("1111111113", 30000L));
		given(accountService.findAccountByUserId(anyLong())).willReturn(responses);

		//expect
		mockMvc.perform(get("/account?user_id=1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("성공적으로 계좌가 조회되었습니다."))
			.andExpect(jsonPath("$.entity[0].accountNumber").value("1111111111"))
			.andExpect(jsonPath("$.entity[0].balance").value(10000L))

			.andExpect(jsonPath("$.entity[1].accountNumber").value("1111111112"))
			.andExpect(jsonPath("$.entity[1].balance").value(20000L))

			.andExpect(jsonPath("$.entity[2].accountNumber").value("1111111113"))
			.andExpect(jsonPath("$.entity[2].balance").value(30000L));

	}

	@Test
	@DisplayName("유효하지 않은 user Id 를 요청으로 보내면 응답코드 400과 함깨 실패이유를 응답 받아야한다.")
	void find_invalidRequest() throws Exception {
		//given
		given(accountService.findAccountByUserId(anyLong()))
			.willThrow(new UserNotFoundException("해당 사용자가 존재하지 않습니다."));
		//expect
		mockMvc.perform(get("/account?user_id=-1")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
			.andExpect(jsonPath("$.reasons.userId").value("해당 사용자가 존재하지 않습니다."));
		then(accountService).should(times(1)).findAccountByUserId(anyLong());
	}

	@Test
	@DisplayName(" 존재하지 않는 계좌번호의 User id 를 요청으로 보내면 응답코드 400과 함깨 실패이유를 응답 받아야한다.")
	void find_notFoundAccount() throws Exception {
		//given
		given(accountService.findAccountByUserId(anyLong()))
			.willThrow(new AccountNotFoundException("해당 계좌가 존재하지 않습니다."));
		//expect
		mockMvc.perform(get("/account?user_id=1")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
			.andExpect(jsonPath("$.reasons.account").value("해당 계좌가 존재하지 않습니다."));
		then(accountService).should(times(1)).findAccountByUserId(anyLong());
	}


}