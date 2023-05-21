package core.accountserver.generator;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.springframework.stereotype.Component;

@Component
public class HashAccountNumberGenerator implements AccountNumberGenerator {

	@Override
	public String generator(Long userId) {
		Random random = new Random();
		int randomNumber = random.nextInt(100000);
		String key = userId + String.valueOf(randomNumber);
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(key.getBytes(StandardCharsets.UTF_8));
			BigInteger hashInteger = new BigInteger(1, hashBytes);
			BigInteger modulus = BigInteger.valueOf(10).pow(10);
			return String.format("%010d",  hashInteger.mod(modulus));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("계좌번호 생성 중 문제가 발생 하였습니다.");
		}
	}
}
