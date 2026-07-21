package space.cubicworld.core.http;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerResponse;
import space.cubicworld.core.CorePlugin;

import javax.net.ssl.SSLException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public class CoreHttpServer {

    private HttpServer server;

    @Getter
    @Setter(AccessLevel.PRIVATE)
    private DisposableServer disposableServer;

    public CoreHttpServer(CorePlugin plugin) {
        Function<String, File> loadFile = name -> plugin.getDirectory().resolve(plugin.getConfig().get("http." + name).toString()).toFile();

        File key = loadFile.apply("key-file");
        File cert = loadFile.apply("cert-file");
        File client = loadFile.apply("client-cert-file");

        SslContext sslContext;
        try {
            sslContext = SslContextBuilder
                    .forServer(cert, key)
                    .trustManager(client)
                    .build();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }

        server = HttpServer.create()
                .port(plugin.getConfig().get("http.port"))
                .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));
    }

    public void with(Function<HttpServer, HttpServer> func) {
        server = func.apply(server);
    }

    public void bind() {
        server.bind().subscribe(this::setDisposableServer);
    }

    public void close() {
        disposableServer.dispose();
    }

    public static Mono<Void> badRequest(HttpServerResponse response, String message) {
        return response
                .status(HttpResponseStatus.BAD_REQUEST)
                .sendByteArray(Mono.just(message.getBytes(StandardCharsets.UTF_8)))
                .then();
    }

}
