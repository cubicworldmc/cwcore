package space.cubicworld.core;

import com.electronwill.nightconfig.core.file.FileConfig;
import lombok.Data;
import org.slf4j.Logger;

import java.nio.file.Path;

@Data
public class CoreBootstrap {

    private final ClassLoader classLoader;
    private final CoreResolver resolver;
    private final FileConfig config;
    private final Logger logger;
    private final Path directory;

}
