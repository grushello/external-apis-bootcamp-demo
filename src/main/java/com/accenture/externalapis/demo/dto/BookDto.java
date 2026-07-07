package com.accenture.externalapis.demo.dto;

public record BookDto(String title,
                       String author,
                       String genre,
                       double price) {
}