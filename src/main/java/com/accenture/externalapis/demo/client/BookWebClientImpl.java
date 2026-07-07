package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

// TODO: Make this class implement BookWebClient.
@Component
public class BookWebClientImpl implements BookWebClient{

    private final WebClient webClient;

    public BookWebClientImpl(WebClient.Builder builder, ExternalServiceProperties properties) {
        this.webClient = builder
                .baseUrl(properties.baseUrl())
                .build();
    }
    @Override
    public Mono<BookDto> getBookAsync(Long id) {
        return webClient.get()
                .uri("/books/{id}", id)
                .retrieve()
                .bodyToMono(BookApiResponse.class)
                .map(this::mapToDto)
                .switchIfEmpty(Mono.error(
                        new ClientException("Received null response from external service")))
                .onErrorMap(WebClientResponseException.NotFound.class,
                        e -> new ClientException("Book with id '" + id + "' not found", e))
                .onErrorMap(WebClientResponseException.class,
                        e -> new ClientException("External service error", e))
                .onErrorMap(WebClientRequestException.class,
                        e -> new ClientException("External service unavailable", e));
    }
    @Override
    public Flux<BookDto> getAllBooksAsync() {
        return webClient.get()
                .uri("/books")
                .retrieve()
                .bodyToFlux(BookApiResponse.class)
                .map(this::mapToDto)
                .switchIfEmpty(Flux.error(
                        new ClientException("Received null response from external service")))
                .onErrorMap(WebClientResponseException.class,
                        e -> new ClientException("External service error", e))
                .onErrorMap(WebClientRequestException.class,
                        e -> new ClientException("External service unavailable", e));
    }

    @Override
    public Mono<List<BookDto>> getBooksInParallel(Long id1, Long id2) {

        Mono<BookDto> book1 = getBookAsync(id1);
        Mono<BookDto> book2 = getBookAsync(id2);

        return Mono.zip(book1, book2)
                .map(tuple -> List.of(tuple.getT1(), tuple.getT2()));
    }

    private BookDto mapToDto(BookApiResponse response){
        return new BookDto(
                response.title(),
                response.author(),
                response.genre(),
                response.price());
    }
}
