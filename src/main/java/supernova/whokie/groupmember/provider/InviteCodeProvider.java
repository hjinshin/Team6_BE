package supernova.whokie.groupmember.provider;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import supernova.whokie.global.exception.InviteCodeException;

@Component
public class InviteCodeProvider {

    @Value("${url.secret-key}")
    private String urlSecretKey; // 16-byte key for AES

    private InviteCodeProvider() {
        // 인스턴스화 방지
    }

    public String createCode(Long groupId, LocalDateTime startDateTime,
        LocalDateTime endDateTime) {
        String data = groupId + "|" + startDateTime + "|" + endDateTime;
        try {
            return encrypt(data);
        } catch (Exception e) {
            throw new InviteCodeException("URL 제작에 실패했습니다.");
        }
    }

    private String encrypt(String data) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(urlSecretKey.getBytes(StandardCharsets.UTF_8),
            "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().encodeToString(encryptedBytes); // URL-safe encoding
    }

    private String decrypt(String encryptedData) {
        try {
            return decryptData(encryptedData);
        } catch (Exception e) {
            e.printStackTrace();
            throw new InviteCodeException("코드가 옳바르지 않습니다.");
        }
    }

    private String decryptData(String encryptedData) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(urlSecretKey.getBytes(StandardCharsets.UTF_8),
            "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        byte[] decodedBytes = Base64.getUrlDecoder().decode(encryptedData);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    public CodeData parseCodeData(String encryptedUrl) {
        try {
            String decryptedData = decrypt(encryptedUrl);
            String[] parts = decryptedData.split("\\|");
            Long groupId = Long.parseLong(parts[0]);
            LocalDateTime startDateTime = LocalDateTime.parse(parts[1]);
            LocalDateTime endDateTime = LocalDateTime.parse(parts[2]);

            // 만료 시간 검사
            if (endDateTime.isBefore(LocalDateTime.now())) {
                throw new InviteCodeException("코드가 만료되었습니다.");
            }

            return CodeData.builder()
                .groupId(groupId)
                .startDateTime(startDateTime)
                .endDateTime(endDateTime)
                .build();
        } catch (Exception e) {
            e.printStackTrace();
            throw new InviteCodeException("코드가 옳바르지 않습니다.");
        }
    }


}
