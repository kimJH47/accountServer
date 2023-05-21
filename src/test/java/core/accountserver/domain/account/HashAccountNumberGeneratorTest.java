package core.accountserver.domain.account;

import java.util.ArrayList;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import core.accountserver.generator.HashAccountNumberGenerator;

class HashAccountNumberGeneratorTest {


	@Test
	@DisplayName("계좌번호 생성테스트")
	void generate() throws Exception {
	    //given
		HashAccountNumberGenerator hashAccountNumberGenerator = new HashAccountNumberGenerator();
		//when
	    //then
		ArrayList<String> list = new ArrayList<>();
		for (long i = 0; i < 100; i++) {
			String generator = hashAccountNumberGenerator.generator(i);
			System.out.println(generator);
			list.add(generator);
		}
		long count = list.stream().distinct().count();
		System.out.println(count);
	}

}