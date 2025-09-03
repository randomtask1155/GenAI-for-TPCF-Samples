package org.cloudfoundry.samples.music.web;

import java.util.*;

import org.cloudfoundry.samples.music.config.ai.MessageRetriever;
import org.cloudfoundry.samples.music.domain.Album;
import org.cloudfoundry.samples.music.domain.MessageRequest;
import org.cloudfoundry.samples.music.domain.RandomIdGenerator;
import org.cloudfoundry.samples.music.domain.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;


@RestController
@Profile("llm")
public class AIController {
    private static final Logger logger = LoggerFactory.getLogger(AIController.class);
    private MessageRetriever messageRetriever;
    private VectorStore vectorStore;
    private RandomIdGenerator idGenerator;

    public static String generateVectorDoc(Album album) {
            return "artist: " + album.getArtist() + "\n" +
            "title: " + album.getTitle() + "\n" +
            "releaseYear: " + album.getReleaseYear() + "\n" +
            "genre: " + album.getGenre() + "\n" +
            "userReview: " + album.getUserReview() + "\n" +
            "userScore: " + album.getUserScore() + "\n";
    }

    @Autowired
    public AIController(VectorStore vectorStore, MessageRetriever messageRetriever) {
        this.messageRetriever = messageRetriever;
        this.vectorStore = vectorStore;
        this.idGenerator = new RandomIdGenerator();
    }
    
    @RequestMapping(value = "/ai/rag", method = RequestMethod.POST)
    public Map<String,Object> generate(@RequestBody MessageRequest messageRequest) {
        Message[] messages = messageRequest.getMessages();

        for (Message message : messages) {
            logger.info("Getting Messages " + message.getText());
        }

        String query = messages[messages.length - 1].getText();
        String result = messageRetriever.retrieve(query);

        return Map.of("text",result);
    }

    @RequestMapping(value = "/ai/addDoc", method = RequestMethod.POST)
    public String addDoc(@RequestBody Album album) {

        if (album.getId() == null) {
            album.setId(idGenerator.generateId());
        }

        String text = generateVectorDoc(album);
        Document doc = new Document(album.getId(), text, new HashMap<>());
        logger.info("Adding Album " + doc.toString());
        this.vectorStore.add(List.of(doc));
        return text;
    }

    @RequestMapping(value = "/ai/deleteDoc", method = RequestMethod.POST)
    public String deleteDoc(@RequestBody String id) {
        logger.info("Deleting Album " + id);
        this.vectorStore.delete(List.of(id));
        return id;
    }
}
