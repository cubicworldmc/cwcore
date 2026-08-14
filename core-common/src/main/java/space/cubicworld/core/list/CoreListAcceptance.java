package space.cubicworld.core.list;

import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import space.cubicworld.core.CorePlugin;
import space.cubicworld.core.message.CoreMessage;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public interface CoreListAcceptance {

    class None implements CoreListAcceptance {
        @Override
        public Component acceptanceInstructionsMessage(String list, String playerName) {
            return Component.empty();
        }
    }

    class LinkCode implements CoreListAcceptance {

        @SneakyThrows
        private static Cipher getCipher() {
            return Cipher.getInstance("AES");
        }

        private final String url;
        private final SecretKey key;

        public LinkCode(CorePlugin plugin, String list) {
            try {
                getCipher();
                url = plugin.getConfig().get("lists.%s.acceptance.url".formatted(list));
                key = plugin.getBootstrap().loadKey(plugin.getConfig().get("lists.%s.acceptance.key-file".formatted(list)), "AES");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        @SneakyThrows
        public Component acceptanceInstructionsMessage(String list, String playerName) {
            Cipher cipher = getCipher();
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] code = cipher.doFinal(playerName.getBytes(StandardCharsets.UTF_8));
            return CoreMessage.listLinkCodeAcceptanceInstructions(
                    list,
                    url,
                    Base64.getEncoder().encodeToString(code)
            );
        }
    }

    Component acceptanceInstructionsMessage(String list, String playerName);

}
