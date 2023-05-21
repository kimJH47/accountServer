package core.accountserver.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import core.accountserver.dto.request.CreateAccountRequest;
import core.accountserver.dto.response.CreateAccountResponse;
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
		willReturn(new CreateAccountResponse(1L, "1100100111", now))
			.given(accountService)
			.createAccount(anyLong(), anyLong());

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

}