package com.email.writer.app.service;

import com.email.writer.app.dto.EmailRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailGeneratorService {

    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;
    @Value("${gemini.api.key}")
    private String geminiApiKey;



// method to generate email reply for controller class method
    public String generateEmailReply(EmailRequest emailRequest){
        // Step 1
        // Build the prompt
         String prompt = buildPrompt(emailRequest);
        // Step 2
        // Craft a request because it should be in a specific format
//        {"contents": [{
//                "parts": [{
//                    "text": "Explain how AI works in 100 words"
//                }]
//            }]
//        }
//         this is the format in which request needs to be sent, it basically contains 3 nested maps(key value pair)

        Map<String,Object> requestBody = Map.of(
                "contents",new Object[]{  // first map key is content and its value is and Array Object
                        Map.of("parts",new Object[]{
                                Map.of("text", prompt)
                        })
                }
        );

        // Step 3
        // Do request and get response
//      here for making an api call we make use of web client, way to do a api request, build on top of project reactor, and enable us to handle asynchronous non-blocking http request and responses
//        it makes this thing well suited for modern reactive web application

        String response = webClient.post() // sending data (the email prompt) using POST
                .uri(geminiApiUrl+geminiApiKey)  // This sets the API endpoint URL you’re calling.
                .header("Content-Type","application/json") // This means: I’m sending data in JSON format.
                .bodyValue(requestBody)
                .retrieve() //  It actually sends the request to the Gemini API, Gemini responds, .retrieve() prepares to receive that response
                .bodyToMono(String.class) //  ake the response body and convert it into a String wrapped in a Mono, we have two Mono for single reply and Flux for stream of reply
                // Mono<String> = a “promise” that one String value will arrive in the future, It’s part of the reactive programming idea — the response isn’t here yet, but it will come.
                .block(); // It blocks the current thread until the Gemini API sends the reply, synchronous

        // Step 4
        // Extract response from gemini api format and Return it
        return extractResponseContent(response);
    }

    private String extractResponseContent(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper(); // tool from jackson library which helps to work with json data
            // it can read write and convert json data to java object and java object to json data
            JsonNode rootNode = mapper.readTree(response); // readTree method convert json response into tree like structure
            return rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
            // navigating tree to our text which is response form api
        } catch (Exception e) {
            return "Error processing request: " + e.getMessage();
        }

    }

    // Prompt for gemini APi is crafted in this method
    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a email reply for the following email content. Please don't generate a subject line.Only generate reply nothing else ");
        if(emailRequest.getTone()!=null  && !emailRequest.getTone().isEmpty()){
            prompt.append("use an ").append(emailRequest.getTone()).append(" tone.");
        }
        prompt.append("\nOriginal email: \n").append(emailRequest.getEmailContent());
        return prompt.toString();
    }


}






