package core.accountserver.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
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

import core.accountserver.dto.request.CreateAccountRequest;
import core.accountserver.dto.response.CreateAccountResponse;
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
}