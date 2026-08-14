package space.cubicworld.core;

import com.electronwill.nightconfig.core.file.FileConfig;
import lombok.Data;
import lombok.SneakyThrows;
import org.slf4j.Logger;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Base64;

@Data
public class CoreBootstrap {

    private final ClassLoader classLoader;
    private final CoreResolver resolver;
    private final FileConfig config;
    private final Logger logger;
    private final Path directory;

    public SecretKey loadKey(String key, String algorithm) throws Exception {
        byte[] bytes;
        try (InputStream is = new FileInputStream(directory.resolve(key).toFile())) {
            bytes = is.readAllBytes();
        }
        String str = new String(bytes).strip();
        bytes = Base64.getDecoder().decode(str);
        return new SecretKeySpec(bytes, algorithm);
    }

}
