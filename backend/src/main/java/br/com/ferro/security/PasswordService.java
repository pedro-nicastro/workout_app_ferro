package br.com.ferro.security;

import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class PasswordService {

    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    public String hash(String password) {
        try {
            byte[] salt = new byte[SALT_LENGTH];
            new SecureRandom().nextBytes(salt);

            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();

            return ITERATIONS + ":" +
                    Base64.getEncoder().encodeToString(salt) + ":" +
                    Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Could not protect the password.", e);
        }
    }

    public boolean matches(String password, String armazenada) {
        try {
            String[] partes = armazenada.split(":");
            if (partes.length != 3) return false;

            int iterations = Integer.parseInt(partes[0]);
            byte[] salt = Base64.getDecoder().decode(partes[1]);
            byte[] esperado = Base64.getDecoder().decode(partes[2]);

            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, esperado.length * 8);
            byte[] currentUser = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();

            if (currentUser.length != esperado.length) return false;

            int diff = 0;
            for (int i = 0; i < currentUser.length; i++) {
                diff |= currentUser[i] ^ esperado[i];
            }
            return diff == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
