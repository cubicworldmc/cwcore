package space.cubicworld.core;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface CorePlugin {

    Logger getLogger();

    InputStream readResource(String resource) throws IOException;

    Path getDataPath();

}
