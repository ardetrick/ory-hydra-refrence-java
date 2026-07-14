package com.ardetrick.oryhydrareference.demo;

/** The Java object representing the data submitted by the exchange form in callback.ftlh. */
public record ExchangeForm(String code, String redirectUri, String clientId, String clientSecret) {}
