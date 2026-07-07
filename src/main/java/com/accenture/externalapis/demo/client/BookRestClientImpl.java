package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

@Component
public class BookRestClientImpl implements BookRestClient {

    private final RestClient restClient;

    public BookRestClientImpl(RestClient.Builder builder, ExternalServiceProperties properties) {
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }
    @Override
    public BookDto getBook(Long id) {
        try {
            BookApiResponse response = restClient.get()
                    .uri("/books/{id}", id)
                    .retrieve()
                    .body(BookApiResponse.class);
            if (response == null) {
                throw new ClientException("Received null response from external service");
            }
            return mapToDto(response);

        } catch (HttpClientErrorException e) {
            throw new ClientException("Book with id '" + id + "' not found", e);
        } catch (HttpServerErrorException e) {
            throw new ClientException("External service error", e);
        } catch (ResourceAccessException e) {
            throw new ClientException("External service unavailable", e);
        }
    }

    @Override
    public List<BookDto> getAllBooks() {
        try {
            BookApiResponse[] response = restClient.get()
                    .uri("/books")
                    .retrieve()
                    .body(BookApiResponse[].class);

            if (response == null) {
                throw new ClientException("Received empty response from external service");
            }

            return Arrays.stream(response)
                    .map(this::mapToDto)
                    .toList();

        } catch (HttpClientErrorException e) {
            throw new ClientException("Client error while fetching books", e);
        } catch (HttpServerErrorException e) {
            throw new ClientException("External service error", e);
        } catch (ResourceAccessException e) {
            throw new ClientException("External service unavailable", e);
        }
    }

    private BookDto mapToDto(BookApiResponse response){
        return new BookDto(
                response.title(),
                response.author(),
                response.genre(),
                response.price());
    }
}
